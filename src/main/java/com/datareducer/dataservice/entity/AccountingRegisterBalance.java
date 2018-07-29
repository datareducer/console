/*
 * Этот файл — часть программы DataReducer Console.
 *
 * DataReducer Console — R-консоль для "1С:Предприятия"
 * <http://datareducer.ru>
 *
 * Copyright (c) 2017,2018 Kirill Mikhaylov
 * <admin@datareducer.ru>
 *
 * Программа DataReducer Console является свободным
 * программным обеспечением. Вы вправе распространять ее
 * и/или модифицировать в соответствии с условиями версии 2
 * либо, по вашему выбору, с условиями более поздней версии
 * Стандартной Общественной Лицензии GNU, опубликованной
 * Free Software Foundation.
 *
 * Программа DataReducer Console распространяется в надежде,
 * что она будет полезной, но БЕЗО ВСЯКИХ ГАРАНТИЙ,
 * в том числе ГАРАНТИИ ТОВАРНОГО СОСТОЯНИЯ ПРИ ПРОДАЖЕ
 * и ПРИГОДНОСТИ ДЛЯ ИСПОЛЬЗОВАНИЯ В КОНКРЕТНЫХ ЦЕЛЯХ.
 * Подробнее см. в Стандартной Общественной Лицензии GNU.
 *
 * Вы должны были получить копию Стандартной Общественной
 * Лицензии GNU вместе с этой программой. Если это не так, см.
 * <https://www.gnu.org/licenses/>.
 */
package com.datareducer.dataservice.entity;

import java.time.Instant;
import java.util.*;

