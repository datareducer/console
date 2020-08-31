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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Описание виртуальной таблицы субконто регистра бухгалтерии 1С
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class AccountingRegisterExtDimensions implements AccountingRegisterVirtualTable {
    /** Префикс ресурса для обращения к REST-сервису 1С */
    public static final String RESOURCE_PREFIX = "AccountingRegister_";

    private final String name;
    private final Set<Field> virtualTableFields;
    private final Set<Field> fieldsParam;
    private final boolean allFields;
    private final LinkedHashSet<Field> presentationFields;
    private final Condition condition;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    private Duration cacheLifetime;

    private int hashCode;

    /**
     * Создаёт описание запроса к виртуальной таблице субконто регистра бухгалтерии.
     *
     * @param name               Имя Регистра бухгалтерии, как оно задано в конфигураторе.
     * @param virtualTableFields Набор всех полей виртуальной таблицы субконто регистра бухгалтерии.
     * @param fieldsParam        Набор полей, которые необходимо получить.
     * @param allFields          Получить значения всех полей. Используется для оптимизации запроса.
     * @param condition          Отбор, применяемый при запросе к ресурсу.
     *                           Если отбор не устанавливается, передаётся пустой Condition.
     * @param allowedOnly        Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public AccountingRegisterExtDimensions(String name, LinkedHashSet<Field> virtualTableFields, LinkedHashSet<Field> fieldsParam,
                                           boolean allFields, Condition condition, boolean allowedOnly) {
        if (name == null) {
            throw new IllegalArgumentException("Значение параметра 'name': null");
        }
        if (virtualTableFields == null) {
            throw new IllegalArgumentException("Значение параметра 'virtualTableFields': null");
        }
        if (virtualTableFields.isEmpty()) {
            throw new IllegalArgumentException("Набор 'virtualTableFields' пуст");
        }
        if (fieldsParam == null) {
            throw new IllegalArgumentException("Значение параметра 'fieldsParam': null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Значение параметра 'condition': null");
        }
        this.name = name;
        this.virtualTableFields = new LinkedHashSet<>(virtualTableFields);
        this.fieldsParam = new LinkedHashSet<>(fieldsParam);
        this.allFields = allFields;
        this.condition = condition.clone();
        this.allowedOnly = allowedOnly;

        this.fieldsLookup = new HashMap<>();
        for (Field field : this.virtualTableFields) {
            fieldsLookup.put(field.getName(), field);
            String presentationName = field.getPresentationName();
            fieldsLookup.put(presentationName, new Field(presentationName, FieldType.STRING));
        }

        this.presentationFields = Field.presentations(getFieldsParam());

        this.cacheLifetime = getDefaultCacheLifetime();
    }

    /**
     * Создаёт описание объекта конфигурации 1С - Регистра бухгалтерии.
     *
     * @param name               Имя регистра бухгалтерии, как оно задано в конфигураторе.
     * @param virtualTableFields Поля виртуальной таблицы субконто регистра бухгалтерии.
     */
    public AccountingRegisterExtDimensions(String name, LinkedHashSet<Field> virtualTableFields) {
        this(name, virtualTableFields, new LinkedHashSet<>(), false, new Condition(), false);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LinkedHashSet<Field> getFields() {
        return new LinkedHashSet<>(fieldsLookup.values());
    }

    @Override
    public LinkedHashSet<Field> getVirtualTableFields() {
        return new LinkedHashSet<>(virtualTableFields);
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
        return "Субконто";
    }

    @Override
    public String getMnemonicName() {
        return String.format("Субконто регистра бухгалтерии \"%s\"", name);
    }

    @Override
    public LinkedHashSet<Field> getFieldsParam() {
        return new LinkedHashSet<>(fieldsParam);
    }

    @Override
    public String getType() {
        return getResourceName() + "_ExtDimensions";
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
    public LinkedHashSet<Field> getPresentationFields() {
        return new LinkedHashSet<>(presentationFields);
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
        if (!(o instanceof AccountingRegisterExtDimensions)) {
            return false;
        }
        AccountingRegisterExtDimensions that = (AccountingRegisterExtDimensions) o;
        return that.name.equals(name)
                && that.fieldsParam.equals(fieldsParam)
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
            result = 31 * result + fieldsParam.hashCode();
            result = 31 * result + presentationFields.hashCode();
            result = 31 * result + (allFields ? 1 : 0);
            result = 31 * result + condition.hashCode();
            result = 31 * result + (allowedOnly ? 1 : 0);
            hashCode = result;
        }
        return result;
    }

}
