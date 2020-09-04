/*
 * Copyright (c) 2017-2020 Kirill Mikhaylov <admin@datareducer.ru>
 *
 * Этот файл — часть программы DataReducer Console <http://datareducer.ru>.
 *
 * Программа DataReducer Console является свободным программным обеспечением.
 * Вы вправе распространять ее и/или модифицировать только в соответствии с условиями
 * версии 2 Стандартной Общественной Лицензии GNU, опубликованной Free Software Foundation.
 *
 * Программа DataReducer Console распространяется в надежде, что она будет полезной,
 * но БЕЗО ВСЯКИХ ГАРАНТИЙ, в том числе ГАРАНТИИ ТОВАРНОГО СОСТОЯНИЯ ПРИ ПРОДАЖЕ
 * и ПРИГОДНОСТИ ДЛЯ ИСПОЛЬЗОВАНИЯ В КОНКРЕТНЫХ ЦЕЛЯХ.
 * Подробнее см. в Стандартной Общественной Лицензии GNU.
 *
 * Вы должны были получить копию Стандартной Общественной Лицензии GNU
 * вместе с этой программой. Если это не так, см. <https://www.gnu.org/licenses/>.
 */
package ru.datareducer.dataservice.entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Дерево матаданных 1С
 *
 * @author Kirill Mikhaylov
 */
public final class MetadataTree {
    private final Map<String, Catalog> catalogs;
    private final Map<String, ChartOfCharacteristicTypes> chartOfCharacteristicTypes;
    private final Map<String, ChartOfAccounts> chartOfAccounts;
    private final Map<String, AccumulationRegister> accumulationRegisters;
    private final Map<String, AccountingRegister> accountingRegisters;
    private final Map<String, Constant> constants;
    private final Map<String, ExchangePlan> exchangePlans;
    private final Map<String, Document> documents;
    private final Map<String, DocumentJournal> documentJournals;
    private final Map<String, ChartOfCalculationTypes> chartOfCalculationTypes;
    private final Map<String, InformationRegister> informationRegisters;
    private final Map<String, CalculationRegister> calculationRegisters;
    private final Map<String, BusinessProcess> businessProcesses;
    private final Map<String, Task> tasks;

    public MetadataTree() {
        this.catalogs = new HashMap<>();
        this.chartOfCharacteristicTypes = new HashMap<>();
        this.chartOfAccounts = new HashMap<>();
        this.accumulationRegisters = new HashMap<>();
        this.accountingRegisters = new HashMap<>();
        this.constants = new HashMap<>();
        this.exchangePlans = new HashMap<>();
        this.documents = new HashMap<>();
        this.documentJournals = new HashMap<>();
        this.chartOfCalculationTypes = new HashMap<>();
        this.informationRegisters = new HashMap<>();
        this.calculationRegisters = new HashMap<>();
        this.businessProcesses = new HashMap<>();
        this.tasks = new HashMap<>();
    }

