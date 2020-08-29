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
 * Описание виртуальной таблицы остатков регистра накопления 1С
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class AccumulationRegisterBalance implements AccumulationRegisterVirtualTable {
    /**
     * Префикс ресурса для обращения к REST-сервису 1С
     */
    public static final String RESOURCE_PREFIX = "AccumulationRegister_";

    private final String name;
    private final Set<Field> dimensions;
    private final Set<Field> resources;
    private final Set<Field> dimensionsParam;
    private final LinkedHashSet<Field> presentationFields;
    private final boolean allDimensions;
    private final Condition condition;
    private final Instant period;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    private Duration cacheLifetime;

    private int hashCode;

    /**
     * Создаёт описание запроса к виртуальной таблице остатков регистра накопления.
     * Если allDimensions == true, набор dimensionsParam должен содержать все имеющиеся измерения объекта.
     *
     * @param name            Имя Регистра накопления, как оно задано в конфигураторе.
     * @param dimensions      Набор всех измерений виртуальной таблицы остатков.
     * @param resources       Набор всех ресурсов виртуальной таблицы остатков.
     * @param dimensionsParam Набор измерений, в разрезе которых будут получены остатки.
     * @param allDimensions   Остатки по всем измерениям. Используется для оптимизации запроса.
     * @param condition       Условие ограничения состава исходных записей,
     *                        по которым при построении виртуальной таблицы будут собираться итоги.
     *                        Если ограничение не устанавливается, передаётся пустой Condition.
     * @param period          Дата, на которую необходимо получить остатки регистра накопления.
     *                        Если null, остатки рассчитываются по самую последнюю запись.
     * @param allowedOnly     Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public AccumulationRegisterBalance(String name, LinkedHashSet<Field> dimensions, LinkedHashSet<Field> resources,
                                       LinkedHashSet<Field> dimensionsParam, boolean allDimensions, Condition condition,
                                       Instant period, boolean allowedOnly) {
        if (name == null) {
            throw new IllegalArgumentException("Значение параметра 'name': null");
        }
        if (dimensions == null) {
            throw new IllegalArgumentException("Значение параметра 'dimensions': null");
        }
        if (dimensions.isEmpty()) {
            throw new IllegalArgumentException("Набор измерений регистра пуст");
        }
        if (resources == null) {
            throw new IllegalArgumentException("Значение параметра 'resources': null");
        }
        if (resources.isEmpty()) {
            throw new IllegalArgumentException("Набор ресурсов пуст");
        }
        if (dimensionsParam == null) {
            throw new IllegalArgumentException("Значение параметра 'dimensionsParam': null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Значение параметра 'condition': null");
        }
        this.name = name;
        this.dimensions = new LinkedHashSet<>(dimensions);
        this.resources = new LinkedHashSet<>(resources);
        this.dimensionsParam = new LinkedHashSet<>(dimensionsParam);
        this.allDimensions = allDimensions;
        this.condition = condition.clone();
        this.period = period;
        this.allowedOnly = allowedOnly;

        this.fieldsLookup = new LinkedHashMap<>();

        for (Field field : dimensions) {
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
     * Создаёт описание виртуальной таблицы остатков регистра накопления.
     *
     * @param name       Имя Регистра накопления, как оно задано в конфигураторе.
     * @param dimensions Набор всех измерений виртуальной таблицы остатков.
     * @param resources  Набор всех ресурсов виртуальной таблицы остатков.
     */
    public AccumulationRegisterBalance(String name, LinkedHashSet<Field> dimensions, LinkedHashSet<Field> resources) {
        this(name, dimensions, resources, new LinkedHashSet<>(), false, new Condition(), null, false);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LinkedHashSet<Field> getDimensions() {
        return new LinkedHashSet<>(dimensions);
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
                presentationFields.add(new Field(f.getPresentationName(), FieldType.STRING));
            }
        }
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
        return String.format("Остатки регистра накопления \"%s\"", name);
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
        if (!(o instanceof AccumulationRegisterBalance)) {
            return false;
        }
        AccumulationRegisterBalance that = (AccumulationRegisterBalance) o;
        return that.name.equals(name)
                // Запрос к виртуальной таблице остатков не включает перечисления ресурсов,
                // поэтому для сравнения этих запросов значим только набор измерений.
                && that.dimensionsParam.equals(dimensionsParam)
                && that.presentationFields.equals(presentationFields)
                && that.condition.equals(condition)
                && (that.period != null ? that.period.equals(period) : period == null)
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
            result = 31 * result + (allowedOnly ? 1 : 0);
            hashCode = result;
        }
        return result;
    }

}
