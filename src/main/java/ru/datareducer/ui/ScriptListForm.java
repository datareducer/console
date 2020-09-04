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
import ru.datareducer.model.ReducerConfiguration;
import ru.datareducer.model.Script;

import java.io.IOException;

public class ScriptListForm extends VBox implements ModelReplacedListener {
    @FXML
    private TableView<Script> scriptTable;
    @FXML
    private TableColumn<Script, String> idCol;
    @FXML
    private TableColumn<Script, String> nameCol;

    // Тулбар
    @FXML
    private Button addBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button openBtn;

    // Контекстное меню
    @FXML
    private ContextMenu ctxMenu;
    @FXML
    private MenuItem openItem;
    @FXML
    private MenuItem deleteItem;

    public ScriptListForm() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("fxml/ScriptListForm.fxml"));
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
    }

    @Override
    public void acceptModel(ReducerConfiguration model) {
        scriptTable.setItems(model.getScripts());
        scriptTable.refresh();
    }

    Script getSelectedItem() {
        return scriptTable.getSelectionModel().getSelectedItem();
    }

    TableView<Script> getScriptTable() {
        return scriptTable;
    }

    Button getAddBtn() {
        return addBtn;
    }

    Button getDeleteBtn() {
        return deleteBtn;
    }

    Button getOpenBtn() {
        return openBtn;
    }

    ContextMenu getCtxMenu() {
        return ctxMenu;
    }

    MenuItem getOpenItem() {
        return openItem;
    }

    MenuItem getDeleteItem() {
        return deleteItem;
    }
}
