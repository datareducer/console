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
package com.datareducer.dataservice.test;

import com.datareducer.dataservice.client.ClientException;
import com.datareducer.dataservice.entity.*;
import com.datareducer.model.InfoBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static com.datareducer.dataservice.client.DataServiceClient.DATE_TIME_FORMATTER;
import static com.datareducer.dataservice.entity.LogicalOperator.AND;
import static com.datareducer.dataservice.entity.RelationalOperator.*;
import static org.junit.Assert.assertTrue;

public class DataServiceClientTest {
    private DataServiceEmulator dataService;
    private InfoBase infoBase;

    @Before
    public void setUp() {
        dataService = new DataServiceEmulator();
        dataService.start();
        infoBase = new InfoBase("Информационная база", DataServiceEmulator.HOST, DataServiceEmulator.BASE, "odata.user", "123");

        Map<String, String> applicationParams = new HashMap<>();
        applicationParams.put("orientdb-engine", "memory");
        applicationParams.put("orientdb-user", "admin");
        applicationParams.put("orientdb-password", "admin");

        infoBase.setApplicationParams(applicationParams);
    }

    @Test
    public void metadataTest() throws ClientException {
        MetadataTree metadata = infoBase.getMetadataTree();
        assertTrue(metadata.getCatalogs().size() == 4);
    }

    @Test
    public void catalogTest() throws ClientException {
        Catalog ent = infoBase.getMetadataTree().getCatalogByName("БанковскиеСчетаОрганизаций");
        Condition cnd1 = new Condition(new RelationalExpression(ent.getFieldByName("ВыводитьСуммуБезКопеек"), EQUAL, false, null));
        DataServiceRequest req1 = new Catalog("БанковскиеСчетаОрганизаций", ent.getFields(), true, cnd1, true);

        Condition cnd2 = new Condition(new RelationalExpression(ent.getFieldByName("Description"), EQUAL, "КОНГРЕСС-БАНК ОАО, Торговый дом \"Комплексный\" (EUR)", null));
        DataServiceRequest req2 = new Catalog("БанковскиеСчетаОрганизаций", ent.getFields(), false, cnd2, true);

        // Выполняем запросы по 2 раза для тестирования кэша
        for (int i = 0; i < 2; i++) {
            List<Map<Field, Object>> result1 = infoBase.get(req1);
            assertTrue(result1.size() == 3);
            UUID refKey = (UUID) result1.get(2).get(req1.getFieldByName("Ref_Key"));
            assertTrue(refKey.equals(UUID.fromString("51ed67de-7220-11df-b336-0011955cba6b")));
        }
        for (int i = 0; i < 2; i++) {
            List<Map<Field, Object>> result2 = infoBase.get(req2);
            assertTrue(result2.size() == 1);
            assertTrue(result2.get(0).get(req2.getFieldByName("НомерСчета")).equals("40702810838110014563"));
        }
    }

    @Test
    public void accumulationRegisterTest() throws ClientException, ParseException {
        AccumulationRegister ent = infoBase.getMetadataTree().getAccumulationRegisterByName("ПартииТоваровПереданныеНаКомиссию");

        LocalDateTime ldt = LocalDateTime.parse("2014-12-04T00:00:00", DATE_TIME_FORMATTER);
        Instant instant = ldt.atZone(DATE_TIME_FORMATTER.getZone()).toInstant();

        Condition cnd = new Condition(new RelationalExpression(ent.getFieldByName("Period"), GREATER_OR_EQUAL, instant, null));
        DataServiceRequest req = new AccumulationRegister("ПартииТоваровПереданныеНаКомиссию", ent.getFields(), true, cnd, true);

        // Выполняем запрос 2 раза для тестирования кэша ресурсов
        for (int i = 0; i < 2; i++) {
            List<Map<Field, Object>> result = infoBase.get(req);
            assertTrue(result.size() == 5);
        }
    }

    @Test
    public void accountingRegisterTest() throws ClientException {
        AccountingRegister ent = infoBase.getMetadataTree().getAccountingRegisterByName("Хозрасчетный");

        Condition cnd = new Condition(new RelationalExpression(ent.getFieldByName("Сумма"), GREATER, 10000000, null));

        DataServiceRequest req = new AccountingRegister("Хозрасчетный", ent.getFields(), true, cnd, false);

        // Выполняем запрос 2 раза для тестирования кэша ресурсов
        for (int i = 0; i < 2; i++) {
            List<Map<Field, Object>> result = infoBase.get(req);
            assertTrue(result.size() == 3);
        }
    }

