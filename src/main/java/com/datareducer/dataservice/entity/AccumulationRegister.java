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
import java.util.*;

/**
 * Описание объекта конфигурации 1С - Регистра накопления
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class AccumulationRegister implements DataServiceRequest {
    /** Префикс ресурса для обращения к REST-сервису 1С */
    public static final String RESOURCE_PREFIX = "AccumulationRegister_";

    private final String name;
    private final Set<Field> fields;
    private final LinkedHashSet<Field> presentationFields;
    private final boolean allFields;
    private final Condition condition;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    // Остатки регистра накопления
    private AccumulationRegisterBalance balance;
    // Обороты регистра накопления
    private AccumulationRegisterTurnovers turnovers;
    // Остатки и обороты регистра накопления
    private AccumulationRegisterBalanceAndTurnovers balanceAndTurnovers;

    private Duration cacheLifetime;

    private int hashCode;

    /**
     * Создаёт описание запроса к ресурсу Регистра накопления.
     *
     * @param name        Имя Регистра накопления, как оно задано в конфигураторе.
     * @param fields      Коллекция полей (измерений, ресурсов, реквизитов), которые необходимо получить.
     * @param allFields   Получить значения всех полей. Используется для оптимизации запроса.
     * @param condition   Отбор, применяемый при запросе к ресурсу.
     *                    Если отбор не устанавливается, передаётся пустой Condition.
     * @param allowedOnly Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public AccumulationRegister(String name, LinkedHashSet<Field> fields, boolean allFields, Condition condition, boolean allowedOnly) {
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
     * Создаёт описание объекта конфигурации 1С - Регистра накопления.
     *
     * @param name   Имя Регистра накопления, как оно задано в конфигураторе.
     * @param fields Поля (измерения, ресурсы, реквизиты) Регистра накопления.
     */
    public AccumulationRegister(String name, LinkedHashSet<Field> fields) {
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
        return String.format("Регистр накопления \"%s\"", name);
    }

    @Override
    public boolean isAllFields() {
        return allFields;
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

    public AccumulationRegisterBalance getBalance() {
        return balance;
    }

    public void setBalance(AccumulationRegisterBalance balance) {
        this.balance = balance;
    }

    public AccumulationRegisterTurnovers getTurnovers() {
        return turnovers;
    }

    public void setTurnovers(AccumulationRegisterTurnovers turnovers) {
        this.turnovers = turnovers;
    }

    public AccumulationRegisterBalanceAndTurnovers getBalanceAndTurnovers() {
        return balanceAndTurnovers;
    }

    public void setBalanceAndTurnovers(AccumulationRegisterBalanceAndTurnovers balanceAndTurnovers) {
        this.balanceAndTurnovers = balanceAndTurnovers;
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
        if (!(o instanceof AccumulationRegister)) {
            return false;
        }
        AccumulationRegister that = (AccumulationRegister) o;
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
