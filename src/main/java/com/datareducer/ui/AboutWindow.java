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

import com.datareducer.Reducer;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Окно "О программе"
 *
 * @author Kirill Mikhaylov
 */
class AboutWindow {
    private static AboutWindow instance;

    private final Stage stage;
    private final AboutForm form;
    private final Reducer reducer;

    private AboutWindow(Reducer reducer) {
        this.form = new AboutForm();
        this.stage = new Stage();
        this.reducer = reducer;
        initialize();
    }

    static AboutWindow getInstance(Reducer reducer) {
        if (instance == null) {
            instance = new AboutWindow(reducer);
        }
        return instance;
    }

    private void initialize() {
        stage.setScene(new Scene(form, 550, 400));
        stage.initOwner(reducer.getPrimaryStage());
        stage.setResizable(false);
        stage.setTitle("О программе");

        stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                close();
            }
        });
    }

    void show() {
        stage.show();
        placeWindow();
    }

    void close() {
        stage.close();
    }

    private class AboutForm extends GridPane {
        AboutForm() {
            initialize();
        }

        private void initialize() {
            Image logoImg = new Image(getClass().getClassLoader().getResourceAsStream("image/logo.png"));
            ImageView logoImgView = new ImageView((logoImg));

            Text lbl1 = new Text("DataReducer Console");
            lbl1.setFont(Font.font(22));

            Text lbl2 = new Text("R-консоль для \"1С:Предприятия\"");
            lbl2.setFont(Font.font(13));

            Hyperlink link = new Hyperlink();
            link.setText("https://datareducer.ru");
            link.setOnAction(e -> reducer.getHostServices().showDocument(link.getText()));

            TextArea aboutArea = new TextArea();
            aboutArea.setEditable(false);

            aboutArea.appendText("Версия: 1.1.12");
            aboutArea.appendText("\n\nCopyright © Кирилл Михайлов, 2017-2019");
            aboutArea.appendText("\n<admin@datareducer.ru>");

            aboutArea.appendText("\n\nПрограмма DataReducer Console является свободным \n" +
                    "программным обеспечением. Вы вправе распространять ее\n" +
                    "и/или модифицировать в соответствии с условиями версии 2\n" +
                    "либо, по вашему выбору, с условиями более поздней версии \n" +
                    "Стандартной Общественной Лицензии GNU, опубликованной \n" +
                    "Free Software Foundation. \n" +
                    "\n" +
                    "Программа DataReducer Console распространяется в надежде, \n" +
                    "что она будет полезной, но БЕЗО ВСЯКИХ ГАРАНТИЙ, \n" +
                    "в том числе ГАРАНТИИ ТОВАРНОГО СОСТОЯНИЯ ПРИ ПРОДАЖЕ\n" +
                    "и ПРИГОДНОСТИ ДЛЯ ИСПОЛЬЗОВАНИЯ В КОНКРЕТНЫХ ЦЕЛЯХ.\n" +
                    "Подробнее см. в Стандартной Общественной Лицензии GNU.\n" +
                    "\n" +
                    "Вы должны были получить копию Стандартной Общественной \n" +
                    "Лицензии GNU вместе с этой программой. Если это не так, см.\n" +
                    "<https://www.gnu.org/licenses/>.");

            aboutArea.appendText("\n\nИсходный код программы: https://github.com/datareducer/console");

            aboutArea.appendText("\n\nВ этот продукт включено следующее программное обеспечение:");

            aboutArea.appendText("\n\nRserve by Simon Urbanek / LGPL v.2.1");
            aboutArea.appendText("\nJersey by Oracle Corporation / CDDL v.1.1");
            aboutArea.appendText("\nOrientDB by OrientDB LTD / Apache License 2.0");
            aboutArea.appendText("\nLog4j by Apache Software Foundation / Apache License 2.0");
            aboutArea.appendText("\nAce by Ajax.org / BSD License 2.0");

            aboutArea.appendText("\n\nИконки: <http://www.fatcow.com/free-icons> / CC BY 3.0");

            setVgrow(aboutArea, Priority.ALWAYS);

            setHgrow(aboutArea, Priority.ALWAYS);
            setHgrow(lbl1, Priority.ALWAYS);
            setHgrow(lbl2, Priority.ALWAYS);
            setHgrow(link, Priority.ALWAYS);
            setHgrow(logoImgView, Priority.ALWAYS);

            getChildren().addAll(lbl1, lbl2, link, logoImgView, aboutArea);

            GridPane.setConstraints(lbl1, 0, 0);  // (c0, r0)
            GridPane.setConstraints(lbl2, 0, 1);
            GridPane.setConstraints(link, 0, 2);
            GridPane.setConstraints(logoImgView, 1, 0);
            GridPane.setConstraints(aboutArea, 0, 3);

            GridPane.setRowSpan(logoImgView, 3);
            GridPane.setColumnSpan(aboutArea, 2);

            GridPane.setHalignment(logoImgView, HPos.RIGHT);

            GridPane.setMargin(aboutArea, new Insets(10, 10, 10, 10));
            GridPane.setMargin(logoImgView, new Insets(10, 10, 0, 10));
            GridPane.setMargin(lbl1, new Insets(10, 0, 0, 10));
            GridPane.setMargin(lbl2, new Insets(0, 0, 0, 11));
            GridPane.setMargin(link, new Insets(0, 0, 0, 8));

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