    @Test
    public void accumulationRegisterBalanceTest() throws ClientException {
        AccumulationRegisterBalance ent = infoBase.getMetadataTree().getAccumulationRegisterByName("ТоварыКОтгрузке").getBalance();

        LinkedHashSet<Field> dimensionsParam = new LinkedHashSet<>();
        dimensionsParam.add(ent.getFieldByName("Номенклатура_Key"));
        dimensionsParam.add(ent.getFieldByName("Склад_Key"));

        ent.getFieldByName("Номенклатура_Key").setPresentation(true);

        Condition cnd = new Condition(new RelationalExpression(ent.getFieldByName("Назначение_Key"), EQUAL, UUID.fromString("00000000-0000-0000-0000-000000000000"), null));

        LocalDateTime ldt = LocalDateTime.parse("2016-12-22T00:00:00", DATE_TIME_FORMATTER);
        Instant period = ldt.atZone(DATE_TIME_FORMATTER.getZone()).toInstant();

        AccumulationRegisterBalance req = new AccumulationRegisterBalance("ТоварыКОтгрузке", ent.getDimensions(), ent.getResources(), dimensionsParam, false, cnd, period, false);

        List<Map<Field, Object>> result = infoBase.getAccumulationRegisterVirtualTable(req);
        assertTrue(result.size() == 14);
        assertTrue(result.get(2).get(ent.getFieldByName("Номенклатура_Key")).equals(UUID.fromString("e8a71fec-55bc-11d9-848a-00112f43529a")));
        assertTrue(result.get(2).get(ent.getFieldByName("Номенклатура____Presentation")).equals("Мясорубка MOULINEX  A 15"));
    }

    @Test
    public void accumulationRegisterTurnoversTest() throws ClientException {
        AccumulationRegisterTurnovers ent = infoBase.getMetadataTree().getAccumulationRegisterByName("ДенежныеСредстваБезналичные").getTurnovers();

        LinkedHashSet<Field> dimensionsParam = new LinkedHashSet<>();
        dimensionsParam.add(ent.getFieldByName("Организация_Key"));

        ent.getFieldByName("Организация_Key").setPresentation(true);

        LocalDateTime startPeriodLdt = LocalDateTime.parse("2015-01-01T00:00:00", DATE_TIME_FORMATTER);
        Instant startPeriod = startPeriodLdt.atZone(DATE_TIME_FORMATTER.getZone()).toInstant();

        LocalDateTime endPeriodLdt = LocalDateTime.parse("2017-01-01T00:00:00", DATE_TIME_FORMATTER);
        Instant endPeriod = endPeriodLdt.atZone(DATE_TIME_FORMATTER.getZone()).toInstant();

        AccumulationRegisterTurnovers req = new AccumulationRegisterTurnovers("ДенежныеСредстваБезналичные", ent.getDimensions(), ent.getResources(),
                dimensionsParam, false, new Condition(), startPeriod, endPeriod, false);

        List<Map<Field, Object>> result = infoBase.getAccumulationRegisterVirtualTable(req);
        assertTrue(result.size() == 5);
        assertTrue(result.get(0).get(ent.getFieldByName("Организация_Key")).equals(UUID.fromString("51ed67a3-7220-11df-b336-0011955cba6b")));
        assertTrue(result.get(0).get(ent.getFieldByName("Организация____Presentation")).equals("Торговый дом \"Комплексный\""));
    }

    @Test
    public void chartOfCharacteristicTypesTest() throws ClientException {
        ChartOfCharacteristicTypes ent = infoBase.getMetadataTree().getChartOfCharacteristicTypesByName("ДополнительныеРеквизитыИСведения");
        Condition cnd = new Condition(new RelationalExpression(ent.getFieldByName("DeletionMark"), EQUAL, false, null));
        DataServiceRequest req = new ChartOfCharacteristicTypes("ДополнительныеРеквизитыИСведения", ent.getFields(), true, cnd, false);

        // Выполняем запрос 2 раза для тестирования кэша ресурсов
        for (int i = 0; i < 2; i++) {
            // При втором запросе кэш окажется просроченным
            List<Map<Field, Object>> result = infoBase.get(req, 1);
            assertTrue(result.size() == 3);
            assertTrue((Boolean) result.get(2).get(ent.getFieldByName("Доступен")));
        }
    }

    @Test
    public void tabularSectionTest() throws ClientException {
        Catalog parent = infoBase.getMetadataTree().getCatalogByName("Организации");
        TabularSection ent = parent.getTabularSectionByName("КонтактнаяИнформация");
        Condition cnd = new Condition(new RelationalExpression(ent.getFieldByName("Тип"), EQUAL, "Адрес", null))
                .append(AND)
                .append(new RelationalExpression(ent.getFieldByName("Вид_Key"), EQUAL, UUID.fromString("1d78f39b-c180-11e4-a7a9-000d884fd00d"), null));

        TabularSection req = new TabularSection(parent, "КонтактнаяИнформация", ent.getFields(), true, cnd, false);

        // Выполняем запрос 2 раза для тестирования кэша ресурсов
        for (int i = 0; i < 2; i++) {
            List<Map<Field, Object>> result = infoBase.get(req);
            assertTrue(result.size() == 2);
        }
    }

    @After
    public void tearDown() {
        infoBase.close();
        dataService.stop();
    }
}
