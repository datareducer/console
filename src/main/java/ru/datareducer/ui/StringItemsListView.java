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

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Форма для ввода списка имён полей.
 *
 * @author Kirill Mikhaylov
 */
public class StringItemsListView extends VBox {
    @FXML
    private Button addBtn;
    @FXML
    private Button upBtn;
    @FXML
    private Button downBtn;
    @FXML
    private Button delBtn;
    @FXML
    private ListView<String> itemsList;

    public StringItemsListView() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("fxml/StringItemsListView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    StringItemsListView(ObservableList<String> items) {
        this();
        initialize(items);
    }

    void initialize(ObservableList<String> items) {
        itemsList.setItems(items);
        itemsList.setEditable(true);
        itemsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        itemsList.setCellFactory(TextFieldListCell.forListView());

        registerEventHandlers();
    }

    private void registerEventHandlers() {
        MultipleSelectionModel<String> selectionModel = itemsList.getSelectionModel();

        addBtn.setOnAction(e -> itemsList.getItems().add(""));

        upBtn.setOnAction(e -> {
            String sel = selectionModel.getSelectedItem();
            if (sel == null) {
                return;
            }
            int index = selectionModel.getSelectedIndex();
            if (index != 0) {
                itemsList.getItems().remove(sel);
                itemsList.getItems().add(index - 1, sel);
                selectionModel.clearSelection();
                selectionModel.select(sel);
            }
        });

        downBtn.setOnAction(e -> {
            String sel = selectionModel.getSelectedItem();
            if (sel == null) {
                return;
            }
            int index = selectionModel.getSelectedIndex();
            if (index != itemsList.getItems().size() - 1) {
                itemsList.getItems().remove(sel);
                itemsList.getItems().add(index + 1, sel);
                selectionModel.clearSelection();
                selectionModel.select(sel);
            }
        });

        delBtn.setOnAction(e -> {
            for (String item : getSelectedItems()) {
                itemsList.getItems().remove(item);
            }
        });
    }

    private List<String> getSelectedItems() {
        return new ArrayList<>(itemsList.getSelectionModel().getSelectedItems());
    }
}
