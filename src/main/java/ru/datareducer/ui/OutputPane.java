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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import ru.datareducer.LogAreaAppender;
import ru.datareducer.model.ReducerConfiguration;

import java.io.IOException;

public class OutputPane extends VBox implements ModelReplacedListener {
    @FXML
    private TextArea logArea;
    @FXML
    private TextArea outputArea;
    @FXML
    private TabPane outputTabPane;
    @FXML
    private Tab logAreaTab;
    @FXML
    private Tab outputAreaTab;

    public OutputPane() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("fxml/OutputPane.fxml"));
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
        LogAreaAppender.setLogArea(logArea);
        logArea.textProperty().addListener((observable, oldValue, newValue) -> outputTabPane.getSelectionModel().select(logAreaTab));
        outputArea.textProperty().addListener((observable, oldValue, newValue) -> outputTabPane.getSelectionModel().select(outputAreaTab));
    }

    @Override
    public void acceptModel(ReducerConfiguration model) {
        logArea.clear();
        outputArea.clear();
    }

    public TextArea getLogArea() {
        return logArea;
    }

    public TextArea getOutputArea() {
        return outputArea;
    }

}
