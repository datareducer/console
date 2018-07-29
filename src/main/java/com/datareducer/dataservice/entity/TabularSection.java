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
 * Описание объекта конфигурации 1С - Табличной части
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class TabularSection implements DataServiceRequest {
    /**
     * Имя суперкласса для классов всех Табличных частей в кэше
     */
    public static final String SUPERCLASS_NAME = "TabularSection";
    /**
     * Ключевые поля
     */
    public static final Set<Field> KEY_FIELDS;

    private final DataServiceEntity parent;
    private final String name;
    private final Set<Field> fields;
    private final boolean allFields;
    private final Condition condition;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    private int hashCode;

    static {
        Set<Field> keyFields = new LinkedHashSet<>();
        keyFields.add(new Field("Ref_Key", FieldType.GUID, 1));
        keyFields.add(new Field("LineNumber", FieldType.LONG, 2));
        KEY_FIELDS = Collections.unmodifiableSet(keyFields);
    }

    /**
     * Создаёт описание запроса к Табличной части.
     * Коллекция полей дополняется ключевыми полями объекта конфигурации и полями отбора.
     * Если allFields == true, набор полей fields должен содержать все имеющиеся поля объекта.
     *
     * @param parent      Владелец табличной части
     * @param name        Имя Табличной части, как оно задано в конфигураторе.
     * @param fields      Набор полей, которые необходимо получить.
     * @param allFields   Получить значения всех полей. Используется для оптимизации запроса.
     * @param condition   Отбор, применяемый при запросе к ресурсу.
     *                    Если отбор не устанавливается, передаётся пустой Condition.
     * @param allowedOnly Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public TabularSection(DataServiceEntity parent, String name, LinkedHashSet<Field> fields, boolean allFields,
                          Condition condition, boolean allowedOnly) {
        if (parent == null) {
            throw new IllegalArgumentException("Значение параметра 'parent': null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Значение параметра 'name': null");
        }
        if (fields == null) {
            throw new IllegalArgumentException("Значение параметра 'fields': null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Значение параметра 'condition': null");
        }
        this.parent = parent;
        this.name = name;
        this.fields = new LinkedHashSet<>(fields);
        this.allFields = allFields;
        this.condition = condition.clone();
        this.allowedOnly = allowedOnly;

        // Дополняем поля запроса.
        if (!allFields) {
            this.fields.addAll(KEY_FIELDS);
            this.fields.addAll(condition.getFilterFields());
        }

        this.fieldsLookup = new HashMap<>();
        for (Field field : this.fields) {
            fieldsLookup.put(field.getName(), field);
        }
    }

    /**
     * Создаёт описание объекта конфигурации 1С - Табличной части.
     *
     * @param parent Владелец табличной части
     * @param name   Имя Табличной части, как оно задано в конфигураторе.
     * @param fields Поля Табличной части.
     */
    public TabularSection(DataServiceEntity parent, String name, LinkedHashSet<Field> fields) {
        this(parent, name, fields, false, new Condition(), false);
    }

    /**
     * Создаёт описание объекта конфигурации 1С - Табличной части.
     *
     * @param parent Владелец табличной части
     * @param name   Имя Табличной части, как оно задано в конфигураторе.
     */
    public TabularSection(DataServiceEntity parent, String name) {
        this(parent, name, new LinkedHashSet<>(), false, new Condition(), false);
    }

    /**
     * Возвращает владельца табличной части
     *
     * @return Владелец табличной части
     */
    public DataServiceEntity getParent() {
        return parent;
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
    public String getResourceName() {
        return parent.getResourceName() + "_" + name;
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
        return SUPERCLASS_NAME + "_" + getResourceName();
    }

    @Override
    public String getMnemonicName() {
        return String.format("Табличная часть \"%s\" (%s)", name, parent.getMnemonicName());
    }

    @Override
    public boolean isAllFields() {
        return allFields;
    }

    @Override
    public Field getFieldByName(String name) {
        return fieldsLookup.get(name);
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TabularSection)) {
            return false;
        }
        TabularSection that = (TabularSection) o;
        return that.getResourceName().equals(getResourceName())
                && that.fields.equals(fields)
                && that.allFields == allFields
                && that.condition.equals(condition)
                && that.allowedOnly == allowedOnly;
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = 31 * result + getResourceName().hashCode();
            result = 31 * result + fields.hashCode();
            result = 31 * result + (allFields ? 1 : 0);
            result = 31 * result + condition.hashCode();
            result = 31 * result + (allowedOnly ? 1 : 0);
            hashCode = result;
        }
        return result;
    }
}
