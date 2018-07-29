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
package com.datareducer.dataservice.jaxb;

import com.datareducer.dataservice.entity.AdaptedRelationalExpression;
import com.datareducer.dataservice.entity.Field;
import com.datareducer.dataservice.entity.FilterElement;
import com.datareducer.dataservice.entity.RelationalExpression;
import com.datareducer.model.ScriptParameter;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Instant;

import static com.datareducer.dataservice.client.DataServiceClient.DATE_TIME_FORMATTER;
import static com.datareducer.dataservice.entity.RelationalExpression.DECIMAL_FORMAT;

public class FilterElementAdapter extends XmlAdapter<Object, FilterElement> {
    @Override
    public FilterElement unmarshal(Object v) throws Exception {
        if (v instanceof AdaptedRelationalExpression) {
            return restoreRelationalExpression((AdaptedRelationalExpression) v);
        }
        return (FilterElement) v;
    }

    @Override
    public Object marshal(FilterElement v) throws Exception {
        if (v instanceof RelationalExpression) {
            return adaptRelationalExpression((RelationalExpression) v);
        }
        return v;
    }

    public static RelationalExpression restoreRelationalExpression(AdaptedRelationalExpression a) {
        Field field = a.getField();
        Object value;
        String valStr = a.getValue();
        if (ScriptParameter.PARAM_PATTERN.matcher(valStr).matches()) {
            value = valStr;
        } else {
            value = field.getFieldType().parseValue(valStr);
        }
        return new RelationalExpression(field, a.getOperator(), value, a.getComment());
    }

    public static AdaptedRelationalExpression adaptRelationalExpression(RelationalExpression r) {
        Object value = r.getValue();
        String valueStr;
        if (value instanceof Instant) {
            valueStr = DATE_TIME_FORMATTER.format((Instant) value);
        } else if (value instanceof Double) {
            valueStr = DECIMAL_FORMAT.format(value);
        } else if (value instanceof Boolean) {
            valueStr = (Boolean) value ? "true" : "false";
        } else {
            valueStr = value.toString();
        }
        return new AdaptedRelationalExpression(r.getField(), r.getOperator(), valueStr, r.getComment());
    }
}
