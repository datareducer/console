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

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Виртуальная таблица регистра расчета.
 *
 * @author Kirill Mikhaylov
 * @see CalculationRegisterScheduleData
 */
public interface CalculationRegisterVirtualTable extends DataServiceRequest {
    /**
     * Возвращает набор полей ресурса, которые необходимо получить.
     *
     * @return Набор полей ресурса, которые необходимо получить.
     */
    LinkedHashSet<Field> getFieldsParam();

    /**
     * Возвращает набор всех полей виртуальной таблицы.
     *
     * @return Набор всех полей виртуальной таблицы.
     */
    LinkedHashSet<Field> getVirtualTableFields();

    /**
     * Возвращает имя перерасчета регистра расчета.
     *
     * @return Имя перерасчета регистра расчета.
     */
    default String getRecalculationName() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает имя базового регистра расчета.
     *
     * @return Имя базового регистра расчета.
     */
    default String getBaseRegisterName() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает имена измерений основного регистра расчета, по которому строится таблица базовых данных.
     * (для виртуальной таблицы базовых данных регистра расчета)
     *
     * @return имена измерений основного регистра расчета, по которому строится таблица базовых данных.
     */
    default List<String> getMainRegisterDimensions() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает имена измерений базового регистра расчета, по которому строится таблица базовых данных.
     * (для виртуальной таблицы базовых данных регистра расчета)
     *
     * @return имена измерений базового регистра расчета, по которому строится таблица базовых данных.
     */
    default List<String> getBaseRegisterDimensions() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает имена полей базового регистра расчета, по которым производится суммирование базовых данных.
     * (для виртуальной таблицы базовых данных регистра расчета)
     *
     * @return имена полей базового регистра расчета, по которым производится суммирование базовых данных.
     */
    default List<String> getViewPoints() {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isVirtual() {
        return true;
    }


}
