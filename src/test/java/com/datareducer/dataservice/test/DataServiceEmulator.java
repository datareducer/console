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

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Scanner;

@Path("/")
public class DataServiceEmulator {
    final static String HOST = "127.0.0.1:8082";
    final static String BASE = "InfoBase";

    private HttpServer server;

    void start() {
        server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(String.format("http://%s/%s/odata/standard.odata", HOST, BASE)),
                new ResourceConfig().register(this.getClass()));
        System.out.println("Эмулятор REST-сервиса 1С запущен");
    }

    @GET
    @Path("$metadata")
    @Produces(MediaType.APPLICATION_XML)
    public String getMetadata() {
        return parseFile("odata/metadata.xml");
    }

    @GET
    @Path("Catalog_БанковскиеСчетаОрганизаций")
    @Produces(MediaType.APPLICATION_ATOM_XML)
    public String getCatalog(@Context UriInfo ui) {
        String expQuery1 = "allowedOnly=true&$filter=ВыводитьСуммуБезКопеек eq false";
        String expQuery2 = "allowedOnly=true&$select=Ref_Key,DataVersion,Description,Owner_Key,DeletionMark," +
                "ВалютаДенежныхСредств_Key,НомерСчета,Банк_Key,БанкДляРасчетов_Key,ТекстКорреспондента,ТекстНазначения," +
                "ВариантВыводаМесяца,ВыводитьСуммуБезКопеек,СрокИсполненияПлатежа,ИспользоватьОбменСБанком,Программа," +
                "Кодировка,ФайлЗагрузки,ФайлВыгрузки,РазрешитьПлатежиБезУказанияЗаявок,Подразделение_Key,БИКБанка," +
                "РучноеИзменениеРеквизитовБанка,НаименованиеБанка,КоррСчетБанка,ГородБанка,АдресБанка,ТелефоныБанка," +
                "БИКБанкаДляРасчетов,РучноеИзменениеРеквизитовБанкаДляРасчетов,НаименованиеБанкаДляРасчетов," +
                "КоррСчетБанкаДляРасчетов,ГородБанкаДляРасчетов,АдресБанкаДляРасчетов,ТелефоныБанкаДляРасчетов," +
                "ГруппаФинансовогоУчета_Key,ИспользоватьПрямойОбменСБанком,ОбменСБанкомВключен,СчетУчета_Key,СВИФТБанка," +
                "СВИФТБанкаДляРасчетов,ИностранныйБанк,СчетВБанкеДляРасчетов,Закрыт,ОтдельныйСчетГОЗ,ГосударственныйКонтракт_Key," +
                "НаправлениеДеятельности_Key&$filter=Description eq 'КОНГРЕСС-БАНК ОАО, Торговый дом \"Комплексный\" (EUR)'";
        String fctQuery = ui.getRequestUri().getQuery();
        if (fctQuery.equals(expQuery1)) {
            return parseFile("odata/Catalog.xml");
        } else if (fctQuery.equals(expQuery2)) {
            return parseFile("odata/Catalog_filtered.xml");
        } else {
            System.err.println("Запрос отличается от ожидаемых: " + fctQuery);
            throw new RuntimeException();
        }
    }

    @GET
    @Path("AccumulationRegister_ПартииТоваровПереданныеНаКомиссию_RecordType")
    @Produces(MediaType.APPLICATION_ATOM_XML)
    public String getAccumulationRegister(@Context UriInfo ui) {
        String expQuery = "allowedOnly=true&$filter=Period ge datetime'2014-12-04T00:00:00'";
        String fctQuery = ui.getRequestUri().getQuery();
        if (!fctQuery.equals(expQuery)) {
            System.err.println("Запрос отличается от ожидаемого (ожидается: " + expQuery + ", передано: " + fctQuery + ")");
            throw new RuntimeException();
        }
        return parseFile("odata/AccumulationRegister_RecordType.xml");
    }

    @GET
    @Path("AccountingRegister_Хозрасчетный_RecordType")
    @Produces(MediaType.APPLICATION_ATOM_XML)
    public String getAccountingRegister(@Context UriInfo ui) {
        String expQuery = "$filter=Сумма gt 10000000";
        String fctQuery = ui.getRequestUri().getQuery();
        if (!fctQuery.equals(expQuery)) {
            System.err.println("Запрос отличается от ожидаемого (ожидается: " + expQuery + ", передано: " + fctQuery + ")");
            throw new RuntimeException();
        }
        return parseFile("odata/AccountingRegister_RecordType.xml");
    }

    @GET
    @Path("AccumulationRegister_ТоварыКОтгрузке/{function}")
    @Produces(MediaType.APPLICATION_XML)
    public String getBalance(@PathParam("function") String fctFunction, @Context UriInfo ui) {
        String expFunction = "Balance(Period=datetime'2016-12-22T00:00:00'," +
                "Condition='Назначение_Key eq guid'00000000-0000-0000-0000-000000000000'',Dimensions='Номенклатура,Склад')";
        if (!fctFunction.equals(expFunction)) {
            System.err.println("Функция отличается от ожидаемой (ожидается: " + expFunction + ", передано: " + fctFunction + ")");
            throw new RuntimeException();
        }
        String expQuery = "$select=*,Номенклатура____Presentation";
        String fctQuery = ui.getRequestUri().getQuery();
        if (!fctQuery.equals(expQuery)) {
            System.err.println("Запрос отличается от ожидаемого (ожидается: " + expQuery + ", передано: " + fctQuery + ")");
            throw new RuntimeException();
        }
        return parseFile("odata/AccumulationRegister_Balance.xml");
    }

    @GET
    @Path("AccumulationRegister_ДенежныеСредстваБезналичные/{function}")
    @Produces(MediaType.APPLICATION_XML)
    public String getTurnovers(@PathParam("function") String fctFunction, @Context UriInfo ui) {
        String expFunction = "Turnovers(StartPeriod=datetime'2015-01-01T00:00:00',EndPeriod=datetime'2017-01-01T00:00:00',Dimensions='Организация')";
        if (!fctFunction.equals(expFunction)) {
            System.err.println("Функция отличается от ожидаемой (ожидается: " + expFunction + ", передано: " + fctFunction + ")");
            throw new RuntimeException();
        }
        String expQuery = "$select=*,Организация____Presentation";
        String fctQuery = ui.getRequestUri().getQuery();
        if (!fctQuery.equals(expQuery)) {
            System.err.println("Запрос отличается от ожидаемого (ожидается: " + expQuery + ", передано: " + fctQuery + ")");
            throw new RuntimeException();
        }
        return parseFile("odata/AccumulationRegister_Turnovers.xml");
    }

    @GET
    @Path("ChartOfCharacteristicTypes_ДополнительныеРеквизитыИСведения")
    @Produces(MediaType.APPLICATION_ATOM_XML)
    public String getChartOfCharacteristicTypes(@Context UriInfo ui) {
        String expQuery = "$filter=DeletionMark eq false";
        String fctQuery = ui.getRequestUri().getQuery();
        if (!fctQuery.equals(expQuery)) {
            System.err.println("Запрос отличается от ожидаемого (ожидается: " + expQuery + ", передано: " + fctQuery + ")");
            throw new RuntimeException();
        }
        return parseFile("odata/ChartOfCharacteristicTypes.xml");
    }

    @GET
    @Path("Catalog_Организации_КонтактнаяИнформация")
    @Produces(MediaType.APPLICATION_ATOM_XML)
    public String getTabularSection(@Context UriInfo ui) {
        String expQuery = "$filter=Тип eq 'Адрес' and Вид_Key eq guid'1d78f39b-c180-11e4-a7a9-000d884fd00d'";
        String fctQuery = ui.getRequestUri().getQuery();
        if (!fctQuery.equals(expQuery)) {
            System.err.println("Запрос отличается от ожидаемого (ожидается: " + expQuery + ", передано: " + fctQuery + ")");
            throw new RuntimeException();
        }
        return parseFile("odata/TabularSection.xml");
    }

    private String parseFile(String fileName) {
        StringBuilder result = new StringBuilder();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        try (Scanner scanner = new Scanner(file, "UTF-8")) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line).append('\n');
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return result.toString();
    }

    void stop() {
        server.shutdownNow();
        System.out.println("Эмулятор REST-сервиса 1С остановлен");
    }
}
