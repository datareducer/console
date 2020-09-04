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
package ru.datareducer.model;

import ru.datareducer.dataservice.entity.*;
import ru.datareducer.dataservice.jaxb.FilterElementAdapter;

/**
 * Токен логического выражения отбора данных - простое логическое выражение.
 *
 * @author Kirill Mikhaylov
 */
public class RelationalExpressionWrapper implements BooleanExpressionToken {
    private final AdaptedRelationalExpression adaptedRelationalExpression;

    /**
     * Создаёт новый токен - простое логическое выражение.
     */
    public RelationalExpressionWrapper() {
        adaptedRelationalExpression = new AdaptedRelationalExpression();
    }

    /**
     * Создаёт новый токен - простое логическое выражение.
     *
     * @param relationalExpression Простое логическое выражение
     */
    public RelationalExpressionWrapper(RelationalExpression relationalExpression) {
        this.adaptedRelationalExpression = FilterElementAdapter.adaptRelationalExpression(relationalExpression);
    }

    @Override
    public FilterElement getToken() {
        return FilterElementAdapter.restoreRelationalExpression(adaptedRelationalExpression);
    }

    @Override
    public Field getField() {
        return adaptedRelationalExpression.getField();
    }

    @Override
    public void setField(Field field) {
        adaptedRelationalExpression.setField(field);
    }

    @Override
    public RelationalOperator getRelationalOperator() {
        return adaptedRelationalExpression.getOperator();
    }

    @Override
    public void setRelationalOperator(RelationalOperator operator) {
        adaptedRelationalExpression.setOperator(operator);
    }

    @Override
    public String getValue() {
        return adaptedRelationalExpression.getValue();
    }

    @Override
    public void setValue(String value) {
        adaptedRelationalExpression.setValue(value);
    }

    @Override
    public String getComment() {
        return adaptedRelationalExpression.getComment();
    }

    @Override
    public void setComment(String comment) {
        adaptedRelationalExpression.setComment(comment);
    }

    @Override
    public LogicalOperator getLogicalOperator() {
        return null;
    }
}
