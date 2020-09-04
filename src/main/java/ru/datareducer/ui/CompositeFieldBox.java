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
package ru.datareducer.ui;

import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.AccessibleRole;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import ru.datareducer.dataservice.entity.RelationalExpression;
import ru.datareducer.model.ScriptParameter;

import java.text.ParseException;
import java.util.Map;

/**
 * Элемент ввода значения и типа значения составного реквизита в поле значения
 * логического выражения отбора данных формы ресурса 1С.
 *
 * @author Kirill Mikhaylov
 */
public class CompositeFieldBox extends ComboBoxBase<String> {
    // Значение составного реквизита
    private final StringProperty fieldValue = new SimpleStringProperty("");
    // Тип значения составного реквизита
    private final StringProperty fieldValueType = new SimpleStringProperty("");

    private ReadOnlyObjectWrapper<TextField> editor;

    public CompositeFieldBox(String value) {
        setAccessibleRole(AccessibleRole.COMBO_BOX);
        getStyleClass().add("combo-box-base");
        if (value != null) {
            Map<String, String> castParams;
            try {
                castParams = RelationalExpression.parseCompositeFieldValue(value);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
            setFieldValue(castParams.get("expr"));
            setFieldValueType(castParams.get("type"));
        }
        setValue(value);
        setEditable(true);
        registerEventHandlers();
    }

    private void registerEventHandlers() {
        getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty() || ScriptParameter.PARAM_PATTERN.matcher(newValue).matches()) {
                setFieldValue("");
                setFieldValueType("");
            } else {
                Map<String, String> castParams;
                try {
                    castParams = RelationalExpression.parseCompositeFieldValue(newValue);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return;
                }
                setFieldValue(castParams.get("expr"));
                setFieldValueType(castParams.get("type"));
            }
        });
    }

    public final TextField getEditor() {
        return editorProperty().get();
    }

    public final ReadOnlyObjectProperty<TextField> editorProperty() {
        if (editor == null) {
            editor = new ReadOnlyObjectWrapper<>(this, "editor");
            editor.set(new ComboBoxListViewSkin.FakeFocusTextField());
        }
        return editor.getReadOnlyProperty();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new CompositeFieldBoxSkin(this);
    }

    public String getFieldValue() {
        return fieldValue.get();
    }

    public StringProperty fieldValueProperty() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue.set(fieldValue);
    }

    public String getFieldValueType() {
        return fieldValueType.get();
    }

    public StringProperty fieldValueTypeProperty() {
        return fieldValueType;
    }

    public void setFieldValueType(String fieldValueType) {
        this.fieldValueType.set(fieldValueType);
    }
}
