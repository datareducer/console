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

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

import java.io.IOException;

public class MainMenu extends MenuBar {
    //Меню "Файл"
    @FXML
    private Menu fileMenu;
    @FXML
    private MenuItem newConfItem;
    @FXML
    private MenuItem openConfItem;
    @FXML
    private MenuItem saveConfItem;
    @FXML
    private MenuItem webGetConfItem;
    @FXML
    private MenuItem webPutConfItem;
    @FXML
    private MenuItem exitItem;

    // Меню "Правка"
    @FXML
    private Menu editMenu;
    @FXML
    private MenuItem addInfoBaseItem;
    @FXML
    private MenuItem addScriptItem;
    @FXML
    private MenuItem optionsItem;

    // Меню "Инструменты"
    @FXML
    private Menu toolsMenu;
    @FXML
    private MenuItem checkRserveItem;
    @FXML
    private MenuItem clipboardCodeItem;

    // Меню "Справка"
    @FXML
    private Menu helpMenu;
    @FXML
    private MenuItem docsItem;
    @FXML
    private MenuItem eulaItem;
    @FXML
    private MenuItem aboutItem;

    public MainMenu() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("fxml/MainMenu.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Меню "Файл"
    MenuItem getNewConfItem() {
        return newConfItem;
    }

    MenuItem getOpenConfItem() {
        return openConfItem;
    }

    MenuItem getSaveConfItem() {
        return saveConfItem;
    }

    MenuItem getWebGetConfItem() {
        return webGetConfItem;
    }

    MenuItem getWebPutConfItem() {
        return webPutConfItem;
    }

    MenuItem getExitItem() {
        return exitItem;
    }

    // Меню "Правка"
    MenuItem getAddInfoBaseItem() {
        return addInfoBaseItem;
    }

    MenuItem getAddScriptItem() {
        return addScriptItem;
    }

    MenuItem getOptionsItem() {
        return optionsItem;
    }

    // Меню "Инструменты"
    Menu getToolsMenu() {
        return toolsMenu;
    }

    MenuItem getCheckRserveItem() {
        return checkRserveItem;
    }

    MenuItem getClipboardCodeItem() {
        return clipboardCodeItem;
    }

    // Меню "Справка"
    MenuItem getDocsItem() {
        return docsItem;
    }

    MenuItem getEulaItem() {
        return eulaItem;
    }

    MenuItem getAboutItem() {
        return aboutItem;
    }

}
