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
package com.datareducer.dataservice.client;

import com.datareducer.dataservice.entity.*;
import com.datareducer.dataservice.jaxb.JaxbUtil;
import com.datareducer.dataservice.jaxb.RestClientBuilder;
import com.datareducer.dataservice.jaxb.atom.Feed;
import com.datareducer.dataservice.jaxb.csdl.EdmxType;
import com.datareducer.dataservice.jaxb.register.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.uri.UriComponent;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.glassfish.jersey.uri.UriComponent.Type.QUERY_PARAM;
import static org.glassfish.jersey.uri.UriComponent.Type.QUERY_PARAM_SPACE_ENCODED;

/**
 * Клиент REST-сервиса конкретной информационной базы 1С
 *
 * @author Kirill Mikhaylov
 */
public final class DataServiceClient {
    public static final DateTimeFormatter DATE_TIME_FORMATTER;

    // Параметры подключения к REST-сервису 1С
    private final ConnectionParams connectionParams;
    // Клиент REST-сервиса 1C
    private final Client rsClient;
    // URL REST-сервиса 1С
    private final String oDataUrl;

    private static final Logger log = LogManager.getFormatterLogger(DataServiceClient.class);

    static {
        DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC"));
    }

    /**
     * Создаёт клиент REST-сервиса 1С
     *
     * @param connectionParams Параметры подключения к REST-сервису 1С
     */
    public DataServiceClient(ConnectionParams connectionParams) {
        if (connectionParams == null) {
            throw new IllegalArgumentException("Значение параметра 'connectionParams': null");
        }
        this.connectionParams = connectionParams;
        this.rsClient = RestClientBuilder.build();
        this.oDataUrl = String.format("http://%s/%s/odata/standard.odata", connectionParams.getHost(), connectionParams.getBase());
    }

