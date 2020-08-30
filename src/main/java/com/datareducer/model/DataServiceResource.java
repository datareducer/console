/*
 * Copyright (c) 2017-2020 Kirill Mikhaylov <admin@datareducer.ru>
 *
 * Этот файл — часть программы DataReducer <http://datareducer.ru>.
 *
 * Программа DataReducer является свободным программным обеспечением.
 * Вы вправе распространять ее и/или модифицировать в соответствии с условиями версии 2
 * либо, по вашему выбору, с условиями более поздней версии
 * Стандартной Общественной Лицензии GNU, опубликованной Free Software Foundation.
 *
 * Программа DataReducer распространяется в надежде, что она будет полезной,
 * но БЕЗО ВСЯКИХ ГАРАНТИЙ, в том числе ГАРАНТИИ ТОВАРНОГО СОСТОЯНИЯ ПРИ ПРОДАЖЕ
 * и ПРИГОДНОСТИ ДЛЯ ИСПОЛЬЗОВАНИЯ В КОНКРЕТНЫХ ЦЕЛЯХ.
 * Подробнее см. в Стандартной Общественной Лицензии GNU.
 *
 * Вы должны были получить копию Стандартной Общественной Лицензии GNU
 * вместе с этой программой. Если это не так, см. <https://www.gnu.org/licenses/>.
 */
package com.datareducer.model;

import com.datareducer.dataservice.client.ClientException;
import com.datareducer.dataservice.entity.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.rosuda.REngine.*;

import javax.xml.bind.annotation.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static com.datareducer.dataservice.client.DataServiceClient.DATE_TIME_FORMATTER;
import static com.datareducer.model.ScriptParameter.PARAM_PATTERN;

