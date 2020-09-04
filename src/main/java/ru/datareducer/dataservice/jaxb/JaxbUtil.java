/*
 * Copyright (c) 2017-2020 Kirill Mikhaylov <admin@datareducer.ru>
 *
 * Этот файл — часть программы DataReducer Console <http://datareducer.ru>.
 *
 * Программа DataReducer Console является свободным программным обеспечением.
 * Вы вправе распространять ее и/или модифицировать только в соответствии с условиями
 * версии 2 Стандартной Общественной Лицензии GNU, опубликованной Free Software Foundation.
 *
 * Программа DataReducer Console распространяется в надежде, что она будет полезной,
 * но БЕЗО ВСЯКИХ ГАРАНТИЙ, в том числе ГАРАНТИИ ТОВАРНОГО СОСТОЯНИЯ ПРИ ПРОДАЖЕ
 * и ПРИГОДНОСТИ ДЛЯ ИСПОЛЬЗОВАНИЯ В КОНКРЕТНЫХ ЦЕЛЯХ.
 * Подробнее см. в Стандартной Общественной Лицензии GNU.
 *
 * Вы должны были получить копию Стандартной Общественной Лицензии GNU
 * вместе с этой программой. Если это не так, см. <https://www.gnu.org/licenses/>.
 */
package ru.datareducer.dataservice.jaxb;

import org.w3c.dom.Node;
import ru.datareducer.dataservice.entity.*;
import ru.datareducer.dataservice.jaxb.atom.Content;
import ru.datareducer.dataservice.jaxb.atom.Entry;
import ru.datareducer.dataservice.jaxb.atom.Feed;
import ru.datareducer.dataservice.jaxb.csdl.*;
import ru.datareducer.dataservice.jaxb.register.Element;
import ru.datareducer.dataservice.jaxb.register.Result;

import javax.xml.bind.JAXBElement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Функции разбора демаршализованных объектов.
 *
 * @author Kirill Mikhaylov
 */
public class JaxbUtil {

    // Шаблон имени виртуальной таблицы перерасчета регистра расчета
    private static final Pattern recalculationPattern = Pattern.compile("^CalculationRegister_([^_]+)_(?!Base|RowType|ScheduleData|ActualActionPeriod)([^_]+)$");
    // Шаблон имени виртуальной таблицы базовых данных регистра расчета
    private static final Pattern baseCalculationRegisterPattern = Pattern.compile("^CalculationRegister_([^_]+)_Base([^_]+)$");

    private JaxbUtil() {
    }

