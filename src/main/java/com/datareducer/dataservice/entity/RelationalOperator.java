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

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Операторы сравнения.
 *
 * @author Kirill Mikhaylov
 */
@XmlRootElement
public enum RelationalOperator implements FilterElement {
    EQUAL(" eq ", "Равно"),
    NOT_EQUAL(" ne ", "Не равно"),
    GREATER(" gt ", "Больше"),
    GREATER_OR_EQUAL(" ge ", "Больше или равно"),
    LESS(" lt ", "Меньше"),
    LESS_OR_EQUAL(" le ", "Меньше или равно");

    private final String httpForm;
    private final String value;

    RelationalOperator(String httpForm, String value) {
        this.httpForm = httpForm;
        this.value = value;
    }

    @Override
    public String getHttpForm() {
        return httpForm;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

}
