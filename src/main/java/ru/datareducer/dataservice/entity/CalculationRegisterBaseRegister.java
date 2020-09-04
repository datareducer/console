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
import java.util.*;

/**
 * Описание виртуальной таблицы базовых данных регистра расчета
 * или запроса к соответствующему ресурсу REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class CalculationRegisterBaseRegister implements CalculationRegisterVirtualTable {
    /**
     * Префикс ресурса для обращения к REST-сервису 1С
     */
    public static final String RESOURCE_PREFIX = "CalculationRegister_";

    private final String name;
    private final String baseRegisterName;
    private final LinkedHashSet<Field> virtualTableFields;
    private final LinkedHashSet<Field> requestedFields;
    private final LinkedHashSet<Field> presentationFields;
    private final boolean allFields;
    private final Condition condition;
    private final List<String> mainRegisterDimensions;
    private final List<String> baseRegisterDimensions;
    private final List<String> viewPoints;
    private final boolean allowedOnly;

    private final Map<String, Field> fieldsLookup;

    private Duration cacheLifetime;

    private int hashCode;

    /**
     * Создаёт описание запроса к виртуальной таблице базовых данных регистра расчета.
     *
     * @param name                   Имя регистра расчета, как оно задано в конфигураторе.
     * @param baseRegisterName       Имя базового регистра расчета.
     * @param virtualTableFields     Все поля виртуальной таблицы.
     * @param requestedFields        Коллекция полей, которые необходимо получить.
     * @param allFields              Получить значения всех полей. Используется для оптимизации запроса.
     * @param condition              Отбор, применяемый при запросе к ресурсу.
     *                               Если отбор не устанавливается, передаётся пустой Condition.
     * @param mainRegisterDimensions Имена измерений основного регистра расчета, по которому строится таблица базовых данных.
     *                               Список не может быть пустым.
     * @param baseRegisterDimensions Имена измерений базового регистра расчета, по которому строится таблица базовых данных.
     *                               Список не может быть пустым.
     * @param viewPoints             Имена полей базового регистра расчета, по которым производится суммирование базовых данных.
     * @param allowedOnly            Выбрать элементы, которые не попадают под ограничения доступа к данным.
     */
    public CalculationRegisterBaseRegister(String name, String baseRegisterName, LinkedHashSet<Field> virtualTableFields,
                                           LinkedHashSet<Field> requestedFields, boolean allFields, Condition condition,
                                           List<String> mainRegisterDimensions, List<String> baseRegisterDimensions,
                                           List<String> viewPoints, boolean allowedOnly) {
        if (name == null) {
            throw new IllegalArgumentException("Значение параметра 'name': null");
        }
        if (baseRegisterName == null) {
            throw new IllegalArgumentException("Значение параметра 'baseRegisterName': null");
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
        if (mainRegisterDimensions == null) {
            throw new IllegalArgumentException("Значение параметра 'mainRegisterDimensions': null");
        }
        if (baseRegisterDimensions == null) {
            throw new IllegalArgumentException("Значение параметра 'baseRegisterDimensions': null");
        }
        if (viewPoints == null) {
            throw new IllegalArgumentException("Значение параметра 'viewPoints': null");
        }

        this.name = name;
        this.baseRegisterName = baseRegisterName;
        this.virtualTableFields = new LinkedHashSet<>(virtualTableFields);
        this.requestedFields = new LinkedHashSet<>(requestedFields);
        this.allFields = allFields;
        this.condition = condition.clone();
        this.mainRegisterDimensions = new ArrayList<>(mainRegisterDimensions);
        this.baseRegisterDimensions = new ArrayList<>(baseRegisterDimensions);
        this.viewPoints = new ArrayList<>(viewPoints);
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
     * Создаёт описание виртуальной таблицы записей перерасчета регистра расчета.
     *
     * @param name               Имя регистра расчета, как оно задано в конфигураторе.
     * @param baseRegisterName   Имя перерасчета регистра расчета, как оно задано в конфигураторе.
     * @param virtualTableFields Поля регистра расчета.
     */
    public CalculationRegisterBaseRegister(String name, String baseRegisterName, LinkedHashSet<Field> virtualTableFields) {
        this(name, baseRegisterName, virtualTableFields, new LinkedHashSet<>(), false, new Condition(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), false);
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBaseRegisterName() {
        return baseRegisterName;
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
        return "База " + baseRegisterName;
    }

    @Override
    public String getMnemonicName() {
        return String.format("Данные базового регистра \"%s\" регистра расчета \"%s\"", baseRegisterName, name);
    }

    @Override
    public String getType() {
        return getResourceName() + "_Base" + baseRegisterName;
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
    public List<String> getMainRegisterDimensions() {
        return new ArrayList<>(mainRegisterDimensions);
    }

    @Override
    public List<String> getBaseRegisterDimensions() {
        return new ArrayList<>(baseRegisterDimensions);
    }

    @Override
    public List<String> getViewPoints() {
        return new ArrayList<>(viewPoints);
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
        if (!(o instanceof CalculationRegisterBaseRegister)) {
            return false;
        }
        CalculationRegisterBaseRegister that = (CalculationRegisterBaseRegister) o;
        return that.name.equals(name)
                && that.baseRegisterName.equals(baseRegisterName)
                && that.requestedFields.equals(requestedFields)
                && that.presentationFields.equals(presentationFields)
                && that.allFields == allFields
                && that.condition.equals(condition)
                && that.mainRegisterDimensions.equals(mainRegisterDimensions)
                && that.baseRegisterDimensions.equals(baseRegisterDimensions)
                && that.viewPoints.equals(viewPoints)
                && that.allowedOnly == allowedOnly;
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = 31 * result + name.hashCode();
            result = 31 * result + baseRegisterName.hashCode();
            result = 31 * result + requestedFields.hashCode();
            result = 31 * result + presentationFields.hashCode();
            result = 31 * result + (allFields ? 1 : 0);
            result = 31 * result + condition.hashCode();
            result = 31 * result + mainRegisterDimensions.hashCode();
            result = 31 * result + baseRegisterDimensions.hashCode();
            result = 31 * result + viewPoints.hashCode();
            result = 31 * result + (allowedOnly ? 1 : 0);
            hashCode = result;
        }
        return result;
    }

}
