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

import com.datareducer.dataservice.entity.*;
import com.datareducer.model.InfoBase;
import com.datareducer.model.ReducerConfiguration;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import java.io.IOException;

public class MetadataForm extends StackPane implements ModelReplacedListener {
    private final String FIELDS_GROUP_NAME = "Поля";
    private final String TABULAR_SECTIONS_GROUP_NAME = "Табличные части";
    private final String DIMENSIONS_GROUP_NAME = "Измерения";
    private final String RESOURCES_GROUP_NAME = "Ресурсы";
    private final String EXT_DIMENSIONS_GROUP_NAME = "Субконто";
    private final String RECALCULATIONS_GROUP_NAME = "Перерасчеты";
    private final String BASE_REGISTERS_GROUP_NAME = "Базовые данные";

    private final LoadMetadataTreeService loadMetadataTreeService;

    private InfoBase infoBase;

    @FXML
    private TreeTableView<DataServiceEntity> metadataTreeTable;
    @FXML
    private Region veil;
    @FXML
    private Label placeholderLbl;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TreeTableColumn<DataServiceEntity, String> nameCol;
    @FXML
    private TreeTableColumn<DataServiceEntity, String> typeCol;
    @FXML
    private TreeTableColumn<DataServiceEntity, String> refCol;

    // Контекстное меню ресурса
    @FXML
    private ContextMenu entityCtxMenu;
    @FXML
    private MenuItem showDataItem;
    @FXML
    private MenuItem addToScriptItem;

    // Контекстное меню поля
    @FXML
    private ContextMenu fieldCtxMenu;
    @FXML
    private MenuItem copyNameItem;

    private TreeItem<DataServiceEntity> rootNode;

    private final TreeItem<DataServiceEntity> constantGroupNode;
    private final TreeItem<DataServiceEntity> catalogGroupNode;
    private final TreeItem<DataServiceEntity> documentGroupNode;
    private final TreeItem<DataServiceEntity> documentJournalGroupNode;
    private final TreeItem<DataServiceEntity> chartOfCharacteristicTypesGroupNode;
    private final TreeItem<DataServiceEntity> chartOfAccountsGroupNode;
    private final TreeItem<DataServiceEntity> chartOfCalculationTypesGroupNode;
    private final TreeItem<DataServiceEntity> informationRegisterGroupNode;
    private final TreeItem<DataServiceEntity> accumulationRegisterGroupNode;
    private final TreeItem<DataServiceEntity> accountingRegisterGroupNode;
    private final TreeItem<DataServiceEntity> calculationRegisterGroupNode;
    private final TreeItem<DataServiceEntity> exchangePlanGroupNode;
    private final TreeItem<DataServiceEntity> businessProcessGroupNode;
    private final TreeItem<DataServiceEntity> taskGroupNode;

    public MetadataForm() {
        loadMetadataTreeService = new LoadMetadataTreeService();

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("fxml/MetadataForm.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        nameCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("metadataName"));
        typeCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("type"));
        refCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("reference"));

