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

import com.datareducer.dataservice.entity.*;
import com.datareducer.model.BooleanExpressionToken;
import com.datareducer.model.DataServiceResource;
import com.datareducer.model.LogicalOperatorWrapper;
import com.datareducer.model.RelationalExpressionWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.ChoiceBoxTreeTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.datareducer.dataservice.entity.FieldType.STRING;
import static com.datareducer.dataservice.entity.LogicalOperator.*;

/**
 * Форма условия отбора данных ресурсов 1С.
 *
 * @author Kirill Mikhaylov
 */
public class ConditionView extends VBox {
    @FXML
    private TreeTableView<BooleanExpressionToken> filterTreeTable;

    @FXML
    private TreeTableColumn<BooleanExpressionToken, LogicalOperator> fltLgcOpCol;
    @FXML
    private TreeTableColumn<BooleanExpressionToken, Field> fltFieldCol;
    @FXML
    private TreeTableColumn<BooleanExpressionToken, RelationalOperator> fltRelOpCol;
    @FXML
    private TreeTableColumn<BooleanExpressionToken, String> flrValueCol;
    @FXML
    private TreeTableColumn<BooleanExpressionToken, String> flrCommentCol;

    @FXML
    private Button addFilterBtn;
    @FXML
    private Button groupFiltersBtn;
    @FXML
    private Button invertFilterBtn;
    @FXML
    private Button filterUpBtn;
    @FXML
    private Button filterDownBtn;
    @FXML
    private Button deleteFilterBtn;

    private DataServiceResource dataServiceResource;
    // Это форма условия отбора по счетам виртуальной таблицы регистра бухгалтерии
    private boolean isAccountCondition;
    // Это форма условия отбора по корреспондирующим счетам виртуальной таблицы регистра бухгалтерии
    private boolean isBalanceAccountCondition;

