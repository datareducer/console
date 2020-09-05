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

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import ru.datareducer.dataservice.client.DataServiceClient;
import ru.datareducer.dataservice.entity.*;
import ru.datareducer.model.BooleanExpressionToken;
import ru.datareducer.model.DataServiceResource;
import ru.datareducer.model.ScriptParameter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Окно ресурса REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
public class DataServiceResourceWindow implements Window<DataServiceResource> {
    private final DataServiceResource dataServiceResource;

    private final Stage stage;
    private final DataServiceResourceForm form;
    private final Stage primaryStage;
    private final LoadResourceService loadResourceService;

    // Если Истина - это окно набора данных скрипта R. Если Ложь - это окно просмотра данных ресурса 1С.
    private final boolean isDataSetWindow;

    private Alert alert;

    public DataServiceResourceWindow(DataServiceResource dataServiceResource, Stage primaryStage) {
        if (dataServiceResource == null) {
            throw new IllegalArgumentException("Значение параметра 'dataServiceResource': null");
        }
        if (primaryStage == null) {
            throw new IllegalArgumentException("Значение параметра 'primaryStage': null");
        }
        this.dataServiceResource = dataServiceResource;
        this.stage = new Stage();
        this.isDataSetWindow = dataServiceResource.getId() != 0;
        this.form = new DataServiceResourceForm();
        this.primaryStage = primaryStage;
        this.loadResourceService = new LoadResourceService();

        initialize();
    }

    private void initialize() {
        Scene scene = new Scene(form, primaryStage.getWidth() / 1.5, primaryStage.getHeight() / 1.3);

        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            scene.getStylesheets().add("css/linux.css");
        } else {
            scene.getStylesheets().add("css/windows.css");
        }

        stage.titleProperty().bind(dataServiceResource.nameProperty());
        stage.setScene(scene);
        stage.initOwner(primaryStage);

        //if (isDataSetWindow) {
        //    stage.initModality(Modality.WINDOW_MODAL);
        //}