        constantGroupNode = new TreeItem<>(new GroupNode("Константы"));
        catalogGroupNode = new TreeItem<>(new GroupNode("Справочники"));
        documentGroupNode = new TreeItem<>(new GroupNode("Документы"));
        documentJournalGroupNode = new TreeItem<>(new GroupNode("Журналы документов"));
        chartOfCharacteristicTypesGroupNode = new TreeItem<>(new GroupNode("Планы видов характеристик"));
        chartOfAccountsGroupNode = new TreeItem<>(new GroupNode("Планы счетов"));
        chartOfCalculationTypesGroupNode = new TreeItem<>(new GroupNode("Планы видов расчета"));
        informationRegisterGroupNode = new TreeItem<>(new GroupNode("Регистры сведений"));
        accumulationRegisterGroupNode = new TreeItem<>(new GroupNode("Регистры накопления"));
        accountingRegisterGroupNode = new TreeItem<>(new GroupNode("Регистры бухгалтерии"));
        calculationRegisterGroupNode = new TreeItem<>(new GroupNode("Регистры расчета"));
        exchangePlanGroupNode = new TreeItem<>(new GroupNode("Планы обмена"));
        businessProcessGroupNode = new TreeItem<>(new GroupNode("Бизнес-процессы"));
        taskGroupNode = new TreeItem<>(new GroupNode("Задачи"));
    }

    @FXML
    public void initialize() {
        nameCol.setCellFactory(new Callback<TreeTableColumn<DataServiceEntity, String>, TreeTableCell<DataServiceEntity, String>>() {
            // Пиктограмма Реквизитов
            private Image fldImg = new Image(getClass().getClassLoader().getResourceAsStream("image/application_control_bar.png"));
            // Пиктограмма Табличных частей
            private Image tblImg = new Image(getClass().getClassLoader().getResourceAsStream("image/application_view_detail.png"));
            // Пиктограмма Остатков
            private Image balImg = new Image(getClass().getClassLoader().getResourceAsStream("image/chart_bar.png"));
            // Пиктограмма Оборотов
            private Image tovImg = new Image(getClass().getClassLoader().getResourceAsStream("image/coins.png"));
            // Пиктограмма Остатков и оборотов
            private Image batImg = new Image(getClass().getClassLoader().getResourceAsStream("image/finance.png"));
            // Пиктограмма Среза последних
            private Image sllImg = new Image(getClass().getClassLoader().getResourceAsStream("image/formatting_greater_than.png"));
            // Пиктограмма Среза первых
            private Image slfImg = new Image(getClass().getClassLoader().getResourceAsStream("image/formatting_less_than.png"));
            // Пиктограмма Субконто регистра бухгалтерии
            private Image exdImg = new Image(getClass().getClassLoader().getResourceAsStream("image/barchart.png"));
            // Пиктограмма Движений с субконто регистра бухгалтерии
            private Image rwdImg = new Image(getClass().getClassLoader().getResourceAsStream("image/table_chart.png"));
            // Пиктограмма Оборотов ДкКт регистра бухгалтерии
            private Image tdcImg = new Image(getClass().getClassLoader().getResourceAsStream("image/chart_stock.png"));
            // Пиктограмма Данных графика регистра расчета
            private Image csdImg = new Image(getClass().getClassLoader().getResourceAsStream("image/table.png"));
            // Пиктограмма Фактического периода действия регистра расчета
            private Image capImg = new Image(getClass().getClassLoader().getResourceAsStream("image/table_split.png"));
            // Пиктограмма Перерасчетов
            private Image recImg = new Image(getClass().getClassLoader().getResourceAsStream("image/arrow_rotate_clockwise.png"));
            // Пиктограмма Базовых данных регистра расчета
            private Image crrImg = new Image(getClass().getClassLoader().getResourceAsStream("image/formatting_dublicate_value.png"));

            @Override
            public TreeTableCell<DataServiceEntity, String> call(TreeTableColumn<DataServiceEntity, String> param) {
                return new TreeTableCell<DataServiceEntity, String>() {
                    private ImageView fldImgView = new ImageView((fldImg));
                    private ImageView tblImgView = new ImageView((tblImg));
                    private ImageView balImgView = new ImageView((balImg));
                    private ImageView tovImgView = new ImageView((tovImg));
                    private ImageView sllImgView = new ImageView((sllImg));
                    private ImageView slfImgView = new ImageView((slfImg));
                    private ImageView batImgView = new ImageView((batImg));
                    private ImageView exdImgView = new ImageView((exdImg));
                    private ImageView rwdImgView = new ImageView((rwdImg));
                    private ImageView tdcImgView = new ImageView((tdcImg));
                    private ImageView csdImgView = new ImageView((csdImg));
                    private ImageView capImgView = new ImageView((capImg));
                    private ImageView recImgView = new ImageView((recImg));
                    private ImageView crrImgView = new ImageView((crrImg));

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item);
                            switch (itemProperty().getValue()) {
                                case FIELDS_GROUP_NAME:
                                    setGraphic(fldImgView);
                                    break;
                                case TABULAR_SECTIONS_GROUP_NAME:
                                    setGraphic(tblImgView);
                                    break;
                                case RECALCULATIONS_GROUP_NAME:
                                    setGraphic(recImgView);
                                    break;
                                case BASE_REGISTERS_GROUP_NAME:
                                    setGraphic(crrImgView);
                                    break;
                                default:
                                    DataServiceEntity entity = getTreeTableRow().getItem();
                                    if (entity instanceof AccumulationRegisterBalance
                                            || entity instanceof AccountingRegisterBalance) {
                                        setGraphic(balImgView);
                                    } else if (entity instanceof AccumulationRegisterTurnovers
                                            || entity instanceof AccountingRegisterTurnovers) {
                                        setGraphic(tovImgView);
                                    } else if (entity instanceof AccumulationRegisterBalanceAndTurnovers
                                            || entity instanceof AccountingRegisterBalanceAndTurnovers) {
                                        setGraphic(batImgView);
                                    } else if (entity instanceof AccountingRegisterExtDimensions) {
                                        setGraphic(exdImgView);
                                    } else if (entity instanceof AccountingRegisterRecordsWithExtDimensions) {
                                        setGraphic(rwdImgView);
                                    } else if (entity instanceof AccountingRegisterDrCrTurnovers) {
                                        setGraphic(tdcImgView);
                                    } else if (entity instanceof InformationRegisterSliceLast) {
                                        setGraphic(sllImgView);
                                    } else if (entity instanceof InformationRegisterSliceFirst) {
                                        setGraphic(slfImgView);
                                    } else if (entity instanceof CalculationRegisterScheduleData) {
                                        setGraphic(csdImgView);
                                    } else if (entity instanceof CalculationRegisterActualActionPeriod) {
                                        setGraphic(capImgView);
                                    } else {
                                        setGraphic(null);
                                    }
                            }
                        }
                    }
                };
            }
        });

        veil.visibleProperty().bind(loadMetadataTreeService.runningProperty());
        progressIndicator.visibleProperty().bind(loadMetadataTreeService.runningProperty());
        placeholderLbl.visibleProperty().bind(loadMetadataTreeService.runningProperty().not());

        registerEventHandlers();
    }

    private void registerEventHandlers() {
        loadMetadataTreeService.setOnSucceeded(e -> {
            MetadataTree metadataTree = loadMetadataTreeService.getValue();

            // Константы
            for (Constant constant : metadataTree.getConstants()) {
                TreeItem<DataServiceEntity> constantsNode = new TreeItem<>(constant);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                for (Field field : constant.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                constantsNode.getChildren().add(fldGroupNode);
                constantGroupNode.getChildren().add(constantsNode);
            }

            // Справочники
            for (Catalog catalog : metadataTree.getCatalogs()) {
                TreeItem<DataServiceEntity> catalogNode = new TreeItem<>(catalog);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                TreeItem<DataServiceEntity> tblGroupNode = new TreeItem<>(new GroupNode(TABULAR_SECTIONS_GROUP_NAME));
                for (Field field : catalog.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                for (TabularSection tabularSection : catalog.getTabularSections()) {
                    TreeItem<DataServiceEntity> tabularSectionNode = new TreeItem<>(tabularSection);
                    for (Field field : tabularSection.getFields()) {
                        tabularSectionNode.getChildren().add(new TreeItem<>(field));
                    }
                    tblGroupNode.getChildren().add(tabularSectionNode);
                }
                catalogNode.getChildren().add(fldGroupNode);
                if (!tblGroupNode.getChildren().isEmpty()) {
                    catalogNode.getChildren().add(tblGroupNode);
                }
                catalogGroupNode.getChildren().add(catalogNode);
            }

            // Документы
            for (Document document : metadataTree.getDocuments()) {
                TreeItem<DataServiceEntity> documentNode = new TreeItem<>(document);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                TreeItem<DataServiceEntity> tblGroupNode = new TreeItem<>(new GroupNode(TABULAR_SECTIONS_GROUP_NAME));
                for (Field field : document.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                for (TabularSection tabularSection : document.getTabularSections()) {
                    TreeItem<DataServiceEntity> tabularSectionNode = new TreeItem<>(tabularSection);
                    for (Field field : tabularSection.getFields()) {
                        tabularSectionNode.getChildren().add(new TreeItem<>(field));
                    }
                    tblGroupNode.getChildren().add(tabularSectionNode);
                }
                documentNode.getChildren().add(fldGroupNode);
                if (!tblGroupNode.getChildren().isEmpty()) {
                    documentNode.getChildren().add(tblGroupNode);
                }
                documentGroupNode.getChildren().add(documentNode);
            }

            // Журналы документов
            for (DocumentJournal journal : metadataTree.getDocumentJournals()) {
                TreeItem<DataServiceEntity> documentJournalsNode = new TreeItem<>(journal);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                for (Field field : journal.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                documentJournalsNode.getChildren().add(fldGroupNode);
                documentJournalGroupNode.getChildren().add(documentJournalsNode);
            }

            // Планы видов характеристик
            for (ChartOfCharacteristicTypes chartOfCharacteristicTypes : metadataTree.getChartOfCharacteristicTypes()) {
                TreeItem<DataServiceEntity> chartOfCharacteristicTypesNode = new TreeItem<>(chartOfCharacteristicTypes);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                TreeItem<DataServiceEntity> tblGroupNode = new TreeItem<>(new GroupNode(TABULAR_SECTIONS_GROUP_NAME));
                for (Field field : chartOfCharacteristicTypes.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                for (TabularSection tabularSection : chartOfCharacteristicTypes.getTabularSections()) {
                    TreeItem<DataServiceEntity> tabularSectionNode = new TreeItem<>(tabularSection);
                    for (Field field : tabularSection.getFields()) {
                        tabularSectionNode.getChildren().add(new TreeItem<>(field));
                    }
                    tblGroupNode.getChildren().add(tabularSectionNode);
                }
                chartOfCharacteristicTypesNode.getChildren().add(fldGroupNode);
                if (!tblGroupNode.getChildren().isEmpty()) {
                    chartOfCharacteristicTypesNode.getChildren().add(tblGroupNode);
                }
                chartOfCharacteristicTypesGroupNode.getChildren().add(chartOfCharacteristicTypesNode);
            }

            // Планы счетов
            for (ChartOfAccounts chartOfAccounts : metadataTree.getChartOfAccounts()) {
                TreeItem<DataServiceEntity> chartOfAccountsNode = new TreeItem<>(chartOfAccounts);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                TreeItem<DataServiceEntity> tblGroupNode = new TreeItem<>(new GroupNode(TABULAR_SECTIONS_GROUP_NAME));
                for (Field field : chartOfAccounts.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                for (TabularSection tabularSection : chartOfAccounts.getTabularSections()) {
                    TreeItem<DataServiceEntity> tabularSectionNode = new TreeItem<>(tabularSection);
                    for (Field field : tabularSection.getFields()) {
                        tabularSectionNode.getChildren().add(new TreeItem<>(field));
                    }
                    tblGroupNode.getChildren().add(tabularSectionNode);
                }
                chartOfAccountsNode.getChildren().add(fldGroupNode);
                if (!tblGroupNode.getChildren().isEmpty()) {
                    chartOfAccountsNode.getChildren().add(tblGroupNode);
                }
                chartOfAccountsGroupNode.getChildren().add(chartOfAccountsNode);
            }

            // Планы видов расчета
            for (ChartOfCalculationTypes chartOfCalculationTypes : metadataTree.getChartOfCalculationTypes()) {
                TreeItem<DataServiceEntity> chartOfCalculationTypesNode = new TreeItem<>(chartOfCalculationTypes);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                TreeItem<DataServiceEntity> tblGroupNode = new TreeItem<>(new GroupNode(TABULAR_SECTIONS_GROUP_NAME));
                for (Field field : chartOfCalculationTypes.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                for (TabularSection tabularSection : chartOfCalculationTypes.getTabularSections()) {
                    TreeItem<DataServiceEntity> tabularSectionNode = new TreeItem<>(tabularSection);
                    for (Field field : tabularSection.getFields()) {
                        tabularSectionNode.getChildren().add(new TreeItem<>(field));
                    }
                    tblGroupNode.getChildren().add(tabularSectionNode);
                }
                chartOfCalculationTypesNode.getChildren().add(fldGroupNode);
                if (!tblGroupNode.getChildren().isEmpty()) {
                    chartOfCalculationTypesNode.getChildren().add(tblGroupNode);
                }
                chartOfCalculationTypesGroupNode.getChildren().add(chartOfCalculationTypesNode);
            }

            // Регистры сведений
            for (InformationRegister informationRegister : metadataTree.getInformationRegisters()) {
                TreeItem<DataServiceEntity> informationRegisterNode = new TreeItem<>(informationRegister);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                for (Field field : informationRegister.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                informationRegisterNode.getChildren().add(fldGroupNode);

                InformationRegisterSliceLast sliceLast = informationRegister.getSliceLast();
                if (sliceLast != null) {
                    TreeItem<DataServiceEntity> sliceLastNode = new TreeItem<>(sliceLast);
                    for (Field field : sliceLast.getRegisterFields()) {
                        sliceLastNode.getChildren().add(new TreeItem<>(field));
                    }
                    informationRegisterNode.getChildren().add(sliceLastNode);
                }
                InformationRegisterSliceFirst sliceFirst = informationRegister.getSliceFirst();
                if (sliceFirst != null) {
                    TreeItem<DataServiceEntity> sliceFirstNode = new TreeItem<>(sliceFirst);
                    for (Field field : sliceFirst.getRegisterFields()) {
                        sliceFirstNode.getChildren().add(new TreeItem<>(field));
                    }
                    informationRegisterNode.getChildren().add(sliceFirstNode);
                }
                informationRegisterGroupNode.getChildren().add(informationRegisterNode);
            }

            // Регистры накопления
            for (AccumulationRegister accumulationRegister : metadataTree.getAccumulationRegisters()) {
                TreeItem<DataServiceEntity> accumulationRegisterNode = new TreeItem<>(accumulationRegister);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                for (Field field : accumulationRegister.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                accumulationRegisterNode.getChildren().add(fldGroupNode);

                AccumulationRegisterBalance balance = accumulationRegister.getBalance();
                if (balance != null) {
                    TreeItem<DataServiceEntity> balanceNode = new TreeItem<>(balance);
                    TreeItem<DataServiceEntity> dimensionsGroupNode = new TreeItem<>(new GroupNode(DIMENSIONS_GROUP_NAME));
                    TreeItem<DataServiceEntity> resourcesGroupNode = new TreeItem<>(new GroupNode(RESOURCES_GROUP_NAME));
                    for (Field field : balance.getDimensions()) {
                        dimensionsGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    for (Field field : balance.getResources()) {
                        resourcesGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    balanceNode.getChildren().add(dimensionsGroupNode);
                    balanceNode.getChildren().add(resourcesGroupNode);
                    accumulationRegisterNode.getChildren().add(balanceNode);
                }
                AccumulationRegisterTurnovers turnovers = accumulationRegister.getTurnovers();
                if (turnovers != null) {
                    TreeItem<DataServiceEntity> turnoversNode = new TreeItem<>(turnovers);
                    TreeItem<DataServiceEntity> dimensionsGroupNode = new TreeItem<>(new GroupNode(DIMENSIONS_GROUP_NAME));
                    TreeItem<DataServiceEntity> resourcesGroupNode = new TreeItem<>(new GroupNode(RESOURCES_GROUP_NAME));
                    for (Field field : turnovers.getDimensions()) {
                        dimensionsGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    for (Field field : turnovers.getResources()) {
                        resourcesGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    turnoversNode.getChildren().add(dimensionsGroupNode);
                    turnoversNode.getChildren().add(resourcesGroupNode);
                    accumulationRegisterNode.getChildren().add(turnoversNode);
                }
                AccumulationRegisterBalanceAndTurnovers balanceAndTurnovers = accumulationRegister.getBalanceAndTurnovers();
                if (balanceAndTurnovers != null) {
                    TreeItem<DataServiceEntity> balanceAndTurnoversNode = new TreeItem<>(balanceAndTurnovers);
                    TreeItem<DataServiceEntity> dimensionsGroupNode = new TreeItem<>(new GroupNode(DIMENSIONS_GROUP_NAME));
                    TreeItem<DataServiceEntity> resourcesGroupNode = new TreeItem<>(new GroupNode(RESOURCES_GROUP_NAME));
                    for (Field field : balanceAndTurnovers.getDimensions()) {
                        dimensionsGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    for (Field field : balanceAndTurnovers.getResources()) {
                        resourcesGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    balanceAndTurnoversNode.getChildren().add(dimensionsGroupNode);
                    balanceAndTurnoversNode.getChildren().add(resourcesGroupNode);
                    accumulationRegisterNode.getChildren().add(balanceAndTurnoversNode);
                }
                accumulationRegisterGroupNode.getChildren().add(accumulationRegisterNode);
            }

            // Регистры бухгалтерии
            for (AccountingRegister accountingRegister : metadataTree.getAccountingRegisters()) {
                TreeItem<DataServiceEntity> accountingRegisterNode = new TreeItem<>(accountingRegister);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                for (Field field : accountingRegister.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                accountingRegisterNode.getChildren().add(fldGroupNode);
                AccountingRegisterBalance balance = accountingRegister.getBalance();
                if (balance != null) {
                    TreeItem<DataServiceEntity> balanceNode = new TreeItem<>(balance);
                    TreeItem<DataServiceEntity> dimensionsGroupNode = new TreeItem<>(new GroupNode(DIMENSIONS_GROUP_NAME));
                    TreeItem<DataServiceEntity> resourcesGroupNode = new TreeItem<>(new GroupNode(RESOURCES_GROUP_NAME));
                    TreeItem<DataServiceEntity> extDimensionsGroupNode = new TreeItem<>(new GroupNode(EXT_DIMENSIONS_GROUP_NAME));
                    for (Field field : balance.getDimensions()) {
                        dimensionsGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    for (Field field : balance.getExtDimensions()) {
                        extDimensionsGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    for (Field field : balance.getResources()) {
                        resourcesGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    balanceNode.getChildren().add(dimensionsGroupNode);
                    balanceNode.getChildren().add(extDimensionsGroupNode);
                    balanceNode.getChildren().add(resourcesGroupNode);
                    accountingRegisterNode.getChildren().add(balanceNode);
                }
                AccountingRegisterTurnovers turnovers = accountingRegister.getTurnovers();
                if (turnovers != null) {
                    TreeItem<DataServiceEntity> turnoversNode = new TreeItem<>(turnovers);
                    TreeItem<DataServiceEntity> dimensionsGroupNode = new TreeItem<>(new GroupNode(DIMENSIONS_GROUP_NAME));
                    TreeItem<DataServiceEntity> resourcesGroupNode = new TreeItem<>(new GroupNode(RESOURCES_GROUP_NAME));
                    TreeItem<DataServiceEntity> extDimensionsGroupNode = new TreeItem<>(new GroupNode(EXT_DIMENSIONS_GROUP_NAME));
                    for (Field field : turnovers.getDimensions()) {
                        dimensionsGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    for (Field field : turnovers.getExtDimensions()) {
                        extDimensionsGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    for (Field field : turnovers.getResources()) {
                        resourcesGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    turnoversNode.getChildren().add(dimensionsGroupNode);
                    turnoversNode.getChildren().add(extDimensionsGroupNode);
                    turnoversNode.getChildren().add(resourcesGroupNode);
                    accountingRegisterNode.getChildren().add(turnoversNode);
                }
                AccountingRegisterBalanceAndTurnovers balanceAndTurnovers = accountingRegister.getBalanceAndTurnovers();
                if (balanceAndTurnovers != null) {
                    TreeItem<DataServiceEntity> balanceAndTurnoversNode = new TreeItem<>(balanceAndTurnovers);
                    TreeItem<DataServiceEntity> dimensionsGroupNode = new TreeItem<>(new GroupNode(DIMENSIONS_GROUP_NAME));
                    TreeItem<DataServiceEntity> resourcesGroupNode = new TreeItem<>(new GroupNode(RESOURCES_GROUP_NAME));
                    TreeItem<DataServiceEntity> extDimensionsGroupNode = new TreeItem<>(new GroupNode(EXT_DIMENSIONS_GROUP_NAME));
                    for (Field field : balanceAndTurnovers.getDimensions()) {
                        dimensionsGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    for (Field field : balanceAndTurnovers.getExtDimensions()) {
                        extDimensionsGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    for (Field field : balanceAndTurnovers.getResources()) {
                        resourcesGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    balanceAndTurnoversNode.getChildren().add(dimensionsGroupNode);
                    balanceAndTurnoversNode.getChildren().add(extDimensionsGroupNode);
                    balanceAndTurnoversNode.getChildren().add(resourcesGroupNode);
                    accountingRegisterNode.getChildren().add(balanceAndTurnoversNode);
                }
                AccountingRegisterExtDimensions extDimensions = accountingRegister.getExtDimensions();
                if (extDimensions != null) {
                    TreeItem<DataServiceEntity> extDimensionsNode = new TreeItem<>(extDimensions);
                    for (Field field : extDimensions.getVirtualTableFields()) {
                        extDimensionsNode.getChildren().add(new TreeItem<>(field));
                    }
                    accountingRegisterNode.getChildren().add(extDimensionsNode);
                }
                AccountingRegisterRecordsWithExtDimensions recordsWithExtDimensions = accountingRegister.getRecordsWithExtDimensions();
                if (recordsWithExtDimensions != null) {
                    TreeItem<DataServiceEntity> recordsWithExtDimensionsNode = new TreeItem<>(recordsWithExtDimensions);
                    for (Field field : recordsWithExtDimensions.getVirtualTableFields()) {
                        recordsWithExtDimensionsNode.getChildren().add(new TreeItem<>(field));
                    }
                    accountingRegisterNode.getChildren().add(recordsWithExtDimensionsNode);
                }
                AccountingRegisterDrCrTurnovers drCrTurnovers = accountingRegister.getDrCrTurnovers();
                if (drCrTurnovers != null) {
                    TreeItem<DataServiceEntity> turnoversNode = new TreeItem<>(drCrTurnovers);
                    TreeItem<DataServiceEntity> dimensionsGroupNode = new TreeItem<>(new GroupNode(DIMENSIONS_GROUP_NAME));
                    TreeItem<DataServiceEntity> resourcesGroupNode = new TreeItem<>(new GroupNode(RESOURCES_GROUP_NAME));
                    TreeItem<DataServiceEntity> extDimensionsGroupNode = new TreeItem<>(new GroupNode(EXT_DIMENSIONS_GROUP_NAME));
                    for (Field field : drCrTurnovers.getDimensions()) {
                        dimensionsGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    for (Field field : drCrTurnovers.getExtDimensions()) {
                        extDimensionsGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    for (Field field : drCrTurnovers.getResources()) {
                        resourcesGroupNode.getChildren().add(new TreeItem<>(field));
                    }
                    turnoversNode.getChildren().add(dimensionsGroupNode);
                    turnoversNode.getChildren().add(extDimensionsGroupNode);
                    turnoversNode.getChildren().add(resourcesGroupNode);
                    accountingRegisterNode.getChildren().add(turnoversNode);
                }

                accountingRegisterGroupNode.getChildren().add(accountingRegisterNode);
            }

            // Регистры расчета
            for (CalculationRegister calculationRegister : metadataTree.getCalculationRegisters()) {
                TreeItem<DataServiceEntity> calculationRegisterNode = new TreeItem<>(calculationRegister);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                for (Field field : calculationRegister.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                calculationRegisterNode.getChildren().add(fldGroupNode);

                CalculationRegisterScheduleData scheduleData = calculationRegister.getScheduleData();
                if (scheduleData != null) {
                    TreeItem<DataServiceEntity> scheduleDataNode = new TreeItem<>(scheduleData);
                    for (Field field : scheduleData.getVirtualTableFields()) {
                        scheduleDataNode.getChildren().add(new TreeItem<>(field));
                    }
                    calculationRegisterNode.getChildren().add(scheduleDataNode);
                }
                CalculationRegisterActualActionPeriod actualActionPeriod = calculationRegister.getActualActionPeriod();
                if (actualActionPeriod != null) {
                    TreeItem<DataServiceEntity> actualActionPeriodNode = new TreeItem<>(actualActionPeriod);
                    for (Field field : scheduleData.getVirtualTableFields()) {
                        actualActionPeriodNode.getChildren().add(new TreeItem<>(field));
                    }
                    calculationRegisterNode.getChildren().add(actualActionPeriodNode);
                }
                if (!calculationRegister.getRecalculations().isEmpty()) {
                    TreeItem<DataServiceEntity> recalcGroupNode = new TreeItem<>(new GroupNode(RECALCULATIONS_GROUP_NAME));
                    for (CalculationRegisterRecalculation recalculation : calculationRegister.getRecalculations()) {
                        TreeItem<DataServiceEntity> recalcNode = new TreeItem<>(recalculation);
                        for (Field field : recalculation.getVirtualTableFields()) {
                            recalcNode.getChildren().add(new TreeItem<>(field));
                        }
                        recalcGroupNode.getChildren().add(recalcNode);
                    }
                    calculationRegisterNode.getChildren().add(recalcGroupNode);
                }
                if (!calculationRegister.getBaseRegisters().isEmpty()) {
                    TreeItem<DataServiceEntity> baseRegGroupNode = new TreeItem<>(new GroupNode(BASE_REGISTERS_GROUP_NAME));
                    for (CalculationRegisterBaseRegister baseRegister : calculationRegister.getBaseRegisters()) {
                        TreeItem<DataServiceEntity> baseRegNode = new TreeItem<>(baseRegister);
                        for (Field field : baseRegister.getVirtualTableFields()) {
                            baseRegNode.getChildren().add(new TreeItem<>(field));
                        }
                        baseRegGroupNode.getChildren().add(baseRegNode);
                    }
                    calculationRegisterNode.getChildren().add(baseRegGroupNode);
                }

                calculationRegisterGroupNode.getChildren().add(calculationRegisterNode);
            }

            // Планы обмена
            for (ExchangePlan exchangePlan : metadataTree.getExchangePlans()) {
                TreeItem<DataServiceEntity> exchangePlanNode = new TreeItem<>(exchangePlan);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                TreeItem<DataServiceEntity> tblGroupNode = new TreeItem<>(new GroupNode(TABULAR_SECTIONS_GROUP_NAME));
                for (Field field : exchangePlan.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                for (TabularSection tabularSection : exchangePlan.getTabularSections()) {
                    TreeItem<DataServiceEntity> tabularSectionNode = new TreeItem<>(tabularSection);
                    for (Field field : tabularSection.getFields()) {
                        tabularSectionNode.getChildren().add(new TreeItem<>(field));
                    }
                    tblGroupNode.getChildren().add(tabularSectionNode);
                }
                exchangePlanNode.getChildren().add(fldGroupNode);
                if (!tblGroupNode.getChildren().isEmpty()) {
                    exchangePlanNode.getChildren().add(tblGroupNode);
                }
                exchangePlanGroupNode.getChildren().add(exchangePlanNode);
            }

            // Бизнес-процессы
            for (BusinessProcess businessProcess : metadataTree.getBusinessProcesses()) {
                TreeItem<DataServiceEntity> businessProcessNode = new TreeItem<>(businessProcess);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                TreeItem<DataServiceEntity> tblGroupNode = new TreeItem<>(new GroupNode(TABULAR_SECTIONS_GROUP_NAME));
                for (Field field : businessProcess.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                for (TabularSection tabularSection : businessProcess.getTabularSections()) {
                    TreeItem<DataServiceEntity> tabularSectionNode = new TreeItem<>(tabularSection);
                    for (Field field : tabularSection.getFields()) {
                        tabularSectionNode.getChildren().add(new TreeItem<>(field));
                    }
                    tblGroupNode.getChildren().add(tabularSectionNode);
                }
                businessProcessNode.getChildren().add(fldGroupNode);
                if (!tblGroupNode.getChildren().isEmpty()) {
                    businessProcessNode.getChildren().add(tblGroupNode);
                }
                businessProcessGroupNode.getChildren().add(businessProcessNode);
            }

            // Задачи
            for (com.datareducer.dataservice.entity.Task task : metadataTree.getTasks()) {
                TreeItem<DataServiceEntity> taskNode = new TreeItem<>(task);
                TreeItem<DataServiceEntity> fldGroupNode = new TreeItem<>(new GroupNode(FIELDS_GROUP_NAME));
                TreeItem<DataServiceEntity> tblGroupNode = new TreeItem<>(new GroupNode(TABULAR_SECTIONS_GROUP_NAME));
                for (Field field : task.getFields()) {
                    fldGroupNode.getChildren().add(new TreeItem<>(field));
                }
                for (TabularSection tabularSection : task.getTabularSections()) {
                    TreeItem<DataServiceEntity> tabularSectionNode = new TreeItem<>(tabularSection);
                    for (Field field : tabularSection.getFields()) {
                        tabularSectionNode.getChildren().add(new TreeItem<>(field));
                    }
                    tblGroupNode.getChildren().add(tabularSectionNode);
                }
                taskNode.getChildren().add(fldGroupNode);
                if (!tblGroupNode.getChildren().isEmpty()) {
                    taskNode.getChildren().add(tblGroupNode);
                }
                taskGroupNode.getChildren().add(taskNode);
            }

            rootNode = new TreeItem<>(infoBase);

            rootNode.getChildren().add(constantGroupNode);
            rootNode.getChildren().add(catalogGroupNode);
            rootNode.getChildren().add(documentGroupNode);
            rootNode.getChildren().add(documentJournalGroupNode);
            rootNode.getChildren().add(chartOfCharacteristicTypesGroupNode);
            rootNode.getChildren().add(chartOfAccountsGroupNode);
            rootNode.getChildren().add(chartOfCalculationTypesGroupNode);
            rootNode.getChildren().add(informationRegisterGroupNode);
            rootNode.getChildren().add(accumulationRegisterGroupNode);
            rootNode.getChildren().add(accountingRegisterGroupNode);
            rootNode.getChildren().add(calculationRegisterGroupNode);
            rootNode.getChildren().add(exchangePlanGroupNode);
            rootNode.getChildren().add(businessProcessGroupNode);
            rootNode.getChildren().add(taskGroupNode);

            metadataTreeTable.setRoot(rootNode);
            metadataTreeTable.setShowRoot(false);
        });

        loadMetadataTreeService.setOnFailed(e -> {
            metadataTreeTable.setRoot(null);
            e.getSource().getException().printStackTrace();
        });
    }

    void loadMetadataTree(InfoBase infoBase, boolean reload) {
        this.infoBase = infoBase;
        if (rootNode != null) {
            for (TreeItem treeItem : rootNode.getChildren()) {
                treeItem.getChildren().clear();
            }
        }
        loadMetadataTreeService.setReload(reload);
        loadMetadataTreeService.restart();
    }

    DataServiceEntity getSelectedItem() {
        return metadataTreeTable.getSelectionModel().getSelectedItem().getValue();
    }

    @Override
    public void acceptModel(ReducerConfiguration model) {
    }

    void clearMetadataTree() {
        metadataTreeTable.setRoot(null);
    }

    public class GroupNode implements DataServiceEntity {
        private final String name;

        private GroupNode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getType() {
            return null;
        }
    }

    private class LoadMetadataTreeService extends Service<MetadataTree> {
        private boolean reload;

        @Override
        protected Task<MetadataTree> createTask() {
            return new LoadMetadataTreeTask(reload);
        }

        void setReload(boolean reload) {
            this.reload = reload;
        }
    }

    private class LoadMetadataTreeTask extends Task<MetadataTree> {
        private boolean reload;

        LoadMetadataTreeTask(boolean reload) {
            this.reload = reload;
        }

        @Override
        protected MetadataTree call() throws Exception {
            if (reload) {
                return infoBase.loadMetadataTree();
            } else {
                return infoBase.getMetadataTree();
            }
        }
    }

    InfoBase getInfoBase() {
        return infoBase;
    }

    TreeTableView<DataServiceEntity> getMetadataTreeTable() {
        return metadataTreeTable;
    }

    ContextMenu getEntityCtxMenu() {
        return entityCtxMenu;
    }

    MenuItem getShowDataItem() {
        return showDataItem;
    }

    MenuItem getAddToScriptItem() {
        return addToScriptItem;
    }

    ContextMenu getFieldCtxMenu() {
        return fieldCtxMenu;
    }

    MenuItem getCopyNameItem() {
        return copyNameItem;
    }
}