    /**
     * Разбирает описание метаданных 1С.
     *
     * @param edmx Описание метаданных 1С
     * @return Дерево конфигурации 1С
     */
    public static MetadataTree parseEdmx(EdmxType edmx) {
        MetadataTree metadataTree = new MetadataTree();
        // Табличные части всех объектов <Имя ресурса, Набор реквизитов>
        Map<String, LinkedHashSet<Field>> tabularSections = new HashMap<>();
        // Все связи.
        Set<AssociationWrapper> associations = new HashSet<>();

        for (Object obj : edmx.getDataServices().getSchema().getEntityTypeOrComplexTypeOrAssociation()) {
            if (obj instanceof EntityTypeType) { // Сущность (объект конфигурации)
                EntityTypeType entityType = (EntityTypeType) obj;
                Set<String> tabularSectionNames = new HashSet<>(); // Табличные части объекта конфигурации
                // Поля ресурса
                LinkedHashSet<Field> fields = new LinkedHashSet<>();
                LinkedHashMap<String, Field> fieldsLookup = new LinkedHashMap<>();

                int order = 1;
                for (PropertyType prop : entityType.getProperty()) {
                    String propName = prop.getName();
                    String propType = prop.getType();

                    if (propName.equals("ValueType")) {
                        // Пропускаем (Свойство "Тип значения характеристик" плана видов характеристик, имеет составной тип OData)
                        continue;
                    }
                    if (propType.startsWith("Collection(")) {
                        tabularSectionNames.add(propName);
                    } else {
                        fields.add(new Field(propName, FieldType.getByEdmType(propType), order++));
                        fieldsLookup.put(propName, new Field(propName, FieldType.getByEdmType(propType), order++));
                    }
                }

                String resourceName = entityType.getName(); // Имя ресурса
                if (isTabularSection(entityType.getKey().getPropertyRef())) {
                    tabularSections.put(resourceName, fields);
                } else {
                    DataServiceEntity entity = null;

                    if (resourceName.startsWith(Constant.RESOURCE_PREFIX)) {
                        entity = new Constant(resourceName.substring(Constant.RESOURCE_PREFIX.length()), fields);
                    } else if (resourceName.startsWith(Catalog.RESOURCE_PREFIX)) {
                        entity = new Catalog(resourceName.substring(Catalog.RESOURCE_PREFIX.length()), fields);
                        for (String name : tabularSectionNames) {
                            entity.getTabularSections().add(new TabularSection(entity, name));
                        }
                    } else if (resourceName.startsWith(Document.RESOURCE_PREFIX)) {
                        entity = new Document(resourceName.substring(Document.RESOURCE_PREFIX.length()), fields);
                        for (String name : tabularSectionNames) {
                            entity.getTabularSections().add(new TabularSection(entity, name));
                        }
                    } else if (resourceName.startsWith(DocumentJournal.RESOURCE_PREFIX)) {
                        entity = new DocumentJournal(resourceName.substring(DocumentJournal.RESOURCE_PREFIX.length()), fields);

                    } else if (resourceName.startsWith(ChartOfCharacteristicTypes.RESOURCE_PREFIX)) {
                        entity = new ChartOfCharacteristicTypes(
                                resourceName.substring(ChartOfCharacteristicTypes.RESOURCE_PREFIX.length()), fields);
                        for (String name : tabularSectionNames) {
                            entity.getTabularSections().add(new TabularSection(entity, name));
                        }
                    } else if (resourceName.startsWith(ChartOfAccounts.RESOURCE_PREFIX)) {
                        entity = new ChartOfAccounts(
                                resourceName.substring(ChartOfAccounts.RESOURCE_PREFIX.length()), fields);
                        for (String name : tabularSectionNames) {
                            entity.getTabularSections().add(new TabularSection(entity, name));
                        }
                    } else if (resourceName.startsWith(ChartOfCalculationTypes.RESOURCE_PREFIX)) {
                        entity = new ChartOfCalculationTypes(
                                resourceName.substring(ChartOfCalculationTypes.RESOURCE_PREFIX.length()), fields);
                        for (String name : tabularSectionNames) {
                            entity.getTabularSections().add(new TabularSection(entity, name));
                        }
                    } else if (resourceName.startsWith(InformationRegister.RESOURCE_PREFIX)) {
                        if (fieldsLookup.containsKey("RecordSet")) {
                            continue;
                        }
                        String name;
                        if (resourceName.endsWith("_RecordType")) {
                            name = resourceName.substring(InformationRegister.RESOURCE_PREFIX.length(), resourceName.indexOf("_RecordType"));
                        } else {
                            name = resourceName.substring(InformationRegister.RESOURCE_PREFIX.length());
                        }
                        entity = new InformationRegister(name, fields);
                        // Таблицы среза последних/первых создаются при создании объекта Регистра сведений,
                        // а не при парсинге описания метаданных, т.к. в описании метаданных эти сущности не представлены.
                        if (((InformationRegister) entity).isPeriodic()) {
                            markCompositeFields(((InformationRegister) entity).getSliceLast());
                            markCompositeFields(((InformationRegister) entity).getSliceFirst());
                        }
                    } else if (resourceName.startsWith(AccumulationRegister.RESOURCE_PREFIX)) {
                        if (fieldsLookup.containsKey("RecordSet")) {
                            continue;
                        }
                        String name;
                        if (resourceName.endsWith("_RecordType")) {
                            name = resourceName.substring(AccumulationRegister.RESOURCE_PREFIX.length(), resourceName.indexOf("_RecordType"));
                        } else {
                            name = resourceName.substring(AccumulationRegister.RESOURCE_PREFIX.length());
                        }
                        entity = new AccumulationRegister(name, fields);
                    } else if (resourceName.startsWith(AccountingRegister.RESOURCE_PREFIX)) {
                        if (fieldsLookup.containsKey("RecordSet")) {
                            continue;
                        }
                        String name;
                        if (resourceName.endsWith("_RecordType")) {
                            name = resourceName.substring(AccountingRegister.RESOURCE_PREFIX.length(), resourceName.indexOf("_RecordType"));
                        } else {
                            name = resourceName.substring(AccountingRegister.RESOURCE_PREFIX.length());
                        }
                        entity = new AccountingRegister(name, fields);
                    } else if (resourceName.startsWith(CalculationRegister.RESOURCE_PREFIX)) {
                        if (fieldsLookup.containsKey("RecordSet")) {
                            continue;
                        }
                        String name;
                        if (resourceName.endsWith("_RecordType")) {
                            name = resourceName.substring(CalculationRegister.RESOURCE_PREFIX.length(), resourceName.indexOf("_RecordType"));
                        } else {
                            name = resourceName.substring(CalculationRegister.RESOURCE_PREFIX.length());
                        }
                        entity = new CalculationRegister(name, fields);
                    } else if (resourceName.startsWith(ExchangePlan.RESOURCE_PREFIX)) {
                        entity = new ExchangePlan(resourceName.substring(ExchangePlan.RESOURCE_PREFIX.length()), fields);
                        for (String name : tabularSectionNames) {
                            entity.getTabularSections().add(new TabularSection(entity, name));
                        }
                    } else if (resourceName.startsWith(BusinessProcess.RESOURCE_PREFIX)) {
                        entity = new BusinessProcess(resourceName.substring(BusinessProcess.RESOURCE_PREFIX.length()), fields);
                        for (String name : tabularSectionNames) {
                            entity.getTabularSections().add(new TabularSection(entity, name));
                        }
                    } else if (resourceName.startsWith(Task.RESOURCE_PREFIX)) {
                        entity = new Task(resourceName.substring(Task.RESOURCE_PREFIX.length()), fields);
                        for (String name : tabularSectionNames) {
                            entity.getTabularSections().add(new TabularSection(entity, name));
                        }
                    }

                    if (entity != null) {
                        markCompositeFields(entity);
                        metadataTree.addEntity(entity);
                    }
                }
            } else if (obj instanceof ComplexTypeType) { // Виртуальные таблицы
                ComplexTypeType complexType = (ComplexTypeType) obj;
                String name = complexType.getName();
                if (name.startsWith(AccumulationRegister.RESOURCE_PREFIX) && name.endsWith("_Balance")) { // Таблица остатков регистра накопления
                    String registerName = name.substring(AccumulationRegister.RESOURCE_PREFIX.length(), name.indexOf("_Balance"));
                    LinkedHashSet<Field> dimensions = new LinkedHashSet<>();
                    LinkedHashSet<Field> resources = new LinkedHashSet<>();
                    int order = 1;
                    for (PropertyType prop : complexType.getProperty()) {
                        String n = prop.getName();
                        String propType = prop.getType();
                        Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                        if (n.endsWith("Balance")) {
                            resources.add(f);
                        } else {
                            dimensions.add(f);
                        }
                    }
                    AccumulationRegisterBalance entity = new AccumulationRegisterBalance(registerName, dimensions, resources);
                    markCompositeFields(entity);
                    metadataTree.getAccumulationRegisterByName(registerName).setBalance(entity);
                } else if (name.startsWith(AccumulationRegister.RESOURCE_PREFIX) && name.endsWith("_Turnover")) { // Таблица оборотов регистра накопления
                    String registerName = name.substring(AccumulationRegister.RESOURCE_PREFIX.length(), name.indexOf("_Turnover"));
                    LinkedHashSet<Field> dimensions = new LinkedHashSet<>();
                    LinkedHashSet<Field> resources = new LinkedHashSet<>();
                    int order = 1;
                    for (PropertyType prop : complexType.getProperty()) {
                        String n = prop.getName();
                        String propType = prop.getType();
                        Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                        if (n.endsWith("Turnover") || n.endsWith("Receipt") || n.endsWith("Expense")) {
                            resources.add(f);
                        } else if (!n.endsWith("Period") && !n.equals("Recorder") && !n.equals("Recorder_Type") && !n.equals("LineNumber")) {
                            // Пропускаем поля, относящиеся к Периодичности (параметр Периодичность не поддерживается REST-сервисом 1С)
                            dimensions.add(f);
                        }
                    }
                    AccumulationRegisterTurnovers entity = new AccumulationRegisterTurnovers(registerName, dimensions, resources);
                    markCompositeFields(entity);
                    metadataTree.getAccumulationRegisterByName(registerName).setTurnovers(entity);
                } else if (name.startsWith(AccumulationRegister.RESOURCE_PREFIX) && name.endsWith("_BalanceAndTurnover")) { // Таблица остатков и оборотов регистра накопления
                    String registerName = name.substring(AccumulationRegister.RESOURCE_PREFIX.length(), name.indexOf("_BalanceAndTurnover"));
                    LinkedHashSet<Field> dimensions = new LinkedHashSet<>();
                    LinkedHashSet<Field> resources = new LinkedHashSet<>();
                    int order = 1;
                    for (PropertyType prop : complexType.getProperty()) {
                        String n = prop.getName();
                        String propType = prop.getType();
                        Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                        if (n.endsWith("Turnover") || n.endsWith("Receipt") || n.endsWith("Expense") || n.endsWith("OpeningBalance") || n.endsWith("ClosingBalance")) {
                            resources.add(f);
                        } else if (!n.endsWith("Period") && !n.equals("Recorder") && !n.equals("Recorder_Type") && !n.equals("LineNumber")) {
                            // Пропускаем поля, относящиеся к Периодичности (параметр Периодичность не поддерживается REST-сервисом 1С)
                            dimensions.add(f);
                        }
                    }
                    AccumulationRegisterBalanceAndTurnovers entity = new AccumulationRegisterBalanceAndTurnovers(registerName, dimensions, resources);
                    markCompositeFields(entity);
                    metadataTree.getAccumulationRegisterByName(registerName).setBalanceAndTurnovers(entity);

                } else if (name.startsWith(AccountingRegister.RESOURCE_PREFIX) && name.endsWith("_Turnover")) { // Таблица оборотов регистра бухгалтерии
                    String registerName = name.substring(AccountingRegister.RESOURCE_PREFIX.length(), name.indexOf("_Turnover"));
                    LinkedHashSet<Field> dimensions = new LinkedHashSet<>();
                    LinkedHashSet<Field> resources = new LinkedHashSet<>();
                    int order = 1;
                    for (PropertyType prop : complexType.getProperty()) {
                        String n = prop.getName();
                        String propType = prop.getType();
                        Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                        if (n.endsWith("Turnover") || n.endsWith("TurnoverDr") || n.endsWith("TurnoverCr")) {
                            resources.add(f);
                        } else if (!n.endsWith("Period") && !n.equals("Recorder") && !n.equals("Recorder_Type") && !n.equals("LineNumber")) {
                            // Пропускаем поля, относящиеся к Периодичности (параметр Периодичность не поддерживается REST-сервисом 1С)
                            dimensions.add(f);
                        }
                    }
                    AccountingRegisterTurnovers entity = new AccountingRegisterTurnovers(registerName, dimensions, resources);
                    markCompositeFields(entity);
                    metadataTree.getAccountingRegisterByName(registerName).setTurnovers(entity);
                } else if (name.startsWith(AccountingRegister.RESOURCE_PREFIX) && name.endsWith("_Balance")) { // Таблица остатков регистра бухгалтерии
                    String registerName = name.substring(AccountingRegister.RESOURCE_PREFIX.length(), name.indexOf("_Balance"));
                    LinkedHashSet<Field> dimensions = new LinkedHashSet<>();
                    LinkedHashSet<Field> resources = new LinkedHashSet<>();
                    int order = 1;
                    for (PropertyType prop : complexType.getProperty()) {
                        String n = prop.getName();
                        String propType = prop.getType();
                        Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                        if (n.endsWith("Balance") || n.endsWith("BalanceDr") || n.endsWith("BalanceCr")) {
                            resources.add(f);
                        } else {
                            dimensions.add(f);
                        }
                    }
                    AccountingRegisterBalance entity = new AccountingRegisterBalance(registerName, dimensions, resources);
                    markCompositeFields(entity);
                    metadataTree.getAccountingRegisterByName(registerName).setBalance(entity);
                } else if (name.startsWith(AccountingRegister.RESOURCE_PREFIX) && name.endsWith("_BalanceAndTurnover")) { // Таблица остатков и оборотов регистра бухгалтерии
                    String registerName = name.substring(AccountingRegister.RESOURCE_PREFIX.length(), name.indexOf("_BalanceAndTurnover"));
                    LinkedHashSet<Field> dimensions = new LinkedHashSet<>();
                    LinkedHashSet<Field> resources = new LinkedHashSet<>();
                    int order = 1;
                    for (PropertyType prop : complexType.getProperty()) {
                        String n = prop.getName();
                        String propType = prop.getType();
                        Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                        if (n.endsWith("Balance") || n.endsWith("BalanceDr") || n.endsWith("BalanceCr")
                                || n.endsWith("Turnover") || n.endsWith("TurnoverDr") || n.endsWith("TurnoverCr")) {
                            resources.add(f);
                        } else if (!n.endsWith("Period") && !n.equals("Recorder") && !n.equals("Recorder_Type") && !n.equals("LineNumber")) {
                            // Пропускаем поля, относящиеся к Периодичности (параметр Периодичность не поддерживается REST-сервисом 1С)
                            dimensions.add(f);
                        }
                    }
                    AccountingRegisterBalanceAndTurnovers entity = new AccountingRegisterBalanceAndTurnovers(registerName, dimensions, resources);
                    markCompositeFields(entity);
                    metadataTree.getAccountingRegisterByName(registerName).setBalanceAndTurnovers(entity);
                } else if (name.startsWith(AccountingRegister.RESOURCE_PREFIX) && name.endsWith("_ExtDimensions")) { // Таблица субконто регистра бухгалтерии
                    String registerName = name.substring(AccountingRegister.RESOURCE_PREFIX.length(), name.indexOf("_ExtDimensions"));
                    LinkedHashSet<Field> fields = new LinkedHashSet<>();
                    int order = 1;
                    for (PropertyType prop : complexType.getProperty()) {
                        String n = prop.getName();
                        if (n.equals("PointInTime")) { // пропускаем (имеет составной тип OData)
                            continue;
                        }
                        String propType = prop.getType();
                        Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                        fields.add(f);
                    }
                    AccountingRegisterExtDimensions entity = new AccountingRegisterExtDimensions(registerName, fields);
                    markCompositeFields(entity);
                    metadataTree.getAccountingRegisterByName(registerName).setExtDimensions(entity);
                } else if (name.startsWith(AccountingRegister.RESOURCE_PREFIX) && name.endsWith("_RecordsWithExtDimensions")) { // Таблица движений с субконто регистра бухгалтерии
                    String registerName = name.substring(AccountingRegister.RESOURCE_PREFIX.length(), name.indexOf("_RecordsWithExtDimensions"));
                    LinkedHashSet<Field> fields = new LinkedHashSet<>();
                    int order = 1;
                    for (PropertyType prop : complexType.getProperty()) {
                        String n = prop.getName();
                        if (n.equals("PointInTime")) { // пропускаем (имеет составной тип OData)
                            continue;
                        }
                        String propType = prop.getType();
                        Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                        fields.add(f);
                    }
                    AccountingRegisterRecordsWithExtDimensions entity = new AccountingRegisterRecordsWithExtDimensions(registerName, fields);
                    markCompositeFields(entity);
                    metadataTree.getAccountingRegisterByName(registerName).setRecordsWithExtDimensions(entity);
                } else if (name.startsWith(AccountingRegister.RESOURCE_PREFIX) && name.endsWith("_DrCrTurnover")) { // Таблица оборотов ДтКт регистра бухгалтерии
                    String registerName = name.substring(AccountingRegister.RESOURCE_PREFIX.length(), name.indexOf("_DrCrTurnover"));
                    LinkedHashSet<Field> dimensions = new LinkedHashSet<>();
                    LinkedHashSet<Field> resources = new LinkedHashSet<>();
                    int order = 1;
                    for (PropertyType prop : complexType.getProperty()) {
                        String n = prop.getName();
                        String propType = prop.getType();
                        Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                        if (n.endsWith("Turnover") || n.endsWith("TurnoverDr") || n.endsWith("TurnoverCr")) {
                            resources.add(f);
                        } else if (!n.endsWith("Period") && !n.equals("Recorder") && !n.equals("Recorder_Type") && !n.equals("LineNumber")) {
                            // Пропускаем поля, относящиеся к Периодичности (параметр Периодичность не поддерживается REST-сервисом 1С)
                            dimensions.add(f);
                        }
                    }
                    AccountingRegisterDrCrTurnovers entity = new AccountingRegisterDrCrTurnovers(registerName, dimensions, resources);
                    markCompositeFields(entity);
                    metadataTree.getAccountingRegisterByName(registerName).setDrCrTurnovers(entity);
                } else if (name.startsWith(CalculationRegister.RESOURCE_PREFIX) && name.endsWith("_ScheduleData")) { // Таблица данных графика регистра расчета
                    String registerName = name.substring(CalculationRegister.RESOURCE_PREFIX.length(), name.indexOf("_ScheduleData"));
                    LinkedHashSet<Field> fields = new LinkedHashSet<>();
                    int order = 1;
                    for (PropertyType prop : complexType.getProperty()) {
                        String n = prop.getName();
                        String propType = prop.getType();
                        Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                        fields.add(f);
                    }
                    CalculationRegisterScheduleData entity = new CalculationRegisterScheduleData(registerName, fields);
                    markCompositeFields(entity);
                    metadataTree.getCalculationRegisterByName(registerName).setScheduleData(entity);
                } else if (name.startsWith(CalculationRegister.RESOURCE_PREFIX) && name.endsWith("_ActualActionPeriod")) { // Таблица фактического периода действия регистра расчета
                    String registerName = name.substring(CalculationRegister.RESOURCE_PREFIX.length(), name.indexOf("_ActualActionPeriod"));
                    LinkedHashSet<Field> fields = new LinkedHashSet<>();
                    int order = 1;
                    for (PropertyType prop : complexType.getProperty()) {
                        String n = prop.getName();
                        String propType = prop.getType();
                        Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                        fields.add(f);
                    }
                    CalculationRegisterActualActionPeriod entity = new CalculationRegisterActualActionPeriod(registerName, fields);
                    markCompositeFields(entity);
                    metadataTree.getCalculationRegisterByName(registerName).setActualActionPeriod(entity);
                } else if (name.startsWith(CalculationRegister.RESOURCE_PREFIX)) {
                    Matcher recalculationPatternMatcher = recalculationPattern.matcher(name);
                    Matcher baseCalculationRegisterPatternMatcher = baseCalculationRegisterPattern.matcher(name);
                    LinkedHashSet<Field> fields = new LinkedHashSet<>();
                    if (recalculationPatternMatcher.matches() || baseCalculationRegisterPatternMatcher.matches()) {
                        fields = new LinkedHashSet<>();
                        int order = 1;
                        for (PropertyType prop : complexType.getProperty()) {
                            String n = prop.getName();
                            String propType = prop.getType();
                            Field f = new Field(n, FieldType.getByEdmType(propType), order++);
                            fields.add(f);
                        }
                    }
                    if (recalculationPatternMatcher.matches()) {
                        String registerName = recalculationPatternMatcher.group(1);
                        String recalculationName = recalculationPatternMatcher.group(2);
                        CalculationRegisterRecalculation entity = new CalculationRegisterRecalculation(registerName, recalculationName, fields);
                        markCompositeFields(entity);
                        metadataTree.getCalculationRegisterByName(registerName).getRecalculations().add(entity);
                    } else if (baseCalculationRegisterPatternMatcher.matches()) {
                        String registerName = baseCalculationRegisterPatternMatcher.group(1);
                        String baseRegisterName = baseCalculationRegisterPatternMatcher.group(2);
                        CalculationRegisterBaseRegister entity = new CalculationRegisterBaseRegister(registerName, baseRegisterName, fields);
                        markCompositeFields(entity);
                        metadataTree.getCalculationRegisterByName(registerName).getBaseRegisters().add(entity);
                    }
                }

            } else if (obj instanceof AssociationType) { // Связь
                associations.add(new AssociationWrapper((AssociationType) obj));
            }
        }
        // Заполняем связи
        for (AssociationWrapper association : associations) {
            String name = association.getName(); // Полное имя ссылочного реквизита
            String ownerName = association.getOwnerName(); // Полное имя владельца ссылочного реквизита
            String reference = association.getReference(); // Тип объекта, на который указывает ссылка

            // Ссылочные реквизиты табличных частей
            if (tabularSections.containsKey(ownerName)) {
                for (Field field : tabularSections.get(ownerName)) {
                    if (name.concat("_Key").equals(ownerName.concat("_").concat(field.getName()))) {
                        field.setReference(reference);
                        break;
                    }
                }
                continue;
            }
            Collection<DataServiceEntity> entities;
            if (name.startsWith(Constant.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getConstants());
            } else if (name.startsWith(Catalog.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getCatalogs());
            } else if (name.startsWith(Document.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getDocuments());
            } else if (name.startsWith(DocumentJournal.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getDocumentJournals());
            } else if (name.startsWith(ChartOfCharacteristicTypes.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getChartOfCharacteristicTypes());
            } else if (name.startsWith(ChartOfAccounts.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getChartOfAccounts());
            } else if (name.startsWith(ChartOfCalculationTypes.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getChartOfCalculationTypes());
            } else if (name.startsWith(InformationRegister.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getInformationRegisters());
            } else if (name.startsWith(AccumulationRegister.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getAccumulationRegisters());
            } else if (name.startsWith(AccountingRegister.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getAccountingRegisters());
            } else if (name.startsWith(CalculationRegister.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getCalculationRegisters());
            } else if (name.startsWith(ExchangePlan.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getExchangePlans());
            } else if (name.startsWith(BusinessProcess.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getBusinessProcesses());
            } else if (name.startsWith(Task.RESOURCE_PREFIX)) {
                entities = new ArrayList<>(metadataTree.getTasks());
            } else {
                throw new IllegalArgumentException(name);
            }
            setFieldReference(entities, name, ownerName, reference);
        }

        // Заполняем реквизиты табличных частей
        for (Map.Entry<String, LinkedHashSet<Field>> entry : tabularSections.entrySet()) {
            if (entry.getKey().startsWith(Catalog.RESOURCE_PREFIX)) {
                for (Catalog entity : metadataTree.getCatalogs()) {
                    fillTabularSectionFields(entry, entity);
                }
            } else if (entry.getKey().startsWith(ChartOfCharacteristicTypes.RESOURCE_PREFIX)) {
                for (ChartOfCharacteristicTypes entity : metadataTree.getChartOfCharacteristicTypes()) {
                    fillTabularSectionFields(entry, entity);
                }
            } else if (entry.getKey().startsWith(ExchangePlan.RESOURCE_PREFIX)) {
                for (ExchangePlan entity : metadataTree.getExchangePlans()) {
                    fillTabularSectionFields(entry, entity);
                }
            } else if (entry.getKey().startsWith(Document.RESOURCE_PREFIX)) {
                for (Document entity : metadataTree.getDocuments()) {
                    fillTabularSectionFields(entry, entity);
                }
            } else if (entry.getKey().startsWith(ChartOfCalculationTypes.RESOURCE_PREFIX)) {
                for (ChartOfCalculationTypes entity : metadataTree.getChartOfCalculationTypes()) {
                    fillTabularSectionFields(entry, entity);
                }
            } else if (entry.getKey().startsWith(BusinessProcess.RESOURCE_PREFIX)) {
                for (BusinessProcess entity : metadataTree.getBusinessProcesses()) {
                    fillTabularSectionFields(entry, entity);
                }
            } else if (entry.getKey().startsWith(Task.RESOURCE_PREFIX)) {
                for (Task entity : metadataTree.getTasks()) {
                    fillTabularSectionFields(entry, entity);
                }
            }
        }

        return metadataTree;
    }

