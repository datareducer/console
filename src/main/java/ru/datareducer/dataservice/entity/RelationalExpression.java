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

import ru.datareducer.dataservice.client.DataServiceClient;
import ru.datareducer.model.ScriptParameter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Простое логическое выражение отбора данных при запросе к REST-сервису 1С.
 *
 * @author Kirill Mikhaylov
 */
public final class RelationalExpression implements FilterElement, Cloneable {
    private final Field field;
    private final RelationalOperator operator;
    private final Object value;
    private final String comment;

    public final static DecimalFormat DECIMAL_FORMAT;
    public final static Pattern CAST_FUNCTION_PATTERN;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT = new DecimalFormat("#", symbols);
        DECIMAL_FORMAT.setMaximumFractionDigits(8);
        CAST_FUNCTION_PATTERN = Pattern.compile("cast\\([^(),]+,[^(),]+\\)");
    }

    /**
     * Создёт логическое выражение отбора данных.
     *
     * @param field    Реквизит объекта.
     * @param operator Оператор сравнения.
     * @param value    Значение реквизита. Тип должен соответствовать типу field.
     */
    public RelationalExpression(Field field, RelationalOperator operator, Object value, String comment) {
        if (field == null) {
            throw new IllegalArgumentException("Параметр 'field' равен null");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Параметр 'operator' равен null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Параметр 'value' равен null");
        }
        if (field.isComposite() && !(CAST_FUNCTION_PATTERN.matcher((String) value).matches()
                || ScriptParameter.PARAM_PATTERN.matcher((String) value).matches())) {
            throw new IllegalArgumentException("Недопустимый формат значения реквизита составного типа: " + value);
        }
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.comment = comment;
    }

    @Override
    public String getHttpForm() {
        StringBuilder builder = new StringBuilder();
        builder.append(field.getName()).append(operator.getHttpForm());
        if (field.isComposite()) {
            builder.append(value);
        } else if (value instanceof Instant) {
            builder.append("datetime'").append(DataServiceClient.DATE_TIME_FORMATTER.format((Instant) value)).append('\'');
        } else if (value instanceof UUID) {
            builder.append("guid'").append(value).append('\'');
        } else if (value instanceof String) {
            builder.append('\'').append(value).append('\'');
        } else if (value instanceof Double) {
            builder.append(DECIMAL_FORMAT.format(value));
        } else {
            builder.append(value);
        }
        return builder.toString();
    }

    public Field getField() {
        return field;
    }

    public RelationalOperator getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    public String getComment() {
        return comment;
    }

    /**
     * Из строкового выражения функции приведения cast(expr, type) выделяет параметры 'expr' и 'type'
     * Если параметр 'expr' имеет вид guid'value' или datetime'value', префиксы guid и datetime отбрасываются.
     * Кавычки отбрасываются.
     * <p>
     * https://its.1c.ru/db/v839doc#bookmark:dev:TI000001757
     *
     * @param value Строковое выражение функции cast(expr, type).
     * @return Соответствие параметров expr и type их значениям.
     * @throws ParseException если не удалось разобрать выражение Cast
     */
    public static Map<String, String> parseCompositeFieldValue(String value) throws ParseException {
        if (value == null) {
            throw new IllegalArgumentException("Параметр 'value' равен null");
        }
        if (!CAST_FUNCTION_PATTERN.matcher(value).matches()) {
            throw new ParseException("Недопустимый формат значения реквизита составного типа: " + value, 0);
        }
        String exprParam = value.substring(5, value.indexOf(',')).replaceAll("['\"]", "").replaceAll("guid", "").replaceAll("datetime", "").trim();
        String typeParam = value.substring(value.indexOf(',') + 1, value.indexOf(')')).replaceAll("['\"]", "").trim();

        Map<String, String> result = new HashMap<>();
        result.put("expr", exprParam);
        result.put("type", typeParam);

        return result;
    }

    @Override
    public RelationalExpression clone() {
        try {
            return (RelationalExpression) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }

    }
}
