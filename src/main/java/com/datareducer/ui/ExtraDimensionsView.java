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

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Форма уникальных идентификаторов видов субконто или корреспондирующих видов субконто
 * запроса к виртуальной таблице регистра бухгалтерии.
 *
 * @author Kirill Mikhaylov
 */
public class ExtraDimensionsView extends VBox {
    @FXML
    private Button addBtn;
    @FXML
    private Button upBtn;
    @FXML
    private Button downBtn;
    @FXML
    private Button delBtn;
    @FXML
    private ListView<UUID> dimensionsList;

    public ExtraDimensionsView() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("fxml/ExtraDimensionsView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    ExtraDimensionsView(ObservableList<UUID> items) {
        this();
        initialize(items);
    }

    void initialize(ObservableList<UUID> items) {
        dimensionsList.setItems(items);
        dimensionsList.setEditable(true);
        dimensionsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        dimensionsList.setCellFactory(param -> new TextFieldListCell<UUID>() {
            private TextField textField;

            private StringConverter<UUID> uuidFormatter = new StringConverter<UUID>() {
                @Override
                public String toString(UUID object) {
                    if (object == null) {
                        return null;
                    }
                    return object.toString();
                }

                @Override
                public UUID fromString(String string) {
                    if (string == null) {
                        throw new IllegalArgumentException();
                    }
                    return UUID.fromString(string);
                }
            };

            @Override
            public void startEdit() {
                super.startEdit();
                if (textField == null) {
                    createTextField();
                }
                setGraphic(textField);
                if (getItem() != null) {
                    textField.setText(uuidFormatter.toString(getItem()));
                }
                textField.selectAll();
                textField.requestFocus();
            }

            private TextField createTextField() {
                textField = new TextField();

                // При нажатии Enter
                textField.setOnAction(e -> {
                    try {
                        UUID value = uuidFormatter.fromString(textField.getText());
                        if (value != null) {
                            commitEdit(value);
                        } else {
                            cancelEdit();
                        }
                    } catch (IllegalArgumentException ex) {
                        cancelEdit();
                    }
                    e.consume();
                });
                return textField;
            }
        });

        attachEventHandlers();
    }

    private void attachEventHandlers() {
        MultipleSelectionModel<UUID> selectionModel = dimensionsList.getSelectionModel();

        addBtn.setOnAction(e -> dimensionsList.getItems().add(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        upBtn.setOnAction(e -> {
            UUID sel = selectionModel.getSelectedItem();
            if (sel == null) {
                return;
            }
            int index = selectionModel.getSelectedIndex();
            if (index != 0) {
                dimensionsList.getItems().remove(sel);
                dimensionsList.getItems().add(index - 1, sel);
                selectionModel.clearSelection();
                selectionModel.select(sel);
            }
        });

        downBtn.setOnAction(e -> {
            UUID sel = selectionModel.getSelectedItem();
            if (sel == null) {
                return;
            }
            int index = selectionModel.getSelectedIndex();
            if (index != dimensionsList.getItems().size() - 1) {
                dimensionsList.getItems().remove(sel);
                dimensionsList.getItems().add(index + 1, sel);
                selectionModel.clearSelection();
                selectionModel.select(sel);
            }
        });

        delBtn.setOnAction(e -> {
            for (UUID uuid : getSelectedItems()) {
                dimensionsList.getItems().remove(uuid);
            }
        });
    }

    private List<UUID> getSelectedItems() {
        return new ArrayList<>(dimensionsList.getSelectionModel().getSelectedItems());
    }

}