    private static void fillTabularSectionFields(Map.Entry<String, LinkedHashSet<Field>> entry, DataServiceEntity entity) {
        Set<TabularSection> tsSet = entity.getTabularSections();
        for (Iterator<TabularSection> it = tsSet.iterator(); it.hasNext(); ) {
            TabularSection tabularSection = it.next();
            if (entry.getKey().equals(entity.getResourceName().concat("_").concat(tabularSection.getName()))) {
                it.remove();
                TabularSection ts = new TabularSection(entity, tabularSection.getName(), entry.getValue());
                markCompositeFields(ts);
                tsSet.add(ts);
                break;
            }
        }
    }

    // Определяет поля, относящиеся к реквизитам составных типов
    private static void markCompositeFields(DataServiceEntity entity) {
        for (Field field : entity.getFields()) {
            String name = field.getName();
            if (entity.getFieldByName(name.concat("_Type")) != null) {
                field.setComposite(true);
            }
        }
    }

    // Определяет, является ли сущность табличной частью, по набору ключевых полей
    private static boolean isTabularSection(List<PropertyRefType> refs) {
        List<String> keys = new ArrayList<>();
        for (PropertyRefType ref : refs) {
            keys.add(ref.getName());
        }
        if (keys.size() != TabularSection.KEY_FIELDS.size()) {
            return false;
        }
        for (Field f : TabularSection.KEY_FIELDS) {
            if (!keys.contains(f.getName())) {
                return false;
            }
        }
        return true;
    }

