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

import com.datareducer.Reducer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class OptionsWindow {
    private StringProperty rserveHost = new SimpleStringProperty();
    private StringProperty rserveUser = new SimpleStringProperty();
    private StringProperty rservePassword = new SimpleStringProperty();

    private StringProperty rapportHost = new SimpleStringProperty();
    private StringProperty rapportUser = new SimpleStringProperty();
    private StringProperty rapportPassword = new SimpleStringProperty();

    // Не отображается на форме:
    private StringProperty rapportWebappName = new SimpleStringProperty();

    private final Stage stage;
    private final OptionsForm form;

    private final Stage primaryStage;

    public OptionsWindow(Stage primaryStage) {
        this.stage = new Stage();
        this.form = new OptionsForm();
        this.primaryStage = primaryStage;

        initialize();
    }

    private void initialize() {
        stage.setScene(new Scene(form));
        stage.initOwner(primaryStage);
        stage.setTitle("Настройки");
        stage.setResizable(false);

        registerEventHandlers();
    }

    public void show(Map<String, String> applicationParams) {
        rserveHost.setValue(applicationParams.get(Reducer.RSERVE_HOST_PARAM_NAME));
        rserveUser.setValue(applicationParams.get(Reducer.RSERVE_USER_PARAM_NAME));
        rservePassword.setValue(applicationParams.get(Reducer.RSERVE_PASSWORD_PARAM_NAME));
        rapportHost.setValue(applicationParams.get(Reducer.RAPPORT_HOST_PARAM_NAME));
        rapportUser.setValue(applicationParams.get(Reducer.RAPPORT_USER_PARAM_NAME));
        rapportPassword.setValue(applicationParams.get(Reducer.RAPPORT_PASSWORD_PARAM_NAME));
        // Не отображается на форме:
        rapportWebappName.setValue(applicationParams.get(Reducer.RAPPORT_WEBAPP_PARAM_NAME));

        stage.toFront();
        stage.show();
        placeWindow();
    }

    private class OptionsForm extends GridPane {
        @FXML
        private TextField rserveHostFld;
        @FXML
        private TextField rserveUserFld;
        @FXML
        private TextField rservePasswordFld;
        @FXML
        private TextField rapportHostFld;
        @FXML
        private TextField rapportUserFld;
        @FXML
        private TextField rapportPasswordFld;
        @FXML
        private Button cancelBtn;
        @FXML
        private Button saveBtn;

        OptionsForm() {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("fxml/OptionsForm.fxml"));
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
            rserveHostFld.textProperty().bindBidirectional(rserveHost);
            rserveUserFld.textProperty().bindBidirectional(rserveUser);
            rservePasswordFld.textProperty().bindBidirectional(rservePassword);
            rapportHostFld.textProperty().bindBidirectional(rapportHost);
            rapportUserFld.textProperty().bindBidirectional(rapportUser);
            rapportPasswordFld.textProperty().bindBidirectional(rapportPassword);
        }
    }

    void close() {
        stage.close();
    }

    Button getSaveButton() {
        return form.saveBtn;
    }

    Map<String, String> getApplicationParams() {
        Map<String, String> params = new LinkedHashMap<>();

        params.put(Reducer.RSERVE_HOST_PARAM_NAME, rserveHost.get());
        params.put(Reducer.RSERVE_USER_PARAM_NAME, rserveUser.get());
        params.put(Reducer.RSERVE_PASSWORD_PARAM_NAME, rservePassword.get());

        params.put(Reducer.RAPPORT_WEBAPP_PARAM_NAME, rapportWebappName.get());
        params.put(Reducer.RAPPORT_HOST_PARAM_NAME, rapportHost.get());
        params.put(Reducer.RAPPORT_USER_PARAM_NAME, rapportUser.get());
        params.put(Reducer.RAPPORT_PASSWORD_PARAM_NAME, rapportPassword.get());

        return params;
    }

    private void registerEventHandlers() {
        form.cancelBtn.setOnAction(e -> stage.close());
    }

    private void placeWindow() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double x = bounds.getMinX() + (bounds.getWidth() - stage.getWidth()) / 2.0;
        double y = bounds.getMinY() + (bounds.getHeight() - stage.getHeight()) / 2.0 - 100;
        stage.setX(x);
        stage.setY(y);
    }

}