/**
 * Ресурс REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
@XmlRootElement(name = "DataServiceResource")
@XmlType(name = "DataServiceResource", propOrder = {"infoBase", "dataServiceEntity", "presentedFields", "requestedFields",
        "condition", "accountCondition", "balanceAccountCondition", "extraDimensions", "balancedExtraDimensions",
        "orderByList", "top", "mainRegisterDimensionsList", "baseRegisterDimensionsList", "viewPointsList", "slicePeriod",
        "balancePeriod", "turnoversStartPeriod", "turnoversEndPeriod", "allowedOnly", "cacheLifetime"})
public class DataServiceResource {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty("");
    private final LongProperty cacheLifetime = new SimpleLongProperty(0L);
    private final BooleanProperty allowedOnly = new SimpleBooleanProperty();

    private InfoBase infoBase;
    private DataServiceEntity dataServiceEntity;

    private Condition condition;

    // Список доступных полей ресурса или измерений виртуальных таблиц
    private final ObservableList<Field> presentedFields = FXCollections.observableArrayList();
    // Список запрашиваемых полей ресурса или измерений виртуальных таблиц
    private final ObservableList<Field> requestedFields = FXCollections.observableArrayList();

    // Параметры виртуальной таблицы среза последних (первых) регистра сведений.
    private String slicePeriod;

    // Параметры виртуальной таблицы остатков
    private String balancePeriod;

    // Параметры виртуальной таблицы оборотов
    private String turnoversStartPeriod;
    private String turnoversEndPeriod;

    // Параметры виртуальной таблицы оборотов регистра бухгалтерии
    private Condition accountCondition;
    private Condition balanceAccountCondition;
    private final ObservableList<UUID> extraDimensions = FXCollections.observableArrayList();
    private final ObservableList<UUID> balancedExtraDimensions = FXCollections.observableArrayList();

    // Параметры виртуальной таблицы движений с субконто регистра бухгалтерии.
    private final ObservableList<String> orderByList = FXCollections.observableArrayList();
    private final IntegerProperty top = new SimpleIntegerProperty();

    // Параметры виртуальной таблицы базовых данных регистра расчета.
    private final ObservableList<String> mainRegisterDimensionsList = FXCollections.observableArrayList();
    private final ObservableList<String> baseRegisterDimensionsList = FXCollections.observableArrayList();
    private final ObservableList<String> viewPointsList = FXCollections.observableArrayList();

    // Общие для всех наборов данных скрипта параметры
    private Map<String, ScriptParameter> parametersLookup;

    public DataServiceResource() {
    }

    /**
     * Создаёт новый объект ресурса REST-сервиса 1С.
     *
     * @param id                Идентификатор ресурса.
     * @param name              Наименование ресурса.
     * @param infoBase          Информационная база 1С.
     * @param dataServiceEntity Объект конфигурации 1С.
     */
    public DataServiceResource(int id, String name, InfoBase infoBase, DataServiceEntity dataServiceEntity) {
        if (name == null) {
            throw new IllegalArgumentException("Значение параметра 'name': null");
        }
        if (infoBase == null) {
            throw new IllegalArgumentException("Значение параметра 'infoBase': null");
        }
        if (dataServiceEntity == null) {
            throw new IllegalArgumentException("Значение параметра 'dataServiceEntity': null");
        }
        setId(id);
        setName(name);
        setCacheLifetime(dataServiceEntity.getDefaultCacheLifetime().toMillis());
        this.infoBase = infoBase;
        this.dataServiceEntity = dataServiceEntity;

        if (dataServiceEntity instanceof AccumulationRegisterVirtualTable) {
            this.presentedFields.addAll(((AccumulationRegisterVirtualTable) dataServiceEntity).getDimensions());
        } else if (dataServiceEntity instanceof AccountingRegisterExtDimensions) {
            this.presentedFields.addAll(((AccountingRegisterExtDimensions) dataServiceEntity).getVirtualTableFields());
        } else if (dataServiceEntity instanceof AccountingRegisterRecordsWithExtDimensions) {
            this.presentedFields.addAll(((AccountingRegisterRecordsWithExtDimensions) dataServiceEntity).getVirtualTableFields());
        } else if (dataServiceEntity instanceof AccountingRegisterVirtualTable) {
            this.presentedFields.addAll(((AccountingRegisterVirtualTable) dataServiceEntity).getDimensions());
        } else if (dataServiceEntity instanceof InformationRegisterVirtualTable) {
            this.presentedFields.addAll(((InformationRegisterVirtualTable) dataServiceEntity).getRegisterFields());
        } else if (dataServiceEntity instanceof CalculationRegisterVirtualTable) {
            this.presentedFields.addAll(((CalculationRegisterVirtualTable) dataServiceEntity).getVirtualTableFields());
        } else {
            this.presentedFields.addAll(dataServiceEntity.getFields());
        }

        this.condition = new Condition();
        this.accountCondition = new Condition();
        this.balanceAccountCondition = new Condition();
    }

    /**
     * Возвращает набор имён параметров.
     *
     * @return Набор имён параметров.
     */
    Set<String> getParameterNamesSet() {
        Set<String> result = new HashSet<>();
        if (slicePeriod != null && PARAM_PATTERN.matcher(slicePeriod).matches()) {
            result.add(ScriptParameter.removeBraces(slicePeriod));
        }
        if (balancePeriod != null && PARAM_PATTERN.matcher(balancePeriod).matches()) {
            result.add(ScriptParameter.removeBraces(balancePeriod));
        }
        if (turnoversStartPeriod != null && PARAM_PATTERN.matcher(turnoversStartPeriod).matches()) {
            result.add(ScriptParameter.removeBraces(turnoversStartPeriod));
        }
        if (turnoversEndPeriod != null && PARAM_PATTERN.matcher(turnoversEndPeriod).matches()) {
            result.add(ScriptParameter.removeBraces(turnoversEndPeriod));
        }
        getConditionParameterNamesSet(condition, result);
        getConditionParameterNamesSet(accountCondition, result);
        getConditionParameterNamesSet(balanceAccountCondition, result);
        return result;
    }

    private Set<String> getConditionParameterNamesSet(Condition condition, Set<String> parameterSet) {
        for (FilterElement element : condition.getElements()) {
            if (element instanceof RelationalExpression) {
                if (((RelationalExpression) element).getValue() instanceof String) {
                    String value = (String) ((RelationalExpression) element).getValue();
                    if (PARAM_PATTERN.matcher(value).matches()) {
                        parameterSet.add(ScriptParameter.removeBraces(value));
                    }
                }
            } else if (element instanceof Condition) {
                getConditionParameterNamesSet((Condition) element, parameterSet);
            }
        }
        return parameterSet;
    }

    /**
     * TODO
     * @return
     * @throws ClientException
     * @throws UndefinedParameterException
     */
    public List<Map<Field, Object>> getResourceData() throws ClientException, UndefinedParameterException {
        DataServiceRequest request;

        LinkedHashSet<Field> fields = new LinkedHashSet<>(requestedFields);
        String name = dataServiceEntity.getName();
        boolean allFields = presentedFields.isEmpty();

        Instant slicePeriod = getInstantParameterValue(this.slicePeriod);
        Instant balancePeriod = getInstantParameterValue(this.balancePeriod);
        Instant turnoversStartPeriod = getInstantParameterValue(this.turnoversStartPeriod);
        Instant turnoversEndPeriod = getInstantParameterValue(this.turnoversEndPeriod);

        Condition condition = getConditionParameterValue(this.condition);
        Condition accountCondition = getConditionParameterValue(this.accountCondition);
        Condition balanceAccountCondition = getConditionParameterValue(this.balanceAccountCondition);

        // Формирование запроса
        if (dataServiceEntity instanceof Constant) {
            request = new Constant(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof Catalog) {
            request = new Catalog(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof Document) {
            request = new Document(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof DocumentJournal) {
            request = new DocumentJournal(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof ChartOfCharacteristicTypes) {
            request = new ChartOfCharacteristicTypes(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof ChartOfAccounts) {
            request = new ChartOfAccounts(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof ChartOfCalculationTypes) {
            request = new ChartOfCalculationTypes(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof InformationRegister) {
            request = new InformationRegister(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof InformationRegisterSliceLast) {
            request = new InformationRegisterSliceLast(name,
                    ((InformationRegisterSliceLast) dataServiceEntity).getRegisterFields(), fields,
                    allFields, condition, slicePeriod, isAllowedOnly());
        } else if (dataServiceEntity instanceof InformationRegisterSliceFirst) {
            request = new InformationRegisterSliceFirst(name,
                    ((InformationRegisterSliceFirst) dataServiceEntity).getRegisterFields(), fields,
                    allFields, condition, slicePeriod, isAllowedOnly());
        } else if (dataServiceEntity instanceof AccumulationRegister) {
            request = new AccumulationRegister(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof AccumulationRegisterBalance) {
            request = new AccumulationRegisterBalance(name,
                    ((AccumulationRegisterBalance) dataServiceEntity).getDimensions(),
                    ((AccumulationRegisterBalance) dataServiceEntity).getResources(),
                    fields, allFields, condition, balancePeriod, isAllowedOnly());
        } else if (dataServiceEntity instanceof AccumulationRegisterTurnovers) {
            request = new AccumulationRegisterTurnovers(name,
                    ((AccumulationRegisterTurnovers) dataServiceEntity).getDimensions(),
                    ((AccumulationRegisterTurnovers) dataServiceEntity).getResources(),
                    fields, allFields, condition,
                    turnoversStartPeriod, turnoversEndPeriod, isAllowedOnly());
        } else if (dataServiceEntity instanceof AccumulationRegisterBalanceAndTurnovers) {
            request = new AccumulationRegisterBalanceAndTurnovers(name,
                    ((AccumulationRegisterBalanceAndTurnovers) dataServiceEntity).getDimensions(),
                    ((AccumulationRegisterBalanceAndTurnovers) dataServiceEntity).getResources(),
                    fields, allFields, condition, turnoversStartPeriod, turnoversEndPeriod, isAllowedOnly());
        } else if (dataServiceEntity instanceof AccountingRegister) {
            request = new AccountingRegister(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof AccountingRegisterBalance) {
            request = new AccountingRegisterBalance(name,
                    ((AccountingRegisterBalance) dataServiceEntity).getProperties(),
                    ((AccountingRegisterBalance) dataServiceEntity).getResources(),
                    fields, allFields, condition, balancePeriod,
                    accountCondition, getExtraDimensions(), isAllowedOnly());
        } else if (dataServiceEntity instanceof AccountingRegisterTurnovers) {
            request = new AccountingRegisterTurnovers(name,
                    ((AccountingRegisterTurnovers) dataServiceEntity).getProperties(),
                    ((AccountingRegisterTurnovers) dataServiceEntity).getResources(),
                    fields, allFields, condition, turnoversStartPeriod, turnoversEndPeriod, accountCondition,
                    balanceAccountCondition, getExtraDimensions(), getBalancedExtraDimensions(), isAllowedOnly());
        } else if (dataServiceEntity instanceof AccountingRegisterBalanceAndTurnovers) {
            request = new AccountingRegisterBalanceAndTurnovers(name,
                    ((AccountingRegisterBalanceAndTurnovers) dataServiceEntity).getProperties(),
                    ((AccountingRegisterBalanceAndTurnovers) dataServiceEntity).getResources(),
                    fields, allFields, condition, turnoversStartPeriod, turnoversEndPeriod, isAllowedOnly());
        } else if (dataServiceEntity instanceof AccountingRegisterExtDimensions) {
            request = new AccountingRegisterExtDimensions(name,
                    ((AccountingRegisterExtDimensions) dataServiceEntity).getVirtualTableFields(), fields, allFields,
                    condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof AccountingRegisterRecordsWithExtDimensions) {
            request = new AccountingRegisterRecordsWithExtDimensions(name,
                    ((AccountingRegisterRecordsWithExtDimensions) dataServiceEntity).getVirtualTableFields(), fields,
                    allFields, condition, turnoversStartPeriod, turnoversEndPeriod,
                    getTop(), getOrderByList(), isAllowedOnly());
        } else if (dataServiceEntity instanceof AccountingRegisterDrCrTurnovers) {
            request = new AccountingRegisterDrCrTurnovers(name,
                    ((AccountingRegisterDrCrTurnovers) dataServiceEntity).getProperties(),
                    ((AccountingRegisterDrCrTurnovers) dataServiceEntity).getResources(),
                    fields, allFields, condition, turnoversStartPeriod, turnoversEndPeriod, accountCondition,
                    balanceAccountCondition, getExtraDimensions(), getBalancedExtraDimensions(), isAllowedOnly());
        } else if (dataServiceEntity instanceof CalculationRegister) {
            request = new CalculationRegister(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof CalculationRegisterScheduleData) {
            request = new CalculationRegisterScheduleData(name,
                    ((CalculationRegisterScheduleData) dataServiceEntity).getVirtualTableFields(), fields,
                    allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof CalculationRegisterActualActionPeriod) {
            request = new CalculationRegisterActualActionPeriod(name,
                    ((CalculationRegisterActualActionPeriod) dataServiceEntity).getVirtualTableFields(), fields,
                    allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof CalculationRegisterRecalculation) {
            request = new CalculationRegisterRecalculation(name,
                    ((CalculationRegisterRecalculation) dataServiceEntity).getRecalculationName(),
                    ((CalculationRegisterRecalculation) dataServiceEntity).getVirtualTableFields(), fields,
                    allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof CalculationRegisterBaseRegister) {
            request = new CalculationRegisterBaseRegister(name,
                    ((CalculationRegisterBaseRegister) dataServiceEntity).getBaseRegisterName(),
                    ((CalculationRegisterBaseRegister) dataServiceEntity).getVirtualTableFields(), fields,
                    allFields, condition, getMainRegisterDimensionsList(), getBaseRegisterDimensionsList(),
                    getViewPointsList(), isAllowedOnly());
        } else if (dataServiceEntity instanceof ExchangePlan) {
            request = new ExchangePlan(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof BusinessProcess) {
            request = new BusinessProcess(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof Task) {
            request = new Task(name, fields, allFields, condition, isAllowedOnly());
        } else if (dataServiceEntity instanceof TabularSection) {
            request = new TabularSection(((TabularSection) dataServiceEntity).getParent(), name, fields,
                    allFields, condition, isAllowedOnly());
        } else {
            throw new ReducerRuntimeException(); // Недостижимо
        }

        request.setCacheLifetime(Duration.ofMillis(getCacheLifetime()));

        return infoBase.get(request);
    }

    private Instant getInstantParameterValue(String value) throws UndefinedParameterException {
        Instant result = null;
        if (value != null) {
            if (PARAM_PATTERN.matcher(value).matches()) {
                ScriptParameter par = parametersLookup.get(ScriptParameter.removeBraces(value));
                if (par != null && par.getValue() != null) {
                    value = par.getValue();
                } else {
                    throw new UndefinedParameterException(value);
                }
            }
            LocalDateTime ldt = LocalDateTime.parse(value, DATE_TIME_FORMATTER);
            result = ldt.atZone(DATE_TIME_FORMATTER.getZone()).toInstant();
        }
        return result;
    }

    private Condition getConditionParameterValue(Condition value) throws UndefinedParameterException {
        Condition result = new Condition();
        for (FilterElement element : value.getElements()) {
            if (element instanceof RelationalExpression) {
                RelationalExpression el = (RelationalExpression) element;
                if (el.getValue() instanceof String) {
                    String val = (String) (el).getValue();
                    if (PARAM_PATTERN.matcher(val).matches()) {
                        ScriptParameter par = parametersLookup.get(ScriptParameter.removeBraces(val));
                        if (par != null && par.getValue() != null) {
                            val = par.getValue();
                            Object v = el.getField().getFieldType().parseValue(val);
                            result.append(new RelationalExpression(el.getField(), el.getOperator(), v, el.getComment()));
                        } else {
                            throw new UndefinedParameterException(val);
                        }
                    } else {
                        result.append(element);
                    }
                } else {
                    result.append(element);
                }
            } else if (element instanceof Condition) {
                result.append(getConditionParameterValue((Condition) element));
            } else {
                result.append(element);
            }
        }
        return result;
    }

    /**
     * Возвращает таблицу данных для передачи RServe
     *
     * @return Таблица данных
     * @throws ClientException
     */
    REXP getDataFrame() throws ClientException, UndefinedParameterException { ///TODO перенести в класс - обертку результата выполнения запроса?
        List<Map<Field, Object>> resourceData = getResourceData();
        // Список имён столбцов
        List<String> colNames = new ArrayList<>();
        // Столбцы таблицы
        Map<Field, Object> cols = new LinkedHashMap<>();
        Set<Field> fields = getResultTableFields();
        for (Field field : fields) {
            colNames.add(field.getName());
            int l = resourceData.size();
            switch (field.getFieldType()) {
                case LONG:
                    cols.put(field, new double[l]);
                    break;
                case SHORT:
                    cols.put(field, new int[l]);
                    break;
                case DOUBLE:
                    cols.put(field, new double[l]);
                    break;
                case BOOLEAN:
                    cols.put(field, new boolean[l]);
                    break;
                default:
                    cols.put(field, new String[l]);
            }
        }
        // Строки таблицы
        for (int rowNum = 0; rowNum < resourceData.size(); rowNum++) {
            Map<Field, Object> row = resourceData.get(rowNum);
            for (Map.Entry<Field, Object> entry : row.entrySet()) {
                Field field = entry.getKey();
                // Набор полей фактического запроса к ресурсу может отличаться от исходного набора.
                if (!fields.contains(field)) {
                    continue;
                }
                Object value = entry.getValue();
                switch (field.getFieldType()) {
                    case GUID:
                        ((String[]) cols.get(field))[rowNum] = value == null ? null : value.toString();
                        break;
                    case LONG:
                        ((double[]) cols.get(field))[rowNum] = (long) value;
                        break;
                    case SHORT:
                        ((int[]) cols.get(field))[rowNum] = (short) value;
                        break;
                    case DOUBLE:
                        ((double[]) cols.get(field))[rowNum] = (double) value;
                        break;
                    case BOOLEAN:
                        ((boolean[]) cols.get(field))[rowNum] = (boolean) value;
                        break;
                    case DATETIME:
                        ((String[]) cols.get(field))[rowNum] = value == null ? null : String.valueOf(value);
                        break;
                    default:
                        ((String[]) cols.get(field))[rowNum] = value == null ? null : (String) value;
                }
            }
        }
        // Столбцы типа REXP
        List<REXP> rCols = new ArrayList<>();
        for (Map.Entry<Field, Object> entry : cols.entrySet()) {
            Field field = entry.getKey();
            Object value = entry.getValue();
            switch (field.getFieldType()) {
                case LONG:
                    rCols.add(new REXPDouble((double[]) value));
                    break;
                case SHORT:
                    rCols.add(new REXPInteger((int[]) value));
                    break;
                case DOUBLE:
                    rCols.add(new REXPDouble((double[]) value));
                    break;
                case BOOLEAN:
                    rCols.add(new REXPLogical((boolean[]) value));
                    break;
                default:
                    rCols.add(new REXPString((String[]) value));
            }
        }
        RList data = new RList(rCols, colNames);
        REXP dataFrame;
        try {
            dataFrame = REXP.createDataFrame(data);
        } catch (REXPMismatchException e) {
            throw new ReducerRuntimeException(e);
        }

        return dataFrame;
    }

    /**
     * Поместить поле ресурса в список запрашиваемых полей.
     *
     * @param presentedField Поле ресурса.
     */
    public void selectField(Field presentedField) {
        if (presentedField == null) {
            throw new IllegalArgumentException("Значение параметра 'presentedField': null");
        }
        presentedFields.remove(presentedField);
        requestedFields.add(presentedField);
        Collections.sort(requestedFields);
    }

    /**
     * Изъять поле ресурса из списка запрашиваемых полей.
     *
     * @param requestedField Поле ресурса.
     */
    public void returnField(Field requestedField) {
        if (requestedField == null) {
            throw new IllegalArgumentException("Значение параметра 'requestedField': null");
        }
        requestedField.setPresentation(false);
        requestedFields.remove(requestedField);
        presentedFields.add(requestedField);
        Collections.sort(presentedFields);
    }

    /**
     * Поместить все имеющиеся поля в список запрашиваемых.
     */
    public void selectAllFields() {
        requestedFields.addAll(presentedFields);
        presentedFields.clear();
        Collections.sort(requestedFields);
    }

    /**
     * Очистить список запрашиваемых полей ресурса.
     */
    public void returnAllFields() {
        presentedFields.addAll(requestedFields);
        requestedFields.clear();
        for (Field field : presentedFields) {
            field.setPresentation(false);
        }
        Collections.sort(presentedFields);
    }

    /**
     * Возвращает все поля объекта конфигурации 1C. Не применяется к виртуальным таблицам.
     * Коллекцию полей нельзя получить из соответствующего свойства объекта DataServiceEntity,
     * т.к. при маршалинге это свойство не инициализируется (в отличие от коллекции измерений виртуальной таблицы).
     *
     * @return Список полей объекта конфигурации 1C
     */
    public List<Field> getResourceFields() {
        if (dataServiceEntity.isVirtual()) {
            throw new ReducerRuntimeException();
        }
        List<Field> result = new ArrayList<>();
        result.addAll(presentedFields);
        result.addAll(requestedFields);
        Collections.sort(result);
        return result;
    }

    /**
     * Возвращает поля таблицы результата запроса к ресурсу REST-сервиса 1С.
     *
     * @return Поля таблицы результата запроса к ресурсу REST-сервиса 1С.
     */
    public LinkedHashSet<Field> getResultTableFields() {
        LinkedHashSet<Field> result = new LinkedHashSet<>();
        for (Field f : getRequestedFields()) {
            result.add(f);
            if (f.isPresentation()) {
                result.add(new Field(f.getPresentationName(), FieldType.STRING));
            }
        }
        if (dataServiceEntity instanceof AccumulationRegisterVirtualTable) {
            result.addAll(((AccumulationRegisterVirtualTable) dataServiceEntity).getResources());
        } else if (dataServiceEntity instanceof AccountingRegisterVirtualTable
                && !(dataServiceEntity instanceof AccountingRegisterExtDimensions)
                && !(dataServiceEntity instanceof AccountingRegisterRecordsWithExtDimensions)) {

            AccountingRegisterVirtualTable virtualTable = (AccountingRegisterVirtualTable) dataServiceEntity;
            result.addAll(virtualTable.getResources());
            if (dataServiceEntity instanceof AccountingRegisterTurnovers
                    || dataServiceEntity instanceof AccountingRegisterDrCrTurnovers) {
                Field accountField = virtualTable.getAccountField();
                result.add(accountField);
                result.add(new Field(accountField.getPresentationName(), FieldType.STRING));

                Field balancedAccountField = virtualTable.getBalancedAccountField();
                result.add(balancedAccountField);
                result.add(new Field(balancedAccountField.getPresentationName(), FieldType.STRING));

                for (Field f : virtualTable.getExtDimensions()) {
                    result.add(f);
                    result.add(new Field(f.getPresentationName(), FieldType.STRING));
                }
            } else if (dataServiceEntity instanceof AccountingRegisterBalance) {
                Field accountField = virtualTable.getAccountField();
                result.add(accountField);
                result.add(new Field(accountField.getPresentationName(), FieldType.STRING));

                for (Field f : virtualTable.getExtDimensions()) {
                    result.add(f);
                    result.add(new Field(f.getPresentationName(), FieldType.STRING));
                }
            } else if (dataServiceEntity instanceof AccountingRegisterBalanceAndTurnovers) {
                Field accountField = virtualTable.getAccountField();
                result.add(accountField);
                result.add(new Field(accountField.getPresentationName(), FieldType.STRING));

                for (Field f : virtualTable.getExtDimensions()) {
                    result.add(f);
                    result.add(new Field(f.getPresentationName(), FieldType.STRING));
                }
            }
        }
        return result;
    }

    public IntegerProperty idProperty() {
        return id;
    }

    @XmlAttribute
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        idProperty().set(id);
    }

    public StringProperty nameProperty() {
        return name;
    }

    @XmlAttribute
    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        nameProperty().set(name);
    }

    public LongProperty cacheLifetimeProperty() {
        return cacheLifetime;
    }

    @XmlElement(name = "CacheLifetime")
    public long getCacheLifetime() {
        return cacheLifetime.get();
    }

    public void setCacheLifetime(long cacheLifetime) {
        cacheLifetimeProperty().set(cacheLifetime);
    }

    public BooleanProperty allowedOnlyProperty() {
        return allowedOnly;
    }

    @XmlElement(name = "AllowedOnly")
    public boolean isAllowedOnly() {
        return allowedOnly.get();
    }

    public void setAllowedOnly(boolean allowedOnly) {
        allowedOnlyProperty().set(allowedOnly);
    }

    @XmlElement(name = "InfoBase")
    @XmlIDREF
    public InfoBase getInfoBase() {
        return infoBase;
    }

    public void setInfoBase(InfoBase infoBase) {
        this.infoBase = infoBase;
    }

    @XmlAnyElement(lax = true)
    public DataServiceEntity getDataServiceEntity() {
        return dataServiceEntity;
    }

    public void setDataServiceEntity(DataServiceEntity dataServiceEntity) {
        this.dataServiceEntity = dataServiceEntity;
    }

    @XmlElement(name = "Condition")
    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    @XmlElementWrapper(name = "PresentedFields")
    @XmlElement(name = "Field")
    public ObservableList<Field> getPresentedFields() {
        return presentedFields;
    }

    @XmlElementWrapper(name = "RequestedFields")
    @XmlElement(name = "Field")
    public ObservableList<Field> getRequestedFields() {
        return requestedFields;
    }

    @XmlElement(name = "SlicePeriod")
    public String getSlicePeriod() {
        return slicePeriod;
    }

    public void setSlicePeriod(String slicePeriod) {
        this.slicePeriod = slicePeriod;
    }

    @XmlElement(name = "Period")
    public String getBalancePeriod() {
        return balancePeriod;
    }

    public void setBalancePeriod(String balancePeriod) {
        this.balancePeriod = balancePeriod;
    }

    @XmlElement(name = "StartPeriod")
    public String getTurnoversStartPeriod() {
        return turnoversStartPeriod;
    }

    public void setTurnoversStartPeriod(String turnoversStartPeriod) {
        this.turnoversStartPeriod = turnoversStartPeriod;
    }

    @XmlElement(name = "EndPeriod")
    public String getTurnoversEndPeriod() {
        return turnoversEndPeriod;
    }

    public void setTurnoversEndPeriod(String turnoversEndPeriod) {
        this.turnoversEndPeriod = turnoversEndPeriod;
    }

    @XmlElement(name = "AccountCondition")
    public Condition getAccountCondition() {
        return accountCondition;
    }

    public void setAccountCondition(Condition accountCondition) {
        this.accountCondition = accountCondition;
    }

    @XmlElement(name = "BalanceAccountCondition")
    public Condition getBalanceAccountCondition() {
        return balanceAccountCondition;
    }

    public void setBalanceAccountCondition(Condition balanceAccountCondition) {
        this.balanceAccountCondition = balanceAccountCondition;
    }

    @XmlElementWrapper(name = "ExtraDimensions")
    @XmlElement(name = "Guid")
    public ObservableList<UUID> getExtraDimensions() {
        return extraDimensions;
    }

    @XmlElementWrapper(name = "BalancedExtraDimensions")
    @XmlElement(name = "Guid")
    public ObservableList<UUID> getBalancedExtraDimensions() {
        return balancedExtraDimensions;
    }

    @XmlElementWrapper(name = "OrderByList")
    @XmlElement(name = "Column")
    public ObservableList<String> getOrderByList() {
        return orderByList;
    }

    public IntegerProperty topProperty() {
        return top;
    }

    @XmlElement(name = "Top")
    public int getTop() {
        return top.get();
    }

    public void setTop(int top) {
        topProperty().set(top);
    }

    @XmlElementWrapper(name = "MainRegisterDimensionsList")
    @XmlElement(name = "Dimension")
    public ObservableList<String> getMainRegisterDimensionsList() {
        return mainRegisterDimensionsList;
    }

    @XmlElementWrapper(name = "BaseRegisterDimensionsList")
    @XmlElement(name = "Dimension")
    public ObservableList<String> getBaseRegisterDimensionsList() {
        return baseRegisterDimensionsList;
    }

    @XmlElementWrapper(name = "ViewPointsList")
    @XmlElement(name = "ViewPoint")
    public ObservableList<String> getViewPointsList() {
        return viewPointsList;
    }

    public void setParametersLookup(Map<String, ScriptParameter> parametersLookup) {
        this.parametersLookup = parametersLookup;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DataServiceResource)) {
            return false;
        }
        DataServiceResource that = (DataServiceResource) o;
        return that.getId() == getId()
                && that.infoBase.equals(infoBase)
                && that.dataServiceEntity.equals(dataServiceEntity);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + getId();
        result = 31 * result + infoBase.hashCode();
        result = 31 * result + dataServiceEntity.hashCode();
        return result;
    }

}
