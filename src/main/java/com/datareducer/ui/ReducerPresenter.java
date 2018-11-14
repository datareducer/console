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
import com.datareducer.dataservice.entity.DataServiceEntity;
import com.datareducer.dataservice.entity.DataServiceRequest;
import com.datareducer.dataservice.entity.Field;
import com.datareducer.model.*;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;

public class ReducerPresenter implements ModelReplacedListener {
    private final Reducer reducer;
    private final ReducerView view;

    private final WindowsManager<InfoBase, InfoBaseWindow> infoBaseWindowsManager;
    private final WindowsManager<Script, ScriptTab> scriptWindowsManager;
    private final WindowsManager<DataServiceResource, DataServiceResourceWindow> dataServiceResourceWindowsManager;

    private OptionsWindow optionsWindow;
    private FileChooser fileChooser;
    private Alert confirmationAlert;
    private Alert errorAlert;
    private Alert infoAlert;

    private final static Logger log = LogManager.getFormatterLogger(ReducerPresenter.class);

    public ReducerPresenter(Reducer reducer) {
        this.reducer = reducer;
        this.view = reducer.getView();

        this.infoBaseWindowsManager = new WindowsManager<>();
        this.scriptWindowsManager = new WindowsManager<>();
        this.dataServiceResourceWindowsManager = new WindowsManager<>();

        optionsWindow = new OptionsWindow(reducer.getPrimaryStage());

        addModelReplacedListeners();
        attachEventHandlers();
    }

    // Регистрируем слушателей замены модели.
    private void addModelReplacedListeners() {
        reducer.addModelReplacedListener(this);
        reducer.addModelReplacedListener(view.getNavigationPanel().getInfoBaseListForm());
        reducer.addModelReplacedListener(view.getNavigationPanel().getScriptListForm());
        reducer.addModelReplacedListener(view.getOutputPane());
        reducer.addModelReplacedListeners(infoBaseWindowsManager, dataServiceResourceWindowsManager, scriptWindowsManager);
    }

    private void attachEventHandlers() {
        attachMainMenuEventHandlers();
        attachInfoBaseListFormEventHandlers();
        attachScriptListFormEventHandlers();
        attachMetadataFormEventHandlers();
        attachWindowsPaneEventHandlers();
        attachOptionsFormEventHandlers();
    }