    private static void setFieldReference(Collection<DataServiceEntity> entities, String name, String ownerName, String reference) {
        for (DataServiceEntity entity : entities) {
            // ownerName может заканчиваться на _RecordType (в отличие от entity.getResourceName()).
            if (ownerName.startsWith(entity.getResourceName())) {
                // Измерения виртуальных таблиц регистров бухгалтерии
                String dim = null;
                String dimBal = null;
                if (entity instanceof AccountingRegisterVirtualTable) {
                    if (entity instanceof AccountingRegisterRecordsWithExtDimensions
                            || entity instanceof AccountingRegisterDrCrTurnovers) {
                        dim = name;
                    } else {
                        dim = name.replace("Dr", "").replace("Cr", "");
                    }
                    dimBal = dim.concat("Balanced");
                }
                for (Field field : entity.getFields()) {
                    if (!field.getName().endsWith("_Key") || field.getName().equals("Ref_Key")) {
                        continue;
                    }
                    String fldName = field.getOriginalName();
                    if (entity instanceof AccountingRegisterVirtualTable) {
                        if (dim.endsWith(fldName) || dimBal.endsWith(fldName) || dim.endsWith("Account") && fldName.equals("BalancedAccount")) {
                            field.setReference(reference);
                        }
                    } else if (name.endsWith(fldName)) {
                        field.setReference(reference);
                        break;
                    }
                }
                // Если поле принадлежит регистру, повторяем процедуру для его виртуальных таблиц.
                if (entity instanceof AccumulationRegister) {
                    Collection<DataServiceEntity> virtualTables = new ArrayList<>();
                    AccumulationRegisterBalance balance = ((AccumulationRegister) entity).getBalance();
                    if (balance != null) {
                        virtualTables.add(balance);
                    }
                    AccumulationRegisterTurnovers turnovers = ((AccumulationRegister) entity).getTurnovers();
                    if (turnovers != null) {
                        virtualTables.add(turnovers);
                    }
                    AccumulationRegisterBalanceAndTurnovers balanceAndTurnovers = ((AccumulationRegister) entity).getBalanceAndTurnovers();
                    if (balanceAndTurnovers != null) {
                        virtualTables.add(balanceAndTurnovers);
                    }
                    if (!virtualTables.isEmpty()) {
                        setFieldReference(virtualTables, name, ownerName, reference);
                    }
                } else if (entity instanceof AccountingRegister) {
                    Collection<DataServiceEntity> virtualTables = new ArrayList<>();
                    AccountingRegisterBalance balance = ((AccountingRegister) entity).getBalance();
                    if (balance != null) {
                        virtualTables.add(balance);
                    }
                    AccountingRegisterTurnovers turnovers = ((AccountingRegister) entity).getTurnovers();
                    if (turnovers != null) {
                        virtualTables.add(turnovers);
                    }
                    AccountingRegisterBalanceAndTurnovers balanceAndTurnovers = ((AccountingRegister) entity).getBalanceAndTurnovers();
                    if (balanceAndTurnovers != null) {
                        virtualTables.add(balanceAndTurnovers);
                    }
                    AccountingRegisterExtDimensions extDimensions = ((AccountingRegister) entity).getExtDimensions();
                    if (extDimensions != null) {
                        virtualTables.add(extDimensions);
                    }
                    AccountingRegisterRecordsWithExtDimensions recordsWithExtDimensions = ((AccountingRegister) entity).getRecordsWithExtDimensions();
                    if (recordsWithExtDimensions != null) {
                        virtualTables.add(recordsWithExtDimensions);
                    }
                    AccountingRegisterDrCrTurnovers drCrTurnovers = ((AccountingRegister) entity).getDrCrTurnovers();
                    if (drCrTurnovers != null) {
                        virtualTables.add(drCrTurnovers);
                    }
                    if (!virtualTables.isEmpty()) {
                        setFieldReference(virtualTables, name, ownerName, reference);
                    }
                } else if (entity instanceof CalculationRegister) {
                    Collection<DataServiceEntity> virtualTables = new ArrayList<>();
                    CalculationRegisterScheduleData scheduleData = ((CalculationRegister) entity).getScheduleData();
                    if (scheduleData != null) {
                        virtualTables.add(scheduleData);
                    }
                    CalculationRegisterActualActionPeriod actualActionPeriod = ((CalculationRegister) entity).getActualActionPeriod();
                    if (actualActionPeriod != null) {
                        virtualTables.add(actualActionPeriod);
                    }
                    virtualTables.addAll(((CalculationRegister) entity).getBaseRegisters());
                    if (!virtualTables.isEmpty()) {
                        setFieldReference(virtualTables, name, ownerName, reference);
                    }
                } else if (entity instanceof InformationRegister && ((InformationRegister) entity).isPeriodic()) {
                    Collection<DataServiceEntity> virtualTables = new ArrayList<>();
                    InformationRegisterSliceLast sliceLast = ((InformationRegister) entity).getSliceLast();
                    virtualTables.add(sliceLast);
                    InformationRegisterSliceFirst sliceFirst = ((InformationRegister) entity).getSliceFirst();
                    virtualTables.add(sliceFirst);
                    setFieldReference(virtualTables, name, ownerName, reference);
                }
            }
        }
    }