    /**
     * Выполняет HTTP-запрос GET и возвращает полученные данные.
     *
     * @param request     Параметры запроса
     * @return Результат выполнения запроса
     * @throws ClientException
     */
    public DataServiceResponse get(DataServiceRequest request) throws ClientException {
        if (request == null) {
            throw new IllegalArgumentException("Значение параметра 'request': null");
        }

        WebTarget wt;
        if (request instanceof AccumulationRegisterVirtualTable) {
            wt = getAccumulationRegisterVirtualTableWebTarget((AccumulationRegisterVirtualTable) request);
        } else if (request instanceof AccountingRegisterVirtualTable) {
            wt = getAccountingRegisterVirtualTableWebTarget((AccountingRegisterVirtualTable) request);
        } else if (request instanceof InformationRegisterVirtualTable) {
            wt = getInformationRegisterVirtualTableWebTarget((InformationRegisterVirtualTable) request);
        } else if (request instanceof CalculationRegisterVirtualTable) {
            wt = getCalculationRegisterVirtualTableWebTarget((CalculationRegisterVirtualTable) request);
        } else {
            wt = getWebTarget(request);
        }

        final int reqId = request.hashCode();

        Invocation.Builder ib = wt.request(MediaType.APPLICATION_ATOM_XML)
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, connectionParams.getUser())
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, connectionParams.getPassword());

        log.info("[%s] Сформирован запрос: %s", reqId, UriComponent.decode(wt.getUri().toString(), QUERY_PARAM));

        List<Map<Field, Object>> result;

        try {
            if (request.isVirtual()) {
                result = JaxbUtil.parseResult(ib.get(Result.class), request);
            } else {
                result = JaxbUtil.parseFeed(ib.get(Feed.class), request);
            }
        } catch (ProcessingException | WebApplicationException e) {
            log.error("[%s] При выполнении запроса к REST-сервису 1C:", reqId, e);
            throw new ClientException(e);
        }
        log.info("[%s] Запрос вернул %s записей '%s'", reqId, result.size(), request.getResourceName());

        return new DataServiceResponse(request, result);
    }

    private WebTarget getWebTarget(DataServiceRequest request) {
        final Condition condition = request.getCondition();
        final Set<Field> presentationFields = request.getPresentationFields();

        final Set<Field> fields = new LinkedHashSet<>();
        fields.addAll(request.getFields());
        fields.addAll(presentationFields);

        WebTarget wt = rsClient.target(oDataUrl).path(request.getResourceName());
        if (request.isAllowedOnly()) {
            wt = wt.queryParam("allowedOnly", "true");
        }
        if (!request.isAllFields()) {
            wt = wt.queryParam("$select", fieldsSetAsString(fields, false));
        } else if (!presentationFields.isEmpty()) {
            wt = wt.queryParam("$select", "*,".concat(fieldsSetAsString(presentationFields, false)));
        }
        if (!condition.isEmpty()) {
            wt = wt.queryParam("$filter", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED));
        }

        return wt;
    }

    private WebTarget getAccumulationRegisterVirtualTableWebTarget(AccumulationRegisterVirtualTable virtualTable) {
        final LinkedHashSet<Field> presentationFields = virtualTable.getPresentationFields();
        final Condition condition = virtualTable.getCondition();

        StringBuilder sb = new StringBuilder();
        List<String> params = new ArrayList<>();

        if (virtualTable instanceof AccumulationRegisterBalance) {
            if (virtualTable.getPeriod() != null) {
                params.add(String.format("Period=datetime'%s'", DATE_TIME_FORMATTER.format(virtualTable.getPeriod())));
            }
        } else {
            if (virtualTable.getStartPeriod() != null) {
                params.add(String.format("StartPeriod=datetime'%s'", DATE_TIME_FORMATTER.format(virtualTable.getStartPeriod())));
            }
            if (virtualTable.getEndPeriod() != null) {
                params.add(String.format("EndPeriod=datetime'%s'", DATE_TIME_FORMATTER.format(virtualTable.getEndPeriod())));
            }
        }

        if (!condition.isEmpty()) {
            params.add(String.format("Condition='%s'", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
        }
        if (!virtualTable.isAllFields()) {
            params.add(String.format("Dimensions='%s'", fieldsSetAsString(virtualTable.getRequestedDimensions(), true)));
        }

        Iterator<String> it = params.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(",");
            }
        }

        String path = null;
        if (virtualTable instanceof AccumulationRegisterBalance) {
            path = String.format("Balance(%s)", sb.toString());
        } else if (virtualTable instanceof AccumulationRegisterTurnovers) {
            path = String.format("Turnovers(%s)", sb.toString());
        } else if (virtualTable instanceof AccumulationRegisterBalanceAndTurnovers) {
            path = String.format("BalanceAndTurnovers(%s)", sb.toString());
        }

        WebTarget wt = rsClient.target(oDataUrl).path(virtualTable.getResourceName()).path(path);
        if (virtualTable.isAllowedOnly()) {
            wt = wt.queryParam("allowedOnly", "true");
        }
        if (!presentationFields.isEmpty()) {
            wt = wt.queryParam("$select", "*,".concat(fieldsSetAsString(presentationFields, false)));
        }

        return wt;
    }

    private WebTarget getAccountingRegisterVirtualTableWebTarget(AccountingRegisterVirtualTable virtualTable) {
        final LinkedHashSet<Field> presentationFields = virtualTable.getPresentationFields();
        final Condition condition = virtualTable.getCondition();

        List<String> params = new ArrayList<>();

        if (virtualTable instanceof AccountingRegisterTurnovers || virtualTable instanceof AccountingRegisterDrCrTurnovers) {
            if (!condition.isEmpty()) {
                params.add(String.format("Condition='%s'", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
            }
            if (!virtualTable.isAllFields()) {
                params.add(String.format("Dimensions='%s'", fieldsSetAsString(virtualTable.getRequestedDimensions(), true)));
            }
            if (!virtualTable.getAccountCondition().isEmpty()) {
                params.add(String.format("AccountCondition='%s'", UriComponent.encode(virtualTable.getAccountCondition().getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
            }
            if (!virtualTable.getBalancedAccountCondition().isEmpty()) {
                params.add(String.format("BalancedAccountCondition='%s'", UriComponent.encode(virtualTable.getBalancedAccountCondition().getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
            }
            if (!virtualTable.getExtraDimensions().isEmpty()) {
                params.add(String.format("ExtraDimensions='%s'", virtualTable.getExtraDimensionsString()));
            }
            if (!virtualTable.getBalancedExtraDimensions().isEmpty()) {
                params.add(String.format("BalancedExtraDimensions='%s'", virtualTable.getBalancedExtraDimensionsString()));
            }
            if (virtualTable.getStartPeriod() != null) {
                params.add(String.format("StartPeriod=datetime'%s'", DATE_TIME_FORMATTER.format(virtualTable.getStartPeriod())));
            }
            if (virtualTable.getEndPeriod() != null) {
                params.add(String.format("EndPeriod=datetime'%s'", DATE_TIME_FORMATTER.format(virtualTable.getEndPeriod())));
            }
        } else if (virtualTable instanceof AccountingRegisterBalance) {
            if (!condition.isEmpty()) {
                params.add(String.format("Condition='%s'", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
            }
            if (!virtualTable.isAllFields()) {
                params.add(String.format("Dimensions='%s'", fieldsSetAsString(virtualTable.getRequestedDimensions(), true)));
            }
            if (!virtualTable.getAccountCondition().isEmpty()) {
                params.add(String.format("AccountCondition='%s'", UriComponent.encode(virtualTable.getAccountCondition().getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
            }
            if (!virtualTable.getExtraDimensions().isEmpty()) {
                params.add(String.format("ExtraDimensions='%s'", virtualTable.getExtraDimensionsString()));
            }
            if (virtualTable.getPeriod() != null) {
                params.add(String.format("Period=datetime'%s'", DATE_TIME_FORMATTER.format(virtualTable.getPeriod())));
            }
        } else if (virtualTable instanceof AccountingRegisterBalanceAndTurnovers) {
            if (!condition.isEmpty()) {
                params.add(String.format("Condition='%s'", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
            }
            if (virtualTable.getStartPeriod() != null) {
                params.add(String.format("StartPeriod=datetime'%s'", DATE_TIME_FORMATTER.format(virtualTable.getStartPeriod())));
            }
            if (virtualTable.getEndPeriod() != null) {
                params.add(String.format("EndPeriod=datetime'%s'", DATE_TIME_FORMATTER.format(virtualTable.getEndPeriod())));
            }
        } else if (virtualTable instanceof AccountingRegisterRecordsWithExtDimensions) {
            if (!condition.isEmpty()) {
                params.add(String.format("Condition='%s'", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
            }
            if (virtualTable.getStartPeriod() != null) {
                params.add(String.format("StartPeriod=datetime'%s'", DATE_TIME_FORMATTER.format(virtualTable.getStartPeriod())));
            }
            if (virtualTable.getEndPeriod() != null) {
                params.add(String.format("EndPeriod=datetime'%s'", DATE_TIME_FORMATTER.format(virtualTable.getEndPeriod())));
            }
            if (virtualTable.getTop() != 0) {
                params.add(String.format("Top=%s", virtualTable.getTop()));
            }
            if (!virtualTable.getOrderBy().isEmpty()) {
                params.add(String.format("OrderBy='%s'", namesListAsString(virtualTable.getOrderBy())));
            }
        }

        Iterator<String> it = params.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(",");
            }
        }

        String path = null;
        if (virtualTable instanceof AccountingRegisterTurnovers) {
            path = String.format("Turnovers(%s)", sb.toString());
        } else if (virtualTable instanceof AccountingRegisterBalance) {
            path = String.format("Balance(%s)", sb.toString());
        } else if (virtualTable instanceof AccountingRegisterBalanceAndTurnovers) {
            path = String.format("BalanceAndTurnovers(%s)", sb.toString());
        } else if (virtualTable instanceof AccountingRegisterExtDimensions) {
            path = "ExtDimensions()";
        } else if (virtualTable instanceof AccountingRegisterRecordsWithExtDimensions) {
            path = String.format("RecordsWithExtDimensions(%s)", sb.toString());
        } else if (virtualTable instanceof AccountingRegisterDrCrTurnovers) {
            path = String.format("DrCrTurnovers(%s)", sb.toString());
        }

        WebTarget wt = rsClient.target(oDataUrl).path(virtualTable.getResourceName()).path(path);
        if (virtualTable.isAllowedOnly()) {
            wt = wt.queryParam("allowedOnly", "true");
        }

        if (virtualTable instanceof AccountingRegisterExtDimensions) {
            if (!virtualTable.isAllFields()) {
                Set<Field> fields = new LinkedHashSet<>();
                fields.addAll(virtualTable.getRequestedFields());
                fields.addAll(presentationFields);
                wt = wt.queryParam("$select", fieldsSetAsString(fields, false));
            } else if (!presentationFields.isEmpty()) {
                wt = wt.queryParam("$select", "*,".concat(fieldsSetAsString(presentationFields, false)));
            }
            if (!condition.isEmpty()) {
                wt = wt.queryParam("$filter", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED));
            }
        } else if (virtualTable instanceof AccountingRegisterRecordsWithExtDimensions) {
            if (!virtualTable.isAllFields()) {
                Set<Field> fields = new LinkedHashSet<>();
                fields.addAll(virtualTable.getRequestedFields());
                fields.addAll(presentationFields);
                wt = wt.queryParam("$select", fieldsSetAsString(fields, false));
            } else if (!presentationFields.isEmpty()) {
                wt = wt.queryParam("$select", "*,".concat(fieldsSetAsString(presentationFields, false)));
            }
        } else {
            if (!presentationFields.isEmpty()) {
                wt = wt.queryParam("$select", "*,".concat(fieldsSetAsString(presentationFields, false)));
            }
        }

        return wt;
    }

    private WebTarget getInformationRegisterVirtualTableWebTarget(InformationRegisterVirtualTable virtualTable) {
        final LinkedHashSet<Field> presentationFields = virtualTable.getPresentationFields();
        final Instant period = virtualTable.getPeriod();
        final Condition condition = virtualTable.getCondition();

        final Set<Field> fields = new LinkedHashSet<>();
        fields.addAll(virtualTable.getRequestedFields());
        fields.addAll(presentationFields);

        StringBuilder sb = new StringBuilder();
        List<String> params = new ArrayList<>();

        if (period != null) {
            params.add(String.format("Period=datetime'%s'", DATE_TIME_FORMATTER.format(period)));
        }
        if (!condition.isEmpty()) {
            params.add(String.format("Condition='%s'", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
        }

        Iterator<String> it = params.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(",");
            }
        }

        String path = String.format(
                virtualTable instanceof InformationRegisterSliceLast ? "SliceLast(%s)" : "SliceFirst(%s)", sb.toString());

        WebTarget wt = rsClient.target(oDataUrl).path(virtualTable.getResourceName()).path(path);
        if (virtualTable.isAllowedOnly()) {
            wt = wt.queryParam("allowedOnly", "true");
        }
        if (!virtualTable.isAllFields()) {
            wt = wt.queryParam("$select", fieldsSetAsString(fields, false));
        } else if (!presentationFields.isEmpty()) {
            wt = wt.queryParam("$select", "*,".concat(fieldsSetAsString(presentationFields, false)));
        }

        return wt;
    }

    private WebTarget getCalculationRegisterVirtualTableWebTarget(CalculationRegisterVirtualTable virtualTable) {
        final LinkedHashSet<Field> presentationFields = virtualTable.getPresentationFields();
        final Condition condition = virtualTable.getCondition();

        List<String> params = new ArrayList<>();

        if (virtualTable instanceof CalculationRegisterScheduleData
                || virtualTable instanceof CalculationRegisterActualActionPeriod) {
            if (!condition.isEmpty()) {
                params.add(String.format("Condition='%s'", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
            }
        } else if (virtualTable instanceof CalculationRegisterBaseRegister) {
            if (!condition.isEmpty()) {
                params.add(String.format("Condition='%s'", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
            }
            params.add(String.format("MainRegisterDimensions='%s'", namesListAsString(virtualTable.getMainRegisterDimensions())));
            params.add(String.format("BaseRegisterDimensions='%s'", namesListAsString(virtualTable.getBaseRegisterDimensions())));
            if (!virtualTable.getViewPoints().isEmpty()) {
                params.add(String.format("ViewPoints='%s'", namesListAsString(virtualTable.getViewPoints())));
            }
        }

        Iterator<String> it = params.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(",");
            }
        }

        String path = null;
        if (virtualTable instanceof CalculationRegisterScheduleData) {
            path = String.format("ScheduleData(%s)", sb.toString());
        } else if (virtualTable instanceof CalculationRegisterActualActionPeriod) {
            path = String.format("ActualActionPeriod(%s)", sb.toString());
        } else if (virtualTable instanceof CalculationRegisterRecalculation) {
            path = virtualTable.getRecalculationName().concat("()");
        } else if (virtualTable instanceof CalculationRegisterBaseRegister) {
            path = String.format("Base".concat(virtualTable.getBaseRegisterName()).concat("(%s)"), sb.toString());
        }

        WebTarget wt = rsClient.target(oDataUrl).path(virtualTable.getResourceName()).path(path);
        if (virtualTable.isAllowedOnly()) {
            wt = wt.queryParam("allowedOnly", "true");
        }

        if (virtualTable instanceof CalculationRegisterScheduleData
                || virtualTable instanceof CalculationRegisterActualActionPeriod
                || virtualTable instanceof CalculationRegisterBaseRegister) {
            if (!virtualTable.isAllFields()) {
                Set<Field> fields = new LinkedHashSet<>();
                fields.addAll(virtualTable.getRequestedFields());
                fields.addAll(presentationFields);
                wt = wt.queryParam("$select", fieldsSetAsString(fields, false));
            } else if (!presentationFields.isEmpty()) {
                wt = wt.queryParam("$select", "*,".concat(fieldsSetAsString(presentationFields, false)));
            }
        } else if (virtualTable instanceof CalculationRegisterRecalculation) {
            if (!virtualTable.isAllFields()) {
                wt = wt.queryParam("$select", fieldsSetAsString(virtualTable.getFields(), false));
            } else if (!presentationFields.isEmpty()) {
                wt = wt.queryParam("$select", "*,".concat(fieldsSetAsString(presentationFields, false)));
            }
            if (!condition.isEmpty()) {
                wt = wt.queryParam("$filter", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED));
            }
        }

        return wt;
    }

    /**
     * Запрашивает и возвращает описание метаданных информационной базы.
     *
     * @return Описание метаданных информационной базы.
     * @throws ClientException
     */
    public MetadataTree metadata() throws ClientException {
        WebTarget wt = rsClient.target(oDataUrl).path("$metadata");

        log.info("Сформирован запрос метаданных конфигурации 1С: %s", wt.getUri());

        Invocation.Builder ib = wt.request(MediaType.APPLICATION_XML)
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, connectionParams.getUser())
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, connectionParams.getPassword());

        MetadataTree metadata;
        try {
            metadata = JaxbUtil.parseEdmx(ib.get(EdmxType.class));
        } catch (ProcessingException | WebApplicationException e) {
            log.error("При загрузке метаданных:", e);
            throw new ClientException(e);
        }
        return metadata;
    }


    private String namesListAsString(List<String> names) {
        Iterator<String> it = names.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Возвращает список имен полей через запятую.
     * Если коллекция пустая, возвращает пустую строку.
     *
     * @param fields        Коллекция полей. <code>null</code> не допустим.
     * @param originalNames Отбрасывать суффиксы имён полей.
     * @return список имен полей через запятую.
     */
    public static String fieldsSetAsString(Collection<Field> fields, boolean originalNames) {
        Set<String> names = new LinkedHashSet<>();
        for (Field field : fields) {
            names.add(originalNames ? field.getOriginalName() : field.getName());
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = names.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Закрывает связанные ресурсы
     */
    public void close() {
        rsClient.close();
    }

}
