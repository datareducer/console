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

import java.util.*;

/**
 * Описание объекта конфигурации 1С - Регистра бухгалтерии
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class AccountingRegister implements DataServiceRequest {
    /**
     * Префикс ресурса для обращения к REST-сервису 1С
     */
    public static final String RESOURCE_PREFIX = "AccountingRegister_";
    /**
     * Имя суперкласса для классов всех Регистров бухгалтерии в кэше
     */
    public static final String SUPERCLASS_NAME = "AccountingRegister";
    /**
     * Ключевые поля. Для Регистра бухгалтерии ключевыми являются поля Регистратора и Номера строки.
     * Если реквизит регистратора имеет составной тип, то ему будет соответствовать два свойства ресурса REST-сервиса
     * вместо одного (см. документацию: https://its.1c.ru/db/v838doc#bookmark:dev:TI000001361).
     * Таким образом, состав ключевых полей разных Регистров бухгалтерии различается.
     * По этой причине ключевые поля для суперкласса классов Регистров бухгалтерии в кэше не указываются.
     */
    public static final Set<Field> KEY_FIELDS;

    private final String name;
    private final Set<Field> fields;
    private final boolean allFields;
    private final Condition condition;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    // Обороты регистра бухгалтерии
    private AccountingRegisterTurnovers turnovers;
    // Остатки регистра бухгалтерии
    private AccountingRegisterBalance balance;
    // Остатки и обороты регистра бухгалтерии
    private AccountingRegisterBalanceAndTurnovers balanceAndTurnovers;
    // Субконто регистра бухгалтерии
    private AccountingRegisterExtDimensions extDimensions;
    // Движения с субконто регистра бухгалтерии
    private AccountingRegisterRecordsWithExtDimensions recordsWithExtDimensions;
    // Обороты ДтКт регистра бухгалтерии
    private AccountingRegisterDrCrTurnovers drCrTurnovers;

    private int hashCode;

    static {
        KEY_FIELDS = Collections.unmodifiableSet(new HashSet<>());
    }

    /**
     * Создаёт описание запроса к ресурсу Регистра бухгалтерии.
     * Коллекция полей дополняется полями отбора.
     * Если allFields == true, коллекция полей fields должна содержать все имеющиеся поля объекта.
     *
     * @param name        Имя Регистра бухгалтерии, как оно задано в конфигураторе.
     * @param fields      Коллекция полей (измерений, ресурсов, реквизитов), которые необходимо получить.
     * @param allFields   Получить значения всех полей. Используется для оптимизации запроса.
     * @param condition   Отбор, применяемый при запросе к ресурсу.
     *                    Если отбор не устанавливается, передаётся пустой Condition.
     * @param allowedOnly Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public AccountingRegister(String name, LinkedHashSet<Field> fields, boolean allFields, Condition condition, boolean allowedOnly) {
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
        this.allFields = allFields;
        this.condition = condition.clone();
        this.allowedOnly = allowedOnly;

        // Дополняем поля запроса.
        if (!allFields) {
            this.fields.addAll(condition.getFilterFields());
        }
        this.fieldsLookup = new HashMap<>();
        for (Field field : this.fields) {
            fieldsLookup.put(field.getName(), field);
        }
    }

    /**
     * Создаёт описание объекта конфигурации 1С - Регистра бухгалтерии.
     *
     * @param name   Имя Регистра бухгалтерии, как оно задано в конфигураторе.
     * @param fields Поля (измерения, ресурсы, реквизиты) Регистра бухгалтерии.
     */
    public AccountingRegister(String name, LinkedHashSet<Field> fields) {
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
    public Set<Field> getKeyFields() {
        return KEY_FIELDS;
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
    public String getMnemonicName() {
        return String.format("Регистр бухгалтерии \"%s\"", name);
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
    public boolean isAllowedOnly() {
        return allowedOnly;
    }

    public AccountingRegisterTurnovers getTurnovers() {
        return turnovers;
    }

    public void setTurnovers(AccountingRegisterTurnovers turnovers) {
        this.turnovers = turnovers;
    }

    public AccountingRegisterBalance getBalance() {
        return balance;
    }

    public void setBalance(AccountingRegisterBalance balance) {
        this.balance = balance;
    }

    public AccountingRegisterBalanceAndTurnovers getBalanceAndTurnovers() {
        return balanceAndTurnovers;
    }

    public void setBalanceAndTurnovers(AccountingRegisterBalanceAndTurnovers balanceAndTurnovers) {
        this.balanceAndTurnovers = balanceAndTurnovers;
    }

    public AccountingRegisterExtDimensions getExtDimensions() {
        return extDimensions;
    }

    public void setExtDimensions(AccountingRegisterExtDimensions extDimensions) {
        this.extDimensions = extDimensions;
    }

    public AccountingRegisterRecordsWithExtDimensions getRecordsWithExtDimensions() {
        return recordsWithExtDimensions;
    }

    public void setRecordsWithExtDimensions(AccountingRegisterRecordsWithExtDimensions recordsWithExtDimensions) {
        this.recordsWithExtDimensions = recordsWithExtDimensions;
    }

    public AccountingRegisterDrCrTurnovers getDrCrTurnovers() {
        return drCrTurnovers;
    }

    public void setDrCrTurnovers(AccountingRegisterDrCrTurnovers drCrTurnovers) {
        this.drCrTurnovers = drCrTurnovers;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AccountingRegister)) {
            return false;
        }
        AccountingRegister that = (AccountingRegister) o;
        return that.name.equals(name)
                && that.fields.equals(fields)
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
            result = 31 * result + (allFields ? 1 : 0);
            result = 31 * result + condition.hashCode();
            result = 31 * result + (allowedOnly ? 1 : 0);
            hashCode = result;
        }
        return result;
    }
}
