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
package com.datareducer.model;

import com.datareducer.dataservice.entity.Field;
import com.datareducer.dataservice.entity.FilterElement;
import com.datareducer.dataservice.entity.LogicalOperator;
import com.datareducer.dataservice.entity.RelationalOperator;

/**
 * Токен логического выражения отбора данных - оператор логического выражения.
 *
 * @author Kirill Mikhaylov
 */
public class LogicalOperatorWrapper implements BooleanExpressionToken {
    private LogicalOperator logicalOperator;

    /**
     * Создаёт новый токен - оператор логического выражения.
     *
     * @param logicalOperator Оператор логического выражения.
     */
    public LogicalOperatorWrapper(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    @Override
    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    @Override
    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    @Override
    public FilterElement getToken() {
        return logicalOperator;
    }

    @Override
    public Field getField() {
        return null;
    }

    @Override
    public RelationalOperator getRelationalOperator() {
        return null;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public String getComment() {
        return null;
    }
}
