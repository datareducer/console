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
package com.datareducer;

import com.datareducer.model.ReducerConfiguration;
import com.datareducer.ui.*;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Reducer extends Application {
    public final static String RSERVE_HOST_PARAM_NAME = "rserve-host";
    public final static String RSERVE_USER_PARAM_NAME = "rserve-user";
    public final static String RSERVE_PASSWORD_PARAM_NAME = "rserve-password";
    public final static String ORIENTDB_ENGINE_PARAM_NAME = "orientdb-engine";
    public final static String ORIENTDB_USER_PARAM_NAME = "orientdb-user";
    public final static String ORIENTDB_PASSWORD_PARAM_NAME = "orientdb-password";
    public final static String ORIENTDB_HOST_PARAM_NAME = "orientdb-host";
    public final static String RAPPORT_WEBAPP_PARAM_NAME = "rapport-webappname";
    public final static String RAPPORT_HOST_PARAM_NAME = "rapport-host";
    public final static String RAPPORT_USER_PARAM_NAME = "rapport-user";
    public final static String RAPPORT_PASSWORD_PARAM_NAME = "rapport-password";

    private final Set<ModelReplacedListener> modelReplacedListeners;

    private ExecutorService executor;

    private Stage primaryStage;

    private ReducerConfiguration model;
    private ReducerView view;
    private ReducerPresenter presenter;

    private Map<String, String> applicationParams;

    private MainMenu mainMenu;
    private WindowsNavigationPane windowsNavigationPane;

    public Reducer() {
        this.modelReplacedListeners = new HashSet<>();
    }

    @Override
    public void start(Stage primaryStage) {
        setPrimaryStage(primaryStage);

        setView(new ReducerView());
        setMainMenu(new MainMenu());
        setWindowsPane(new WindowsNavigationPane());

        setPresenter(new ReducerPresenter(this));
        setModel(new ReducerConfiguration());

        BorderPane borderPane = new BorderPane();

        borderPane.setTop(mainMenu);
        borderPane.setCenter(view);
        borderPane.setBottom(windowsNavigationPane);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(borderPane, bounds.getWidth() - 300, bounds.getHeight() - 100);

        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            scene.getStylesheets().add("css/linux.css");
        } else {
            scene.getStylesheets().add("css/windows.css");
        }

        primaryStage.setScene(scene);
        primaryStage.setTitle("DataReducer Console");
        primaryStage.setOnCloseRequest(event -> getModel().close());

        primaryStage.getIcons().add(new Image("image/logo_16x16.png"));
        primaryStage.getIcons().add(new Image("image/logo_24x24.png"));
        primaryStage.getIcons().add(new Image("image/logo_32x32.png"));
        primaryStage.getIcons().add(new Image("image/logo_48x48.png"));
        primaryStage.getIcons().add(new Image("image/logo_64x64.png"));
        primaryStage.getIcons().add(new Image("image/logo_96x96.png"));
        primaryStage.getIcons().add(new Image("image/logo_256x256.png"));

        primaryStage.show();

        setExecutor(Executors.newCachedThreadPool());
    }

    @Override
    public void stop() {
        executor.shutdownNow();
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Добавить слушатели замены модели
     *
     * @param listeners Слушатели замены модели к добавлению
     */
    public void addModelReplacedListeners(ModelReplacedListener... listeners) {
        modelReplacedListeners.addAll(Arrays.asList(listeners));
    }

    /**
     * Добавить слушатель замены модели
     *
     * @param listener Слушатель замены модели к добавлению
     */
    public void addModelReplacedListener(ModelReplacedListener listener) {
        modelReplacedListeners.add(listener);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public ReducerConfiguration getModel() {
        return model;
    }

    public void setModel(ReducerConfiguration model) {
        if (this.model != null) {
            this.model.close();
        }
        this.model = model;

        applicationParams = getParameters().getNamed();

        // В self-contained приложении параметры считываем из файла.
        if (applicationParams.isEmpty()) {
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(Paths.get("./datareducer.properties"))) {
                props.load(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!props.isEmpty()) {
                Map<String, String> fileParams = new HashMap<>();
                for (String name : props.stringPropertyNames()) {
                    fileParams.put(name, props.getProperty(name));
                }
                applicationParams = Collections.unmodifiableMap(fileParams);
            }
        }

        model.setApplicationParams(applicationParams);

        for (ModelReplacedListener listener : modelReplacedListeners) {
            listener.acceptModel(model);
        }
    }

    public ReducerView getView() {
        return view;
    }

    public void setView(ReducerView view) {
        this.view = view;
    }

    public void setPresenter(ReducerPresenter presenter) {
        this.presenter = presenter;
    }

    public Map<String, String> getApplicationParams() {
        return applicationParams;
    }

    public void setApplicationParams(Map<String, String> params) {
        this.applicationParams = Collections.unmodifiableMap(params);
    }

    public MainMenu getMainMenu() {
        return mainMenu;
    }

    public void setMainMenu(MainMenu mainMenu) {
        this.mainMenu = mainMenu;
    }

    public WindowsNavigationPane getWindowsNavigationPane() {
        return windowsNavigationPane;
    }

    public void setWindowsPane(WindowsNavigationPane windowsNavigationPane) {
        this.windowsNavigationPane = windowsNavigationPane;
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
