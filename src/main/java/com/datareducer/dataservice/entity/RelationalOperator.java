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

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Операторы сравнения.
 *
 * @author Kirill Mikhaylov
 */
@XmlRootElement
public enum RelationalOperator implements FilterElement {
    EQUAL(" eq ", " = ", "Равно"),
    NOT_EQUAL(" ne ", " <> ", "Не равно"),
    GREATER(" gt ", " > ", "Больше"),
    GREATER_OR_EQUAL(" ge ", " >= ", "Больше или равно"),
    LESS(" lt ", " < ", "Меньше"),
    LESS_OR_EQUAL(" le ", " <= ", "Меньше или равно");

    private final String httpForm;
    private final String sqlForm;
    private final String value;

    RelationalOperator(String httpForm, String sqlForm, String value) {
        this.httpForm = httpForm;
        this.sqlForm = sqlForm;
        this.value = value;
    }

    @Override
    public String getHttpForm() {
        return httpForm;
    }

    @Override
    public String getSqlForm() {
        return sqlForm;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

}