        registerButtonBarEventHandlers();
        registerFieldsPaneEventHandlers();
        registerServiceEventHandlers();
        registerResourceDataTableEventHandlers();
    }

    void setParametersLookup(Map<String, ScriptParameter> parametersLookup) {
        dataServiceResource.setParametersLookup(parametersLookup);
    }

    // Обработчики главной панели кнопок
    private void registerButtonBarEventHandlers() {
        // Кнопка "Загрузить"
        form.loadBtn.setOnAction(e -> {
            if (updateDataServiceResource()) {
                loadResourceService.restart();
            }
        });
    }

    boolean updateDataServiceResource() {
        DataServiceEntity entity = dataServiceResource.getDataServiceEntity();

        // Проверяем набор полей запроса.
        if (dataServiceResource.getRequestedFields().isEmpty()) {
            errorWindow("Набор полей пуст!", "Выберите поля ресурса");
            form.accordion.setExpandedPane(form.fieldsPane);
            return false;
        }

        // Устанавливаем время кэширования
        dataServiceResource.setCacheLifetime(Long.parseLong(form.cacheFld.getText()));

        // Валидируем и устанавливаем отборы данных.
        TreeItem<BooleanExpressionToken> conditionRoot = form.conditionView.getFilterTreeTableRoot();
        if (conditionRoot != null) {
            TreeItem<BooleanExpressionToken> invalidNode = ConditionView.validateConditionTree(conditionRoot);
            if (invalidNode != null) {
                errorWindow("Ошибка условий отбора", "Поля условия отбора не заполнены");
                form.accordion.setExpandedPane(form.conditionPane);
                form.conditionView.clearFilterTreeTableSelection();
                form.conditionView.selectFilterTreeNode(invalidNode);
                return false;
            }
            dataServiceResource.setCondition(BooleanExpressionToken.filterTreeToCondition(conditionRoot));
        } else {
            dataServiceResource.setCondition(new Condition());
        }

        // Устанавливаем параметры виртуальных таблиц
        if (entity instanceof AccountingRegisterTurnovers || entity instanceof AccountingRegisterDrCrTurnovers) {
            TreeItem<BooleanExpressionToken> accountConditionRoot = form.accountConditionView.getFilterTreeTableRoot();
            if (accountConditionRoot != null) {
                TreeItem<BooleanExpressionToken> invalidNode = ConditionView.validateConditionTree(accountConditionRoot);
                if (invalidNode != null) {
                    errorWindow("Ошибка условий отбора по счетам", "Поля условия отбора не заполнены");
                    form.accordion.setExpandedPane(form.accountConditionPane);
                    form.accountConditionView.clearFilterTreeTableSelection();
                    form.accountConditionView.selectFilterTreeNode(invalidNode);
                    return false;
                }
                dataServiceResource.setAccountCondition(BooleanExpressionToken.filterTreeToCondition(accountConditionRoot));
            } else {
                dataServiceResource.setAccountCondition(new Condition());
            }
            TreeItem<BooleanExpressionToken> balanceAccountConditionRoot = form.balanceAccountConditionView.getFilterTreeTableRoot();
            if (balanceAccountConditionRoot != null) {
                TreeItem<BooleanExpressionToken> invalidNode = ConditionView.validateConditionTree(balanceAccountConditionRoot);
                if (invalidNode != null) {
                    errorWindow("Ошибка условий отбора по корреспондирующим счетам", "Поля условия отбора не заполнены");
                    form.accordion.setExpandedPane(form.balanceAccountConditionPane);
                    form.balanceAccountConditionView.clearFilterTreeTableSelection();
                    form.balanceAccountConditionView.selectFilterTreeNode(invalidNode);
                    return false;
                }
                dataServiceResource.setBalanceAccountCondition(BooleanExpressionToken.filterTreeToCondition(balanceAccountConditionRoot));
            } else {
                dataServiceResource.setBalanceAccountCondition(new Condition());
            }
        } else if (entity instanceof AccountingRegisterBalance) {
            TreeItem<BooleanExpressionToken> accountConditionRoot = form.accountConditionView.getFilterTreeTableRoot();
            if (accountConditionRoot != null) {
                TreeItem<BooleanExpressionToken> invalidNode = ConditionView.validateConditionTree(accountConditionRoot);
                if (invalidNode != null) {
                    errorWindow("Ошибка условий отбора по счетам", "Поля условия отбора не заполнены");
                    form.accordion.setExpandedPane(form.accountConditionPane);
                    form.accountConditionView.clearFilterTreeTableSelection();
                    form.accountConditionView.selectFilterTreeNode(invalidNode);
                    return false;
                }
                dataServiceResource.setAccountCondition(BooleanExpressionToken.filterTreeToCondition(accountConditionRoot));
            } else {
                dataServiceResource.setAccountCondition(new Condition());
            }
        }

        if (entity instanceof AccumulationRegisterBalance
                || entity instanceof AccountingRegisterBalance) {
            dataServiceResource.setBalancePeriod(form.balancePeriodDtPkr.getStringValue());
        } else if (entity instanceof AccumulationRegisterTurnovers
                || entity instanceof AccumulationRegisterBalanceAndTurnovers
                || entity instanceof AccountingRegisterTurnovers
                || entity instanceof AccountingRegisterBalanceAndTurnovers
                || entity instanceof AccountingRegisterRecordsWithExtDimensions
                || entity instanceof AccountingRegisterDrCrTurnovers) {
            dataServiceResource.setTurnoversStartPeriod(form.turnoversStartPeriodDtPkr.getStringValue());
            dataServiceResource.setTurnoversEndPeriod(form.turnoversEndPeriodDtPkr.getStringValue());
        } else if (entity instanceof InformationRegisterVirtualTable) {
            dataServiceResource.setSlicePeriod(form.slicePeriodDtPkr.getStringValue());
        }

        if (entity instanceof AccountingRegisterRecordsWithExtDimensions) {
            int top;
            try {
                top = Integer.parseInt(form.topFld.textProperty().get());
            } catch (NumberFormatException e) {
                errorWindow("Ошибка установки параметров", "Некорректное значение максимального количества записей");
                return false;
            }
            dataServiceResource.setTop(top);
        }

        return true;
    }

    // Обработчики событий панели выбора полей
    private void registerFieldsPaneEventHandlers() {
        // Кнопка выбора одного поля
        form.selectBtn.setOnAction(e -> {
            Field field = form.getSelectedPresentedField();
            if (field != null) {
                dataServiceResource.selectField(field);
            }
        });
        // Кнопка отмены выбора одного поля
        form.returnBtn.setOnAction(e -> {
            Field field = form.getSelectedRequestedField();
            if (field != null) {
                dataServiceResource.returnField(field);
            }
        });
        // Кнопка выбора всех полей
        form.selectAllBtn.setOnAction(e -> dataServiceResource.selectAllFields());
        // Кнопка отмены выбора всех полей
        form.returnAllBtn.setOnAction(e -> dataServiceResource.returnAllFields());

        form.presentedFieldsTable.setRowFactory(p -> {
            TableRow<Field> row = new TableRow<>();
            registerPresentedFieldsTableRowEventHandlers(row);
            return row;
        });

        form.requestedFieldsTable.setRowFactory(p -> {
            TableRow<Field> row = new TableRow<>();
            registerRequestedFieldsTableRowEventHandlers(row);
            return row;
        });
    }

    private void registerPresentedFieldsTableRowEventHandlers(TableRow<Field> row) {
        row.setOnMouseClicked(e -> {
            // Двойной клик по строке таблицы доступных к выбору полей
            if (e.getClickCount() == 2 && !row.isEmpty()) {
                dataServiceResource.selectField(row.getItem());
            }
        });
    }

    private void registerRequestedFieldsTableRowEventHandlers(TableRow<Field> row) {
        row.setOnMouseClicked(e -> {
            // Двойной клик по строке таблицы выбранных полей
            if (e.getClickCount() == 2 && !row.isEmpty()) {
                dataServiceResource.returnField(row.getItem());
            }
        });
    }

    // Обработчики событий сервиса загрузки данных ресурса
    private void registerServiceEventHandlers() {
        loadResourceService.setOnSucceeded(e -> {
            TableView<Map> resourceDataTable = form.resourceDataTable;
            resourceDataTable.getColumns().clear();

            DataServiceResponse response = loadResourceService.getValue();

            for (Field field : response.getDataTableFields()) {
                TableColumn<Map, Object> column = new TableColumn<>();
                column.setText(field.getName());
                column.setCellValueFactory(new MapValueFactory<>(field));

                column.setCellFactory(param -> new TextFieldTableCell<>(new StringConverter<Object>() {
                    @Override
                    public String toString(Object object) {
                        if (object == null) {
                            return "";
                        } else if (object instanceof Instant) {
                            return DataServiceClient.DATE_TIME_FORMATTER.format((Instant) object);
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

                resourceDataTable.getColumns().add(column);
            }
            resourceDataTable.setItems(FXCollections.observableArrayList(response.asDataTable()));
        });

        loadResourceService.setOnFailed(e -> {
            Throwable ex = e.getSource().getException();
            errorWindow("Не удалось загрузить", ex.getMessage());
            ex.printStackTrace();
        });

        // Затемнение табличного поля при выполнении запроса
        form.veil.visibleProperty().bind(loadResourceService.runningProperty());
        form.progressIndicator.visibleProperty().bind(loadResourceService.runningProperty());
    }

    // Обработчики событий таблицы вывода данных ресурса
    private void registerResourceDataTableEventHandlers() {
        // Контекстное меню - Копировать (содержимое ячейки в буфер обмена)
        form.resourceDataTableCtxMenuCopyItem.setOnAction(e -> {
            TablePosition pos = form.resourceDataTable.getSelectionModel().getSelectedCells().get(0);
            Map<Field, Object> map = form.resourceDataTable.getItems().get(pos.getRow());
            Object data = pos.getTableColumn().getCellObservableValue(map).getValue();

            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();

            String strData = "";
            if (data != null) {
                if (data instanceof Instant) {
                    strData = DataServiceClient.DATE_TIME_FORMATTER.format((Instant) data);
                } else if (data instanceof Double) {
                    // Убираем экспоненциальный формат
                    strData = String.format("%.2f", data);
                } else {
                    strData = data.toString();
                }
            }
            content.putString(strData);

            clipboard.setContent(content);
        });

        form.resourceDataTable.setRowFactory(p -> {
            TableRow<Map> row = new TableRow<>();
            registerResourceDataTableRowEventHandlers(row);
            return row;
        });
    }

    private void registerResourceDataTableRowEventHandlers(TableRow<Map> row) {
        row.setOnMouseClicked(e -> {
            // Вызов контекстного меню правой клавишей мыши
            if (e.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                form.resourceDataTableCtxMenu.show(row, e.getScreenX(), e.getScreenY());
            }
        });
    }

    @Override
    public DataServiceResource getEntity() {
        return dataServiceResource;
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

    ReadOnlyBooleanProperty showingProperty() {
        return stage.showingProperty();
    }

    boolean isDataSetWindow() {
        return isDataSetWindow;
    }

    TextField getNameField() {
        return form.nameFld;
    }

    Stage getStage() {
        return stage;
    }

    TableView<Map> getResourceDataTable() {
        return form.resourceDataTable;
    }

    /*
     * Форма ресурса REST-сервиса 1С
     */
    private class DataServiceResourceForm extends VBox {
        @FXML
        private SplitPane splitPane;
        @FXML
        private TableView<Map> resourceDataTable;
        @FXML
        private ContextMenu resourceDataTableCtxMenu;
        @FXML
        private MenuItem resourceDataTableCtxMenuCopyItem;
        @FXML
        private Button loadBtn;
        @FXML
        private Accordion accordion;

        // Панель выбора полей
        @FXML
        private TitledPane fieldsPane;
        @FXML
        private TableView<Field> presentedFieldsTable;
        @FXML
        private TableColumn<Field, String> prdFldNameCol;
        @FXML
        private TableColumn<Field, String> prdFldTypeCol;
        @FXML
        private TableView<Field> requestedFieldsTable;
        @FXML
        private TableColumn<Field, String> reqFldNameCol;
        @FXML
        private TableColumn<Field, String> reqFldTypeCol;
        @FXML
        private TableColumn<Field, Boolean> reqFldPresCol;
        @FXML
        private Button selectBtn;
        @FXML
        private Button selectAllBtn;
        @FXML
        private Button returnBtn;
        @FXML
        private Button returnAllBtn;

        // Панель отборов
        @FXML
        private TitledPane conditionPane;
        @FXML
        private ConditionView conditionView;

        // Панель настроек
        @FXML
        private TitledPane settingsPane;
        @FXML
        private Label mnemonicNameLbl;
        @FXML
        private Label infoBaseLbl;
        @FXML
        private TextField nameFld;
        @FXML
        private TextField cacheFld;
        @FXML
        private CheckBox allowedOnlyCkBx;
        @FXML
        private Label slicePeriodLbl;
        @FXML
        private DateTimePicker slicePeriodDtPkr;
        @FXML
        private Label balancePeriodLbl;
        @FXML
        private DateTimePicker balancePeriodDtPkr;
        @FXML
        private Label turnoversStartPeriodLbl;
        @FXML
        private DateTimePicker turnoversStartPeriodDtPkr;
        @FXML
        private Label turnoversEndPeriodLbl;
        @FXML
        private DateTimePicker turnoversEndPeriodDtPkr;
        @FXML
        private Label topLbl;
        @FXML
        private TextField topFld;

        @FXML
        private Region veil;
        @FXML
        private ProgressIndicator progressIndicator;

        // Панель условия отбора по счетам таблицы оборотов регистра бухгалтерии
        private TitledPane balanceAccountConditionPane;
        private ConditionView balanceAccountConditionView;

        // Панель условия отбора по корреспондирующим счетам таблицы оборотов регистра бухгалтерии
        private TitledPane accountConditionPane;
        private ConditionView accountConditionView;

        // Панель выбора уникальных идентификаторов видов субконто таблицы оборотов регистра бухгалтерии
        private TitledPane extraDimensionsPane;
        private ExtraDimensionsView extraDimensionsView;

        // Панель выбора уникальных идентификаторов корреспондирующих видов субконто таблицы оборотов регистра бухгалтерии
        private TitledPane balancedExtraDimensionsPane;
        private ExtraDimensionsView balancedExtraDimensionsView;

        // Панель ввода имен колонок, по которым будет выполняться сортировка проводок
        // виртуальной таблицы движений с субконто регистра бухгалтерии.
        private TitledPane orderByPane;
        private StringItemsListView orderByView;

        // Панель ввода имен измерений основного регистра расчета, по которому строится таблица базовых данных
        // регистра расчета
        private TitledPane mainRegisterDimensionsPane;
        private StringItemsListView mainRegisterDimensionsView;

        // Панель ввода измерений базового регистра расчета, по которому строится таблица базовых данных
        // регистра расчета
        private TitledPane baseRegisterDimensionsPane;
        private StringItemsListView baseRegisterDimensionsView;

        // Панель ввода имен полей базового регистра расчета, по которым производится суммирование базовых данных
        // регистра расчета
        private TitledPane viewPointsPane;
        private StringItemsListView viewPointsView;

        DataServiceResourceForm() {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("fxml/DataServiceResourceForm.fxml"));
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
            accordion.setExpandedPane(settingsPane);

            DataServiceEntity entity = dataServiceResource.getDataServiceEntity();

            /* Панель выбора полей */
            if (entity.isVirtual() && !(entity instanceof InformationRegisterVirtualTable)
                    && !(entity instanceof AccountingRegisterBalanceAndTurnovers)
                    && !(entity instanceof AccountingRegisterExtDimensions)
                    && !(entity instanceof AccountingRegisterRecordsWithExtDimensions)
                    && !(entity instanceof CalculationRegisterBaseRegister)) {
                fieldsPane.setText("Выбор измерений");
            }

            prdFldNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            prdFldTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
            reqFldNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            reqFldTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

            requestedFieldsTable.setEditable(true);
            reqFldPresCol.setCellValueFactory(cellData -> {
                ObservableValue<Boolean> value = new ReadOnlyBooleanWrapper(cellData.getValue().isPresentation());
                value.addListener((observable, oldValue, newValue) -> cellData.getValue().setPresentation(newValue));
                return value;
            });
            reqFldPresCol.setCellFactory(CheckBoxTableCell.forTableColumn(reqFldPresCol));

            prdFldNameCol.setSortable(false);
            prdFldTypeCol.setSortable(false);
            reqFldNameCol.setSortable(false);
            reqFldTypeCol.setSortable(false);
            reqFldPresCol.setSortable(false);

            presentedFieldsTable.setItems(dataServiceResource.getPresentedFields());
            requestedFieldsTable.setItems(dataServiceResource.getRequestedFields());

            if (entity instanceof AccumulationRegisterVirtualTable) {
                conditionPane.setText("Ограничение состава исходных записей");
                conditionView.initialize(dataServiceResource, false, false);
            } else if (entity instanceof AccountingRegisterTurnovers || entity instanceof AccountingRegisterDrCrTurnovers) {
                conditionPane.setText("Отбор по значениям субконто и измерений");
                conditionView.initialize(dataServiceResource, false, false);

                accountConditionView = new ConditionView();
                balanceAccountConditionView = new ConditionView();

                accountConditionView.initialize(dataServiceResource, true, false);
                balanceAccountConditionView.initialize(dataServiceResource, false, true);

                accountConditionPane = new TitledPane();
                accountConditionPane.setText("Отбор по счетам");
                accountConditionPane.setContent(accountConditionView);
                accountConditionPane.setAnimated(false);

                balanceAccountConditionPane = new TitledPane();
                balanceAccountConditionPane.setText("Отбор по корреспондирующим счетам");
                balanceAccountConditionPane.setContent(balanceAccountConditionView);
                balanceAccountConditionPane.setAnimated(false);

                accordion.getPanes().add(3, accountConditionPane);
                accordion.getPanes().add(4, balanceAccountConditionPane);

                extraDimensionsView = new ExtraDimensionsView(dataServiceResource.getExtraDimensions());
                balancedExtraDimensionsView = new ExtraDimensionsView(dataServiceResource.getBalancedExtraDimensions());

                extraDimensionsPane = new TitledPane();
                extraDimensionsPane.setText("Виды субконто");
                extraDimensionsPane.setContent(extraDimensionsView);
                extraDimensionsPane.setAnimated(false);

                balancedExtraDimensionsPane = new TitledPane();
                balancedExtraDimensionsPane.setText("Корреспондирующие виды субконто");
                balancedExtraDimensionsPane.setContent(balancedExtraDimensionsView);
                balancedExtraDimensionsPane.setAnimated(false);

                accordion.getPanes().add(5, extraDimensionsPane);
                accordion.getPanes().add(6, balancedExtraDimensionsPane);

                splitPane.setDividerPosition(0, 0.6);
            } else if (entity instanceof AccountingRegisterBalance) {
                conditionPane.setText("Отбор по значениям субконто и измерений");
                conditionView.initialize(dataServiceResource, false, false);

                accountConditionView = new ConditionView();
                balanceAccountConditionView = new ConditionView();

                accountConditionView.initialize(dataServiceResource, true, false);
                balanceAccountConditionView.initialize(dataServiceResource, false, true);

                accountConditionPane = new TitledPane();
                accountConditionPane.setText("Отбор по счетам");
                accountConditionPane.setContent(accountConditionView);
                accountConditionPane.setAnimated(false);

                accordion.getPanes().add(3, accountConditionPane);

                extraDimensionsView = new ExtraDimensionsView(dataServiceResource.getExtraDimensions());

                extraDimensionsPane = new TitledPane();
                extraDimensionsPane.setText("Виды субконто");
                extraDimensionsPane.setContent(extraDimensionsView);
                extraDimensionsPane.setAnimated(false);

                accordion.getPanes().add(4, extraDimensionsPane);

                splitPane.setDividerPosition(0, 0.5);
            } else {
                conditionView.initialize(dataServiceResource, false, false);
            }

            if (entity instanceof AccountingRegisterRecordsWithExtDimensions) {
                orderByView = new StringItemsListView();
                orderByView.initialize(dataServiceResource.getOrderByList());

                orderByPane = new TitledPane();
                orderByPane.setText("Сортировка проводок");
                orderByPane.setContent(orderByView);
                orderByPane.setAnimated(false);

                accordion.getPanes().add(3, orderByPane);
            }

            if (entity instanceof CalculationRegisterBaseRegister) {
                mainRegisterDimensionsView = new StringItemsListView();
                mainRegisterDimensionsView.initialize(dataServiceResource.getMainRegisterDimensionsList());

                mainRegisterDimensionsPane = new TitledPane();
                mainRegisterDimensionsPane.setText("Измерения основного регистра расчета");
                mainRegisterDimensionsPane.setContent(mainRegisterDimensionsView);
                mainRegisterDimensionsPane.setAnimated(false);

                accordion.getPanes().add(3, mainRegisterDimensionsPane);

                baseRegisterDimensionsView = new StringItemsListView();
                baseRegisterDimensionsView.initialize(dataServiceResource.getBaseRegisterDimensionsList());

                baseRegisterDimensionsPane = new TitledPane();
                baseRegisterDimensionsPane.setText("Измерения базового регистра расчета");
                baseRegisterDimensionsPane.setContent(baseRegisterDimensionsView);
                baseRegisterDimensionsPane.setAnimated(false);

                accordion.getPanes().add(4, baseRegisterDimensionsPane);

                viewPointsView = new StringItemsListView();
                viewPointsView.initialize(dataServiceResource.getViewPointsList());

                viewPointsPane = new TitledPane();
                viewPointsPane.setText("Поля суммирования базовых данных");
                viewPointsPane.setContent(viewPointsView);
                viewPointsPane.setAnimated(false);

                accordion.getPanes().add(5, viewPointsPane);

                splitPane.setDividerPosition(0, 0.5);
            }

            /* Панель настроек */
            mnemonicNameLbl.textProperty().setValue(entity.getMnemonicName());
            infoBaseLbl.textProperty().bind(dataServiceResource.getInfoBase().nameProperty());

            nameFld.textProperty().bindBidirectional(dataServiceResource.nameProperty());
            nameFld.setDisable(!isDataSetWindow());

            allowedOnlyCkBx.selectedProperty().bindBidirectional(dataServiceResource.allowedOnlyProperty());

            cacheFld.setTextFormatter(new TextFormatter<>(new IntegerStringConverter() {
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
            }, 0));

            if (entity instanceof AccountingRegisterRecordsWithExtDimensions) {
                topLbl.setVisible(true);
                topFld.textProperty().setValue(Integer.toString(dataServiceResource.getTop()));
                topFld.setVisible(true);
            }

            // При просмотре данных ресурса не кэшируем их.
            if (!isDataSetWindow) {
                cacheFld.setDisable(true);
            } else {
                cacheFld.setText(String.valueOf(dataServiceResource.getCacheLifetime()));
            }
            if (entity instanceof InformationRegisterVirtualTable) {
                slicePeriodDtPkr.setStringValue(dataServiceResource.getSlicePeriod());
                slicePeriodLbl.setVisible(true);
                slicePeriodDtPkr.setVisible(true);
            } else if (entity instanceof AccumulationRegisterBalance
                    || entity instanceof AccountingRegisterBalance) {
                balancePeriodDtPkr.setStringValue(dataServiceResource.getBalancePeriod());
                balancePeriodLbl.setVisible(true);
                balancePeriodDtPkr.setVisible(true);
            } else if (entity instanceof AccumulationRegisterTurnovers
                    || entity instanceof AccumulationRegisterBalanceAndTurnovers
                    || entity instanceof AccountingRegisterTurnovers
                    || entity instanceof AccountingRegisterBalanceAndTurnovers
                    || entity instanceof AccountingRegisterRecordsWithExtDimensions
                    || entity instanceof AccountingRegisterDrCrTurnovers) {
                turnoversStartPeriodDtPkr.setStringValue(dataServiceResource.getTurnoversStartPeriod());
                turnoversEndPeriodDtPkr.setStringValue(dataServiceResource.getTurnoversEndPeriod());
                turnoversStartPeriodLbl.setVisible(true);
                turnoversStartPeriodDtPkr.setVisible(true);
                turnoversEndPeriodLbl.setVisible(true);
                turnoversEndPeriodDtPkr.setVisible(true);
            }

            // Таблица вывода данных
            resourceDataTable.getSelectionModel().setCellSelectionEnabled(true);
        }

        Field getSelectedPresentedField() {
            return presentedFieldsTable.getSelectionModel().getSelectedItem();
        }

        Field getSelectedRequestedField() {
            return requestedFieldsTable.getSelectionModel().getSelectedItem();
        }

    }

    private class LoadResourceService extends Service<DataServiceResponse> {
        @Override
        protected Task<DataServiceResponse> createTask() {
            return new Task<DataServiceResponse>() {
                @Override
                protected DataServiceResponse call() throws Exception {
                    return dataServiceResource.getResourceData();
                }
            };
        }
    }

    private void errorWindow(String header, String content) {
        if (alert == null) {
            alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(stage);
        }
        alert.setTitle(stage.getTitle());
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.setResizable(true);
        alert.showAndWait();
    }

    private void placeWindow() {
        double x = primaryStage.getX() + (primaryStage.getWidth() - stage.getWidth()) / 2.0;
        double y = primaryStage.getY() + (primaryStage.getHeight() - stage.getHeight()) / 2.0 - 50;
        stage.setX(x);
        stage.setY(y);
    }

}
