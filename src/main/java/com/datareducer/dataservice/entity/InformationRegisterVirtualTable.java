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

import java.time.Instant;
import java.util.LinkedHashSet;

/**
 * Виртуальная таблица регистра сведений (Срез последних, Срез первых)
 *
 * @author Kirill Mikhaylov
 * @see InformationRegisterSliceLast
 * @see InformationRegisterSliceFirst
 */
public interface InformationRegisterVirtualTable extends DataServiceRequest {
    /**
     * Возвращает период получения среза.
     *
     * @return Период получения среза.
     */
    Instant getPeriod();

//    /**
//     * Возвращает набор полей ресурса, которые необходимо получить.
//     *
//     * @return Набор полей ресурса, которые необходимо получить.
//     */
//    LinkedHashSet<Field> getFieldsParam();

    /**
     * Возвращает набор представлений полей, которые необходимо получить.
     *
     * @return Набор представлений полей, которые необходимо получить.
     */
    LinkedHashSet<Field> getPresentationFields();

    /**
     * Возвращает набор всех полей регистра.
     *
     * @return Набор всех полей регистра.
     */
    LinkedHashSet<Field> getRegisterFields();

    @Override
    default boolean isVirtual() {
        return true;
    }

}
