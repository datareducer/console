/*
 * Copyright (c) 2017-2020 Kirill Mikhaylov <admin@datareducer.ru>
 *
 * Этот файл — часть программы DataReducer Console <http://datareducer.ru>.
 *
 * Программа DataReducer Console является свободным программным обеспечением.
 * Вы вправе распространять ее и/или модифицировать в соответствии с условиями
 * версии 3 либо, по вашему выбору, с условиями более поздней версии
 * Стандартной Общественной Лицензии GNU, опубликованной Free Software Foundation.
 *
 * Программа DataReducer Console распространяется в надежде, что она будет полезной,
 * но БЕЗО ВСЯКИХ ГАРАНТИЙ, в том числе ГАРАНТИИ ТОВАРНОГО СОСТОЯНИЯ ПРИ ПРОДАЖЕ
 * и ПРИГОДНОСТИ ДЛЯ ИСПОЛЬЗОВАНИЯ В КОНКРЕТНЫХ ЦЕЛЯХ.
 * Подробнее см. в Стандартной Общественной Лицензии GNU.
 *
 * Вы должны были получить копию Стандартной Общественной Лицензии GNU
 * вместе с этой программой. Если это не так, см. <https://www.gnu.org/licenses/>.
 */

package ru.datareducer.ui;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import ru.datareducer.dataservice.entity.Field;
import ru.datareducer.dataservice.entity.RelationalExpression;
import ru.datareducer.model.BooleanExpressionToken;
import ru.datareducer.model.LogicalOperatorWrapper;

import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static ru.datareducer.dataservice.entity.FieldType.BOOLEAN;
import static ru.datareducer.dataservice.entity.FieldType.GUID;
import static ru.datareducer.model.ScriptParameter.PARAM_PATTERN;

/**
 * Ячейка таблицы отбора данных ресурсов 1С.
 *
 * @author Kirill Mikhaylov
 */
public class RelationalExpressionValueCell extends TreeTableCell<BooleanExpressionToken, String> {
    private DateTimePicker datePicker;
    //private CheckBox checkBox;
    private TextField textField;
    private CompositeFieldBox compositeFieldBox;

    private DecimalFormat decimalFormat;

    @Override
    public void startEdit() {
        setEditable(!(getTreeTableRow().getItem() instanceof LogicalOperatorWrapper) && getTreeTableRow().getItem().getField() != null);
        if (!isEditable() || !getTreeTableView().isEditable() || !getTableColumn().isEditable()) {
            return;
        }
        super.startEdit();
        Field f = getTreeTableRow().getItem().getField();
        if (f != null) {
            switch (f.getFieldType()) {
                case DATETIME: {
                    if (datePicker == null) {
                        createDatePicker();
                    }
                    setGraphic(datePicker);
                    break;
                }
//                case BOOLEAN: {
//                    if (checkBox == null) {
//                        createCheckBox();
//                    }
//                    setGraphic(checkBox);
//                    break;
//                }
                case DOUBLE: {
                    if (textField == null) {
                        createTextField();
                    }
                    if (decimalFormat == null) {
                        decimalFormat = RelationalExpression.DECIMAL_FORMAT;
                    }
                    setGraphic(textField);
                    break;
                }
                case LONG: {
                    if (textField == null) {
                        createTextField();
                    }
                    if (decimalFormat == null) {
                        decimalFormat = new DecimalFormat("#");
                    }
                    setGraphic(textField);
                    break;
                }
                case SHORT: {
                    if (textField == null) {
                        createTextField();
                    }
                    if (decimalFormat == null) {
                        decimalFormat = new DecimalFormat("#");
                    }
                    setGraphic(textField);
                    break;
                }
                case STRING: {
                    if (f.isComposite()) {
                        if (compositeFieldBox == null) {
                            createCompositeFieldBox();
                        }
                        setGraphic(compositeFieldBox);
                    } else {
                        if (textField == null) {
                            createTextField();
                        }
                        setGraphic(textField);
                    }
                    break;
                }
                default: {
                    if (textField == null) {
                        createTextField();
                    }
                    setGraphic(textField);
                }
            }
        }
        setText(null);
        if (textField != null) {
            if (getItem() != null) {
                textField.setText(getItem());
            }
            textField.selectAll();
            textField.requestFocus();
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        if (textField != null) {
            // При выходе из режима редактирования поля без нажания Enter возвращаем значение поля к тому,
            // которое было до начала редактирования.
            textField.setText(getItem());
        }
        setGraphic(null);
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
        } else {
            setText(item);
        }
        setGraphic(null);
    }

