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

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WindowsNavigationPane extends FlowPane {
    private final Map<DataServiceResourceWindow, HBox> windowIcons = new HashMap<>();

    public WindowsNavigationPane() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("fxml/WindowsNavigationPane.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void addWindowIcon(DataServiceResourceWindow window) {
        HBox icon = createWindowIcon(window);
        FlowPane.setMargin(icon, new Insets(3, 0, 3, 5));
        getChildren().add(icon);
        window.showingProperty().addListener((obs, oldVal, showing) -> {
            if (showing) {
                getChildren().add(icon);
            } else {
                getChildren().remove(icon);
            }
        });
        windowIcons.put(window, icon);
    }

    void removeWindowIcon(DataServiceResourceWindow window) {
        HBox icon = windowIcons.get(window);
        getChildren().remove(icon);
    }

    private HBox createWindowIcon(DataServiceResourceWindow window) {
        HBox icon = new HBox();

        icon.setStyle("-fx-border-color: gray;");

        Text label = new Text(cropLabel(window.getEntity().getName()));
        window.getEntity().nameProperty().addListener((obs, oldVal, newVal) -> label.setText(cropLabel(newVal)));

        Pane closeButton = createCloseButton();

        icon.setOnMouseClicked(e -> window.show());

        closeButton.setOnMouseClicked(e -> {
            window.close();
            e.consume();
        });

        icon.getChildren().addAll(label, closeButton);

        HBox.setMargin(label, new Insets(4, 5, 4, 5));
        HBox.setMargin(closeButton, new Insets(4, 5, 4, 0));

        return icon;
    }

    private String cropLabel(String label) {
        if (label.length() > 18) {
            return label.substring(0, 15).concat("...");
        }
        return label;
    }

    private StackPane createCloseButton() {
        Line l1 = new Line(0, 7, 7, 0);
        Line l2 = new Line(0, 0, 7, 7);
        StackPane button = new StackPane(Shape.union(l1, l2));
        button.setMinWidth(15);
        return button;
    }

}