    /**
     * Добавляет объект в дерево матаданных.
     *
     * @param entity Объект конфигурации.
     */
    public void addEntity(DataServiceEntity entity) {
        if (entity instanceof Catalog) {
            Catalog ent = (Catalog) entity;
            catalogs.put(ent.getName(), ent);
        } else if (entity instanceof ChartOfCharacteristicTypes) {
            ChartOfCharacteristicTypes ent = (ChartOfCharacteristicTypes) entity;
            chartOfCharacteristicTypes.put(ent.getName(), ent);
        } else if (entity instanceof ChartOfAccounts) {
            ChartOfAccounts ent = (ChartOfAccounts) entity;
            chartOfAccounts.put(ent.getName(), ent);
        } else if (entity instanceof AccumulationRegister) {
            AccumulationRegister ent = (AccumulationRegister) entity;
            accumulationRegisters.put(ent.getName(), ent);
        } else if (entity instanceof AccountingRegister) {
            AccountingRegister ent = (AccountingRegister) entity;
            accountingRegisters.put(ent.getName(), ent);
        } else if (entity instanceof Constant) {
            Constant ent = (Constant) entity;
            constants.put(ent.getName(), ent);
        } else if (entity instanceof ExchangePlan) {
            ExchangePlan ent = (ExchangePlan) entity;
            exchangePlans.put(ent.getName(), ent);
        } else if (entity instanceof Document) {
            Document ent = (Document) entity;
            documents.put(ent.getName(), ent);
        } else if (entity instanceof DocumentJournal) {
            DocumentJournal ent = (DocumentJournal) entity;
            documentJournals.put(ent.getName(), ent);
        } else if (entity instanceof ChartOfCalculationTypes) {
            ChartOfCalculationTypes ent = (ChartOfCalculationTypes) entity;
            chartOfCalculationTypes.put(ent.getName(), ent);
        } else if (entity instanceof InformationRegister) {
            InformationRegister ent = (InformationRegister) entity;
            informationRegisters.put(ent.getName(), ent);
        } else if (entity instanceof CalculationRegister) {
            CalculationRegister ent = (CalculationRegister) entity;
            calculationRegisters.put(ent.getName(), ent);
        } else if (entity instanceof BusinessProcess) {
            BusinessProcess ent = (BusinessProcess) entity;
            businessProcesses.put(ent.getName(), ent);
        } else if (entity instanceof Task) {
            Task ent = (Task) entity;
            tasks.put(ent.getName(), ent);
        }
    }

    public Collection<Catalog> getCatalogs() {
        return catalogs.values();
    }

    public Collection<ChartOfCharacteristicTypes> getChartOfCharacteristicTypes() {
        return chartOfCharacteristicTypes.values();
    }

    public Collection<ChartOfAccounts> getChartOfAccounts() {
        return chartOfAccounts.values();
    }

    public Collection<AccumulationRegister> getAccumulationRegisters() {
        return accumulationRegisters.values();
    }

    public Collection<AccountingRegister> getAccountingRegisters() {
        return accountingRegisters.values();
    }

    public Collection<Constant> getConstants() {
        return constants.values();
    }

    public Collection<ExchangePlan> getExchangePlans() {
        return exchangePlans.values();
    }

    public Collection<Document> getDocuments() {
        return documents.values();
    }

    public Collection<DocumentJournal> getDocumentJournals() {
        return documentJournals.values();
    }

    public Collection<ChartOfCalculationTypes> getChartOfCalculationTypes() {
        return chartOfCalculationTypes.values();
    }

    public Collection<InformationRegister> getInformationRegisters() {
        return informationRegisters.values();
    }

    public Collection<CalculationRegister> getCalculationRegisters() {
        return calculationRegisters.values();
    }

    public Collection<BusinessProcess> getBusinessProcesses() {
        return businessProcesses.values();
    }

    public Collection<Task> getTasks() {
        return tasks.values();
    }

    public Catalog getCatalogByName(String name) {
        return catalogs.get(name);
    }

    public ChartOfCharacteristicTypes getChartOfCharacteristicTypesByName(String name) {
        return chartOfCharacteristicTypes.get(name);
    }

    public ChartOfAccounts getChartOfAccountsByName(String name) {
        return chartOfAccounts.get(name);
    }

    public AccumulationRegister getAccumulationRegisterByName(String name) {
        return accumulationRegisters.get(name);
    }

    public AccountingRegister getAccountingRegisterByName(String name) {
        return accountingRegisters.get(name);
    }

    public Constant getConstantByName(String name) {
        return constants.get(name);
    }

    public ExchangePlan getExchangePlanByName(String name) {
        return exchangePlans.get(name);
    }

    public Document getDocumentByName(String name) {
        return documents.get(name);
    }

    public DocumentJournal getDocumentJournalByName(String name) {
        return documentJournals.get(name);
    }

    public ChartOfCalculationTypes getChartOfCalculationTypesByName(String name) {
        return chartOfCalculationTypes.get(name);
    }

    public InformationRegister getInformationRegisterByName(String name) {
        return informationRegisters.get(name);
    }

    public CalculationRegister getCalculationRegisterByName(String name) {
        return calculationRegisters.get(name);
    }

    public BusinessProcess getBusinessProcessByName(String name) {
        return businessProcesses.get(name);
    }

    public Task getTaskByName(String name) {
        return tasks.get(name);
    }
}
