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
package com.datareducer.dataservice.test;

import com.datareducer.dataservice.entity.Condition;
import com.datareducer.dataservice.entity.Field;
import com.datareducer.dataservice.entity.FieldType;
import com.datareducer.dataservice.entity.RelationalExpression;
import com.datareducer.model.BooleanExpressionToken;
import javafx.scene.control.TreeItem;
import org.junit.Test;

import java.util.UUID;

import static com.datareducer.dataservice.entity.LogicalOperator.*;
import static com.datareducer.dataservice.entity.RelationalOperator.EQUAL;
import static org.junit.Assert.assertTrue;

public class FilterTreeTest {

    @Test
    public void conditionParsingTest1() {
        // A and (B and C or not D) and E
        Condition cnd = new Condition(new RelationalExpression(new Field("Description", FieldType.STRING), EQUAL, "КОНГРЕСС-БАНК ОАО, Торговый дом \"Комплексный\" (EUR)", null))
                .append(AND)
                .append(new Condition(new RelationalExpression(new Field("Банк_Key", FieldType.GUID), EQUAL, UUID.fromString("51ed67ab-7220-11df-b336-0011955cba6b"), null))
                        .append(AND)
                        .append(new RelationalExpression(new Field("НомерСчета", FieldType.STRING), EQUAL, "40702810838110014563", null))
                        .append(OR)
                        .append(NOT)
                        .append(new Condition(new RelationalExpression(new Field("ВалютаДенежныхСредств_Key", FieldType.GUID), EQUAL, UUID.fromString("51ed67a6-7220-11df-b336-0011955cba6b"), null))))
                .append(AND)
                .append(new RelationalExpression(new Field("DeletionMark", FieldType.BOOLEAN), EQUAL, false, null));

        TreeItem<BooleanExpressionToken> tree = BooleanExpressionToken.conditionToFilterTree(cnd);
        Condition cnd1 = BooleanExpressionToken.filterTreeToCondition(tree);

        assertTrue(cnd1.equals(cnd));
    }

    @Test
    public void conditionParsingTest2() {
        // not (A and B and C and not (D and E))
        Condition cnd = new Condition(NOT)
                .append(new Condition(new RelationalExpression(new Field("Description", FieldType.STRING), EQUAL, "КОНГРЕСС-БАНК ОАО, Торговый дом \"Комплексный\" (EUR)", null))
                        .append(AND)
                        .append(new RelationalExpression(new Field("Банк_Key", FieldType.GUID), EQUAL, UUID.fromString("51ed67ab-7220-11df-b336-0011955cba6b"), null))
                        .append(AND)
                        .append(new RelationalExpression(new Field("НомерСчета", FieldType.STRING), EQUAL, "40702810838110014563", null))
                        .append(AND)
                        .append(new RelationalExpression(new Field("ВалютаДенежныхСредств_Key", FieldType.GUID), EQUAL, UUID.fromString("51ed67a6-7220-11df-b336-0011955cba6b"), null))
                        .append(AND)
                        .append(NOT)
                        .append(new Condition(new RelationalExpression(new Field("DeletionMark", FieldType.BOOLEAN), EQUAL, false, null))
                                .append(AND)
                                .append(new RelationalExpression(new Field("ИностранныйБанк", FieldType.BOOLEAN), EQUAL, false, null)))
                );

        TreeItem<BooleanExpressionToken> tree = BooleanExpressionToken.conditionToFilterTree(cnd);
        Condition cnd1 = BooleanExpressionToken.filterTreeToCondition(tree);

        assertTrue(cnd1.equals(cnd));
    }
}
