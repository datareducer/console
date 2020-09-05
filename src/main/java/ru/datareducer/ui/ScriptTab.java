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

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.IntegerStringConverter;
import ru.datareducer.model.DataServiceResource;
import ru.datareducer.model.Script;
import ru.datareducer.model.ScriptParameter;
import ru.datareducer.model.ScriptResult;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.UnaryOperator;

public class ScriptTab extends Tab implements Window<Script> {
    private final ExecuteScriptService executeScriptService;
    private final ExecutorService executor;

    private final Script script;
    private final TabPane reducerTabPane;
    private final TabPane scriptTabPane;
    private final TextArea outputArea;
    private final Tab generalTab;
    private final Tab webAccessTab;
    private final ScriptGeneralForm generalForm;
    private final ScriptWebAccessForm webAccessForm;

    private final static KeyCodeCombination keyCtrlC = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);

    ScriptTab(Script script, TabPane reducerTabPane, TextArea outputArea, ExecutorService executor) {
        if (script == null) {
            throw new IllegalArgumentException("Значение параметра 'script': null");
        }
        if (reducerTabPane == null) {
            throw new IllegalArgumentException("Значение параметра 'reducerTabPane': null");
        }
        if (outputArea == null) {
            throw new IllegalArgumentException("Значение параметра 'outputArea': null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Значение параметра 'executor': null");
        }

        this.executeScriptService = new ExecuteScriptService();
        this.executor = executor;

        this.script = script;
        this.reducerTabPane = reducerTabPane;
        this.scriptTabPane = new TabPane();
        this.outputArea = outputArea;
        this.generalTab = new Tab("Общее");
        this.webAccessTab = new Tab("Веб-доступ");
        this.generalForm = new ScriptGeneralForm();
        this.webAccessForm = new ScriptWebAccessForm();

        initialize();
    }

    private void initialize() {
        generalTab.setContent(generalForm);
        webAccessTab.setContent(webAccessForm);

        generalTab.setClosable(false);
        webAccessTab.setClosable(false);

        scriptTabPane.getTabs().addAll(generalTab, webAccessTab);
        scriptTabPane.setSide(Side.RIGHT);
        setContent(scriptTabPane);

        textProperty().bind(script.nameProperty());

        registerButtonBarsEventHandlers();
        registerExecuteScriptServiceEventHandlers();
        registerScriptEditorEventHandlers();
        registerTemplateEditorEventHandlers();
        // Часть обработчиков событий назначаются в ReducerPresenter
    }

    private void registerButtonBarsEventHandlers() {
        generalForm.runBtn.setOnAction(e -> executeScriptService.restart());
        generalForm.stopBtn.setOnAction(e -> executeScriptService.cancel());

        generalForm.refillParamsBtn.setOnAction(e -> script.refillParameterList());

        webAccessForm.addRoleBtn.setOnAction(e -> {
            script.getSecurityRoles().add("");
            webAccessForm.rolesList.getSelectionModel().selectLast();
        });

        webAccessForm.removeRoleBtn.setOnAction(e -> {
            script.getSecurityRoles().remove(webAccessForm.rolesList.getSelectionModel().getSelectedItem());
        });
    }

    private void registerExecuteScriptServiceEventHandlers() {
        executeScriptService.setOnSucceeded(e -> {
            ScriptResult scriptResult = executeScriptService.getValue();
            List<Map<String, Object>> dataFrame = scriptResult.getDataFrame();
            TableView<Map> scriptResultTable = generalForm.scriptResultTable;
            scriptResultTable.getColumns().clear();
            if (dataFrame != null && !dataFrame.isEmpty()) {
                // Формируем колонки таблицы
                for (Map.Entry<String, Object> entry : dataFrame.get(0).entrySet()) {
                    TableColumn<Map, Object> column = new TableColumn<>();
                    column.setText(entry.getKey());
                    column.setCellValueFactory(new MapValueFactory<>(entry.getKey()));

                    column.setCellFactory(param -> new TextFieldTableCell<>(new StringConverter<Object>() {
                        // Rserve поддерживает не все типы Java. Даты, в том числе, возвращаются в виде строки.
                        @Override
                        public String toString(Object object) {
                            if (object == null) {
                                return "";
                            } else if (object instanceof Double) {
                                // Убираем экспоненциальный формат
                                return String.format("%.2f", object);
                            } else {
                                return object.toString();
                            }
                        }

                        @Override
                        public Object fromString(String string) {
                            return string;
                        }
                    }));

                    scriptResultTable.getColumns().add(column);
                }
                // Заполняем таблицу данными
                scriptResultTable.setItems(FXCollections.observableArrayList(dataFrame));
            } else {
                scriptResultTable.setItems(null);
            }

            // Выводим текст
            String output = scriptResult.getOutput();
            if (output != null && !output.isEmpty()) {
                Platform.runLater(() -> outputArea.appendText(output.concat("\n")));
            }

            // Выводим изображения
            generalForm.plotTabPane.getTabs().clear();
            int ind = 0;
            for (Image image : scriptResult.getImages()) {
                Tab plotTab = new Tab("Plot " + ++ind);
                plotTab.setContent(new ScrollPane(new ImageView(image)));
                generalForm.plotTabPane.getTabs().add(plotTab);
            }

        });

        executeScriptService.setOnFailed(e -> {
            generalForm.scriptResultTable.setItems(null);
            generalForm.scriptResultTable.getColumns().clear();
            generalForm.plotTabPane.getTabs().clear();
            e.getSource().getException().printStackTrace();
        });
    }


    private void registerScriptEditorEventHandlers() {
        WebEngine we = generalForm.scriptEditor.getEngine();

        // Устанавливаем тело скрипта. Это нужно делать после окончания загрузки страницы.
        we.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                we.executeScript("editor.setValue('" + Script.prepareContentString(script.getScriptBody()) + "', -1);");
            }
        });

        generalForm.scriptEditor.setOnKeyReleased(e -> script.setScriptBody((String) we.executeScript("editor.getValue();")));

        // Обход ошибки, не позволяющей в редакторе скопировать текст с помощью Ctrl + C
        generalForm.scriptEditor.addEventHandler(KeyEvent.ANY, e -> {
            if (keyCtrlC.match(e)) {
                putCopyTextToClipboard(we);
            }
        });
    }

    private void registerTemplateEditorEventHandlers() {
        WebEngine we = webAccessForm.templateEditor.getEngine();

        // Устанавливаем тело шаблона. Это нужно делать после окончания загрузки страницы.
        we.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                if (script.isUseDefaultTemplate()) {
                    we.executeScript("editor.setValue('" + Script.prepareContentString(Script.getDefaultTemplate()) + "', -1);");
                } else {
                    we.executeScript("editor.setValue('" + Script.prepareContentString(script.getTemplate()) + "', -1);");
                }
                if (!script.isWebAccess() || script.isUseDefaultTemplate()) {
                    we.executeScript("editor.setReadOnly(true);");
                }
            }
        });

        webAccessForm.defaultTemplateCkBx.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                script.setTemplate(null);
                we.executeScript("editor.setValue('" + Script.prepareContentString(Script.getDefaultTemplate()) + "', -1);");
                we.executeScript("editor.setReadOnly(true);");
            } else {
                script.setTemplate(Script.getDefaultTemplate());
                we.executeScript("editor.setValue('" + Script.prepareContentString(script.getTemplate()) + "', -1);");
                we.executeScript("editor.setReadOnly(false);");
            }
        });

        webAccessForm.webAccessCkBx.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && webAccessForm.defaultTemplateCkBx.selectedProperty().not().get()) {
                we.executeScript("editor.setReadOnly(false);");
            } else {
                we.executeScript("editor.setReadOnly(true);");
            }
        });

        webAccessForm.templateEditor.setOnKeyReleased(e -> {
            if (script.isWebAccess() && !script.isUseDefaultTemplate()) {
                script.setTemplate((String) we.executeScript("editor.getValue();"));
            }
        });

        // Обход ошибки, не позволяющей в редакторе скопировать текст с помощью Ctrl + C
        webAccessForm.templateEditor.addEventHandler(KeyEvent.ANY, e -> {
            if (keyCtrlC.match(e)) {
                putCopyTextToClipboard(we);
            }
        });
    }

    private static void putCopyTextToClipboard(WebEngine we) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String selected = (String) we.executeScript("editor.getCopyText();");
        ClipboardContent content = new ClipboardContent();
        content.putString(selected);
        clipboard.setContent(content);
    }

    @Override
    public Script getEntity() {
        return script;
    }

    @Override
    public void show() {
        if (!reducerTabPane.getTabs().contains(this)) {
            reducerTabPane.getTabs().add(this);
        }
        reducerTabPane.getSelectionModel().select(this);
    }

    @Override
    public void close() {
        reducerTabPane.getTabs().remove(this);
    }

    private class ScriptGeneralForm extends SplitPane {
        @FXML
        private WebView scriptEditor;

        // Основная кнопочная панель
        @FXML
        private Button runBtn;
        @FXML
        private Button stopBtn;

        @FXML
        private Accordion accordion;

        // Таблица наборов данных
        @FXML
        private TitledPane dataSetsPane;
        @FXML
        private TableView<DataServiceResource> dataSetsTable;
        @FXML
        private TableColumn<DataServiceResource, String> dataSetNameCol;
        @FXML
        private TableColumn<DataServiceResource, String> dataSetInfoBaseCol;
        @FXML
        private TableColumn<DataServiceResource, String> dataSetResourceCol;
        @FXML
        private TableColumn<DataServiceResource, String> dataSetCacheCol;

        // Контекстное меню Таблицы наборов данных
        @FXML
        private ContextMenu dataSetsCtxMenu;
        @FXML
        private MenuItem dataSetsCtxMenuOpenItem;
        @FXML
        private MenuItem dataSetsCtxMenuDeleteItem;
        @FXML
        private MenuItem dataSetsCtxMenuCopyNameItem;

        // Панель параметров
        @FXML
        private Button refillParamsBtn;
        @FXML
        private TitledPane parametersPane;
        @FXML
        private TableView<ScriptParameter> parametersTable;
        @FXML
        private TableColumn<ScriptParameter, String> paramNameCol;
        @FXML
        private TableColumn<ScriptParameter, String> paramValueCol;
        @FXML
        private TableColumn<ScriptParameter, Boolean> httpParamCol;

        // Контекстное меню Панели параметров
        @FXML
        private ContextMenu parametersCtxMenu;
        @FXML
        private MenuItem parametersCtxMenuCopyNameItem;

        // Панель настроек
        @FXML
        private TextField nameFld;
        @FXML
        private TextArea descriptionArea;

        @FXML
        private TableView<Map> scriptResultTable;
        @FXML
        private Region veil;
        @FXML
        private ProgressIndicator progressIndicator;

        // Область вывода графики
        @FXML
        private Label plotWidthLbl;
        @FXML
        private TextField plotWidthFld;
        @FXML
        private Label plotHeightLbl;
        @FXML
        private TextField plotHeightFld;
        @FXML
        private TabPane plotTabPane;

        ScriptGeneralForm() {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("fxml/ScriptGeneralForm.fxml"));
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
            WebEngine we = scriptEditor.getEngine();
            try {
                we.load(ScriptTab.class.getResource("/ace_r/editor_r.html").toURI().toURL().toString());
            } catch (MalformedURLException | URISyntaxException e) {
                e.printStackTrace();
            }

            accordion.setExpandedPane(dataSetsPane);

            // Панель наборов данных
            dataSetsTable.setItems(script.getDataServiceResources());
            dataSetNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            dataSetInfoBaseCol.setCellValueFactory(new PropertyValueFactory<>("infoBase"));
            dataSetResourceCol.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getDataServiceEntity().getResourceName()));
            dataSetCacheCol.setCellValueFactory(param -> new ReadOnlyStringWrapper(String.valueOf(param.getValue().getCacheLifetime())));

            // Панель параметров
            parametersTable.setItems(script.getDefaultParams());
            parametersTable.setEditable(true);
            paramNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            paramValueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
            paramValueCol.setCellFactory(TextFieldTableCell.forTableColumn());
            httpParamCol.setCellValueFactory(new PropertyValueFactory<>("httpParameter"));
            httpParamCol.setCellFactory(c -> new CheckBoxTableCell<>());

            // Панель настроек
            nameFld.textProperty().bindBidirectional(script.nameProperty());
            descriptionArea.textProperty().bindBidirectional(script.descriptionProperty());

            // Область вывода графики
            IntegerStringConverter converter = new IntegerStringConverter() {
                @Override
                public Integer fromString(String value) {
                    if (value == null || value.isEmpty()) {
                        return 0;
                    }
                    value = value.trim();
                    Integer result = Integer.valueOf(value);
                    if (result < 0) {
                        return 0;
                    }
                    return result;
                }
            };

            plotWidthFld.setTextFormatter(new TextFormatter<>(converter));
            plotHeightFld.setTextFormatter(new TextFormatter<>(converter));

            plotWidthFld.setText(String.valueOf(script.getPlotWidth()));
            plotHeightFld.setText(String.valueOf(script.getPlotHeight()));

            plotWidthFld.layoutXProperty().bind(plotWidthLbl.layoutXProperty().add(plotWidthLbl.widthProperty().add(10)));
            plotHeightFld.layoutXProperty().bind(plotHeightLbl.layoutXProperty().add(plotHeightLbl.widthProperty().add(10)));

            // Затемнение табличного поля при выполнении
            veil.visibleProperty().bind(executeScriptService.runningProperty());
            progressIndicator.visibleProperty().bind(executeScriptService.runningProperty());

            runBtn.disableProperty().bind(executeScriptService.runningProperty());
            stopBtn.disableProperty().bind(executeScriptService.runningProperty().not());
        }
    }

    private class ScriptWebAccessForm extends SplitPane {
        @FXML
        CheckBox webAccessCkBx;
        @FXML
        TextField resourceNameFld;
        @FXML
        CheckBox defaultTemplateCkBx;
        @FXML
        WebView templateEditor;

        // Список ролей доступа
        @FXML
        Button addRoleBtn;
        @FXML
        Button removeRoleBtn;
        @FXML
        ListView<String> rolesList;

        ScriptWebAccessForm() {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("fxml/ScriptWebAccessForm.fxml"));
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
            webAccessCkBx.selectedProperty().bindBidirectional(script.webAccessProperty());
            defaultTemplateCkBx.selectedProperty().bindBidirectional(script.useDefaultTemplateProperty());
            resourceNameFld.textProperty().bindBidirectional(script.resourceNameProperty());

            WebEngine we = templateEditor.getEngine();
            try {
                we.load(ScriptTab.class.getResource("/ace_ftl/editor_ftl.html").toURI().toURL().toString());
            } catch (MalformedURLException | URISyntaxException e) {
                e.printStackTrace();
            }

            rolesList.setItems(script.getSecurityRoles());
            rolesList.setEditable(true);
            rolesList.setCellFactory(param -> new SecurityRoleListCell());

            resourceNameFld.disableProperty().bind(script.webAccessProperty().not());
            defaultTemplateCkBx.disableProperty().bind(script.webAccessProperty().not());
            addRoleBtn.disableProperty().bind(script.webAccessProperty().not());
            removeRoleBtn.disableProperty().bind(script.webAccessProperty().not());
            rolesList.disableProperty().bind(script.webAccessProperty().not());
        }
    }

    private class SecurityRoleListCell extends TextFieldListCell<String> {
        private TextField textField;

        @Override
        public void startEdit() {
            super.startEdit();
            if (textField == null) {
                createTextField();
            }
            setGraphic(textField);
            if (getItem() != null) {
                textField.setText(getItem());
            }
            textField.selectAll();
            textField.requestFocus();
        }

        private TextField createTextField() {
            textField = new TextField();
            UnaryOperator<TextFormatter.Change> filter = change -> {
                String newText = change.getControlNewText();
                if (newText.matches("^[a-zA-Z0-9_]*$")) {
                    return change;
                }
                return null;
            };
            textField.setTextFormatter(new TextFormatter<>(new DefaultStringConverter(), getItem(), filter));
            // При нажатии Enter
            textField.setOnAction(e -> {
                commitEdit(textField.getText());
                e.consume();
            });
            return textField;
        }

    }

    private class ExecuteScriptService extends Service<ScriptResult> {
        @Override
        protected Task<ScriptResult> createTask() {
            return new Task<ScriptResult>() {
                @Override
                protected ScriptResult call() throws Exception {
                    String plotWidthStr = generalForm.plotWidthFld.getText();
                    String plotHeightStr = generalForm.plotHeightFld.getText();

                    int plotWidth = 0;
                    int plotHeight = 0;

                    if (!plotWidthStr.isEmpty() && !plotHeightStr.isEmpty()) {
                        plotWidth = Integer.parseInt(plotWidthStr);
                        plotHeight = Integer.parseInt(plotHeightStr);
                    }
                    if (plotWidth != 0 && plotHeight != 0) {
                        script.setPlotWidth(plotWidth);
                        script.setPlotHeight(plotHeight);
                    }
                    return script.execute(executor);
                }
            };
        }
    }

    // Возвращает выбранный элемент таблицы наборов данных
    DataServiceResource getSelectedDataSet() {
        return generalForm.dataSetsTable.getSelectionModel().getSelectedItem();
    }

    // Возвращает выбранный элемент таблицы параметров
    ScriptParameter getSelectedScriptParameter() {
        return generalForm.parametersTable.getSelectionModel().getSelectedItem();
    }

    // Возвращает таблицу наборов данных
    TableView<DataServiceResource> getDataSetsTable() {
        return generalForm.dataSetsTable;
    }

    // Возвращает таблицу параметров
    TableView<ScriptParameter> getParametersTable() {
        return generalForm.parametersTable;
    }

    // Возвращает контекстное меню таблицы наборов данных
    ContextMenu getDataSetsTableContextMenu() {
        return generalForm.dataSetsCtxMenu;
    }

    // Возвращает элемент "Открыть" контекстного меню таблицы наборов данных
    MenuItem getDataSetsCtxMenuOpenItem() {
        return generalForm.dataSetsCtxMenuOpenItem;
    }

    // Возвращает элемент "Удалить" контекстного меню таблицы наборов данных
    MenuItem getDataSetsCtxMenuDeleteItem() {
        return generalForm.dataSetsCtxMenuDeleteItem;
    }

    // Возвращает элемент "Копировать имя" контекстного меню таблицы наборов данных
    MenuItem getDataSetsCtxMenuCopyNameItem() {
        return generalForm.dataSetsCtxMenuCopyNameItem;
    }

    // Возвращает контекстное меню таблицы параметров
    ContextMenu getParametersContextMenu() {
        return generalForm.parametersCtxMenu;
    }

    // Возвращает элемент "Копировать имя" контекстного меню таблицы параметров
    MenuItem getParametersCtxMenuCopyNameItem() {
        return generalForm.parametersCtxMenuCopyNameItem;
    }

    // Возвращает имя веб-ресурса
    TextField getResourceNameField() {
        return webAccessForm.resourceNameFld;
    }

}
