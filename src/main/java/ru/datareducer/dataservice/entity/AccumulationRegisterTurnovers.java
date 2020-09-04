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
import java.time.Instant;
import java.util.*;

/**
 * Описание виртуальной таблицы оборотов регистра накопления 1С
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 * <p>
 * Разворот итогов по периодам (параметр "Периодичность") не поддерживается REST-сервисом 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class AccumulationRegisterTurnovers implements AccumulationRegisterVirtualTable {
    /**
     * Префикс ресурса для обращения к REST-сервису 1С
     */
    public static final String RESOURCE_PREFIX = "AccumulationRegister_";

    private final String name;
    private final Set<Field> dimensions;
    private final Set<Field> resources;
    private final Set<Field> requestedDimensions;
    private final LinkedHashSet<Field> presentationFields;
    private final boolean allDimensions;
    private final Condition condition;
    private final Instant startPeriod;
    private final Instant endPeriod;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    private Duration cacheLifetime;

    private int hashCode;

    /**
     * Создаёт описание запроса к виртуальной таблице оборотов регистра накопления.
     *
     * @param name                Имя Регистра накопления, как оно задано в конфигураторе.
     * @param dimensions          Набор всех измерений виртуальной таблицы оборотов.
     * @param resources           Набор всех ресурсов виртуальной таблицы оборотов.
     * @param requestedDimensions Набор измерений, в разрезе которых будут получены обороты.
     * @param allDimensions       Обороты по всем измерениям. Используется для оптимизации запроса.
     * @param condition           Условие ограничения состава исходных записей,
     *                            по которым при построении виртуальной таблицы оборотов будут собираться обороты.
     *                            Строится по полям измерений регистра накопления.
     *                            Если ограничение не устанавливается, передаётся пустой Condition.
     * @param startPeriod         Начало периода, за который требуется получить обороты.
     *                            Если null, обороты рассчитываются с самой первой записи.
     * @param endPeriod           Конец периода, за который требуется получить обороты.
     *                            Если null, обороты рассчитываются по самую последнюю запись.
     * @param allowedOnly         Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public AccumulationRegisterTurnovers(String name, LinkedHashSet<Field> dimensions, LinkedHashSet<Field> resources,
                                         LinkedHashSet<Field> requestedDimensions, boolean allDimensions, Condition condition,
                                         Instant startPeriod, Instant endPeriod, boolean allowedOnly) {
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
        if (requestedDimensions == null) {
            throw new IllegalArgumentException("Значение параметра 'dimensionsParam': null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Значение параметра 'condition': null");
        }
        if (startPeriod != null && endPeriod != null && startPeriod.compareTo(endPeriod) > 0) {
            throw new IllegalArgumentException("Начало периода получения оборотов больше его конца");
        }

        this.name = name;
        this.dimensions = new LinkedHashSet<>(dimensions);
        this.resources = new LinkedHashSet<>(resources);
        this.requestedDimensions = new LinkedHashSet<>(requestedDimensions);
        this.allDimensions = allDimensions;
        this.condition = condition.clone();
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
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

        this.presentationFields = Field.presentations(getRequestedDimensions());

        this.cacheLifetime = getDefaultCacheLifetime();
    }

    /**
     * Создаёт описание виртуальной таблицы оборотов регистра накопления.
     *
     * @param name       Имя Регистра накопления, как оно задано в конфигураторе.
     * @param dimensions Набор всех измерений виртуальной таблицы оборотов.
     * @param resources  Набор всех ресурсов виртуальной таблицы оборотов.
     */
    public AccumulationRegisterTurnovers(String name, LinkedHashSet<Field> dimensions, LinkedHashSet<Field> resources) {
        this(name, dimensions, resources, new LinkedHashSet<>(), false, new Condition(), null, null, false);
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
    public LinkedHashSet<Field> getRequestedDimensions() {
        return new LinkedHashSet<>(requestedDimensions);
    }

    @Override
    public LinkedHashSet<Field> getPresentationFields() {
        return new LinkedHashSet<>(presentationFields);
    }

    @Override
    public LinkedHashSet<Field> getRequestedFields() {
        return getRequestedDimensions();
    }

    @Override
    public boolean isAllFields() {
        return !allDimensions;
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
    public String getType() {
        return getResourceName() + "_Turnover";
    }

    @Override
    public boolean isAllowedOnly() {
        return allowedOnly;
    }

    @Override
    public String getMetadataName() {
        return "Обороты";
    }

    @Override
    public String getMnemonicName() {
        return String.format("Обороты регистра накопления \"%s\"", name);
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
        if (!(o instanceof AccumulationRegisterTurnovers)) {
            return false;
        }
        AccumulationRegisterTurnovers that = (AccumulationRegisterTurnovers) o;
        return that.name.equals(name)
                // Запрос к виртуальной таблице оборотов не включает перечисления ресурсов,
                // поэтому для сравнения этих запросов значим только набор измерений.
                && that.requestedDimensions.equals(requestedDimensions)
                && that.presentationFields.equals(presentationFields)
                && that.condition.equals(condition)
                && (Objects.equals(that.startPeriod, startPeriod))
                && (Objects.equals(that.endPeriod, endPeriod))
                && that.allowedOnly == allowedOnly;
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = 31 * result + name.hashCode();
            result = 31 * result + requestedDimensions.hashCode();
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