    public ConditionView() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("fxml/ConditionView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void initialize(DataServiceResource dataServiceResource, boolean isAccountCondition, boolean isBalanceAccountCondition) {
        this.dataServiceResource = dataServiceResource;
        this.isAccountCondition = isAccountCondition;
        this.isBalanceAccountCondition = isBalanceAccountCondition;

        attachEventHandlers();

        if (isAccountCondition && !dataServiceResource.getAccountCondition().isEmpty()) {
            filterTreeTable.setRoot(BooleanExpressionToken.conditionToFilterTree(dataServiceResource.getAccountCondition()));
        } else if (isBalanceAccountCondition && !dataServiceResource.getBalanceAccountCondition().isEmpty()) {
            filterTreeTable.setRoot(BooleanExpressionToken.conditionToFilterTree(dataServiceResource.getBalanceAccountCondition()));
        } else if (!isAccountCondition && !isBalanceAccountCondition && !dataServiceResource.getCondition().isEmpty()) {
            filterTreeTable.setRoot(BooleanExpressionToken.conditionToFilterTree(dataServiceResource.getCondition()));
        }

        filterTreeTable.setEditable(true);
        filterTreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Столбец группировок
        fltLgcOpCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("logicalOperator"));
        fltLgcOpCol.setOnEditCommit(e -> e.getRowValue().getValue().setLogicalOperator(e.getNewValue()));
        fltLgcOpCol.setCellFactory(new Callback<TreeTableColumn<BooleanExpressionToken, LogicalOperator>, TreeTableCell<BooleanExpressionToken, LogicalOperator>>() {
            // Пиктограмма в первом столбце необходима для выделения уровней группировки
            private Image lvlImg = new Image(getClass().getClassLoader().getResourceAsStream("image/bullet_blue.png"));

            @Override
            public TreeTableCell<BooleanExpressionToken, LogicalOperator> call(TreeTableColumn<BooleanExpressionToken, LogicalOperator> param) {
                return new ChoiceBoxTreeTableCell<BooleanExpressionToken, LogicalOperator>(AND, OR) {
                    private ImageView lvlImgView = new ImageView((lvlImg));

                    @Override
                    public void updateItem(LogicalOperator item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty && item == null) {
                            setGraphic(lvlImgView);
                            setEditable(false);
                        } else if (item == NOT) {
                            setText(item.toString());
                            setEditable(false);
                        } else if (empty) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.toString());
                            setEditable(true);
                        }
                    }
                };
            }
        });
        fltLgcOpCol.setSortable(false);

        // Столбец поля
        DataServiceEntity entity = dataServiceResource.getDataServiceEntity();
        if (entity instanceof AccumulationRegisterVirtualTable) {
            fltFieldCol.setText("Измерение");
        }
        fltFieldCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("field"));
        fltFieldCol.setOnEditCommit(e -> {
            e.getRowValue().getValue().setField(e.getNewValue());
            e.getRowValue().getValue().setValue(e.getNewValue().getFieldType() == FieldType.BOOLEAN ? "false" : null);
            refreshFilterTreeTable();
        });
        fltFieldCol.setCellFactory(new Callback<TreeTableColumn<BooleanExpressionToken, Field>, TreeTableCell<BooleanExpressionToken, Field>>() {
            @Override
            public TreeTableCell<BooleanExpressionToken, Field> call(TreeTableColumn<BooleanExpressionToken, Field> param) {
                List<Field> fields = new ArrayList<>();
                if (isAccountCondition) {
                    fields.add(((AccountingRegisterVirtualTable) entity).getAccountField());
                } else if (isBalanceAccountCondition) {
                    fields.add(((AccountingRegisterVirtualTable) entity).getBalancedAccountField());
                } else if (entity instanceof AccountingRegisterTurnovers
                        || entity instanceof AccountingRegisterBalance
                        || entity instanceof AccountingRegisterBalanceAndTurnovers
                        || entity instanceof AccountingRegisterDrCrTurnovers) {
                    fields.addAll(((AccountingRegisterVirtualTable) entity).getDimensions());
                    fields.addAll(((AccountingRegisterVirtualTable) entity).getExtDimensions());
                } else if (entity instanceof AccountingRegisterExtDimensions) {
                    fields.addAll(((AccountingRegisterExtDimensions) entity).getVirtualTableFields());
                } else if (entity instanceof AccountingRegisterRecordsWithExtDimensions) {
                    fields.addAll(((AccountingRegisterRecordsWithExtDimensions) entity).getVirtualTableFields());
                } else if (entity instanceof AccumulationRegisterVirtualTable) {
                    fields.addAll(((AccumulationRegisterVirtualTable) entity).getDimensions());
                } else if (entity instanceof InformationRegisterVirtualTable) {
                    fields.addAll(((InformationRegisterVirtualTable) entity).getRegisterFields());
                } else if (entity instanceof CalculationRegisterVirtualTable) {
                    fields.addAll(((CalculationRegisterVirtualTable) entity).getVirtualTableFields());
                } else {
                    fields.addAll(dataServiceResource.getResourceFields());
                }
                return new ChoiceBoxTreeTableCell<BooleanExpressionToken, Field>(fields.toArray(new Field[fields.size()])) {
                    @Override
                    public void updateItem(Field item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.toString());
                        }
                        if (getTreeTableRow().getItem() instanceof LogicalOperatorWrapper || isAccountCondition || isBalanceAccountCondition) {
                            setEditable(false);
                        } else {
                            setEditable(true);
                        }
                    }
                };
            }
        });
        fltFieldCol.setSortable(false);

        // Столбец оператора сравнения
        fltRelOpCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("relationalOperator"));
        fltRelOpCol.setOnEditCommit(e -> e.getRowValue().getValue().setRelationalOperator(e.getNewValue()));
        fltRelOpCol.setCellFactory(new Callback<TreeTableColumn<BooleanExpressionToken, RelationalOperator>, TreeTableCell<BooleanExpressionToken, RelationalOperator>>() {
            @Override
            public TreeTableCell<BooleanExpressionToken, RelationalOperator> call(TreeTableColumn<BooleanExpressionToken, RelationalOperator> param) {
                return new ChoiceBoxTreeTableCell<BooleanExpressionToken, RelationalOperator>(RelationalOperator.values()) {
                    @Override
                    public void updateItem(RelationalOperator item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.toString());
                        }
                        setEditable(!(getTreeTableRow().getItem() instanceof LogicalOperatorWrapper));
                    }
                };
            }
        });
        fltRelOpCol.setSortable(false);

        // Столбец значения
        flrValueCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));
        flrValueCol.setOnEditCommit(e -> e.getRowValue().getValue().setValue(e.getNewValue()));
        flrValueCol.setCellFactory(param -> new RelationalExpressionValueCell());
        flrValueCol.setSortable(false);

        // Столбец комментария
        flrCommentCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("comment"));
        flrCommentCol.setOnEditCommit(e -> e.getRowValue().getValue().setComment(e.getNewValue()));
        flrCommentCol.setCellFactory(new Callback<TreeTableColumn<BooleanExpressionToken, String>, TreeTableCell<BooleanExpressionToken, String>>() {
            @Override
            public TreeTableCell<BooleanExpressionToken, String> call(TreeTableColumn<BooleanExpressionToken, String> param) {
                return new TextFieldTreeTableCell<BooleanExpressionToken, String>(new DefaultStringConverter()) {
                    @Override
                    public void startEdit() {
                        setEditable(!(getTreeTableRow().getItem() instanceof LogicalOperatorWrapper));
                        if (!isEditable() || !getTreeTableView().isEditable() || !getTableColumn().isEditable()) {
                            return;
                        }
                        super.startEdit();
                    }
                };
            }
        });
        flrCommentCol.setSortable(false);
    }

    private void attachEventHandlers() {
        // Кнопка "Добавить фильтр"
        addFilterBtn.setOnAction(e -> {
            DataServiceEntity entity = dataServiceResource.getDataServiceEntity();
            TreeItem<BooleanExpressionToken> filterItem = new TreeItem<>(new RelationalExpressionWrapper());
            filterItem.getValue().setRelationalOperator(RelationalOperator.EQUAL); // По умолчанию
            if (isAccountCondition) {
                filterItem.getValue().setField(((AccountingRegisterVirtualTable) entity).getAccountField());
            } else if (isBalanceAccountCondition) {
                filterItem.getValue().setField(((AccountingRegisterVirtualTable) entity).getBalancedAccountField());
            }
            TreeItem<BooleanExpressionToken> filterTreeRoot = filterTreeTable.getRoot();
            if (filterTreeRoot != null) {
                if (filterTreeRoot.getValue() instanceof RelationalExpressionWrapper) {
                    TreeItem<BooleanExpressionToken> newRoot = new TreeItem<>(new LogicalOperatorWrapper(AND));
                    newRoot.getChildren().add(filterTreeRoot);
                    newRoot.getChildren().add(filterItem);
                    newRoot.setExpanded(true);
                    filterTreeTable.setRoot(newRoot);
                } else {
                    TreeItem<BooleanExpressionToken> sel = filterTreeTable.getSelectionModel().getSelectedItem();
                    // Для добавления нового элемента в группу нужно выделить корень этой группы.
                    // Иначе новый элемент добавляется в корень дерева.
                    if (sel != null && sel.getValue() instanceof LogicalOperatorWrapper && sel.getValue().getToken() != NOT) {
                        sel.getChildren().add(filterItem);
                    } else {
                        filterTreeRoot.getChildren().add(filterItem);
                    }
                }
            } else {
                filterTreeTable.setRoot(filterItem);
            }
            clearFilterTreeTableSelection();
            filterTreeTable.getSelectionModel().select(filterItem);
            // Для обнуления свойства graphic ячеек колонки "Значение" (в противном случае остаются артефакты)
            refreshFilterTreeTable();
        });

        // Кнопка "Сгруппировать"
        groupFiltersBtn.setOnAction(e -> {
            List<TreeItem<BooleanExpressionToken>> selectedItems = getSelectedFilters();
            Iterator<TreeItem<BooleanExpressionToken>> it = selectedItems.iterator();
            TreeItem<BooleanExpressionToken> parent = it.next().getParent();
            clearFilterTreeTableSelection();
            parent.getChildren().removeAll(selectedItems);
            TreeItem<BooleanExpressionToken> groupRoot = new TreeItem<>(new LogicalOperatorWrapper(AND));
            groupRoot.getChildren().setAll(selectedItems);
            groupRoot.setExpanded(true);
            parent.getChildren().add(groupRoot);
            refreshFilterTreeTable();
        });

        // Кнопка "Инвертировать"
        invertFilterBtn.setOnAction(e -> {
            List<TreeItem<BooleanExpressionToken>> selectedItems = getSelectedFilters();
            // Выделение важно снимать ДО удаления элементов.
            clearFilterTreeTableSelection();
            for (TreeItem<BooleanExpressionToken> item : selectedItems) {
                TreeItem<BooleanExpressionToken> parent = item.getParent();
                if (item.getValue().getToken() == NOT) {
                    if (parent != null) {
                        parent.getChildren().addAll(item.getChildren());
                        parent.getChildren().remove(item);
                    } else {
                        TreeItem<BooleanExpressionToken> child = item.getChildren().get(0);
                        item.getChildren().remove(child);
                        filterTreeTable.setRoot(child);
                    }
                } else {
                    TreeItem<BooleanExpressionToken> nodeNot = new TreeItem<>(new LogicalOperatorWrapper(NOT));
                    if (parent != null) {
                        parent.getChildren().remove(item);
                        parent.getChildren().add(nodeNot);
                    } else {
                        filterTreeTable.setRoot(nodeNot);
                    }
                    nodeNot.getChildren().add(item);
                    nodeNot.setExpanded(true);
                }
            }
            refreshFilterTreeTable();
        });

        // Кнопка "Переместить выше"
        filterUpBtn.setOnAction(e -> {
            TreeItem<BooleanExpressionToken> sel = filterTreeTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                return;
            }
            TreeItem<BooleanExpressionToken> parent = sel.getParent();
            if (parent == null) {
                return;
            }
            int index = parent.getChildren().indexOf(sel);
            if (index != 0) {
                parent.getChildren().remove(index);
                parent.getChildren().add(index - 1, sel);
                clearFilterTreeTableSelection();
                filterTreeTable.getSelectionModel().select(sel);
            }
            refreshFilterTreeTable();
        });

        // Кнопка "Переместить ниже"
        filterDownBtn.setOnAction(e -> {
            TreeItem<BooleanExpressionToken> sel = filterTreeTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                return;
            }
            TreeItem<BooleanExpressionToken> parent = sel.getParent();
            if (parent == null) {
                return;
            }
            int index = parent.getChildren().indexOf(sel);
            if (index != parent.getChildren().size() - 1) {
                parent.getChildren().remove(index);
                parent.getChildren().add(index + 1, sel);
                clearFilterTreeTableSelection();
                filterTreeTable.getSelectionModel().select(sel);
            }
            refreshFilterTreeTable();
        });

        // Кнопка "Удалить"
        deleteFilterBtn.setOnAction(e -> {
            List<TreeItem<BooleanExpressionToken>> selectedItems = getSelectedFilters();
            // Выделение важно снимать ДО удаления элементов.
            clearFilterTreeTableSelection();
            selectedItems.forEach(this::removeFilterTreeNode);
            refreshFilterTreeTable();
        });

        // Определяем, доступна ли группировка фильтров при текущем выделении
        filterTreeTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            List<TreeItem<BooleanExpressionToken>> selectedItems = getSelectedFilters();
            // Проверяем, что выбрано два или более элементов для группировки
            if (selectedItems.size() < 2) {
                groupFiltersBtn.setDisable(true);
                return;
            }
            // Проверяем, что все выбранные элементы одного уровня.
            Iterator<TreeItem<BooleanExpressionToken>> it = selectedItems.iterator();
            TreeItem<BooleanExpressionToken> parent = it.next().getParent();
            while (it.hasNext()) {
                if (parent == null || !parent.equals(it.next().getParent())) {
                    groupFiltersBtn.setDisable(true);
                    return;
                }
            }
            // Проверяем, что выбранные элементы ещё не сгруппированы
            if (parent.getChildren().size() == selectedItems.size()) {
                groupFiltersBtn.setDisable(true);
                return;
            }
            groupFiltersBtn.setDisable(false);
        });

        // Определяем, доступны ли кнопки "Переместить выше/ниже" при текущем выделении
        filterTreeTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            List<TreeItem<BooleanExpressionToken>> selectedItems = getSelectedFilters();
            if (selectedItems.size() != 1) {
                filterUpBtn.setDisable(true);
                filterDownBtn.setDisable(true);
                return;
            }
            TreeItem<BooleanExpressionToken> sel = selectedItems.get(0);
            TreeItem<BooleanExpressionToken> parent = sel.getParent();
            if (parent == null) {
                filterUpBtn.setDisable(true);
                filterDownBtn.setDisable(true);
                return;
            }
            int index = parent.getChildren().indexOf(sel);
            if (index == 0) {
                filterUpBtn.setDisable(true);
                filterDownBtn.setDisable(false);
                return;
            }
            if (index == parent.getChildren().size() - 1) {
                filterDownBtn.setDisable(true);
                filterUpBtn.setDisable(false);
                return;
            }
            filterUpBtn.setDisable(false);
            filterDownBtn.setDisable(false);
        });

        // Определяем, доступны ли кнопки "Удалить" и "Инвертировать" при текущем выделении
        filterTreeTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            List<TreeItem<BooleanExpressionToken>> selectedItems = getSelectedFilters();
            deleteFilterBtn.setDisable(selectedItems.size() == 0);
            invertFilterBtn.setDisable(selectedItems.size() == 0);
        });

    }

    static TreeItem<BooleanExpressionToken> validateConditionTree(TreeItem<BooleanExpressionToken> node) {
        BooleanExpressionToken token = node.getValue();
        if (token instanceof RelationalExpressionWrapper) {
            Field f = token.getField();
            String v = token.getValue();
            if (f == null || v == null || (v.isEmpty() && f.getFieldType() != STRING)) {
                return node;
            }
        } else {
            for (TreeItem<BooleanExpressionToken> item : node.getChildren()) {
                TreeItem<BooleanExpressionToken> result = validateConditionTree(item);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    TreeItem<BooleanExpressionToken> getFilterTreeTableRoot() {
        return filterTreeTable.getRoot();
    }

    void clearFilterTreeTableSelection() {
        filterTreeTable.getSelectionModel().clearSelection();
    }

    void selectFilterTreeNode(TreeItem<BooleanExpressionToken> node) {
        filterTreeTable.getSelectionModel().select(node);
    }

    private List<TreeItem<BooleanExpressionToken>> getSelectedFilters() {
        return new ArrayList<>(filterTreeTable.getSelectionModel().getSelectedItems());
    }

    // Для обнуления свойства graphic ячеек колонки "Значение" (в противном случае остаются артефакты)
    private void refreshFilterTreeTable() {
        filterTreeTable.refresh();
    }

    private void removeFilterTreeNode(TreeItem<BooleanExpressionToken> node) {
        TreeItem<BooleanExpressionToken> parent = node.getParent();
        if (parent != null) {
            parent.getChildren().remove(node);
            if (parent.getValue().getToken() != NOT) {
                if (parent.getChildren().size() == 1) {
                    TreeItem<BooleanExpressionToken> lastChild = parent.getChildren().get(0);
                    TreeItem<BooleanExpressionToken> grandParent = parent.getParent();
                    if (grandParent != null) {
                        grandParent.getChildren().remove(parent);
                        grandParent.getChildren().add(lastChild);
                    } else {
                        filterTreeTable.setRoot(lastChild);
                    }
                }
            } else {
                removeFilterTreeNode(parent);
            }
        } else {
            filterTreeTable.setRoot(null);
        }
    }

}
