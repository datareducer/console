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

import com.datareducer.dataservice.entity.*;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static com.datareducer.dataservice.entity.LogicalOperator.*;

/**
 * Токен логического выражения отбора данных (логический оператор или простое логическое выражение).
 * Используется для параметризации таблицы отборов данных формы ресурса 1C.
 *
 * @author Kirill Mikhaylov
 * @see LogicalOperatorWrapper
 * @see RelationalExpressionWrapper
 */
public interface BooleanExpressionToken {

    /**
     * Формирует дерево разбора логического выражения отбора данных.
     * <p>
     * Пример:
     * Логическое выражение отбора данных: A and (B and C or not D) and E
     * A,B,C,D и E - это простые логические выражения (объекты типа RelationalExpression);
     * Выражение с скобках - это сложное логическое выражение (объект типа Condition).
     * <p>
     * Дерево разбора:
     * <p>
     * and
     * |
     * |---A
     * |
     * |---or
     * |   |---and
     * |   |    |---B
     * |   |    |---C
     * |   |
     * |   |---not
     * |        |---D
     * |
     * |---E
     *
     * @param condition Логическое выражение.
     * @return Дерево разбора.
     */
    static TreeItem<BooleanExpressionToken> conditionToFilterTree(Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Значение параметра 'condition': null");
        }
        TreeItem<BooleanExpressionToken> current = new TreeItem<>();
        for (FilterElement token : condition.getElements()) {
            if (token instanceof RelationalExpression) {
                RelationalExpressionWrapper expToken = new RelationalExpressionWrapper((RelationalExpression) token);
                if (current.getValue() != null) {
                    assert current.getValue().getToken() == NOT;
                    current.getChildren().add(new TreeItem<>(expToken));
                } else {
                    current.setValue(expToken);
                }
            } else if (token instanceof LogicalOperator) {
                TreeItem<BooleanExpressionToken> parent = current.getParent();
                TreeItem<BooleanExpressionToken> blankNode = new TreeItem<>();
                if (token != NOT && parent == null) {
                    TreeItem<BooleanExpressionToken> nodeOp = new TreeItem<>(new LogicalOperatorWrapper((LogicalOperator) token));
                    nodeOp.getChildren().add(current);
                    nodeOp.getChildren().add(blankNode);
                    nodeOp.setExpanded(true);
                    current = blankNode;
                    continue;
                }
                if (token != NOT && token == parent.getValue().getToken()) {
                    parent.getChildren().add(blankNode);
                    current = blankNode;
                    continue;
                }
                switch ((LogicalOperator) token) {
                    case AND: {
                        assert parent.getValue().getToken() == OR;
                        TreeItem<BooleanExpressionToken> nodeAnd = new TreeItem<>(new LogicalOperatorWrapper(AND));
                        parent.getChildren().remove(current);
                        nodeAnd.getChildren().add(current);
                        nodeAnd.getChildren().add(blankNode);
                        nodeAnd.setExpanded(true);
                        parent.getChildren().add(nodeAnd);
                        current = blankNode;
                        break;
                    }
                    case OR: {
                        assert parent.getValue().getToken() == AND;
                        if (parent.getParent() != null && parent.getParent().getValue().getToken() == OR) {
                            parent.getParent().getChildren().add(blankNode);
                        } else {
                            TreeItem<BooleanExpressionToken> nodeOr = new TreeItem<>(new LogicalOperatorWrapper(OR));
                            nodeOr.getChildren().add(parent);
                            nodeOr.getChildren().add(blankNode);
                            nodeOr.setExpanded(true);
                        }
                        current = blankNode;
                        break;
                    }
                    case NOT: {
                        current.setValue(new LogicalOperatorWrapper(NOT));
                        current.setExpanded(true);
                    }
                }
            } else if (token instanceof Condition) {
                TreeItem<BooleanExpressionToken> nodeCnd = conditionToFilterTree((Condition) token);
                if (current.getValue() == null) {
                    TreeItem<BooleanExpressionToken> parent = current.getParent();
                    if (parent != null) {
                        parent.getChildren().remove(current);
                        parent.getChildren().add(nodeCnd);
                    }
                    current = nodeCnd;
                } else {
                    assert current.getValue().getToken() == NOT;
                    current.getChildren().add(nodeCnd);
                }
            }
        }
        // Получаем корень дерева
        TreeItem<BooleanExpressionToken> rootNode = current;
        while (rootNode.getParent() != null) {
            rootNode = rootNode.getParent();
        }
        return rootNode;
    }

    /**
     * Формирует логическое выражение отбора данных на основе дерева разбора.
     *
     * @param filterTree Дерево разбора.
     * @return Логическое выражение.
     */
    static Condition filterTreeToCondition(TreeItem<BooleanExpressionToken> filterTree) {
        if (filterTree == null) {
            throw new IllegalArgumentException("Значение параметра 'filterTree': null");
        }
        FilterElement rootToken = filterTree.getValue().getToken();
        if (rootToken instanceof RelationalExpression) {
            return new Condition(rootToken);
        }
        List<FilterElement> filterElements = new ArrayList<>();
        for (TreeItem<BooleanExpressionToken> node : filterTree.getChildren()) {
            FilterElement token = node.getValue().getToken();
            if (token instanceof RelationalExpression) {
                filterElements.add(token);
            } else if (token instanceof LogicalOperator) {
                if (token == rootToken && token != NOT || rootToken == AND && token == OR) {
                    filterElements.add(filterTreeToCondition(node));
                } else {
                    filterElements.addAll(filterTreeToCondition(node).getElements());
                }
            }
        }
        Condition result = new Condition();
        ListIterator<FilterElement> it = filterElements.listIterator();
        while (it.hasNext()) {
            FilterElement element = it.next();
            result.append(element);
            if (!(element instanceof LogicalOperator) && it.hasNext()) {
                FilterElement nextEl = it.next();
                if (!(nextEl instanceof LogicalOperator) || nextEl == NOT) {
                    result.append(rootToken);
                }
                it.previous();
            }
        }
        if (rootToken == NOT) {
            result = new Condition(NOT).append(result.size() != 1 ? result : new Condition(result.getElements().get(0)));
        }
        return result;
    }

    FilterElement getToken();

    LogicalOperator getLogicalOperator();

    default void setLogicalOperator(LogicalOperator logicalOperator) {
        throw new UnsupportedOperationException();
    }

    Field getField();

    default void setField(Field field) {
        throw new UnsupportedOperationException();
    }

    RelationalOperator getRelationalOperator();

    default void setRelationalOperator(RelationalOperator operator) {
        throw new UnsupportedOperationException();
    }

    String getValue();

    default void setValue(String value) {
        throw new UnsupportedOperationException();
    }

    String getComment();

    default void setComment(String comment) {
        throw new UnsupportedOperationException();
    }

}