/**
 * Описание виртуальной таблицы остатков регистра бухгалтерии 1С
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class AccountingRegisterBalance implements AccountingRegisterVirtualTable {
    /**
     * Префикс ресурса для обращения к REST-сервису 1С
     */
    public static final String RESOURCE_PREFIX = "AccountingRegister_";
    /**
     * Имя суперкласса для хранения результатов выполнения запросов
     * к виртуальным таблицам остатков регистров бухгалтерии 1С в кэше.
     */
    public static final String SUPERCLASS_NAME = "AccountingRegisterBalance";

    private final String name;
    private final Set<Field> properties;
    private final Set<Field> resources;
    private final Set<Field> dimensionsParam;
    private final LinkedHashSet<Field> presentationFields;
    private final boolean allDimensions;
    private final Condition condition;
    private final Instant period;
    private final Condition accountCondition;
    private final List<UUID> extraDimensions;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    private int hashCode;

    /**
     * Создаёт описание запроса к виртуальной таблице остатков регистра бухгалтерии.
     * Если allDimensions == true, набор dimensionsParam должен содержать все имеющиеся измерения объекта.
     *
     * @param name                     Имя Регистра бухгалтерии, как оно задано в конфигураторе.
     * @param properties               Набор всех измерений, субконто и полей счетов виртуальной таблицы остатков.
     * @param resources                Набор всех ресурсов виртуальной таблицы остатков.
     * @param dimensionsParam          Набор измерений, в разрезе которых будут получены остатки.
     * @param allDimensions            Остатки по всем измерениям. Используется для оптимизации запроса.
     * @param condition                Отбор данных виртуальной таблицей по значениям субконто и измерений
     *                                 регистра бухгалтерии. Если отбор не устанавливается, передаётся пустой Condition.
     * @param period                   Дата, на которую необходимо получить остатки.
     *                                 Если null, остатки рассчитываются по самую последнюю запись.
     * @param accountCondition         Условие отбора по счетам.
     *                                 Если отбор не устанавливается, передаётся пустой Condition.
     * @param extraDimensions          Список уникальных идентификаторов видов субконто.
     *                                 Если не устанавливается, передаётся пустой список.
     * @param allowedOnly              Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public AccountingRegisterBalance(String name, LinkedHashSet<Field> properties, LinkedHashSet<Field> resources,
                                       LinkedHashSet<Field> dimensionsParam, boolean allDimensions,
                                       Condition condition, Instant period, Condition accountCondition,
                                       List<UUID> extraDimensions, boolean allowedOnly) {
        if (name == null) {
            throw new IllegalArgumentException("Значение параметра 'name': null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("Значение параметра 'properties': null");
        }
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("Набор 'properties' пуст");
        }
        if (resources == null) {
            throw new IllegalArgumentException("Значение параметра 'resources': null");
        }
        if (resources.isEmpty()) {
            throw new IllegalArgumentException("Набор 'resources' пуст");
        }
        if (dimensionsParam == null) {
            throw new IllegalArgumentException("Значение параметра 'dimensionsParam': null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Значение параметра 'condition': null");
        }
        if (accountCondition == null) {
            throw new IllegalArgumentException("Значение параметра 'accountCondition': null");
        }
        if (extraDimensions == null) {
            throw new IllegalArgumentException("Значение параметра 'extraDimensions': null");
        }

        this.name = name;
        this.properties = new LinkedHashSet<>(properties);
        this.resources = new LinkedHashSet<>(resources);
        this.dimensionsParam = new LinkedHashSet<>(dimensionsParam);
        this.allDimensions = allDimensions;
        this.condition = condition.clone();
        this.period = period;
        this.accountCondition = accountCondition.clone();
        this.extraDimensions = new ArrayList<>(extraDimensions);
        this.allowedOnly = allowedOnly;

        this.fieldsLookup = new LinkedHashMap<>();

        for (Field field : properties) {
            fieldsLookup.put(field.getName(), field);
            String presentationName = field.getPresentationName();
            fieldsLookup.put(presentationName, new Field(presentationName, FieldType.STRING));
        }
        for (Field field : resources) {
            fieldsLookup.put(field.getName(), field);
        }

        this.presentationFields = new LinkedHashSet<>();
        initPresentationFields();
    }

    /**
     * Создаёт описание виртуальной таблицы остатков регистра бухгалтерии.
     *
     * @param name       Имя Регистра бухгалтерии, как оно задано в конфигураторе.
     * @param properties Набор всех измерений, субконто и полей счетов виртуальной таблицы остатков.
     * @param resources  Набор всех ресурсов виртуальной таблицы остатков.
     */
    public AccountingRegisterBalance(String name, LinkedHashSet<Field> properties, LinkedHashSet<Field> resources) {
        this(name, properties, resources, new LinkedHashSet<>(), false, new Condition(), null,
                new Condition(), new ArrayList<>(), false);
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public LinkedHashSet<Field> getProperties() {
        return new LinkedHashSet<>(properties);
    }

    @Override
    public LinkedHashSet<Field> getDimensions() {
        LinkedHashSet<Field> result = getProperties();
        result.removeAll(getExtDimensions());
        result.remove(getAccountField());
        return result;
    }

    @Override
    public LinkedHashSet<Field> getExtDimensions() {
        LinkedHashSet<Field> result = new LinkedHashSet<>();
        result.add(getFieldByName("ExtDimension1"));
        result.add(getFieldByName("ExtDimension2"));
        result.add(getFieldByName("ExtDimension3"));
        result.add(getFieldByName("ExtDimension1_Type"));
        result.add(getFieldByName("ExtDimension2_Type"));
        result.add(getFieldByName("ExtDimension3_Type"));
        return result;
    }

    @Override
    public Field getAccountField() {
        return getFieldByName("Account_Key");
    }

    @Override
    public LinkedHashSet<Field> getResources() {
        return new LinkedHashSet<>(resources);
    }

    @Override
    public LinkedHashSet<Field> getDimensionsParam() {
        return new LinkedHashSet<>(dimensionsParam);
    }

    @Override
    public LinkedHashSet<Field> getPresentationFields() {
        return new LinkedHashSet<>(presentationFields);
    }

    private void initPresentationFields() {
        for (Field f : getDimensionsParam()) {
            if (f.isPresentation()) {
                presentationFields .add(new Field(f.getPresentationName(), FieldType.STRING));
            }
        }
        presentationFields.add(new Field(getAccountField().getPresentationName(), FieldType.STRING));
        for (Field f : getExtDimensions()) {
            presentationFields .add(new Field(f.getPresentationName(), FieldType.STRING));
        }
    }

    @Override
    public String getSuperclassName() {
        return SUPERCLASS_NAME;
    }

    @Override
    public String getClassName() {
        return SUPERCLASS_NAME + "_" + name;
    }

    @Override
    public boolean isAllFields() {
        return allDimensions;
    }

    @Override
    public Condition getCondition() {
        return condition.clone();
    }

    @Override
    public Instant getPeriod() {
        return period;
    }

    @Override
    public LinkedHashSet<Field> getFields() {
        return new LinkedHashSet<>(fieldsLookup.values());
    }

    @Override
    public Field getFieldByName(String name) {
        return fieldsLookup.get(name);
    }

    @Override
    public String getResourceName() {
        return RESOURCE_PREFIX + name;
    }

    @Override
    public Condition getAccountCondition() {
        return accountCondition.clone();
    }

    @Override
    public ArrayList<UUID> getExtraDimensions() {
        return new ArrayList<>(extraDimensions);
    }

    @Override
    public String getType() {
        return getResourceName() + "_Balance";
    }

    @Override
    public boolean isAllowedOnly() {
        return allowedOnly;
    }

    @Override
    public String getMetadataName() {
        return "Остатки";
    }

    @Override
    public String getMnemonicName() {
        return String.format("Остатки регистра бухгалтерии \"%s\"", name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AccountingRegisterBalance)) {
            return false;
        }
        AccountingRegisterBalance that = (AccountingRegisterBalance) o;
        return that.name.equals(name)
                && that.dimensionsParam.equals(dimensionsParam)
                && that.presentationFields.equals(presentationFields)
                && that.condition.equals(condition)
                && (that.period != null ? that.period.equals(period) : period == null)
                && that.accountCondition.equals(accountCondition)
                && that.extraDimensions.equals(extraDimensions)
                && that.allowedOnly == allowedOnly;
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = 31 * result + name.hashCode();
            result = 31 * result + dimensionsParam.hashCode();
            result = 31 * result + presentationFields.hashCode();
            result = 31 * result + condition.hashCode();
            result = 31 * result + (period != null ? period.hashCode() : 0);
            result = 31 * result + accountCondition.hashCode();
            result = 31 * result + extraDimensions.hashCode();
            result = 31 * result + (allowedOnly ? 1 : 0);
            hashCode = result;
        }
        return result;
    }

}
