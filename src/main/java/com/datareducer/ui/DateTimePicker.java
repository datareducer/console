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
package com.datareducer.ui;

import com.datareducer.dataservice.client.DataServiceClient;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.datareducer.model.ScriptParameter.PARAM_PATTERN;

/**
 * Хакнутый DatePicker.
 * <p>
 * Добавлена возможность ввода времени. DatePicker - потомок класса, параметризуемого типом LocalDate (а не LocalDateTime).
 * Это не позволяет стандартным способом ввести в ячейку время;
 * Добавлена возможность ввода имени параметра вместо значения.
 * <p>
 * После создания объекта должен быть вызван метод setStringValue() для инициализации значения DatePicker.
 *
 * @author Kirill Mikhaylov
 */
public class DateTimePicker extends DatePicker {
    private final StringProperty stringValue = new SimpleStringProperty();

    private LocalDateTime dateTimeValue;
    private String parameterName;

    private final DateTimeFormatter dateFormatter = DataServiceClient.DATE_TIME_FORMATTER;

    // Признак пустого поля
    private boolean isEmpty;
    // Признак ввода параметра вместо значения
    private boolean isParam;

    public DateTimePicker() {
        super();
        initialize();
    }

    /*
        3 случая установки значений:
        - через метод setStringValue(): setStringValue() -> StringConverter#toString
        - с помощью календаря: StringConverter#toString
        - ручной ввод в поле: StringConverter#fromString -> StringConverter#toString
     */
    private void initialize() {
        setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate value) {
                if (value == null || isEmpty) {
                    isEmpty = false;
                    return "";
                } else if (isParam) {
                    isParam = false;
                    return parameterName;
                } else if (dateTimeValue != null && value.equals(dateTimeValue.toLocalDate())) {
                    return dateTimeValue.format(dateFormatter);
                } else {
                    // Если выбрали другую дату - обнуляем время.
                    dateTimeValue = value.atStartOfDay();
                    String s = dateTimeValue.format(dateFormatter);
                    stringValue.setValue(s);
                    return s;
                }
            }

            @Override
            public LocalDate fromString(String value) {
                if (value.isEmpty()) {
                    dateTimeValue = null;
                    parameterName = null;
                    stringValue.setValue(null);
                    // Не можем возвратить null, в противном случае в поле нельзя будет ввести имя параметра
                    // (DatePicker всегда отображает пустое поле, если его основное значение равно null).
                    isEmpty = true;
                    return getValue();
                } else if (PARAM_PATTERN.matcher(value).matches()) {
                    dateTimeValue = null;
                    parameterName = value;
                    stringValue.setValue(value);
                    isParam = true;
                    // Не можем возвратить null
                    return getValue();
                } else {
                    LocalDateTime ldt = LocalDateTime.parse(value, dateFormatter);
                    dateTimeValue = ldt;
                    parameterName = null;
                    stringValue.setValue(value);
                    return ldt.toLocalDate();
                }
            }
        });
    }

    void setStringValue(String value) {
        if (value == null) {
            dateTimeValue = null;
            parameterName = null;
            // Основное значение DatePicker должно быть заполнено, чтобы можно было ввести имя параметра
            isEmpty = true;
            setValue(LocalDate.now());
        } else if (PARAM_PATTERN.matcher(value).matches()) {
            dateTimeValue = null;
            parameterName = value;
            stringValue.setValue(value);
            isParam = true;
            setValue(LocalDate.now());
        } else {
            LocalDateTime ldt = LocalDateTime.parse(value, dateFormatter);
            dateTimeValue = ldt;
            parameterName = null;
            setValue(ldt.toLocalDate());
        }
    }

    String getStringValue() {
        return stringValue.get();
    }

    StringProperty stringValueProperty() {
        return stringValue;
    }

}