    private void createTextField() {
        textField = new TextField();

        // При нажатии Enter
        textField.setOnAction(e -> {
            try {
                Object value = textField.getTextFormatter().getValueConverter().fromString(textField.getText());
                if (value != null) {
                    commitEdit(value.toString());
                } else {
                    cancelEdit();
                }
            } catch (IllegalArgumentException ex) {
                cancelEdit();
            }
            e.consume();
        });

        // При нажатии Escape
        textField.setOnKeyReleased(t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
                t.consume();
            }
        });

        // Форматтер для типа UUID. Позволяет вводить в ячейку имя параметра.
        StringConverter<Object> uuidFormatter = new StringConverter<Object>() {
            @Override
            public String toString(Object object) {
                if (object == null) {
                    return null;
                }
                return object.toString();
            }

            @Override
            public Object fromString(String string) {
                if (string == null) {
                    throw new IllegalArgumentException();
                }
                if (PARAM_PATTERN.matcher(string).matches()) {
                    return string;
                }
                return UUID.fromString(string);
            }
        };

        // Форматтер для типа Boolean. Позволяет вводить в ячейку имя параметра.
        StringConverter<Object> booleanFormatter = new StringConverter<Object>() {
            @Override
            public String toString(Object object) {
                if (object == null) {
                    return null;
                }
                return object.toString();
            }

            @Override
            public Object fromString(String string) {
                if (string == null) {
                    throw new IllegalArgumentException();
                }
                if (PARAM_PATTERN.matcher(string).matches()) {
                    return string;
                }
                return Boolean.parseBoolean(string);
            }
        };

        // Проверяем вводимый пользователем текст на соответствие типу поля
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText(); // Текст ячейки после редактирования
            if (newText.isEmpty()) {
                return change;
            }
            if (PARAM_PATTERN.matcher(newText).matches()) {
                return change;
            }
            switch (getTreeTableRow().getItem().getField().getFieldType()) {
                case DOUBLE: {
                    ParsePosition parsePosition = new ParsePosition(0);
                    Number num = decimalFormat.parse(newText, parsePosition);
                    if (num == null || parsePosition.getIndex() < newText.length()) {
                        return null;
                    } else {
                        return change;
                    }
                }
                case LONG: {
                    ParsePosition parsePosition = new ParsePosition(0);
                    Number num = decimalFormat.parse(newText, parsePosition);
                    if (num == null || parsePosition.getIndex() < newText.length()
                            || num.longValue() >= Long.MAX_VALUE || num.longValue() <= Long.MIN_VALUE) {
                        return null;
                    } else {
                        return change;
                    }
                }
                case SHORT: {
                    ParsePosition parsePosition = new ParsePosition(0);
                    Number num = decimalFormat.parse(newText, parsePosition);
                    if (num == null || parsePosition.getIndex() < newText.length()
                            || num.longValue() > Short.MAX_VALUE || num.longValue() < Short.MIN_VALUE) {
                        return null;
                    } else {
                        return change;
                    }
                }
                default: {
                    return change;
                }
            }
        };

        if (getTreeTableRow().getItem().getField().getFieldType() == GUID) {
            textField.setTextFormatter(new TextFormatter<>(uuidFormatter, UUID.fromString("00000000-0000-0000-0000-000000000000"), filter));
        } else if (getTreeTableRow().getItem().getField().getFieldType() == BOOLEAN) {
            textField.setTextFormatter(new TextFormatter<>(booleanFormatter, false, filter));
        } else {
            textField.setTextFormatter(new TextFormatter<>(new DefaultStringConverter(), "", filter));
        }
    }

    private void createCompositeFieldBox() {
        compositeFieldBox = new CompositeFieldBox(getItem());
        compositeFieldBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                commitEdit(null);
            } else {
                commitEdit(compositeFieldBox.getValue());
            }
        });
        // Проверяем, что вводимый в ячейку текст соответствует формату функии Cast().
        compositeFieldBox.getEditor().setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText(); // Текст ячейки после редактирования
            if (newText.isEmpty()) {
                return change;
            }
            if (PARAM_PATTERN.matcher(newText).matches()) {
                return change;
            }
            if (!RelationalExpression.CAST_FUNCTION_PATTERN.matcher(newText).matches()) {
                return null;
            }
            return change;
        }));

        compositeFieldBox.setPrefWidth(getWidth() - getGraphicTextGap() * 2);
    }

//    private void createCheckBox() {
//        checkBox = new CheckBox();
//        // Устанавливаем текущее значение CheckBox
//        if (getItem() != null) {
//            checkBox.setSelected(getItem().equals("true"));
//        }
//        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> commitEdit(newValue.toString()));
//    }


    private void createDatePicker() {
        datePicker = new DateTimePicker();
        // Устанавливаем текущее значение DatePicker
        datePicker.setStringValue(getItem());

        datePicker.stringValueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                commitEdit(null);
            } else {
                commitEdit(datePicker.getStringValue());
            }
        });

        datePicker.setPrefWidth(getWidth() - getGraphicTextGap() * 2);
        datePicker.setEditable(true);
    }
}
