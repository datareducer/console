/*
 * Copyright (c) 2017-2020 Kirill Mikhaylov <admin@datareducer.ru>
 *
 * Этот файл — часть программы DataReducer Console <http://datareducer.ru>.
 *
 * Программа DataReducer Console является свободным программным обеспечением.
 * Вы вправе распространять ее и/или модифицировать в соответствии с условиями
 * версии 3 либо, по вашему выбору, с условиями более поздней версии
 * Стандартной Общественной Лицензии GNU, опубликованной Free Software Foundation.
 *
 * Программа DataReducer Console распространяется в надежде, что она будет полезной,
 * но БЕЗО ВСЯКИХ ГАРАНТИЙ, в том числе ГАРАНТИИ ТОВАРНОГО СОСТОЯНИЯ ПРИ ПРОДАЖЕ
 * и ПРИГОДНОСТИ ДЛЯ ИСПОЛЬЗОВАНИЯ В КОНКРЕТНЫХ ЦЕЛЯХ.
 * Подробнее см. в Стандартной Общественной Лицензии GNU.
 *
 * Вы должны были получить копию Стандартной Общественной Лицензии GNU
 * вместе с этой программой. Если это не так, см. <https://www.gnu.org/licenses/>.
 */

package ru.datareducer.dataservice.entity;

import java.time.Duration;
import java.util.*;

/**
 * Описание объекта конфигурации 1С - Регистра расчета
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class CalculationRegister implements DataServiceRequest {
    /**
     * Префикс ресурса для обращения к REST-сервису 1С
     */
    public static final String RESOURCE_PREFIX = "CalculationRegister_";

    private final String name;
    private final Set<Field> fields;
    private final LinkedHashSet<Field> presentationFields;
    private final boolean allFields;
    private final Condition condition;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    private Duration cacheLifetime;

    private int hashCode;

    // Виртуальная таблица данных графика.
    private CalculationRegisterScheduleData scheduleData;
    // Виртуальная таблица фактического периода действия.
    private CalculationRegisterActualActionPeriod actualActionPeriod;
    // Виртуальные таблицы записей перерасчетов.
    private final Set<CalculationRegisterRecalculation> recalculations;
    // Виртуальные таблицы базовых данных.
    private final Set<CalculationRegisterBaseRegister> baseRegisters;

    /**
     * Создаёт описание запроса к ресурсу Регистра расчета.
     *
     * @param name        Имя Регистра расчета, как оно задано в конфигураторе.
     * @param fields      Коллекция полей (измерений, ресурсов, реквизитов), которые необходимо получить.
     * @param allFields   Получить значения всех полей. Используется для оптимизации запроса.
     * @param condition   Отбор, применяемый при запросе к ресурсу.
     *                    Если отбор не устанавливается, передаётся пустой Condition.
     * @param allowedOnly Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public CalculationRegister(String name, LinkedHashSet<Field> fields, boolean allFields, Condition condition, boolean allowedOnly) {
        if (name == null) {
            throw new IllegalArgumentException("Значение параметра 'name': null");
        }
        if (fields == null) {
            throw new IllegalArgumentException("Значение параметра 'fields': null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Значение параметра 'condition': null");
        }
        this.name = name;
        this.fields = new LinkedHashSet<>(fields);
        this.presentationFields = Field.presentations(getFields());
        this.allFields = allFields;
        this.condition = condition.clone();
        this.allowedOnly = allowedOnly;

        this.recalculations = new HashSet<>();
        this.baseRegisters = new HashSet<>();

        this.fieldsLookup = new HashMap<>();
        for (Field field : this.fields) {
            fieldsLookup.put(field.getName(), field);
        }
        for (Field field : this.presentationFields) {
            fieldsLookup.put(field.getName(), field);
        }

        this.cacheLifetime = getDefaultCacheLifetime();
    }

    /**
     * Создаёт описание объекта конфигурации 1С - Регистра расчета.
     *
     * @param name   Имя Регистра расчета, как оно задано в конфигураторе.
     * @param fields Поля (измерения, ресурсы, реквизиты) Регистра расчета.
     */
    public CalculationRegister(String name, LinkedHashSet<Field> fields) {
        this(name, fields, false, new Condition(), false);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LinkedHashSet<Field> getFields() {
        return new LinkedHashSet<>(fields);
    }

    @Override
    public Field getFieldByName(String name) {
        return fieldsLookup.get(name);
    }

    @Override
    public String getResourceName() {
        return RESOURCE_PREFIX + name + "_RecordType";
    }

    @Override
    public String getMnemonicName() {
        return String.format("Регистр расчета \"%s\"", name);
    }

    @Override
    public LinkedHashSet<Field> getRequestedFields() {
        return getFields();
    }

    @Override
    public boolean isAllFields() {
        return !allFields;
    }

    @Override
    public LinkedHashSet<Field> getPresentationFields() {
        return new LinkedHashSet<>(presentationFields);
    }

    @Override
    public Condition getCondition() {
        return condition.clone();
    }

    @Override
    public boolean isAllowedOnly() {
        return allowedOnly;
    }

    @Override
    public Duration getCacheLifetime() {
        return cacheLifetime;
    }

    @Override
    public void setCacheLifetime(Duration cacheLifetime) {
        this.cacheLifetime = cacheLifetime;
    }

    public CalculationRegisterScheduleData getScheduleData() {
        return scheduleData;
    }

    public void setScheduleData(CalculationRegisterScheduleData scheduleData) {
        this.scheduleData = scheduleData;
    }

    public CalculationRegisterActualActionPeriod getActualActionPeriod() {
        return actualActionPeriod;
    }

    public void setActualActionPeriod(CalculationRegisterActualActionPeriod actualActionPeriod) {
        this.actualActionPeriod = actualActionPeriod;
    }

    public Set<CalculationRegisterRecalculation> getRecalculations() {
        return recalculations;
    }

    public Set<CalculationRegisterBaseRegister> getBaseRegisters() {
        return baseRegisters;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CalculationRegister)) {
            return false;
        }
        CalculationRegister that = (CalculationRegister) o;
        return that.name.equals(name)
                && that.fields.equals(fields)
                && that.presentationFields.equals(presentationFields)
                && that.allFields == allFields
                && that.condition.equals(condition)
                && that.allowedOnly == allowedOnly;
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = 31 * result + name.hashCode();
            result = 31 * result + fields.hashCode();
            result = 31 * result + presentationFields.hashCode();
            result = 31 * result + (allFields ? 1 : 0);
            result = 31 * result + condition.hashCode();
            result = 31 * result + (allowedOnly ? 1 : 0);
            hashCode = result;
        }
        return result;
    }

}
