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
package com.datareducer.dataservice.jaxb;

import com.datareducer.dataservice.entity.*;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.LinkedHashSet;

public class DataServiceEntityAdapter extends XmlAdapter<Object, DataServiceEntity> {
    @Override
    public DataServiceEntity unmarshal(Object v) {
        if (v == null) {
            throw new IllegalArgumentException("Сущность равна null");
        }
        if (v instanceof AdaptedConstant) {
            return new Constant(((AdaptedConstant) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedCatalog) {
            return new Catalog(((AdaptedCatalog) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedDocument) {
            return new Document(((AdaptedDocument) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedDocumentJournal) {
            return new DocumentJournal(((AdaptedDocumentJournal) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedChartOfCharacteristicTypes) {
            return new ChartOfCharacteristicTypes(((AdaptedChartOfCharacteristicTypes) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedChartOfAccounts) {
            return new ChartOfAccounts(((AdaptedChartOfAccounts) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedChartOfCalculationTypes) {
            return new ChartOfCalculationTypes(((AdaptedChartOfCalculationTypes) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedInformationRegister) {
            return new InformationRegister(((AdaptedInformationRegister) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedInformationRegisterSliceLast) {
            AdaptedInformationRegisterSliceLast slice = (AdaptedInformationRegisterSliceLast) v;
            return new InformationRegisterSliceLast(slice.getName(), slice.getRegisterFields());
        } else if (v instanceof AdaptedInformationRegisterSliceFirst) {
            AdaptedInformationRegisterSliceFirst slice = (AdaptedInformationRegisterSliceFirst) v;
            return new InformationRegisterSliceFirst(slice.getName(), slice.getRegisterFields());
        } else if (v instanceof AdaptedAccumulationRegister) {
            return new AccumulationRegister(((AdaptedAccumulationRegister) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedAccumulationRegisterBalance) {
            AdaptedAccumulationRegisterBalance balance = (AdaptedAccumulationRegisterBalance) v;
            return new AccumulationRegisterBalance(balance.getName(), balance.getDimensions(), balance.getResources());
        } else if (v instanceof AdaptedAccumulationRegisterTurnovers) {
            AdaptedAccumulationRegisterTurnovers turnovers = (AdaptedAccumulationRegisterTurnovers) v;
            return new AccumulationRegisterTurnovers(turnovers.getName(), turnovers.getDimensions(), turnovers.getResources());
        } else if (v instanceof AdaptedAccumulationRegisterBalanceAndTurnovers) {
            AdaptedAccumulationRegisterBalanceAndTurnovers turnovers = (AdaptedAccumulationRegisterBalanceAndTurnovers) v;
            return new AccumulationRegisterBalanceAndTurnovers(turnovers.getName(), turnovers.getDimensions(), turnovers.getResources());
        } else if (v instanceof AdaptedAccountingRegister) {
            return new AccountingRegister(((AdaptedAccountingRegister) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedAccountingRegisterBalance) {
            AdaptedAccountingRegisterBalance balance = (AdaptedAccountingRegisterBalance) v;
            return new AccountingRegisterBalance(balance.getName(), balance.getProperties(), balance.getResources());
        } else if (v instanceof AdaptedAccountingRegisterTurnovers) {
            AdaptedAccountingRegisterTurnovers turnovers = (AdaptedAccountingRegisterTurnovers) v;
            return new AccountingRegisterTurnovers(turnovers.getName(), turnovers.getProperties(), turnovers.getResources());
        } else if (v instanceof AdaptedAccountingRegisterBalanceAndTurnovers) {
            AdaptedAccountingRegisterBalanceAndTurnovers turnovers = (AdaptedAccountingRegisterBalanceAndTurnovers) v;
            return new AccountingRegisterBalanceAndTurnovers(turnovers.getName(), turnovers.getProperties(), turnovers.getResources());
        } else if (v instanceof AdaptedAccountingRegisterExtDimensions) {
            AdaptedAccountingRegisterExtDimensions extDimensions = (AdaptedAccountingRegisterExtDimensions) v;
            return new AccountingRegisterExtDimensions(extDimensions.getName(), extDimensions.getVirtualTableFields());
        } else if (v instanceof AdaptedAccountingRegisterRecordsWithExtDimensions) {
            AdaptedAccountingRegisterRecordsWithExtDimensions recordsWithExtDimensions = (AdaptedAccountingRegisterRecordsWithExtDimensions) v;
            return new AccountingRegisterRecordsWithExtDimensions(recordsWithExtDimensions.getName(), recordsWithExtDimensions.getVirtualTableFields());
        } else if (v instanceof AdaptedAccountingRegisterDrCrTurnovers) {
            AdaptedAccountingRegisterDrCrTurnovers turnovers = (AdaptedAccountingRegisterDrCrTurnovers) v;
            return new AccountingRegisterDrCrTurnovers(turnovers.getName(), turnovers.getProperties(), turnovers.getResources());
        } else if (v instanceof AdaptedCalculationRegister) {
            return new CalculationRegister(((AdaptedCalculationRegister) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedCalculationRegisterScheduleData) {
            AdaptedCalculationRegisterScheduleData scheduleData = (AdaptedCalculationRegisterScheduleData) v;
            return new CalculationRegisterScheduleData(scheduleData.getName(), scheduleData.getVirtualTableFields());
        } else if (v instanceof AdaptedCalculationRegisterActualActionPeriod) {
            AdaptedCalculationRegisterActualActionPeriod actualActionPeriod = (AdaptedCalculationRegisterActualActionPeriod) v;
            return new CalculationRegisterActualActionPeriod(actualActionPeriod.getName(), actualActionPeriod.getVirtualTableFields());
        } else if (v instanceof AdaptedCalculationRegisterRecalculation) {
            AdaptedCalculationRegisterRecalculation registerRecalculation = (AdaptedCalculationRegisterRecalculation) v;
            return new CalculationRegisterRecalculation(registerRecalculation.getName(), registerRecalculation.getRecalculationName(),
                    registerRecalculation.getVirtualTableFields());
        } else if (v instanceof AdaptedCalculationRegisterBaseRegister) {
            AdaptedCalculationRegisterBaseRegister baseRegister = (AdaptedCalculationRegisterBaseRegister) v;
            return new CalculationRegisterBaseRegister(baseRegister.getName(), baseRegister.getBaseRegisterName(), baseRegister.getVirtualTableFields());
        } else if (v instanceof AdaptedExchangePlan) {
            return new ExchangePlan(((AdaptedExchangePlan) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedBusinessProcess) {
            return new BusinessProcess(((AdaptedBusinessProcess) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedTask) {
            return new Task(((AdaptedTask) v).getName(), new LinkedHashSet<>());
        } else if (v instanceof AdaptedTabularSection) {
            AdaptedTabularSection tabularSection = (AdaptedTabularSection) v;
            return new TabularSection(tabularSection.getParent(), tabularSection.getName());
        }
        throw new IllegalArgumentException("Неизвестная сущность: " + v.getClass().getName());
    }

    @Override
    public Object marshal(DataServiceEntity v) {
        if (v == null) {
            throw new IllegalArgumentException("Сущность равна null");
        }
        if (v instanceof Constant) {
            AdaptedConstant adapted = new AdaptedConstant();
            adapted.setName(v.getName());
            return adapted;
        }else if (v instanceof Catalog) {
            AdaptedCatalog adapted = new AdaptedCatalog();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof Document) {
            AdaptedDocument adapted = new AdaptedDocument();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof DocumentJournal) {
            AdaptedDocumentJournal adapted = new AdaptedDocumentJournal();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof ChartOfCharacteristicTypes) {
            AdaptedChartOfCharacteristicTypes adapted = new AdaptedChartOfCharacteristicTypes();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof ChartOfAccounts) {
            AdaptedChartOfAccounts adapted = new AdaptedChartOfAccounts();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof ChartOfCalculationTypes) {
            AdaptedChartOfCalculationTypes adapted = new AdaptedChartOfCalculationTypes();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof InformationRegister) {
            AdaptedInformationRegister adapted = new AdaptedInformationRegister();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof InformationRegisterSliceLast) {
            AdaptedInformationRegisterSliceLast adapted = new AdaptedInformationRegisterSliceLast();
            adapted.setName(v.getName());
            adapted.setRegisterFields(((InformationRegisterSliceLast) v).getRegisterFields());
            return adapted;
        } else if (v instanceof InformationRegisterSliceFirst) {
            AdaptedInformationRegisterSliceFirst adapted = new AdaptedInformationRegisterSliceFirst();
            adapted.setName(v.getName());
            adapted.setRegisterFields(((InformationRegisterSliceFirst) v).getRegisterFields());
            return adapted;
        } else if (v instanceof AccumulationRegister) {
            AdaptedAccumulationRegister adapted = new AdaptedAccumulationRegister();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof AccumulationRegisterBalance) {
            AdaptedAccumulationRegisterBalance adapted = new AdaptedAccumulationRegisterBalance();
            AccumulationRegisterBalance balance = (AccumulationRegisterBalance) v;
            adapted.setName(balance.getName());
            adapted.setDimensions(balance.getDimensions());
            adapted.setResources(balance.getResources());
            return adapted;
        } else if (v instanceof AccumulationRegisterTurnovers) {
            AdaptedAccumulationRegisterTurnovers adapted = new AdaptedAccumulationRegisterTurnovers();
            AccumulationRegisterTurnovers turnovers = (AccumulationRegisterTurnovers) v;
            adapted.setName(turnovers.getName());
            adapted.setDimensions(turnovers.getDimensions());
            adapted.setResources(turnovers.getResources());
            return adapted;
        } else if (v instanceof AccumulationRegisterBalanceAndTurnovers) {
            AdaptedAccumulationRegisterBalanceAndTurnovers adapted = new AdaptedAccumulationRegisterBalanceAndTurnovers();
            AccumulationRegisterBalanceAndTurnovers balanceAndTurnovers = (AccumulationRegisterBalanceAndTurnovers) v;
            adapted.setName(balanceAndTurnovers.getName());
            adapted.setDimensions(balanceAndTurnovers.getDimensions());
            adapted.setResources(balanceAndTurnovers.getResources());
            return adapted;
        } else if (v instanceof AccountingRegister) {
            AdaptedAccountingRegister adapted = new AdaptedAccountingRegister();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof AccountingRegisterBalance) {
            AdaptedAccountingRegisterBalance adapted = new AdaptedAccountingRegisterBalance();
            AccountingRegisterBalance balance = (AccountingRegisterBalance) v;
            adapted.setName(balance.getName());
            adapted.setProperties(balance.getProperties());
            adapted.setResources(balance.getResources());
            return adapted;
        } else if (v instanceof AccountingRegisterTurnovers) {
            AdaptedAccountingRegisterTurnovers adapted = new AdaptedAccountingRegisterTurnovers();
            AccountingRegisterTurnovers turnovers = (AccountingRegisterTurnovers) v;
            adapted.setName(turnovers.getName());
            adapted.setProperties(turnovers.getProperties());
            adapted.setResources(turnovers.getResources());
            return adapted;
        } else if (v instanceof AccountingRegisterBalanceAndTurnovers) {
            AdaptedAccountingRegisterBalanceAndTurnovers adapted = new AdaptedAccountingRegisterBalanceAndTurnovers();
            AccountingRegisterBalanceAndTurnovers balanceAndTurnovers = (AccountingRegisterBalanceAndTurnovers) v;
            adapted.setName(balanceAndTurnovers.getName());
            adapted.setProperties(balanceAndTurnovers.getProperties());
            adapted.setResources(balanceAndTurnovers.getResources());
            return adapted;
        } else if (v instanceof AccountingRegisterExtDimensions) {
            AdaptedAccountingRegisterExtDimensions adapted = new AdaptedAccountingRegisterExtDimensions();
            adapted.setName(v.getName());
            adapted.setVirtualTableFields(((AccountingRegisterExtDimensions) v).getVirtualTableFields());
            return adapted;
        } else if (v instanceof AccountingRegisterRecordsWithExtDimensions) {
            AdaptedAccountingRegisterRecordsWithExtDimensions adapted = new AdaptedAccountingRegisterRecordsWithExtDimensions();
            adapted.setName(v.getName());
            adapted.setVirtualTableFields(((AccountingRegisterRecordsWithExtDimensions) v).getVirtualTableFields());
            return adapted;
        } else if (v instanceof AccountingRegisterDrCrTurnovers) {
            AdaptedAccountingRegisterDrCrTurnovers adapted = new AdaptedAccountingRegisterDrCrTurnovers();
            AccountingRegisterDrCrTurnovers turnovers = (AccountingRegisterDrCrTurnovers) v;
            adapted.setName(turnovers.getName());
            adapted.setProperties(turnovers.getProperties());
            adapted.setResources(turnovers.getResources());
            return adapted;
        } else if (v instanceof CalculationRegister) {
            AdaptedCalculationRegister adapted = new AdaptedCalculationRegister();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof CalculationRegisterScheduleData) {
            AdaptedCalculationRegisterScheduleData adapted = new AdaptedCalculationRegisterScheduleData();
            adapted.setName(v.getName());
            adapted.setVirtualTableFields(((CalculationRegisterScheduleData) v).getVirtualTableFields());
            return adapted;
        } else if (v instanceof CalculationRegisterActualActionPeriod) {
            AdaptedCalculationRegisterActualActionPeriod adapted = new AdaptedCalculationRegisterActualActionPeriod();
            adapted.setName(v.getName());
            adapted.setVirtualTableFields(((CalculationRegisterActualActionPeriod) v).getVirtualTableFields());
            return adapted;
        } else if (v instanceof CalculationRegisterRecalculation) {
            AdaptedCalculationRegisterRecalculation adapted = new AdaptedCalculationRegisterRecalculation();
            adapted.setName(v.getName());
            adapted.setRecalculationName(((CalculationRegisterRecalculation) v).getRecalculationName());
            adapted.setVirtualTableFields(((CalculationRegisterRecalculation) v).getVirtualTableFields());
            return adapted;
        } else if (v instanceof CalculationRegisterBaseRegister) {
            AdaptedCalculationRegisterBaseRegister adapted = new AdaptedCalculationRegisterBaseRegister();
            adapted.setName(v.getName());
            adapted.setBaseRegisterName(((CalculationRegisterBaseRegister) v).getBaseRegisterName());
            adapted.setVirtualTableFields(((CalculationRegisterBaseRegister) v).getVirtualTableFields());
            return adapted;
        } else if (v instanceof ExchangePlan) {
            AdaptedExchangePlan adapted = new AdaptedExchangePlan();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof BusinessProcess) {
            AdaptedBusinessProcess adapted = new AdaptedBusinessProcess();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof Task) {
            AdaptedTask adapted = new AdaptedTask();
            adapted.setName(v.getName());
            return adapted;
        } else if (v instanceof TabularSection) {
            AdaptedTabularSection adapted = new AdaptedTabularSection();
            TabularSection tabularSection = (TabularSection) v;
            adapted.setName(tabularSection.getName());
            adapted.setParent(tabularSection.getParent());
            return adapted;
        }
        throw new IllegalArgumentException("Неизвестная сущность: " + v.getClass().getName());
    }
}
