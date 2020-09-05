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

import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;
import javafx.stage.Stage;
import ru.datareducer.Reducer;
import ru.datareducer.model.ReducerRuntimeException;
import ru.datareducer.model.Script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Окно "Лицензионное соглашение"
 *
 * @author Kirill Mikhaylov
 */
public class LicenseWindow {
    private static LicenseWindow instance;

    private final Stage stage;
    private final LicenseForm form;
    private final Reducer reducer;

    private LicenseWindow(Reducer reducer) {
        this.form = new LicenseForm();
        this.stage = new Stage();
        this.reducer = reducer;
        initialize();
    }

    static LicenseWindow getInstance(Reducer reducer) {
        if (instance == null) {
            instance = new LicenseWindow(reducer);
        }
        return instance;
    }

    private void initialize() {
        stage.setScene(new Scene(form, 800, 500));
        stage.initOwner(reducer.getPrimaryStage());
        stage.setTitle("Лицензионное соглашение");
    }

    void show() {
        stage.show();
        placeWindow();
    }

    void close() {
        stage.close();
    }

    private class LicenseForm extends GridPane {
        LicenseForm() {
            initialize();
        }

        private void initialize() {
            TextArea licenseArea = new TextArea();
            licenseArea.setWrapText(true);
            licenseArea.setEditable(false);

            InputStream stream = Script.class.getResourceAsStream("/COPYING.txt");
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                licenseArea.setText(buffer.lines().collect(Collectors.joining("\n")));
            } catch (IOException e) {
                throw new ReducerRuntimeException();
            }

            ButtonBar buttonBar = new ButtonBar();
            Button okButton = new Button("Закрыть");
            okButton.setDefaultButton(true);
            okButton.setOnAction(e -> close());
            buttonBar.getButtons().add(okButton);

            setVgrow(licenseArea, Priority.ALWAYS);
            setHgrow(licenseArea, Priority.ALWAYS);

            getChildren().addAll(licenseArea, buttonBar);

            GridPane.setConstraints(licenseArea, 0, 0);  // (c0, r0)
            GridPane.setConstraints(buttonBar, 0, 1);

            GridPane.setMargin(licenseArea, new Insets(10, 10, 7, 10));
            GridPane.setMargin(buttonBar, new Insets(0, 10, 7, 0));

            setStyle("-fx-background-color:WHITE");
        }
    }

    private void placeWindow() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double x = bounds.getMinX() + (bounds.getWidth() - stage.getWidth()) / 2.0;
        double y = bounds.getMinY() + (bounds.getHeight() - stage.getHeight()) / 2.0 - 100;
        stage.setX(x);
        stage.setY(y);
    }
}
