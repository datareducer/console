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
 * Описание виртуальной таблицы оборотов по регистру бухгалтерии 1С за произвольный период с определенной агрегацией
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 * <p>
 * Разворот итогов по периодам (параметр "Периодичность") не поддерживается REST-сервисом 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class AccountingRegisterDrCrTurnovers implements AccountingRegisterVirtualTable {
    /**
     * Префикс ресурса для обращения к REST-сервису 1С
     */
    public static final String RESOURCE_PREFIX = "AccountingRegister_";

    private final String name;
    private final Set<Field> properties;
    private final Set<Field> resources;
    private final Set<Field> requestedDimensions;
    private final LinkedHashSet<Field> presentationFields;
    private final boolean allDimensions;
    private final Condition condition;
    private final Instant startPeriod;
    private final Instant endPeriod;
    private final Condition accountCondition;
    private final Condition balancedAccountCondition;
    private final List<UUID> extraDimensions;
    private final List<UUID> balancedExtraDimensions;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    private Duration cacheLifetime;

    private int hashCode;

    /**
     * Создаёт описание запроса к виртуальной таблице оборотов ДтКт регистра бухгалтерии.
     *
     * @param name                     Имя Регистра бухгалтерии, как оно задано в конфигураторе.
     * @param properties               Набор всех измерений, субконто и полей счетов виртуальной таблицы оборотов.
     * @param resources                Набор всех ресурсов виртуальной таблицы оборотов.
     * @param requestedDimensions      Набор измерений, в разрезе которых будут получены обороты.
     * @param allDimensions            Обороты по всем измерениям. Используется для оптимизации запроса.
     * @param condition                Отбор данных виртуальной таблицей по значениям субконто и измерений
     *                                 регистра бухгалтерии. Если отбор не устанавливается, передаётся пустой Condition.
     * @param startPeriod              Начало периода времени, за который будут получены обороты.
     *                                 Если null, обороты рассчитываются с самой первой записи.
     * @param endPeriod                Конец периода времени, за который будут получены обороты.
     *                                 Если null, обороты рассчитываются по самую последнюю запись.
     * @param accountCondition         Условие отбора по счетам.
     *                                 Если отбор не устанавливается, передаётся пустой Condition.
     * @param balancedAccountCondition Условие отбора по корреспондирующим счетам.
     *                                 Если отбор не устанавливается, передаётся пустой Condition.
     * @param extraDimensions          Список уникальных идентификаторов видов субконто.
     *                                 Если не устанавливается, передаётся пустой список.
     * @param balancedExtraDimensions  Список уникальных идентификаторов корреспондирующих видов субконто.
     *                                 Если не устанавливается, передаётся пустой список.
     * @param allowedOnly              Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public AccountingRegisterDrCrTurnovers(String name, LinkedHashSet<Field> properties, LinkedHashSet<Field> resources,
                                           LinkedHashSet<Field> requestedDimensions, boolean allDimensions,
                                           Condition condition, Instant startPeriod, Instant endPeriod,
                                           Condition accountCondition, Condition balancedAccountCondition,
                                           List<UUID> extraDimensions, List<UUID> balancedExtraDimensions,
                                           boolean allowedOnly) {
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
        if (requestedDimensions == null) {
            throw new IllegalArgumentException("Значение параметра 'requestedDimensions': null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Значение параметра 'condition': null");
        }
        if (startPeriod != null && endPeriod != null && startPeriod.compareTo(endPeriod) > 0) {
            throw new IllegalArgumentException("Начало периода получения оборотов больше его конца");
        }
        if (accountCondition == null) {
            throw new IllegalArgumentException("Значение параметра 'accountCondition': null");
        }
        if (balancedAccountCondition == null) {
            throw new IllegalArgumentException("Значение параметра 'balancedAccountCondition': null");
        }
        if (extraDimensions == null) {
            throw new IllegalArgumentException("Значение параметра 'extraDimensions': null");
        }
        if (balancedExtraDimensions == null) {
            throw new IllegalArgumentException("Значение параметра 'balancedExtraDimensions': null");
        }

        this.name = name;
        this.properties = new LinkedHashSet<>(properties);
        this.resources = new LinkedHashSet<>(resources);
        this.requestedDimensions = new LinkedHashSet<>(requestedDimensions);
        this.allDimensions = allDimensions;
        this.condition = condition.clone();
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.accountCondition = accountCondition.clone();
        this.balancedAccountCondition = balancedAccountCondition.clone();
        this.extraDimensions = new ArrayList<>(extraDimensions);
        this.balancedExtraDimensions = new ArrayList<>(balancedExtraDimensions);
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

        this.presentationFields = Field.presentations(getRequestedDimensions());
        presentationFields.add(new Field(getAccountField().getPresentationName(), FieldType.STRING));
        presentationFields.add(new Field(getBalancedAccountField().getPresentationName(), FieldType.STRING));
        for (Field f : getExtDimensions()) {
            presentationFields.add(new Field(f.getPresentationName(), FieldType.STRING));
        }

        this.cacheLifetime = getDefaultCacheLifetime();
    }

    /**
     * Создаёт описание виртуальной таблицы оборотов ДтКт регистра бухгалтерии.
     *
     * @param name       Имя Регистра бухгалтерии, как оно задано в конфигураторе.
     * @param properties Набор всех измерений, субконто и полей счетов виртуальной таблицы оборотов.
     * @param resources  Набор всех ресурсов виртуальной таблицы оборотов.
     */
    public AccountingRegisterDrCrTurnovers(String name, LinkedHashSet<Field> properties, LinkedHashSet<Field> resources) {
        this(name, properties, resources, new LinkedHashSet<>(), false, new Condition(), null, null,
                new Condition(), new Condition(), new ArrayList<>(), new ArrayList<>(), false);
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
        result.remove(getBalancedAccountField());
        return result;
    }

    @Override
    public LinkedHashSet<Field> getExtDimensions() {
        LinkedHashSet<Field> result = new LinkedHashSet<>();
        result.add(getFieldByName("ExtDimensionDr1"));
        result.add(getFieldByName("ExtDimensionDr2"));
        result.add(getFieldByName("ExtDimensionDr3"));
        result.add(getFieldByName("ExtDimensionCr1"));
        result.add(getFieldByName("ExtDimensionCr2"));
        result.add(getFieldByName("ExtDimensionCr3"));
        result.add(getFieldByName("ExtDimensionDr1_Type"));
        result.add(getFieldByName("ExtDimensionDr2_Type"));
        result.add(getFieldByName("ExtDimensionDr3_Type"));
        result.add(getFieldByName("ExtDimensionCr1_Type"));
        result.add(getFieldByName("ExtDimensionCr2_Type"));
        result.add(getFieldByName("ExtDimensionCr3_Type"));
        return result;
    }

    @Override
    public Field getAccountField() {
        return getFieldByName("AccountDr_Key");
    }

    @Override
    public Field getBalancedAccountField() {
        return getFieldByName("AccountCr_Key");
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
    public Condition getAccountCondition() {
        return accountCondition.clone();
    }

    @Override
    public Condition getBalancedAccountCondition() {
        return balancedAccountCondition.clone();
    }

    @Override
    public ArrayList<UUID> getExtraDimensions() {
        return new ArrayList<>(extraDimensions);
    }

    @Override
    public ArrayList<UUID> getBalancedExtraDimensions() {
        return new ArrayList<>(balancedExtraDimensions);
    }

    @Override
    public String getType() {
        return getResourceName() + "_DrCrTurnovers";
    }

    @Override
    public boolean isAllowedOnly() {
        return allowedOnly;
    }

    @Override
    public String getMetadataName() {
        return "Обороты ДтКт";
    }

    @Override
    public String getMnemonicName() {
        return String.format("Обороты ДтКт регистра бухгалтерии \"%s\"", name);
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
        if (!(o instanceof AccountingRegisterDrCrTurnovers)) {
            return false;
        }
        AccountingRegisterDrCrTurnovers that = (AccountingRegisterDrCrTurnovers) o;
        return that.name.equals(name)
                && that.requestedDimensions.equals(requestedDimensions)
                && that.presentationFields.equals(presentationFields)
                && that.condition.equals(condition)
                && (Objects.equals(that.startPeriod, startPeriod))
                && (Objects.equals(that.endPeriod, endPeriod))
                && that.accountCondition.equals(accountCondition)
                && that.balancedAccountCondition.equals(balancedAccountCondition)
                && that.extraDimensions.equals(extraDimensions)
                && that.balancedExtraDimensions.equals(balancedExtraDimensions)
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
            result = 31 * result + accountCondition.hashCode();
            result = 31 * result + balancedAccountCondition.hashCode();
            result = 31 * result + extraDimensions.hashCode();
            result = 31 * result + balancedExtraDimensions.hashCode();
            result = 31 * result + (allowedOnly ? 1 : 0);
            hashCode = result;
        }
        return result;
    }
}
