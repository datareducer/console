/*
 * Copyright (c) 2017-2020 Kirill Mikhaylov <admin@datareducer.ru>
 *
 * Этот файл — часть программы DataReducer <http://datareducer.ru>.
 *
 * Программа DataReducer является свободным программным обеспечением.
 * Вы вправе распространять ее и/или модифицировать в соответствии с условиями версии 2
 * либо, по вашему выбору, с условиями более поздней версии
 * Стандартной Общественной Лицензии GNU, опубликованной Free Software Foundation.
 *
 * Программа DataReducer распространяется в надежде, что она будет полезной,
 * но БЕЗО ВСЯКИХ ГАРАНТИЙ, в том числе ГАРАНТИИ ТОВАРНОГО СОСТОЯНИЯ ПРИ ПРОДАЖЕ
 * и ПРИГОДНОСТИ ДЛЯ ИСПОЛЬЗОВАНИЯ В КОНКРЕТНЫХ ЦЕЛЯХ.
 * Подробнее см. в Стандартной Общественной Лицензии GNU.
 *
 * Вы должны были получить копию Стандартной Общественной Лицензии GNU
 * вместе с этой программой. Если это не так, см. <https://www.gnu.org/licenses/>.
 */
package com.datareducer.ui;

import com.datareducer.model.InfoBase;
import com.datareducer.model.ReducerConfiguration;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class InfoBaseWindow implements Window<InfoBase> {
    private final InfoBase infoBase;
    private final ReducerConfiguration model;

    // Это форма создания новых настроек подключения.
    private final boolean isCreation;

    private final Stage stage;
    private final InfoBaseForm form;

    private final Stage primaryStage;

    public InfoBaseWindow(InfoBase infoBase, ReducerConfiguration model, Stage primaryStage) {
        if (infoBase == null) {
            throw new IllegalArgumentException("Значение параметра 'infoBase': null");
        }
        if (model == null) {
            throw new IllegalArgumentException("Значение параметра 'model': null");
        }

        this.model = model;
        this.infoBase = infoBase;
        this.isCreation = !model.getInfoBases().contains(infoBase);
        this.stage = new Stage();
        this.form = new InfoBaseForm();
        this.primaryStage = primaryStage;

        initialize();
    }

    private void initialize() {
        stage.setScene(new Scene(form));
        stage.initOwner(primaryStage);
        stage.setTitle("Настройки подключения к 1С");
        stage.setResizable(false);

        registerEventHandlers();
    }

    @Override
    public InfoBase getEntity() {
        return infoBase;
    }

    @Override
    public void show() {
        stage.toFront();
        stage.show();
        placeWindow();
    }

    @Override
    public void close() {
        stage.close();
    }

    private class InfoBaseForm extends GridPane {
        @FXML
        private TextField nameFld;
        @FXML
        private TextField hostFld;
        @FXML
        private TextField baseFld;
        @FXML
        private TextField userFld;
        @FXML
        private PasswordField passFld;
        @FXML
        private Button cancelBtn;
        @FXML
        private Button closeBtn;

        private Alert alert;

        InfoBaseForm() {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("fxml/InfoBaseForm.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            try {
                loader.load();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @FXML
        private void initialize() {
            cancelBtn.setVisible(isCreation);
            closeBtn.setText(isCreation ? "Создать" : "Закрыть");

            nameFld.textProperty().bindBidirectional(infoBase.nameProperty());
            hostFld.textProperty().bindBidirectional(infoBase.hostProperty());
            baseFld.textProperty().bindBidirectional(infoBase.baseProperty());
            userFld.textProperty().bindBidirectional(infoBase.userProperty());
            passFld.textProperty().bindBidirectional(infoBase.passwordProperty());
        }

        private boolean validate() {
            if (nameFld.getLength() == 0) {
                showError("Наименование подключения не задано");
                return false;
            }
            if (hostFld.getLength() == 0) {
                showError("Сервер не задан");
                return false;
            }
            if (baseFld.getLength() == 0) {
                showError("Имя информационной базы не задано");
                return false;
            }
            if (userFld.getLength() == 0) {
                showError("Пользователь не задан");
                return false;
            }
            if (passFld.getLength() == 0) {
                showError("Пароль не задан");
                return false;
            }
            return true;
        }

        private void showError(String message) {
            if (alert == null) {
                alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(stage);
            }
            alert.setTitle(stage.getTitle());
            alert.setHeaderText("Ошибка");
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    private void registerEventHandlers() {
        // При интерактивном закрытии окна
        stage.setOnCloseRequest(c -> {
            if (!isCreation && !form.validate()) {
                c.consume();
            } else {
                close();
            }
        });

        // Кнопка "Закрыть/Создать"
        form.closeBtn.setOnAction(event -> {
            if (form.validate()) {
                if (isCreation) {
                    infoBase.setId(String.valueOf(model.getInfoBaseSequence().incrementAndGet()));
                    model.addInfoBase(infoBase);
                }
                close();
            }
        });

        // Кнопка "Отмена"
        form.cancelBtn.setOnAction(event -> {
            if (isCreation) {
                close();
            }
        });
    }

    private void placeWindow() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double x = bounds.getMinX() + (bounds.getWidth() - stage.getWidth()) / 2.0;
        double y = bounds.getMinY() + (bounds.getHeight() - stage.getHeight()) / 2.0 - 100;
        stage.setX(x);
        stage.setY(y);
    }

}
