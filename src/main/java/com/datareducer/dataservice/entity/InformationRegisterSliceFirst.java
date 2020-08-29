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

/**
 * Описание виртуальной таблицы Среза первых периодического регистра сведений 1С
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class InformationRegisterSliceFirst implements InformationRegisterVirtualTable {
    /**
     * Префикс ресурса для обращения к REST-сервису 1С
     */
    public static final String RESOURCE_PREFIX = "InformationRegister_";

    private final String name;
    private final LinkedHashSet<Field> registerFields;
    private final LinkedHashSet<Field> fieldsParam;
    private final LinkedHashSet<Field> presentationFields;
    private final boolean allFields;
    private final Condition condition;
    private final Instant period;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    private Duration cacheLifetime;

    private int hashCode;

    /**
     * Создаёт описание запроса к виртуальной таблице Среза первых периодического регистра сведений.
     *
     * @param name           Имя Регистра сведений, как оно задано в конфигураторе.
     * @param registerFields Все поля Регистра сведений.
     * @param fieldsParam    Коллекция полей, которые необходимо получить.
     * @param allFields      Получить значения всех полей. Используется для оптимизации запроса.
     * @param condition      Отбор, применяемый при запросе к ресурсу.
     *                       Если отбор не устанавливается, передаётся пустой Condition.
     * @param period         Период получения среза. Если null, получаем срез наиболее поздних значений.
     * @param allowedOnly    Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public InformationRegisterSliceFirst(String name, LinkedHashSet<Field> registerFields, LinkedHashSet<Field> fieldsParam,
                                         boolean allFields, Condition condition, Instant period, boolean allowedOnly) {
        if (name == null) {
            throw new IllegalArgumentException("Значение параметра 'name': null");
        }
        if (registerFields == null) {
            throw new IllegalArgumentException("Значение параметра 'registerFields': null");
        }
        if (registerFields.isEmpty()) {
            throw new IllegalArgumentException("Набор полей пуст");
        }
        if (fieldsParam == null) {
            throw new IllegalArgumentException("Значение параметра 'fieldsParam': null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Значение параметра 'condition': null");
        }

        this.name = name;
        this.registerFields = new LinkedHashSet<>(registerFields);
        this.fieldsParam = new LinkedHashSet<>(fieldsParam);
        this.allFields = allFields;
        this.condition = condition.clone();
        this.period = period;
        this.allowedOnly = allowedOnly;

        this.fieldsLookup = new LinkedHashMap<>();

        for (Field field : registerFields) {
            fieldsLookup.put(field.getName(), field);
            String presentationName = field.getPresentationName();
            fieldsLookup.put(presentationName, new Field(presentationName, FieldType.STRING));
        }

        this.presentationFields = new LinkedHashSet<>();
        initPresentationFields();

        this.cacheLifetime = getDefaultCacheLifetime();
    }

    /**
     * Создаёт описание виртуальной таблицы среза первых периодического регистра сведений.
     *
     * @param name           Имя Регистра сведений, как оно задано в конфигураторе.
     * @param registerFields Поля Регистра сведений.
     */
    public InformationRegisterSliceFirst(String name, LinkedHashSet<Field> registerFields) {
        this(name, registerFields, new LinkedHashSet<>(), false, new Condition(), null, false);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LinkedHashSet<Field> getRegisterFields() {
        return new LinkedHashSet<>(registerFields);
    }

    @Override
    public LinkedHashSet<Field> getFields() {
        return new LinkedHashSet<>(fieldsLookup.values());
    }

    @Override
    public LinkedHashSet<Field> getFieldsParam() {
        return new LinkedHashSet<>(fieldsParam);
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
    }

    @Override
    public Field getFieldByName(String name) {
        return fieldsLookup.get(name);
    }

    @Override
    public String getResourceName() {
        // В соответствии с документацией "_RecordType" добавляется к имени ресурса только зависимого регистра сведений.
        return RESOURCE_PREFIX + name + (isDependent() ? "_RecordType" : "");
    }

    @Override
    public String getMetadataName() {
        return "Срез первых";
    }

    @Override
    public String getMnemonicName() {
        return String.format("Срез первых регистра сведений \"%s\"", name);
    }

    @Override
    public String getType() {
        return getResourceName() + "_SliceFirst";
    }

    /**
     * Возвращает признак зависимого регистра сведений.
     *
     * @return Признак зависимого регистра сведений.
     */
    public boolean isDependent() {
        return fieldsLookup.containsKey("Recorder");
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

    @Override
    public Instant getPeriod() {
        return period;
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
        if (!(o instanceof InformationRegisterSliceFirst)) {
            return false;
        }
        InformationRegisterSliceFirst that = (InformationRegisterSliceFirst) o;
        return that.name.equals(name)
                && that.fieldsParam.equals(fieldsParam)
                && that.presentationFields.equals(presentationFields)
                && that.allFields == allFields
                && that.condition.equals(condition)
                && (that.period != null ? that.period.equals(period) : period == null)
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
            result = 31 * result + (period != null ? period.hashCode() : 0);
            result = 31 * result + (allowedOnly ? 1 : 0);
            hashCode = result;
        }
        return result;
    }

}
