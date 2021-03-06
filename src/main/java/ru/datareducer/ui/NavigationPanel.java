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
import javafx.scene.layout.VBox;

import java.io.IOException;

public class NavigationPanel extends VBox {
    @FXML
    private InfoBaseListForm infoBaseListForm;
    @FXML
    private ScriptListForm scriptListForm;
    @FXML
    private MetadataForm metadataForm;

    public NavigationPanel() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("fxml/NavigationPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    InfoBaseListForm getInfoBaseListForm() {
        return infoBaseListForm;
    }

    ScriptListForm getScriptListForm() {
        return scriptListForm;
    }

    MetadataForm getMetadataForm() {
        return metadataForm;
    }
}
