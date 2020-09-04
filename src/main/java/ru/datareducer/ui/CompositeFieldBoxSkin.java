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

import com.sun.javafx.scene.control.skin.ComboBoxPopupControl;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import ru.datareducer.dataservice.entity.*;

public class CompositeFieldBoxSkin extends ComboBoxPopupControl<String> {
    private final CompositeFieldBox compositeFieldBox;

    private TextField displayNode;
    private CompositeFieldBoxContent compositeFieldBoxContent;

    public CompositeFieldBoxSkin(CompositeFieldBox compositeFieldBox) {
        super(compositeFieldBox, new CompositeFieldBoxBehavior(compositeFieldBox));
        this.compositeFieldBox = compositeFieldBox;
    }

    @Override
    protected Node getPopupContent() {
        if (compositeFieldBoxContent == null) {
            compositeFieldBoxContent = new CompositeFieldBoxContent();
        }
        return compositeFieldBoxContent;
    }

    /*
     * Метод переопределён для обхода ошибки, возникающей при нажатии Alt+Shift
     * в поле CompositeFieldBoxContent#fieldValueFld или CompositeFieldBoxContent#fieldValueTypeFld.
     * При этом все элементы ввода колонки становятся недоступными для редактирования до закрытия окна.
     * (Java 1.8.0_112, Linux)
     */
    @Override
    protected PopupControl getPopup() {
        popup = super.getPopup();
        popup.setAutoHide(false);
        return popup;
    }

    @Override
    protected TextField getEditor() {
        return ((CompositeFieldBox) getSkinnable()).getEditor();
    }

    @Override
    protected StringConverter<String> getConverter() {
        return new DefaultStringConverter();
    }

    @Override
    public Node getDisplayNode() {
        if (displayNode == null) {
            displayNode = getEditableInputNode();
            updateDisplayNode();
        }
        return displayNode;
    }

    private class CompositeFieldBoxContent extends GridPane {
        private TextField fieldValueFld;
        private TextField fieldValueTypeFld;
        private Button okBtn;

        CompositeFieldBoxContent() {
            initialize();
            registerEventHandlers();
        }

        private void initialize() {
            fieldValueFld = new TextField();
            fieldValueTypeFld = new TextField();

            Label fieldValueLbl = new Label("Значение:");
            Label fieldValueTypeLbl = new Label("Тип:");

            ButtonBar buttonBar = new ButtonBar();
            okBtn = new Button("Ok");
            okBtn.setDefaultButton(true);
            ButtonBar.setButtonData(okBtn, ButtonBar.ButtonData.OK_DONE);
            buttonBar.getButtons().add(okBtn);

            setConstraints(fieldValueLbl, 0, 0); // (c0, r0)
            setConstraints(fieldValueTypeLbl, 0, 1);
            setConstraints(fieldValueFld, 1, 0);
            setConstraints(fieldValueTypeFld, 1, 1);
            setConstraints(buttonBar, 0, 2);

            getChildren().addAll(fieldValueLbl, fieldValueTypeLbl, fieldValueFld, fieldValueTypeFld, buttonBar);

            GridPane.setColumnSpan(buttonBar, 2);

            ColumnConstraints col0 = new ColumnConstraints();
            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPrefWidth(300);
            getColumnConstraints().addAll(col0, col1);

            setPadding(new Insets(5, 5, 5, 5));
            setVgap(4);
            setHgap(7);

            setStyle("-fx-background-color:GAINSBORO; -fx-border-color:SILVER");

            fieldValueFld.textProperty().bindBidirectional(compositeFieldBox.fieldValueProperty());
            fieldValueTypeFld.textProperty().bindBidirectional(compositeFieldBox.fieldValueTypeProperty());
        }

        private void registerEventHandlers() {
            okBtn.setOnAction(event -> {
                String fieldValue = compositeFieldBox.getFieldValue();
                String fieldValueType = compositeFieldBox.getFieldValueType();

                fieldValueType = fieldValueType.replace("StandardODATA.", "");

                boolean isDate = compositeFieldBox.getFieldValueType().equals("Date");
                boolean isReferenceType = fieldValueType.startsWith(Catalog.RESOURCE_PREFIX)
                        || fieldValueType.startsWith(Document.RESOURCE_PREFIX)
                        || fieldValueType.startsWith(ChartOfCharacteristicTypes.RESOURCE_PREFIX)
                        || fieldValueType.startsWith(ChartOfAccounts.RESOURCE_PREFIX)
                        || fieldValueType.startsWith(ChartOfCalculationTypes.RESOURCE_PREFIX)
                        || fieldValueType.startsWith(ExchangePlan.RESOURCE_PREFIX)
                        || fieldValueType.startsWith(BusinessProcess.RESOURCE_PREFIX)
                        || fieldValueType.startsWith(Task.RESOURCE_PREFIX);

                String funcFormat;
                if (isReferenceType) {
                    funcFormat = "cast(guid'%s', '%s')";
                } else if (isDate) {
                    funcFormat = "cast(datetime'%s', '%s')";
                } else {
                    funcFormat = "cast('%s', '%s')";
                }
                String value = String.format(funcFormat, fieldValue, fieldValueType);
                compositeFieldBox.setValue(value);
                compositeFieldBox.getEditor().setText(value);
                compositeFieldBox.hide();
            });

        }
    }
}
