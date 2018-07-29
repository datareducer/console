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
package com.datareducer.dataservice.client;

import com.datareducer.dataservice.cache.Cache;
import com.datareducer.dataservice.cache.CacheException;
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
    // Кэш ресурсов
    private final Cache cacheDatabase;

    private static final Logger log = LogManager.getFormatterLogger(DataServiceClient.class);

    static {
        DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC"));
    }

    /**
     * Создаёт клиент REST-сервиса 1С
     *
     * @param connectionParams Параметры подключения к REST-сервису 1С
     * @param cacheDatabase    Кэш ресурсов
     */
    public DataServiceClient(ConnectionParams connectionParams, Cache cacheDatabase) {
        if (connectionParams == null) {
            throw new IllegalArgumentException("Значение параметра 'connectionParams': null");
        }
        if (cacheDatabase == null) {
            throw new IllegalArgumentException("Значение параметра 'cacheDatabase': null");
        }
        this.connectionParams = connectionParams;
        this.cacheDatabase = cacheDatabase;
        this.rsClient = RestClientBuilder.build();
        this.oDataUrl = String.format("http://%s/%s/odata/standard.odata", connectionParams.getHost(), connectionParams.getBase());

    }

    /**
     * Выполняет HTTP-запрос GET и возвращает полученные данные.
     * Для обеспечения целостности данных в кэше поля запроса дополняются всеми полями других запросов к этому ресурсу,
     * которые были выполнены ранее с использованием кэширования. Результат выполнения запроса включает добавленные поля.
     *
     * @param request     Параметры запроса
     * @param cacheMaxAge Время с момента добавления в кэш, по прошествии которого данные в кэше
     *                    считать просроченными (мс). При значении, равном нулю, кэш не используется.
     * @return Результат выполнения запроса
     * @throws ClientException
     */
    public List<Map<Field, Object>> get(DataServiceRequest request, long cacheMaxAge) throws ClientException {
        if (request == null) {
            throw new IllegalArgumentException("Значение параметра 'request': null");
        }
        if (request.isVirtual()) {
            throw new IllegalArgumentException("Не используется для виртуальных таблиц");
        }
        if (cacheMaxAge < 0) {
            throw new IllegalArgumentException("Значение параметра 'cacheMaxAge' отрицательное: " + cacheMaxAge);
        }
        if (request.getFields().isEmpty()) {
            throw new IllegalStateException("Пустая коллекция полей запроса");
        }

        final String resourceName = request.getResourceName();
        final boolean allFields = request.isAllFields();
        final Condition condition = request.getCondition();
        final boolean allowedOnly = request.isAllowedOnly();
        final boolean useCache = cacheMaxAge != Cache.NO_CACHE;

        /* Дополняем поля запроса.*/
        LinkedHashSet<Field> extendedFields = new LinkedHashSet<>(request.getFields());
        // Поля кэша
        if (useCache) {
            try {
                extendedFields.addAll(cacheDatabase.getDeclaredProperties(request.getClassName()));
            } catch (CacheException e) {
                log.error("При получении метаданных кэша", e);
                throw new ClientException(e);
            }
        }
        // Представления полей
        LinkedHashSet<Field> presentationFields = new LinkedHashSet<>();
        for (Field f : request.getFields()) {
            if (f.isPresentation()) {
                presentationFields.add(new Field(f.getPresentationName(), FieldType.STRING));
            }
        }
        extendedFields.addAll(presentationFields);

        if (!request.getFields().equals(extendedFields)) {
            if (request instanceof Constant) {
                request = new Constant(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof Catalog) {
                request = new Catalog(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof Document) {
                request = new Document(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof DocumentJournal) {
                request = new DocumentJournal(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof ChartOfCharacteristicTypes) {
                request = new ChartOfCharacteristicTypes(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof ChartOfAccounts) {
                request = new ChartOfAccounts(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof ChartOfCalculationTypes) {
                request = new ChartOfCalculationTypes(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof AccumulationRegister) {
                request = new AccumulationRegister(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof InformationRegister) {
                request = new InformationRegister(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof AccountingRegister) {
                request = new AccountingRegister(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof CalculationRegister) {
                request = new CalculationRegister(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof ExchangePlan) {
                request = new ExchangePlan(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof BusinessProcess) {
                request = new BusinessProcess(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof Task) {
                request = new Task(request.getName(), extendedFields, allFields, condition, allowedOnly);
            } else if (request instanceof TabularSection) {
                request = new TabularSection(((TabularSection) request).getParent(), request.getName(),
                        extendedFields, allFields, condition, allowedOnly);
            }
        }
        log.debug("[%s] Обрабатывается запрос на выборку данных '%s' [fields=%s, condition=%s, allowedOnly=%s, cacheMaxAge=%s]",
                request.hashCode(), resourceName, fieldsSetAsString(request.getFields(), false),
                condition, allowedOnly, cacheMaxAge);

        // Выборка данных из кэша
        if (useCache && cacheDatabase.hasCachedResult(request)) {
            List<Map<Field, Object>> cachedData;
            try {
                cachedData = cacheDatabase.fetch(request, cacheMaxAge);
            } catch (CacheException e) {
                log.error("При получении данных из кэша", e);
                throw new ClientException(e);
            }
            if (cachedData != null && !cachedData.isEmpty()) {
                log.info("[%s] Из кэша получено %s записей '%s'", request.hashCode(), cachedData.size(), resourceName);
                return cachedData;
            }
        }

        // Запрос к REST-сервису 1С
        WebTarget wt = rsClient.target(oDataUrl).path(resourceName);
        if (allowedOnly) {
            wt = wt.queryParam("allowedOnly", "true");
        }
        if (!allFields) {
            wt = wt.queryParam("$select", fieldsSetAsString(request.getFields(), false));
        } else if (!presentationFields.isEmpty()) {
            wt = wt.queryParam("$select", "*,".concat(fieldsSetAsString(presentationFields, false)));
        }
        if (!condition.isEmpty()) {
            wt = wt.queryParam("$filter", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED));
        }
        Invocation.Builder ib = wt.request(MediaType.APPLICATION_ATOM_XML)
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, connectionParams.getUser())
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, connectionParams.getPassword());

        log.info("[%s] Сформирован запрос: %s", request.hashCode(), UriComponent.decode(wt.getUri().toString(), QUERY_PARAM));

        List<Map<Field, Object>> result;
        try {
            result = JaxbUtil.parseFeed(ib.get(Feed.class), request);
        } catch (ProcessingException | WebApplicationException e) {
            log.error("[%s] При выполнении запроса к REST-сервису 1C:", request.hashCode(), e);
            throw new ClientException(e);
        }
        log.info("[%s] Запрос вернул %s '%s'", request.hashCode(), result.size(), resourceName);

        // Кэширование
        if (useCache) {
            try {
                cacheDatabase.store(result, request);
            } catch (CacheException e) {
                log.error("[%s] При кэшировании данных:", request.hashCode(), e);
                throw new ClientException(e);
            }
        }

        return result;
    }

    /**
     * Получает записи виртуальной таблицы регистра накопления
     *
     * @param virtualTable Параметры запроса
     * @param cacheMaxAge  Время с момента добавления в кэш, по прошествии которого данные в кэше
     *                     считать просроченными (мс). При значении, равном нулю, кэш не используется.
     * @return записи виртуальной таблицы регистра бухгалтерии
     * @throws ClientException
     */
    public List<Map<Field, Object>> getAccumulationRegisterVirtualTable(AccumulationRegisterVirtualTable virtualTable,
                                                                        long cacheMaxAge) throws ClientException {
        if (virtualTable == null) {
            throw new IllegalArgumentException("Значение параметра 'virtualTable': null");
        }
        if (virtualTable.getDimensionsParam().isEmpty()) {
            throw new IllegalStateException("Набор измерений пуст");
        }
        if (cacheMaxAge < 0) {
            throw new IllegalArgumentException("Значение параметра 'cacheMaxAge' отрицательное: " + cacheMaxAge);
        }

        final int reqId = virtualTable.hashCode();
        final String name = virtualTable.getName();
        final String resourceName = virtualTable.getResourceName();
        final Set<Field> dimensionsParam = virtualTable.getDimensionsParam();
        final LinkedHashSet<Field> presentationFields = virtualTable.getPresentationFields();
        final boolean allDimensions = virtualTable.isAllFields();
        final Condition condition = virtualTable.getCondition();
        final boolean allowedOnly = virtualTable.isAllowedOnly();
        final boolean useCache = cacheMaxAge != Cache.NO_CACHE;

        if (virtualTable instanceof AccumulationRegisterBalance) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы остатков регистра накопления '%s' "
                            + "[dimensions=%s, period=%s, condition=%s, allowedOnly=%s]", reqId,
                    name, dimensionsParam, virtualTable.getPeriod(), condition, allowedOnly);
        } else if (virtualTable instanceof AccumulationRegisterTurnovers) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы оборотов регистра накопления '%s' "
                            + "[dimensions=%s, startPeriod=%s, endPeriod=%s, condition=%s, allowedOnly=%s]", reqId,
                    name, dimensionsParam, virtualTable.getStartPeriod(), virtualTable.getEndPeriod(), condition, allowedOnly);
        } else if (virtualTable instanceof AccumulationRegisterBalanceAndTurnovers) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы остатков и оборотов регистра накопления '%s' "
                            + "[dimensions=%s, startPeriod=%s, endPeriod=%s, condition=%s, allowedOnly=%s]", reqId,
                    name, dimensionsParam, virtualTable.getStartPeriod(), virtualTable.getEndPeriod(), condition, allowedOnly);
        }

        // Выборка данных из кэша
        if (useCache && cacheDatabase.hasCachedResult(virtualTable)) {
            List<Map<Field, Object>> cachedData;
            try {
                cachedData = cacheDatabase.fetch(virtualTable, cacheMaxAge);
            } catch (CacheException e) {
                log.error("При получении данных из кэша", e);
                throw new ClientException(e);
            }
            if (cachedData != null && !cachedData.isEmpty()) {
                log.info("[%s] Из кэша получено %s записей '%s'", reqId, cachedData.size(), resourceName);
                return cachedData;
            }
        }

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
        if (!allDimensions) {
            params.add(String.format("Dimensions='%s'", fieldsSetAsString(dimensionsParam, true)));
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

        WebTarget wt = rsClient.target(oDataUrl).path(resourceName).path(path);
        if (allowedOnly) {
            wt = wt.queryParam("allowedOnly", "true");
        }
        if (!presentationFields.isEmpty()) {
            wt = wt.queryParam("$select", "*,".concat(fieldsSetAsString(presentationFields, false)));
        }

        log.info("[%s] Сформирован запрос: %s", reqId, UriComponent.decode(wt.getUri().toString(), QUERY_PARAM));

        Invocation.Builder ib = wt.request(MediaType.APPLICATION_XML)
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, connectionParams.getUser())
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, connectionParams.getPassword());

        List<Map<Field, Object>> result;
        try {
            result = JaxbUtil.parseResult(ib.get(Result.class), virtualTable);
        } catch (ProcessingException | WebApplicationException e) {
            log.error("[%s] При выполнении запроса к REST-сервису 1C:", reqId, e);
            throw new ClientException(e);
        }

        log.info("[%s] Запрос вернул %s записей виртуальной таблицы регистра накопления '%s'",
                reqId, result.size(), name);

        // Кэширование
        if (useCache) {
            try {
                cacheDatabase.store(result, virtualTable);
            } catch (CacheException e) {
                log.error("[%s] При кэшировании данных:", reqId, e);
                throw new ClientException(e);
            }
        }

        return result;
    }

    /**
     * Получает записи виртуальной таблицы регистра бухгалтерии
     *
     * @param virtualTable Параметры запроса
     * @param cacheMaxAge  Время с момента добавления в кэш, по прошествии которого данные в кэше
     *                     считать просроченными (мс). При значении, равном нулю, кэш не используется.
     * @return записи виртуальной таблицы регистра бухгалтерии
     * @throws ClientException
     */
    public List<Map<Field, Object>> getAccountingRegisterVirtualTable(AccountingRegisterVirtualTable virtualTable,
                                                                      long cacheMaxAge) throws ClientException {
        if (virtualTable == null) {
            throw new IllegalArgumentException("Значение параметра 'virtualTable': null");
        }
        if (cacheMaxAge < 0) {
            throw new IllegalArgumentException("Значение параметра 'cacheMaxAge' отрицательное: " + cacheMaxAge);
        }

        final int reqId = virtualTable.hashCode();
        final String name = virtualTable.getName();
        final String resourceName = virtualTable.getResourceName();
        final LinkedHashSet<Field> presentationFields = virtualTable.getPresentationFields();
        final Condition condition = virtualTable.getCondition();
        final boolean allowedOnly = virtualTable.isAllowedOnly();
        final boolean useCache = cacheMaxAge != Cache.NO_CACHE;

        if (virtualTable instanceof AccountingRegisterTurnovers) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы оборотов регистра бухгалтерии '%s' "
                            + "[dimensions=%s, startPeriod=%s, endPeriod=%s, condition=%s, accountCondition=%s, balancedAccountCondition=%s," +
                            " extraDimensions=%s, balancedExtraDimensions=%s, allowedOnly=%s]",
                    reqId, name, virtualTable.getDimensionsParam(), virtualTable.getStartPeriod(), virtualTable.getEndPeriod(),
                    condition, virtualTable.getAccountCondition(), virtualTable.getBalancedAccountCondition(),
                    virtualTable.getExtraDimensions(), virtualTable.getBalancedExtraDimensions(), allowedOnly);
        } else if (virtualTable instanceof AccountingRegisterBalance) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы остатков регистра бухгалтерии '%s' "
                            + "[dimensions=%s, period=%s, condition=%s, accountCondition=%s, extraDimensions=%s, allowedOnly=%s]",
                    reqId, name, virtualTable.getDimensionsParam(), virtualTable.getPeriod(), condition,
                    virtualTable.getAccountCondition(), virtualTable.getExtraDimensions(), allowedOnly);
        } else if (virtualTable instanceof AccountingRegisterBalanceAndTurnovers) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы остатков и оборотов регистра бухгалтерии '%s' "
                            + "[fields=%s, startPeriod=%s, endPeriod=%s, condition=%s, allowedOnly=%s]",
                    reqId, name, virtualTable.getFieldsParam(), virtualTable.getStartPeriod(), virtualTable.getEndPeriod(),
                    condition, allowedOnly);
        } else if (virtualTable instanceof AccountingRegisterExtDimensions) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы субконто регистра бухгалтерии '%s' "
                            + "[fields=%s, condition=%s, allowedOnly=%s]",
                    reqId, name, virtualTable.getFieldsParam(), condition, allowedOnly);
        } else if (virtualTable instanceof AccountingRegisterRecordsWithExtDimensions) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы движений с субконто регистра бухгалтерии '%s' "
                            + "[fields=%s, startPeriod=%s, endPeriod=%s, condition=%s, top=%s, orderBy=%s, allowedOnly=%s]",
                    reqId, name, virtualTable.getFieldsParam(), virtualTable.getStartPeriod(), virtualTable.getEndPeriod(),
                    condition, virtualTable.getTop(), virtualTable.getOrderBy(), allowedOnly);
        } else if (virtualTable instanceof AccountingRegisterDrCrTurnovers) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы оборотов ДтКт регистра бухгалтерии '%s' "
                            + "[dimensions=%s, startPeriod=%s, endPeriod=%s, condition=%s, accountCondition=%s, balancedAccountCondition=%s," +
                            " extraDimensions=%s, balancedExtraDimensions=%s, allowedOnly=%s]",
                    reqId, name, virtualTable.getDimensionsParam(), virtualTable.getStartPeriod(), virtualTable.getEndPeriod(),
                    condition, virtualTable.getAccountCondition(), virtualTable.getBalancedAccountCondition(),
                    virtualTable.getExtraDimensions(), virtualTable.getBalancedExtraDimensions(), allowedOnly);
        }

        // Выборка данных из кэша
        if (useCache && cacheDatabase.hasCachedResult(virtualTable)) {
            List<Map<Field, Object>> cachedData;
            try {
                cachedData = cacheDatabase.fetch(virtualTable, cacheMaxAge);
            } catch (CacheException e) {
                log.error("При получении данных из кэша", e);
                throw new ClientException(e);
            }
            if (cachedData != null && !cachedData.isEmpty()) {
                log.info("[%s] Из кэша получено %s записей '%s'", reqId, cachedData.size(), resourceName);
                return cachedData;
            }
        }

        List<String> params = new ArrayList<>();

        if (virtualTable instanceof AccountingRegisterTurnovers || virtualTable instanceof AccountingRegisterDrCrTurnovers) {
            if (!condition.isEmpty()) {
                params.add(String.format("Condition='%s'", UriComponent.encode(condition.getHttpForm(), QUERY_PARAM_SPACE_ENCODED)));
            }
            if (!virtualTable.isAllFields()) {
                params.add(String.format("Dimensions='%s'", fieldsSetAsString(virtualTable.getDimensionsParam(), true)));
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
                params.add(String.format("Dimensions='%s'", fieldsSetAsString(virtualTable.getDimensionsParam(), true)));
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

        WebTarget wt = rsClient.target(oDataUrl).path(resourceName).path(path);
        if (allowedOnly) {
            wt = wt.queryParam("allowedOnly", "true");
        }

        if (virtualTable instanceof AccountingRegisterExtDimensions) {
            if (!virtualTable.isAllFields()) {
                Set<Field> fields = new LinkedHashSet<>();
                fields.addAll(virtualTable.getFieldsParam());
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
                fields.addAll(virtualTable.getFieldsParam());
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

        log.info("[%s] Сформирован запрос: %s", reqId, UriComponent.decode(wt.getUri().toString(), QUERY_PARAM));

        Invocation.Builder ib = wt.request(MediaType.APPLICATION_XML)
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, connectionParams.getUser())
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, connectionParams.getPassword());

        List<Map<Field, Object>> result;
        try {
            result = JaxbUtil.parseResult(ib.get(Result.class), virtualTable);
        } catch (ProcessingException | WebApplicationException e) {
            log.error("[%s] При выполнении запроса к REST-сервису 1C:", virtualTable.hashCode(), e);
            throw new ClientException(e);
        }

        log.info("[%s] Запрос вернул %s записей виртуальной таблицы регистра бухгалтерии '%s'",
                reqId, result.size(), name);

        // Кэширование
        if (useCache) {
            try {
                cacheDatabase.store(result, virtualTable);
            } catch (CacheException e) {
                log.error("[%s] При кэшировании данных:", reqId, e);
                throw new ClientException(e);
            }
        }

        return result;
    }

    /**
     * Получает записи виртуальной таблицы регистра сведений
     *
     * @param virtualTable Параметры запроса
     * @param cacheMaxAge  Время с момента добавления в кэш, по прошествии которого данные в кэше
     *                     считать просроченными (мс). При значении, равном нулю, кэш не используется.
     * @return записи виртуальной таблицы регистра сведений
     * @throws ClientException
     */
    public List<Map<Field, Object>> getInformationRegisterVirtualTable(InformationRegisterVirtualTable virtualTable,
                                                                       long cacheMaxAge) throws ClientException {
        if (virtualTable == null) {
            throw new IllegalArgumentException("Значение параметра 'virtualTable': null");
        }
        if (cacheMaxAge < 0) {
            throw new IllegalArgumentException("Значение параметра 'cacheMaxAge' отрицательное: " + cacheMaxAge);
        }

        final int reqId = virtualTable.hashCode();
        final String name = virtualTable.getName();
        final String resourceName = virtualTable.getResourceName();
        final LinkedHashSet<Field> presentationFields = virtualTable.getPresentationFields();
        final boolean allFields = virtualTable.isAllFields();
        final Instant period = virtualTable.getPeriod();
        final Condition condition = virtualTable.getCondition();
        final boolean allowedOnly = virtualTable.isAllowedOnly();
        final boolean useCache = cacheMaxAge != Cache.NO_CACHE;

        final Set<Field> fields = new LinkedHashSet<>();
        fields.addAll(virtualTable.getFieldsParam());
        fields.addAll(presentationFields);

        log.debug("[%s] Обрабатывается запрос на получения cреза %s регистра сведений '%s' "
                        + "[fields=%s, period=%s, condition=%s, allowedOnly=%s]",
                reqId, virtualTable instanceof InformationRegisterSliceLast ? "последних" : "первых",
                name, fields, period, condition, allowedOnly);

        // Выборка данных из кэша
        if (useCache && cacheDatabase.hasCachedResult(virtualTable)) {
            List<Map<Field, Object>> cachedData;
            try {
                cachedData = cacheDatabase.fetch(virtualTable, cacheMaxAge);
            } catch (CacheException e) {
                log.error("При получении данных из кэша", e);
                throw new ClientException(e);
            }
            if (cachedData != null && !cachedData.isEmpty()) {
                log.info("[%s] Из кэша получено %s записей '%s'", reqId, cachedData.size(), resourceName);
                return cachedData;
            }
        }

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

        WebTarget wt = rsClient.target(oDataUrl).path(resourceName).path(path);
        if (allowedOnly) {
            wt = wt.queryParam("allowedOnly", "true");
        }

        if (!allFields) {
            wt = wt.queryParam("$select", fieldsSetAsString(fields, false));
        } else if (!presentationFields.isEmpty()) {
            wt = wt.queryParam("$select", "*,".concat(fieldsSetAsString(presentationFields, false)));
        }

        log.info("[%s] Сформирован запрос: %s", reqId, UriComponent.decode(wt.getUri().toString(), QUERY_PARAM));

        Invocation.Builder ib = wt.request(MediaType.APPLICATION_XML)
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, connectionParams.getUser())
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, connectionParams.getPassword());

        List<Map<Field, Object>> result;
        try {
            result = JaxbUtil.parseFeed(ib.get(Feed.class), virtualTable);
        } catch (ProcessingException | WebApplicationException e) {
            log.error("[%s] При выполнении запроса к REST-сервису 1C:", virtualTable.hashCode(), e);
            throw new ClientException(e);
        }

        log.info("[%s] Запрос вернул %s записей виртуальной таблицы регистра сведений '%s'",
                reqId, result.size(), name);

        // Кэширование
        if (useCache) {
            try {
                cacheDatabase.store(result, virtualTable);
            } catch (CacheException e) {
                log.error("[%s] При кэшировании данных:", reqId, e);
                throw new ClientException(e);
            }
        }

        return result;
    }

    /**
     * Получает записи виртуальной таблицы регистра расчета
     *
     * @param virtualTable Параметры запроса
     * @param cacheMaxAge  Время с момента добавления в кэш, по прошествии которого данные в кэше
     *                     считать просроченными (мс). При значении, равном нулю, кэш не используется.
     * @return записи виртуальной таблицы регистра расчета
     * @throws ClientException
     */
    public List<Map<Field, Object>> getCalculationRegisterVirtualTable(CalculationRegisterVirtualTable virtualTable,
                                                                       long cacheMaxAge) throws ClientException {
        if (virtualTable == null) {
            throw new IllegalArgumentException("Значение параметра 'virtualTable': null");
        }
        if (cacheMaxAge < 0) {
            throw new IllegalArgumentException("Значение параметра 'cacheMaxAge' отрицательное: " + cacheMaxAge);
        }

        if (virtualTable instanceof CalculationRegisterBaseRegister) {
            if (virtualTable.getMainRegisterDimensions().isEmpty()) {
                throw new IllegalArgumentException("Список имён измерений основного регистра расчета пуст");
            }
            if (virtualTable.getBaseRegisterDimensions().isEmpty()) {
                throw new IllegalArgumentException("Список измерений базового регистра расчета пуст");
            }
        }

        final int reqId = virtualTable.hashCode();
        final String name = virtualTable.getName();
        final String resourceName = virtualTable.getResourceName();
        final LinkedHashSet<Field> presentationFields = virtualTable.getPresentationFields();
        final Condition condition = virtualTable.getCondition();
        final boolean allowedOnly = virtualTable.isAllowedOnly();
        final boolean useCache = cacheMaxAge != Cache.NO_CACHE;

        if (virtualTable instanceof CalculationRegisterScheduleData) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы данных графика регистра расчета '%s' "
                            + "[fields=%s, condition=%s, allowedOnly=%s]",
                    reqId, name, virtualTable.getFieldsParam(), condition, allowedOnly);
        } else if (virtualTable instanceof CalculationRegisterActualActionPeriod) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы фактического периода действия регистра расчета '%s' "
                            + "[fields=%s, condition=%s, allowedOnly=%s]",
                    reqId, name, virtualTable.getFieldsParam(), condition, allowedOnly);
        } else if (virtualTable instanceof CalculationRegisterRecalculation) {
            log.debug("[%s] Обрабатывается запрос на получения данных виртуальной таблицы записей перерасчета '%s' регистра расчета '%s' "
                            + "[fields=%s, condition=%s, allowedOnly=%s]",
                    reqId, virtualTable.getRecalculationName(), name, virtualTable.getFieldsParam(), condition, allowedOnly);
        } else if (virtualTable instanceof CalculationRegisterBaseRegister) {
            log.debug("[%s] Обрабатывается запрос на получения данных базового регистра '%s' регистра расчета '%s' "
                            + "[fields=%s, condition=%s, mainRegisterDimensions=%s, baseRegisterDimensions=%s, viewPoints=%s, allowedOnly=%s]",
                    reqId, virtualTable.getBaseRegisterName(), name, virtualTable.getFieldsParam(), condition,
                    virtualTable.getMainRegisterDimensions(), virtualTable.getBaseRegisterDimensions(), virtualTable.getViewPoints(), allowedOnly);
        }

        // Выборка данных из кэша
        if (useCache && cacheDatabase.hasCachedResult(virtualTable)) {
            List<Map<Field, Object>> cachedData;
            try {
                cachedData = cacheDatabase.fetch(virtualTable, cacheMaxAge);
            } catch (CacheException e) {
                log.error("При получении данных из кэша", e);
                throw new ClientException(e);
            }
            if (cachedData != null && !cachedData.isEmpty()) {
                log.info("[%s] Из кэша получено %s записей '%s'", reqId, cachedData.size(), resourceName);
                return cachedData;
            }
        }

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

        WebTarget wt = rsClient.target(oDataUrl).path(resourceName).path(path);
        if (allowedOnly) {
            wt = wt.queryParam("allowedOnly", "true");
        }

        if (virtualTable instanceof CalculationRegisterScheduleData
                || virtualTable instanceof CalculationRegisterActualActionPeriod
                || virtualTable instanceof CalculationRegisterBaseRegister) {
            if (!virtualTable.isAllFields()) {
                Set<Field> fields = new LinkedHashSet<>();
                fields.addAll(virtualTable.getFieldsParam());
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

        log.info("[%s] Сформирован запрос: %s", reqId, UriComponent.decode(wt.getUri().toString(), QUERY_PARAM));

        Invocation.Builder ib = wt.request(MediaType.APPLICATION_XML)
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, connectionParams.getUser())
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, connectionParams.getPassword());

        List<Map<Field, Object>> result;
        try {
            result = JaxbUtil.parseResult(ib.get(Result.class), virtualTable);
        } catch (ProcessingException | WebApplicationException e) {
            log.error("[%s] При выполнении запроса к REST-сервису 1C:", virtualTable.hashCode(), e);
            throw new ClientException(e);
        }

        log.info("[%s] Запрос вернул %s записей виртуальной таблицы регистра расчета '%s'",
                reqId, result.size(), name);

        // Кэширование
        if (useCache) {
            try {
                cacheDatabase.store(result, virtualTable);
            } catch (CacheException e) {
                log.error("[%s] При кэшировании данных:", reqId, e);
                throw new ClientException(e);
            }
        }

        return result;
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