    @Override
    public void acceptModel(ReducerConfiguration model) {
        //ReducerPresenter в приложении инстанцируется раньше, чем Модель.
        reducer.getModel().getInfoBases().addListener(infoBaseWindowsManager::onModelChanged);
        reducer.getModel().getScripts().addListener(scriptWindowsManager::onModelChanged);

        // При удалении информационной базы закрываем все окна ресурсов и очищаем дерево метаданных
        reducer.getModel().getInfoBases().addListener(new ListChangeListener<InfoBase>() {
            @Override
            public void onChanged(Change<? extends InfoBase> change) {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        dataServiceResourceWindowsManager.closeAllWindows();
                        view.getNavigationPanel().getMetadataForm().clearMetadataTree();
                    }
                }
            }
        });
    }

    private void attachMainMenuEventHandlers() {
        MainMenu menu = reducer.getMainMenu();
        /* Меню "Файл" */
        // Новая модель
        menu.getNewConfItem().setOnAction(e -> newConfiguration());
        // Сохранить
        menu.getSaveConfItem().setOnAction(e -> saveConfiguration());
        // Открыть
        menu.getOpenConfItem().setOnAction(e -> openConfiguration());
        // Получить модель с сервера
        menu.getWebGetConfItem().setOnAction(e -> webGetConfiguration());
        // Загрузить модель на сервер
        menu.getWebPutConfItem().setOnAction(e -> webPutConfiguration());
        // Выход из приложения
        menu.getExitItem().setOnAction(e -> exitApplication());

        /* Меню "Правка" */
        // Добавить базу 1С
        menu.getAddInfoBaseItem().setOnAction(e -> openInfoBaseWindow(new InfoBase()));
        // Добавить запрос
        menu.getAddScriptItem().setOnAction(e -> openNewScriptTab());
        // Окно настроек
        menu.getOptionsItem().setOnAction(e -> optionsWindow.show(reducer.getApplicationParams()));

        /* Меню "Инструменты" */
        menu.getToolsMenu().setOnShown(e -> menu.getClipboardCodeItem().setDisable(getActiveScriptTab() == null));
        // Проверить подключение к Rserve
        menu.getCheckRserveItem().setOnAction(e -> checkRserveConnection());
        // Код R с разметкой в буфер обмена
        menu.getClipboardCodeItem().setOnAction(e -> clipboardRCode());

        /* Меню "Справка" */
        // Лицензионное соглашение
        menu.getEulaItem().setOnAction(e -> LicenseWindow.getInstance(reducer).show());
        // Руководство (онлайн)
        menu.getDocsItem().setOnAction(e -> reducer.getHostServices().showDocument("http://datareducer.ru/documentation"));
        // О программе
        menu.getAboutItem().setOnAction(e -> AboutWindow.getInstance(reducer).show());
    }

    private void attachInfoBaseListFormEventHandlers() {
        InfoBaseListForm listForm = view.getNavigationPanel().getInfoBaseListForm();
        MetadataForm metadataForm = view.getNavigationPanel().getMetadataForm();

        // Тулбар - Добавить подключение
        listForm.getAddBtn().setOnAction(e -> openInfoBaseWindow(new InfoBase()));
        // Тулбар - Удалить подключение
        listForm.getDeleteBtn().setOnAction(e -> removeInfoBase(listForm.getSelectedItem()));
        // Тулбар - Редактировать подключение
        listForm.getEditBtn().setOnAction(e -> openInfoBaseWindow(listForm.getSelectedItem()));
        // Контекстное меню - Изменить
        listForm.getEditItem().setOnAction(e -> openInfoBaseWindow(listForm.getSelectedItem()));
        // Контекстное меню - Удалить подключение
        listForm.getDeleteItem().setOnAction(e -> removeInfoBase(listForm.getSelectedItem()));
        // Контекстное меню - Загрузить метаданные
        listForm.getLoadMetadataItem().setOnAction(e -> metadataForm.loadMetadataTree(listForm.getSelectedItem(), true));

        // Выделение элементов списка
        listForm.getBasesList().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                metadataForm.loadMetadataTree(newVal, false);
            }
        });

        listForm.getBasesList().setRowFactory(p -> {
            TableRow<InfoBase> row = new TableRow<>();
            attachInfoBaseListRowEventHandlers(row);
            return row;
        });
    }

    private void attachInfoBaseListRowEventHandlers(TableRow<InfoBase> row) {
        row.setOnMouseClicked(e -> {
            // Двойной клик по строке
            if (e.getClickCount() == 2 && !row.isEmpty()) {
                openInfoBaseWindow(row.getItem());
            }
            // Вызов контекстного меню правой клавишей мыши
            if (e.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                view.getNavigationPanel().getInfoBaseListForm().getCtxMenu().show(row, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private void attachScriptListFormEventHandlers() {
        ScriptListForm listForm = view.getNavigationPanel().getScriptListForm();
        TabPane tabPane = view.getCentralPane();
        TextArea outputArea = view.getOutputPane().getOutputArea();

        // Тулбар - Добавить скрипт.
        listForm.getAddBtn().setOnAction(e -> openNewScriptTab());
        // Тулбар - Удалить скрипт
        listForm.getDeleteBtn().setOnAction(e -> removeScript(listForm.getSelectedItem()));
        // Тулбар - Открыть скрипт
        listForm.getOpenBtn().setOnAction(e -> openScriptTab(listForm.getSelectedItem(), tabPane, outputArea));
        // Контекстное меню - Открыть скрипт
        listForm.getOpenItem().setOnAction(e -> openScriptTab(listForm.getSelectedItem(), tabPane, outputArea));
        // Контекстное меню - Удалить скрипт
        listForm.getDeleteItem().setOnAction(e -> removeScript(listForm.getSelectedItem()));

        listForm.getScriptTable().setRowFactory(p -> {
            TableRow<Script> row = new TableRow<>();
            attachScriptListRowEventHandlers(row, tabPane, outputArea);
            return row;
        });
    }

    private void attachScriptListRowEventHandlers(TableRow<Script> row, TabPane tabPane, TextArea outputArea) {
        row.setOnMouseClicked(e -> {
            // Двойной клик по строке
            if (e.getClickCount() == 2 && !row.isEmpty()) {
                openScriptTab(row.getItem(), tabPane, outputArea);
            }
            // Вызов контекстного меню правой клавишей мыши
            if (e.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                view.getNavigationPanel().getScriptListForm().getCtxMenu().show(row, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private void attachMetadataFormEventHandlers() {
        MetadataForm metadataForm = view.getNavigationPanel().getMetadataForm();
        TreeTableView<DataServiceEntity> metadataTreeTable = metadataForm.getMetadataTreeTable();

        // Контекстное меню ресурса - Просмотр данных
        metadataForm.getShowDataItem().setOnAction(e -> {
            DataServiceEntity entity = metadataForm.getSelectedItem();
            InfoBase infoBase = metadataForm.getInfoBase();
            DataServiceResource resource = new DataServiceResource(0, entity.getType(), infoBase, entity);
            resource.selectAllFields();
            openDataServiceResourceWindow(resource, null);
        });

        // Контекстное меню ресурса - Добавить в скрипт
        metadataForm.getAddToScriptItem().setOnAction(e -> {
            DataServiceEntity entity = metadataForm.getSelectedItem();
            Script script = getActiveScript();
            InfoBase infoBase = metadataForm.getInfoBase();
            int resourceId = reducer.getModel().getDataServiceResourceSequence().incrementAndGet();
            String name = entity.getType();
            // Проверяем, что имя набора данных уникально в пределах скрипта
            Set<String> names = new HashSet<>();
            for (DataServiceResource res : script.getDataServiceResources()) {
                names.add(res.getName());
            }
            int i = 0;
            while (names.contains(name)) {
                name = entity.getType() + resourceId + (i == 0 ? "" : "_" + i);
                i++;
            }
            DataServiceResource resource = new DataServiceResource(resourceId, name, infoBase, entity);
            resource.selectAllFields();
            script.getDataServiceResources().add(resource);
        });

        // Контекстное меню поля - Копировать имя
        metadataForm.getCopyNameItem().setOnAction(e -> {
            Field field = (Field) metadataForm.getSelectedItem();
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(field.getName());
            clipboard.setContent(content);
        });

        metadataTreeTable.setRowFactory(p -> {
            TreeTableRow<DataServiceEntity> row = new TreeTableRow<>();
            attachMetadataTreeRowEventHandlers(row);
            return row;
        });
    }

    private void attachMetadataTreeRowEventHandlers(TreeTableRow<DataServiceEntity> row) {
        row.setOnMouseClicked(e -> {
            DataServiceEntity entity = row.getItem();
            // Вызов контекстного меню правой клавишей мыши
            if (e.getButton() == MouseButton.SECONDARY) {
                MetadataForm form = view.getNavigationPanel().getMetadataForm();
                if (entity instanceof DataServiceRequest) {
                    form.getAddToScriptItem().setDisable(view.getCentralPane().getTabs().isEmpty());
                    form.getEntityCtxMenu().show(row, e.getScreenX(), e.getScreenY());
                } else if (entity instanceof Field) {
                    form.getFieldCtxMenu().show(row, e.getScreenX(), e.getScreenY());
                }
            }
        });
    }

    /**
     * Обработка событий вкладки скрипта
     *
     * @param tab Вкладка скрипта
     */
    private void attachScriptTabEventHandlers(ScriptTab tab) {
        Script script = tab.getEntity();

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        // Таблица наборов данных
        // Контекстное меню - Открыть
        tab.getDataSetsCtxMenuOpenItem().setOnAction(e -> openDataServiceResourceWindow(tab.getSelectedDataSet(), script));
        // Контекстное меню - Удалить
        tab.getDataSetsCtxMenuDeleteItem().setOnAction(e -> {
            DataServiceResource sel = tab.getSelectedDataSet();
            if (confirmationWindow("", String.format("Удалить набор данных '%s'?", sel.getName()), "")) {
                script.getDataServiceResources().remove(sel);
            }
        });
        // Контекстное меню - Копировать имя
        tab.getDataSetsCtxMenuCopyNameItem().setOnAction(e -> {
            content.putString(tab.getSelectedDataSet().getName());
            clipboard.setContent(content);
        });
        tab.getDataSetsTable().setRowFactory(p -> {
            TableRow<DataServiceResource> row = new TableRow<>();
            attachDataSetsTableRowEventHandlers(row, tab.getDataSetsTableContextMenu(), script);
            return row;
        });

        // Таблица параметров
        tab.getParametersCtxMenuCopyNameItem().setOnAction(e -> {
            content.putString(tab.getSelectedScriptParameter().getName());
            clipboard.setContent(content);
        });
        tab.getParametersTable().setRowFactory(p -> {
            TableRow<ScriptParameter> row = new TableRow<>();
            attachScriptParametersTableRowEventHandlers(row, tab.getParametersContextMenu());
            return row;
        });

        // Вкладка "Веб-доступ"
        // Поле имени веб-ресурса
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^[a-zA-Z0-9_]*$")) {
                return change;
            }
            return null;
        };

        StringConverter<String> formatter = new StringConverter<String>() {
            @Override
            public String toString(String object) {
                return object;
            }

            @Override
            public String fromString(String newName) {
                if (newName.isEmpty()) {
                    errorWindow("Ошибка", "Имя ресурса не может быть пустым", reducer.getPrimaryStage());
                    throw new IllegalArgumentException();
                }
                for (Script s : reducer.getModel().getScripts()) {
                    if (s.equals(script)) {
                        continue;
                    }
                    if (s.getResourceName().equalsIgnoreCase(newName)) {
                        errorWindow("Ошибка", "Имя ресурса не уникально: " + newName, reducer.getPrimaryStage());
                        tab.getResourceNameField().requestFocus();
                        throw new IllegalArgumentException();
                    }
                }
                return newName;
            }
        };

        tab.getResourceNameField().setTextFormatter(new TextFormatter<>(formatter, script.getResourceName(), filter));
    }

    /**
     * Обработка событий окна набора данных
     *
     * @param window Окно набора данных
     */
    private void attachDataServiceResourceWindowEventHandlers(DataServiceResourceWindow window, Script script) {
        DataServiceResource resource = window.getEntity();

        if (window.isDataSetWindow()) {
            Stage stage = window.getStage();
            // При закрытии окна
            stage.setOnCloseRequest(event -> {
                // Снимаем фокус с полей ввода для вызова их обработчиков значений
                window.getResourceDataTable().requestFocus();

                if (!window.updateDataServiceResource()) {
                    event.consume();
                }
            });

            // Поле имени набора данных
            UnaryOperator<TextFormatter.Change> filter = change -> {
                String newText = change.getControlNewText();
                if (newText.matches("^[а-яА-ЯёЁa-zA-Z0-9_]*$")) {
                    return change;
                }
                return null;
            };

            StringConverter<String> formatter = new StringConverter<String>() {
                @Override
                public String toString(String object) {
                    return object;
                }

                @Override
                public String fromString(String newName) {
                    if (newName.isEmpty()) {
                        errorWindow("Ошибка", "Имя набора данных не может быть пустым", stage);
                        throw new IllegalArgumentException();
                    }
                    for (DataServiceResource r : script.getDataServiceResources()) {
                        if (r.equals(resource)) {
                            continue;
                        }
                        if (r.getName().equalsIgnoreCase(newName)) {
                            errorWindow("Ошибка", "Имя набора данных не уникально: " + newName, stage);
                            window.getNameField().requestFocus();
                            throw new IllegalArgumentException();
                        }
                    }
                    return newName;
                }
            };

            window.getNameField().setTextFormatter(new TextFormatter<>(formatter, resource.getName(), filter));
        }
    }

    private void attachDataSetsTableRowEventHandlers(TableRow<DataServiceResource> row, ContextMenu ctxMenu, Script script) {
        row.setOnMouseClicked(e -> {
            // Двойной клик по строке
            if (e.getClickCount() == 2 && !row.isEmpty()) {
                openDataServiceResourceWindow(row.getItem(), script);
            }
            // Вызов контекстного меню правой клавишей мыши
            if (e.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                ctxMenu.show(row, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private void attachScriptParametersTableRowEventHandlers(TableRow<ScriptParameter> row, ContextMenu ctxMenu) {
        row.setOnMouseClicked(e -> {
            // Вызов контекстного меню правой клавишей мыши
            if (e.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                ctxMenu.show(row, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private void attachWindowsPaneEventHandlers() {
        WindowsPane windowsPane = reducer.getWindowsPane();
        dataServiceResourceWindowsManager.windowsProperty()
                .addListener(new MapChangeListener<DataServiceResource, DataServiceResourceWindow>() {
                    @Override
                    public void onChanged(Change<? extends DataServiceResource, ? extends DataServiceResourceWindow> change) {
                        DataServiceResourceWindow window = change.getValueAdded();
                        if (window != null) {
                            //if (!window.isDataSetWindow()) {
                            if (change.wasAdded()) {
                                windowsPane.addWindowIcon(window);
                            } else if (change.wasRemoved()) {
                                windowsPane.removeWindowIcon(window);
                            }
                            //}
                        }
                    }
                });
    }

    private void attachOptionsFormEventHandlers() {
        optionsWindow.getSaveButton().setOnAction(e -> {
            Map<String, String> paramsMap = optionsWindow.getApplicationParams();
            Properties props = new Properties();
            for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                if (entry.getValue() != null) {
                    props.put(entry.getKey(), entry.getValue());
                }
            }
            try (OutputStream os = Files.newOutputStream(Paths.get("./datareducer.properties"))) {
                props.store(os, null);
                reducer.setApplicationParams(Collections.unmodifiableMap(paramsMap));
                log.info("Настройки успешно сохранены");
            } catch (IOException ex) {
                log.error("Не удалось сохранить настройки. Проверьте права на запись в каталог программы", ex);
            }
            optionsWindow.close();
        });
    }

    /**
     * Открывает окно редактирования настроек подключения к REST-сервису 1С.
     *
     * @param infoBase Настройки подключения к REST-сервису 1С.
     */
    private void openInfoBaseWindow(InfoBase infoBase) {
        if (infoBase == null) {
            return;
        }
        InfoBaseWindow win = infoBaseWindowsManager.getWindow(infoBase);
        infoBaseWindowsManager.showWindow(win != null ? win : new InfoBaseWindow(infoBase, reducer.getModel(), reducer.getPrimaryStage()));
    }

    /**
     * Открывает вкладку скрипта.
     *
     * @param script  Скрипт.
     * @param tabPane Панель вкладок.
     */
    private void openScriptTab(Script script, TabPane tabPane, TextArea outputArea) {
        if (script == null) {
            return;
        }
        ScriptTab tab = scriptWindowsManager.getWindow(script);
        if (tab == null) {
            tab = new ScriptTab(script, tabPane, outputArea, reducer.getExecutor());
            attachScriptTabEventHandlers(tab);
        }
        scriptWindowsManager.showWindow(tab);
    }

    /**
     * Создает новый скрипт и открывает его вкладку.
     */
    private void openNewScriptTab() {
        Script script = new Script(reducer.getModel().getScriptSequence().incrementAndGet());
        reducer.getModel().addScript(script);
        openScriptTab(script, view.getCentralPane(), view.getOutputPane().getOutputArea());
    }

    /**
     * Открывает окно ресурса REST-сервиса 1С / набора данных скрипта R
     *
     * @param dataServiceResource Ресурс REST-сервиса 1С.
     * @param script              Скрипт R. Null, если это окно ресурса.
     */
    private void openDataServiceResourceWindow(DataServiceResource dataServiceResource, Script script) {
        DataServiceResourceWindow window = dataServiceResourceWindowsManager.getWindow(dataServiceResource);
        if (window == null) {
            DataServiceResourceWindow newWindow = new DataServiceResourceWindow(dataServiceResource, reducer.getPrimaryStage());
            if (newWindow.isDataSetWindow()) {
                attachDataServiceResourceWindowEventHandlers(newWindow, script);
                newWindow.setParametersLookup(script.getDefaultParamsLookup());
            }
            dataServiceResourceWindowsManager.showWindow(newWindow);
        } else {
            if (window.isDataSetWindow()) {
                window.setParametersLookup(script.getDefaultParamsLookup());
            }
            dataServiceResourceWindowsManager.showWindow(window);
        }
    }

    /**
     * Удаляет настройки подключения с подтверждением.
     *
     * @param infoBase настройки подключения к удалению.
     */
    private void removeInfoBase(InfoBase infoBase) {
        if (infoBase == null) {
            return;
        }
        if (confirmationWindow("", String.format("Удалить подключение '%s'?", infoBase.getName()), "")) {
            reducer.getModel().removeInfoBase(infoBase);
        }
    }

    private void removeScript(Script request) {
        if (request == null) {
            return;
        }
        if (confirmationWindow("", String.format("Удалить скрипт '%s'?", request.getName()), "")) {
            reducer.getModel().removeScript(request);
        }
    }

    private void newConfiguration() {
        reducer.setModel(new ReducerConfiguration());
        log.info("Новая модель создана");
    }

    private void saveConfiguration() {
        if (fileChooser == null) {
            fileChooser = new FileChooser();
        }
        fileChooser.setTitle("Сохранить в файл");
        fileChooser.setInitialFileName("datareducer.xml");
        File file = fileChooser.showSaveDialog(reducer.getPrimaryStage());
        if (file == null) {
            return;
        }
        try {
            Marshaller jm = ReducerConfiguration.getJaxbContext().createMarshaller();
            jm.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jm.marshal(reducer.getModel(), file);
        } catch (JAXBException e) {
            errorWindow("Не удалось сохранить", e.getMessage(), reducer.getPrimaryStage());
            throw new LoadConfigurationException(e);
        }
    }

    private void openConfiguration() {
        if (fileChooser == null) {
            fileChooser = new FileChooser();
        }
        fileChooser.setTitle("Загрузить из файла");
        File file = fileChooser.showOpenDialog(reducer.getPrimaryStage());
        if (file == null) {
            return;
        }
        try {
            Unmarshaller um = ReducerConfiguration.getJaxbContext().createUnmarshaller();
            ReducerConfiguration model = (ReducerConfiguration) um.unmarshal(file);
            reducer.setModel(model);
        } catch (JAXBException e) {
            errorWindow("Не удалось загрузить", e.getMessage(), reducer.getPrimaryStage());
            throw new LoadConfigurationException(e);
        }
    }

    private void webGetConfiguration() {
        if (confirmationWindow("Загрузка модели", "Модель будет получена с сервера", "Продолжить?")) {
            Map<String, String> params = reducer.getApplicationParams();
            String host = params.get(Reducer.RAPPORT_HOST_PARAM_NAME);
            String user = params.get(Reducer.RAPPORT_USER_PARAM_NAME);
            String pswd = params.get(Reducer.RAPPORT_PASSWORD_PARAM_NAME);
            String name = params.get(Reducer.RAPPORT_WEBAPP_PARAM_NAME);
            if (host == null || user == null || pswd == null || name == null) {
               RuntimeException ex = new LoadConfigurationException("Не установлены параметры подключения");
               log.error("Не удалось получить модель:", ex);
               errorWindow("Не удалось получить модель", ex.getMessage(), reducer.getPrimaryStage());
               throw ex;
            }
            Client client = ClientBuilder.newClient()
                    .register(ReducerConfiguration.getConfigBodyReader())
                    .register(HttpAuthenticationFeature.basicBuilder().build());
            WebTarget wt = client.target("http://" + host).path(name).path("configuration");
            Invocation.Builder ib = wt.request(MediaType.APPLICATION_XML)
                    .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, user)
                    .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, pswd);
            Response response;
            try {
                response = ib.get();
            } catch (ProcessingException ex) {
                RuntimeException e = new LoadConfigurationException("Не удалось получить модель", ex);
                log.error("Не удалось получить модель:", e.getCause());
                errorWindow("Не удалось получить модель", ex.getMessage(), reducer.getPrimaryStage());
                throw e;
            }
            int status = response.getStatus();
            if (status == 200) {
                reducer.setModel(response.readEntity(ReducerConfiguration.class));
                log.info("Модель успешно получена c сервера");
                infoWindow("Загрузка модели", "Модель успешно получена c сервера", "");
            } else {
                log.error("Не удалось получить модель с сервера: %s %s", wt.getUri().toString(), response.getStatusInfo());
                errorWindow("Не удалось получить модель с сервера", response.getStatusInfo().toString(), reducer.getPrimaryStage());
            }
        }
    }

    private void webPutConfiguration() {
        if (confirmationWindow("Загрузка модели", "Модель будет загружена на сервер", "Продолжить?")) {
            Map<String, String> params = reducer.getApplicationParams();
            String host = params.get(Reducer.RAPPORT_HOST_PARAM_NAME);
            String user = params.get(Reducer.RAPPORT_USER_PARAM_NAME);
            String pswd = params.get(Reducer.RAPPORT_PASSWORD_PARAM_NAME);
            String name = params.get(Reducer.RAPPORT_WEBAPP_PARAM_NAME);
            if (host == null || user == null || pswd == null || name == null) {
                RuntimeException ex = new LoadConfigurationException("Не установлены параметры подключения");
                log.error("Не удалось загрузить модель:", ex);
                errorWindow("Не удалось загрузить модель", ex.getMessage(), reducer.getPrimaryStage());
                throw ex;
            }
            Client client = ClientBuilder.newClient()
                    .register(ReducerConfiguration.getConfigBodyWriter())
                    .register(HttpAuthenticationFeature.basicBuilder().build());
            WebTarget wt = client.target("http://" + host).path(name).path("configuration");
            Invocation.Builder ib = wt.request(MediaType.APPLICATION_XML)
                    .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, user)
                    .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, pswd);
            Response response;
            try {
                response = ib.put(Entity.entity(reducer.getModel(), MediaType.APPLICATION_XML));
            } catch (ProcessingException ex) {
                RuntimeException e = new LoadConfigurationException("Не удалось загрузить модель", ex);
                log.error("Не удалось загрузить модель:", ex);
                errorWindow("Не удалось загрузить модель", ex.getMessage(), reducer.getPrimaryStage());
                throw e;
            }
            int status = response.getStatus();
            if (status == 202) {
                log.info("Модель успешно загружена на сервер");
                infoWindow("Загрузка модели", "Модель успешно загружена на сервер", "");
            } else {
                log.error("Не удалось загрузить модель: %s %s", wt.getUri().toString(), response.getStatusInfo());
                errorWindow("Не удалось загрузить модель", response.getStatusInfo().toString(), reducer.getPrimaryStage());
            }
        }
    }

    private void clipboardRCode() {
        Script script = getActiveScript();
        assert script != null;
        String text = script.getScriptBody();
        if (text.isEmpty()) {
            return;
        }
        text = Script.prepareContentString(text);
        Script s = new Script();
        s.setApplicationParams(reducer.getApplicationParams());
        s.setScriptBody("library('highlight'); tf <- tempfile(); body <- as.symbol('" + text + "'); write(body, file = tf ); " +
                "highlight( file = tf, renderer = renderer_html(document = TRUE), show_line_numbers = TRUE);");
        s.setDefaultParams(script.getDefaultParams());
        ScriptResult sr = null;
        try {
            sr = s.execute(reducer.getExecutor());
        } catch (UndefinedParameterException | ScriptException e) {
            // Недостижимо
            e.printStackTrace();
        }
        Clipboard cb = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(sr.getOutput());
        cb.setContent(content);
    }

    private void checkRserveConnection() {
        Script s = new Script();
        s.setApplicationParams(reducer.getApplicationParams());
        s.setScriptBody("R.version.string");
        try {
            ScriptResult sr = s.execute(reducer.getExecutor());
            Platform.runLater(() -> view.getOutputPane().getOutputArea().appendText(sr.getOutput().concat("\n")));
        } catch (UndefinedParameterException | ScriptException e) {
            e.printStackTrace();
        }
    }

    private Script getActiveScript() {
        ScriptTab tab = getActiveScriptTab();
        if (tab != null) {
            return tab.getEntity();
        }
        return null;
    }

    private ScriptTab getActiveScriptTab() {
        return view.getCentralPane().getSelectedTab();
    }

    private boolean confirmationWindow(String tittle, String header, String message) {
        if (confirmationAlert == null) {
            confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationAlert.initOwner(reducer.getPrimaryStage());
        }
        confirmationAlert.setTitle(tittle);
        confirmationAlert.setHeaderText(header);
        confirmationAlert.setContentText(message);
        confirmationAlert.setResizable(true);
        Optional<ButtonType> result = confirmationAlert.showAndWait();
        return result.get() == ButtonType.OK;
    }

    private void errorWindow(String header, String content, Stage owner) {
        if (errorAlert == null) {
            errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.initOwner(owner);
        }
        errorAlert.setTitle(owner.getTitle());
        errorAlert.setHeaderText(header);
        errorAlert.setContentText(content);
        errorAlert.setResizable(true);
        errorAlert.showAndWait();
    }

    private void infoWindow(String tittle, String header, String message) {
        if (infoAlert == null) {
            infoAlert = new Alert(Alert.AlertType.INFORMATION);
            infoAlert.initOwner(reducer.getPrimaryStage());
        }
        infoAlert.setTitle(tittle);
        infoAlert.setHeaderText(header);
        infoAlert.setContentText(message);
        infoAlert.setResizable(true);
        infoAlert.showAndWait();
    }

    private void exitApplication() {
        reducer.getModel().close();
        Platform.exit();
    }

}