    private static class AssociationWrapper {
        final AssociationType association;
        String ownerName;
        String reference;

        AssociationWrapper(AssociationType association) {
            this.association = association;
            for (EndType end : association.getEnd()) {
                String type = end.getType().substring("StandardODATA.".length());
                switch (end.getRole()) {
                    case "Begin":
                        ownerName = type;
                        break;
                    case "End":
                        reference = type;
                }
            }
        }

        // Полное имя ссылочного реквизита
        String getName() {
            return association.getName();
        }

        // Полное имя владельца ссылочного реквизита
        String getOwnerName() {
            return ownerName;
        }

        // Тип объекта, на который указывает ссылка
        String getReference() {
            return reference;
        }
    }

    /**
     * Разбирает демаршализованную ленту Atom.
     *
     * @param feed   Лента Atom
     * @param entity Описание ресурса, данные которого представлены в ленте Atom
     * @return коллекция полей и их значений.
     */
    public static List<Map<Field, Object>> parseFeed(Feed feed, DataServiceEntity entity) {
        List<Map<Field, Object>> result = new ArrayList<>();
        for (Entry entry : feed.getEntry()) {
            for (JAXBElement el : entry.getCategoryOrContentOrId()) {
                if (el.getDeclaredType().equals(Content.class)) {
                    Map<Field, Object> map = new LinkedHashMap<>();
                    List<Object> props = ((Content) el.getValue()).getProperties().getAny();
                    for (Object obj : props) {
                        Node node = (Node) obj;
                        String fieldName = node.getLocalName();
                        if (fieldName.equals("ValueType")) {
                            // Пропускаем (Свойство "Тип значения характеристик" плана видов характеристик, имеет составной тип OData)
                            continue;
                        }
                        if (node.getAttributes().getNamedItem("m:type") != null) {
                            // Это табличная часть
                            continue;
                        }
                        Field field = entity.getFieldByName(fieldName);
                        if (field == null) {
                            throw new IllegalStateException("Неизвестное поле: " + fieldName);
                        }
                        String value = node.getTextContent();
                        Object fieldValue = !value.isEmpty() ? field.getFieldType().parseValue(value) : null;
                        map.put(field, fieldValue);
                    }
                    result.add(map);
                }
            }
        }
        return result;
    }

    /**
     * Разбирает демаршализованные записи виртуальной таблицы регистра 1С.
     *
     * @param data   Записи виртуальной таблицы регистра 1С
     * @param entity Описание виртуальной таблицы
     * @return коллекция полей и их значений.
     */
    public static List<Map<Field, Object>> parseResult(Result data, DataServiceEntity entity) {
        List<Map<Field, Object>> result = new ArrayList<>();
        for (Element el : data.getElement()) {
            Map<Field, Object> map = new LinkedHashMap<>();
            for (Object obj : el.getAny()) {
                Node node = (Node) obj;
                String fieldName = node.getLocalName();
                Field field = entity.getFieldByName(fieldName);
                if (field == null) {
                    throw new IllegalStateException("Неизвестное поле: " + fieldName);
                }
                Node nullAttr = node.getAttributes().getNamedItem("m:null");
                if (nullAttr != null && nullAttr.getNodeValue().equals("true")) {
                    map.put(field, null);
                } else {
                    map.put(field, field.getFieldType().parseValue(node.getTextContent()));
                }
            }
            result.add(map);
        }
        return result;
    }
}
