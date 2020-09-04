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

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import ru.datareducer.model.InfoBase;
import ru.datareducer.model.ReducerConfiguration;

import java.io.IOException;

public class InfoBaseListForm extends VBox implements ModelReplacedListener {
    @FXML
    private TableView<InfoBase> basesList;
    @FXML
    private TableColumn<InfoBase, String> idCol;
    @FXML
    private TableColumn<InfoBase, String> nameCol;
    @FXML
    private TableColumn<InfoBase, String> hostCol;
    @FXML
    private TableColumn<InfoBase, String> baseCol;

    // Тулбар
    @FXML
    private Button addBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button editBtn;

    // Контекстное меню
    @FXML
    private ContextMenu ctxMenu;
    @FXML
    private MenuItem editItem;
    @FXML
    private MenuItem deleteItem;
    @FXML
    private MenuItem loadMetadataItem;

    public InfoBaseListForm() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("fxml/InfoBaseListForm.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        hostCol.setCellValueFactory(new PropertyValueFactory<>("host"));
        baseCol.setCellValueFactory(new PropertyValueFactory<>("base"));
    }

    @Override
    public void acceptModel(ReducerConfiguration model) {
        basesList.setItems(model.getInfoBases());
        basesList.refresh();
    }

    InfoBase getSelectedItem() {
        return basesList.getSelectionModel().getSelectedItem();
    }

    TableView<InfoBase> getBasesList() {
        return basesList;
    }

    Button getAddBtn() {
        return addBtn;
    }

    Button getDeleteBtn() {
        return deleteBtn;
    }

    Button getEditBtn() {
        return editBtn;
    }

    ContextMenu getCtxMenu() {
        return ctxMenu;
    }

    MenuItem getEditItem() {
        return editItem;
    }

    MenuItem getDeleteItem() {
        return deleteItem;
    }

    MenuItem getLoadMetadataItem() {
        return loadMetadataItem;
    }
}
