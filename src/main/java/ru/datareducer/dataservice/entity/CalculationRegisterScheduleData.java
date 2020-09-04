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
package ru.datareducer.dataservice.entity;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Описание виртуальной таблицы данных графика регистра расчета
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 * <p>
 * Только для регистра расчета с заданным графиком.
 *
 * @author Kirill Mikhaylov
 */
public final class CalculationRegisterScheduleData implements CalculationRegisterVirtualTable {
    /**
     * Префикс ресурса для обращения к REST-сервису 1С
     */
    public static final String RESOURCE_PREFIX = "CalculationRegister_";

    private final String name;
    private final LinkedHashSet<Field> virtualTableFields;
    private final LinkedHashSet<Field> requestedFields;
    private final LinkedHashSet<Field> presentationFields;
    private final boolean allFields;
    private final Condition condition;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    private Duration cacheLifetime;

    private int hashCode;

    /**
     * Создаёт описание запроса к виртуальной таблице данных графика регистра расчета.
     *
     * @param name               Имя Регистра расчета, как оно задано в конфигураторе.
     * @param virtualTableFields Все поля виртуальной таблицы Регистра расчета.
     * @param requestedFields    Коллекция полей, которые необходимо получить.
     * @param allFields          Получить значения всех полей. Используется для оптимизации запроса.
     * @param condition          Отбор, применяемый при запросе к ресурсу.
     *                           Если отбор не устанавливается, передаётся пустой Condition.
     * @param allowedOnly        Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public CalculationRegisterScheduleData(String name, LinkedHashSet<Field> virtualTableFields, LinkedHashSet<Field> requestedFields,
                                           boolean allFields, Condition condition, boolean allowedOnly) {
        if (name == null) {
            throw new IllegalArgumentException("Значение параметра 'name': null");
        }
        if (virtualTableFields == null) {
            throw new IllegalArgumentException("Значение параметра 'virtualTableFields': null");
        }
        if (virtualTableFields.isEmpty()) {
            throw new IllegalArgumentException("Набор полей пуст");
        }
        if (requestedFields == null) {
            throw new IllegalArgumentException("Значение параметра 'requestedFields': null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Значение параметра 'condition': null");
        }

        this.name = name;
        this.virtualTableFields = new LinkedHashSet<>(virtualTableFields);
        this.requestedFields = new LinkedHashSet<>(requestedFields);
        this.allFields = allFields;
        this.condition = condition.clone();
        this.allowedOnly = allowedOnly;

        this.fieldsLookup = new LinkedHashMap<>();

        for (Field field : this.virtualTableFields) {
            fieldsLookup.put(field.getName(), field);
            String presentationName = field.getPresentationName();
            fieldsLookup.put(presentationName, new Field(presentationName, FieldType.STRING));
        }

        this.presentationFields = Field.presentations(getRequestedFields());

        this.cacheLifetime = getDefaultCacheLifetime();
    }

    /**
     * Создаёт описание виртуальной таблицы данных графика регистра расчета.
     *
     * @param name               Имя Регистра расчета, как оно задано в конфигураторе.
     * @param virtualTableFields Поля Регистра расчета.
     */
    public CalculationRegisterScheduleData(String name, LinkedHashSet<Field> virtualTableFields) {
        this(name, virtualTableFields, new LinkedHashSet<>(), false, new Condition(), false);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LinkedHashSet<Field> getVirtualTableFields() {
        return new LinkedHashSet<>(virtualTableFields);
    }

    @Override
    public LinkedHashSet<Field> getFields() {
        return new LinkedHashSet<>(fieldsLookup.values());
    }

    @Override
    public LinkedHashSet<Field> getRequestedFields() {
        return new LinkedHashSet<>(requestedFields);
    }

    @Override
    public LinkedHashSet<Field> getPresentationFields() {
        return new LinkedHashSet<>(presentationFields);
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
    public String getMetadataName() {
        return "Данные графика";
    }

    @Override
    public String getMnemonicName() {
        return String.format("Данные графика регистра расчета \"%s\"", name);
    }

    @Override
    public String getType() {
        return getResourceName() + "_ScheduleData";
    }

    @Override
    public boolean isAllFields() {
        return !allFields;
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

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CalculationRegisterScheduleData)) {
            return false;
        }
        CalculationRegisterScheduleData that = (CalculationRegisterScheduleData) o;
        return that.name.equals(name)
                && that.requestedFields.equals(requestedFields)
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
            result = 31 * result + requestedFields.hashCode();
            result = 31 * result + presentationFields.hashCode();
            result = 31 * result + (allFields ? 1 : 0);
            result = 31 * result + condition.hashCode();
            result = 31 * result + (allowedOnly ? 1 : 0);
            hashCode = result;
        }
        return result;
    }
}
