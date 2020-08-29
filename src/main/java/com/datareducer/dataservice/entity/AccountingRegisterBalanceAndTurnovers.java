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
package com.datareducer.dataservice.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Описание виртуальной таблицы остатков и оборотов регистра бухгалтерии 1С
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 * <p>
 * Разворот итогов по периодам (параметр "Периодичность") не поддерживается REST-сервисом 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class AccountingRegisterBalanceAndTurnovers implements AccountingRegisterVirtualTable {
    /**
     * Префикс ресурса для обращения к REST-сервису 1С
     */
    public static final String RESOURCE_PREFIX = "AccountingRegister_";

    private final String name;
    private final Set<Field> properties;
    private final Set<Field> resources;
    private final LinkedHashSet<Field> fieldsParam;
    private final boolean allFields;
    private final LinkedHashSet<Field> presentationFields;
    private final Condition condition;
    private final Instant startPeriod;
    private final Instant endPeriod;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    private Duration cacheLifetime;

    private int hashCode;

    /**
     * Создаёт описание запроса к виртуальной таблице остатков и оборотов регистра бухгалтерии.
     *
     * @param name        Имя Регистра бухгалтерии, как оно задано в конфигураторе.
     * @param properties  Набор всех измерений, субконто и полей счетов виртуальной таблицы остатков и оборотов.
     * @param resources   Набор всех ресурсов виртуальной таблицы остатков и оборотов.
     * @param fieldsParam Коллекция полей, которые необходимо получить.
     * @param allFields   Получить значения всех полей. Используется для оптимизации запроса.
     * @param condition   Отбор данных виртуальной таблицей по значениям субконто и измерений
     *                    регистра бухгалтерии. Если отбор не устанавливается, передаётся пустой Condition.
     * @param startPeriod Начало периода времени, за который будут получены обороты.
     *                    Если null, обороты рассчитываются с самой первой записи.
     * @param endPeriod   Конец периода времени, за который будут получены обороты.
     *                    Если null, обороты рассчитываются по самую последнюю запись.
     * @param allowedOnly Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public AccountingRegisterBalanceAndTurnovers(String name, LinkedHashSet<Field> properties,
                                                 LinkedHashSet<Field> resources, LinkedHashSet<Field> fieldsParam, boolean allFields,
                                                 Condition condition, Instant startPeriod, Instant endPeriod, boolean allowedOnly) {
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
        if (fieldsParam == null) {
            throw new IllegalArgumentException("Значение параметра 'fieldsParam': null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Значение параметра 'condition': null");
        }
        if (startPeriod != null && endPeriod != null && startPeriod.compareTo(endPeriod) > 0) {
            throw new IllegalArgumentException("Начало периода получения оборотов больше его конца");
        }

        this.name = name;
        this.properties = new LinkedHashSet<>(properties);
        this.resources = new LinkedHashSet<>(resources);
        this.fieldsParam = new LinkedHashSet<>(fieldsParam);
        this.allFields = allFields;
        this.condition = condition.clone();
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
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

        this.cacheLifetime = getDefaultCacheLifetime();
    }

    /**
     * Создаёт описание виртуальной таблицы остатков и оборотов регистра бухгалтерии.
     *
     * @param name       Имя Регистра бухгалтерии, как оно задано в конфигураторе.
     * @param properties Набор всех измерений, субконто и полей счетов виртуальной таблицы остатков и оборотов.
     * @param resources  Набор всех ресурсов виртуальной таблицы остатков и оборотов.
     */
    public AccountingRegisterBalanceAndTurnovers(String name, LinkedHashSet<Field> properties, LinkedHashSet<Field> resources) {
        this(name, properties, resources, new LinkedHashSet<>(), false, new Condition(), null, null, false);
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
    public LinkedHashSet<Field> getPresentationFields() {
        return new LinkedHashSet<>(presentationFields);
    }

    private void initPresentationFields() {
        for (Field f : getFieldsParam()) {
            if (f.isPresentation()) {
                presentationFields.add(new Field(f.getPresentationName(), FieldType.STRING));
            }
        }
        presentationFields.add(new Field(getAccountField().getPresentationName(), FieldType.STRING));
        for (Field f : getExtDimensions()) {
            presentationFields.add(new Field(f.getPresentationName(), FieldType.STRING));
        }
    }

    @Override
    public boolean isAllFields() {
        return allFields;
    }

    @Override
    public Condition getCondition() {
        return condition.clone();
    }

    @Override
    public Instant getStartPeriod() {
        return startPeriod;
    }

    @Override
    public Instant getEndPeriod() {
        return endPeriod;
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
    public LinkedHashSet<Field> getFieldsParam() {
        return fieldsParam;
    }

    @Override
    public String getType() {
        return getResourceName() + "_BalanceAndTurnovers";
    }

    @Override
    public boolean isAllowedOnly() {
        return allowedOnly;
    }

    @Override
    public String getMetadataName() {
        return "Остатки и обороты";
    }

    @Override
    public String getMnemonicName() {
        return String.format("Остатки и обороты регистра бухгалтерии \"%s\"", name);
    }

    @Override
    public Duration getCacheLifetime() {
        return cacheLifetime;
    }

    @Override
    public void setCacheLifetime(Duration cacheLifetime) {
        this.cacheLifetime = cacheLifetime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AccountingRegisterBalanceAndTurnovers)) {
            return false;
        }
        AccountingRegisterBalanceAndTurnovers that = (AccountingRegisterBalanceAndTurnovers) o;
        return that.name.equals(name)
                && that.fieldsParam.equals(fieldsParam)
                && that.presentationFields.equals(presentationFields)
                && that.condition.equals(condition)
                && (that.startPeriod != null ? that.startPeriod.equals(startPeriod) : startPeriod == null)
                && (that.endPeriod != null ? that.endPeriod.equals(endPeriod) : endPeriod == null)
                && that.allowedOnly == allowedOnly;
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = 31 * result + name.hashCode();
            result = 31 * result + fieldsParam.hashCode();
            result = 31 * result + presentationFields.hashCode();
            result = 31 * result + condition.hashCode();
            result = 31 * result + (startPeriod != null ? startPeriod.hashCode() : 0);
            result = 31 * result + (endPeriod != null ? endPeriod.hashCode() : 0);
            result = 31 * result + (allowedOnly ? 1 : 0);
            hashCode = result;
        }
        return result;
    }

}
