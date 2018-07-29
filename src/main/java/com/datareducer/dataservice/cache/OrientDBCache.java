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
package com.datareducer.dataservice.cache;

import com.datareducer.Reducer;
import com.datareducer.dataservice.client.DataServiceClient;
import com.datareducer.dataservice.entity.*;
import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.tx.OTransaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реализация кэша ресурсов конкретной информационной базы 1С
 * с использованием документо-ориентированной базы данных <a href="http://orientdb.com/orientdb/">OrientDB</a>
 *
 * @author Kirill Mikhaylov
 */
public final class OrientDBCache implements Cache {
    // Пул соединений с базой данных.
    private final OPartitionedDatabasePoolFactory poolFactory;
    // Подключение к удаленному серверу
    private OServerAdmin remoteServerAdmin;
    // Тип базы данных
    private final Engine engine;
    // URL базы данных
    private final String databaseUrl;
    // Выполненные запросы на выборку данных
    private final Set<DataServiceRequest> performedRequests;

    private String orientdbEngine;
    private String orientdbServer;
    private String orientdbUser;
    private String orientdbPassword;

    private static final Logger log = LogManager.getFormatterLogger(OrientDBCache.class);

    /**
     * Создаёт базу данных OrientDB для кэширования ресурсов 1C
     *
     * @param databaseName Имя базы данных
     */
    public OrientDBCache(String databaseName, Map<String, String> applicationParams) {
        initParams(applicationParams);

        this.engine = Engine.valueOf(orientdbEngine.toUpperCase());
        this.databaseUrl = getDatabaseUrl(databaseName);
        this.poolFactory = new OPartitionedDatabasePoolFactory();
        this.remoteServerAdmin = getServerAdmin();
        this.performedRequests = ConcurrentHashMap.newKeySet();

        createCacheDatabase();
    }

    private void initParams(Map<String, String> applicationParams) {
        orientdbEngine = applicationParams.get(Reducer.ORIENTDB_ENGINE_PARAM_NAME);
        if (orientdbEngine == null || orientdbEngine.isEmpty()) {
            RuntimeException ex = new CacheRuntimeException("Не задано значение параметра " + Reducer.ORIENTDB_ENGINE_PARAM_NAME);
            log.error("При создании базы данных кэша:", ex);
            throw ex;
        }
        if (orientdbEngine.equals("remote")) {
            orientdbServer = applicationParams.get(Reducer.ORIENTDB_HOST_PARAM_NAME);
            if (orientdbServer == null || orientdbServer.isEmpty()) {
                RuntimeException ex = new CacheRuntimeException("Не задано значение параметра " + Reducer.ORIENTDB_HOST_PARAM_NAME);
                log.error("При создании базы данных кэша:", ex);
                throw ex;
            }
        }
        orientdbUser = applicationParams.get(Reducer.ORIENTDB_USER_PARAM_NAME);
        if (orientdbUser == null || orientdbUser.isEmpty()) {
            RuntimeException ex = new CacheRuntimeException("Не задано значение параметра " + Reducer.ORIENTDB_USER_PARAM_NAME);
            log.error("При создании базы данных кэша:", ex);
            throw ex;
        }
        orientdbPassword = applicationParams.get(Reducer.ORIENTDB_PASSWORD_PARAM_NAME);
        if (orientdbPassword == null || orientdbPassword.isEmpty()) {
            RuntimeException ex = new CacheRuntimeException("Не задано значение параметра " + Reducer.ORIENTDB_PASSWORD_PARAM_NAME);
            log.error("При создании базы данных кэша:", ex);
            throw ex;
        }
    }

    @Override
    public synchronized List<Map<Field, Object>> fetch(DataServiceRequest request, long cacheLifeTime) {
        final int reqId = request.hashCode();
        log.debug("[%s] Получение данных из кэша...", reqId);
        final LinkedHashSet<Field> fields = request.getFields();
        final String resourceName = request.getResourceName();
        final String className = request.getClassName();
        final boolean allFields = request.isAllFields();
        final Condition condition = request.getCondition();
        final boolean isVirtualTable = request.isVirtual();

        fields.add(new Field(CACHING_TIMESTAMP_FIELD, FieldType.LONG));

        Map<String, Field> fieldsMap = new HashMap<>();
        for (Field field : fields) {
            fieldsMap.put(field.getName(), field);
        }

        List<Map<Field, Object>> result = new ArrayList<>();
        String query;
        if (!isVirtualTable) {
            query = String.format("SELECT %s FROM %s%s", allFields ? "" : DataServiceClient.fieldsSetAsString(fields, false),
                    className, !condition.isEmpty() ? " WHERE " + condition.getSqlForm() : "");
        } else {
            query = String.format("SELECT FROM %s WHERE %s=%s", className, VIRTUAL_TABLE_REQUEST_ID, reqId);
        }
        log.debug("[%s] Сформирован запрос к кэшу: %s", reqId, query);

        ODatabaseDocumentTx conn = null;
        try {
            conn = getConnection();
            long lastUpdate = 0L;
            for (Object o : conn.query(new OSQLSynchQuery<ODocument>(query))) {
                ODocument doc = (ODocument) o;
                long versionTime = doc.field(CACHING_TIMESTAMP_FIELD);
                if (lastUpdate == 0L) {
                    lastUpdate = versionTime;
                } else {
                    if (versionTime != lastUpdate) {
                        log.debug("[%s] Данные '%s' в кэше не согласованы", reqId, resourceName);
                        return null;
                    }
                }
                if (System.currentTimeMillis() - versionTime > cacheLifeTime) {
                    log.debug("[%s] Срок хранения данных '%s' в кэше истек", reqId, resourceName);
                    return null;
                }
                Map<Field, Object> map = new HashMap<>();

                // XXX В remote-версии OrientDB (в docker-контейнере) был замечен баг при выборке данных
                // из полей с кириллическими именами: значения entry.getValue() в doc.toMap().entrySet() равны null.
                doc.toMap().forEach((fieldName, fieldValue) -> {
                    if (fieldName.startsWith("@")) {
                        //Служебное поле OrientDB
                        return;
                    }
                    if (fieldName.equals(CACHING_TIMESTAMP_FIELD) || fieldName.equals(VIRTUAL_TABLE_REQUEST_ID)) {
                        return;
                    }
                    Field field = fieldsMap.get(fieldName);
                    if (field == null) {
                        throw new CacheRuntimeException("Не найдено поле " + fieldName);
                    }
                    if (fieldValue != null && field.getFieldType() == FieldType.GUID) {
                        // OrientDB не содержит специального типа для GUID
                        fieldValue = UUID.fromString((String) fieldValue);
                    } else if (fieldValue != null && field.getFieldType() == FieldType.DATETIME) {
                        fieldValue = ((Date) fieldValue).toInstant();
                    }
                    map.put(field, fieldValue);
                });

                result.add(map);
            }
        } catch (OException e) {
            RuntimeException ex = new CacheRuntimeException(e);
            log.error("При выборке данных из кэша:", ex);
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (OException e) {
                    log.error("При закрытии подключения к OrientDB:", e);
                }
            }
        }
        log.debug("[%s] Запрос к кэшу вернул %s '%s'", reqId, result.size(), className);
        return result;
    }

    @Override
    public synchronized void store(List<Map<Field, Object>> data, DataServiceRequest request) {
        final int reqId = request.hashCode();
        log.debug("[%s] Обновление кэша...", reqId);
        final String className = request.getClassName();
        final Condition condition = request.getCondition();
        final boolean isVirtualTable = request.isVirtual();
        final boolean hasCachedResult = hasCachedResult(request);

        ODatabaseDocumentTx conn = null;
        try {
            conn = getConnection();
            final OClass oClass = OrientDBCache.createOrMergeClass(conn, request);
            final List<ODocument> docs = new ArrayList<>();
            final long time = System.currentTimeMillis();

            for (Map<Field, Object> props : data) {
                ODocument doc = new ODocument(oClass);
                for (Map.Entry<Field, Object> entry : props.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Instant) {
                        value = Date.from((Instant) value);
                    }
                    doc.field(entry.getKey().getName(), value);
                }
                doc.field(CACHING_TIMESTAMP_FIELD, time);
                if (isVirtualTable) {
                    // XXX Вероятность совпадения хэш-кодов в нашем случае пренебрежимо мала,
                    // поэтому мы можем использовать их в качестве идентификаторов с дополнительной проверкой уникальности.
                    if (!hasCachedResult) {
                        // Проверяем уникальность идентификатора нового кэшируемого запроса.
                        String query = String.format("SELECT FROM %s WHERE %s=%s", className, VIRTUAL_TABLE_REQUEST_ID, reqId);
                        List<Object> objects = conn.query(new OSQLSynchQuery<ODocument>(query));
                        if (!objects.isEmpty()) {
                            throw new CacheRuntimeException("Идентификатор запроса к виртуальной таблице не уникален");
                        }
                    }
                    doc.field(VIRTUAL_TABLE_REQUEST_ID, reqId);
                }
                docs.add(doc);
            }
            String delQuery = null;
            if (!isVirtualTable) {
                // Удаляем из кэша данные, соответствующие отбору, чтобы избежать дублирования.
                delQuery = String.format("DELETE FROM %s%s", className, !condition.isEmpty() ? " WHERE " + condition.getSqlForm() : "");
            } else if (hasCachedResult) {
                // Удаляем из кэша устаревшие данные результата запроса к виртуальной таблице.
                delQuery = String.format("DELETE FROM %s WHERE %s=%s", className, VIRTUAL_TABLE_REQUEST_ID, reqId);
            }

            conn.begin(OTransaction.TXTYPE.OPTIMISTIC);

            int deleted = 0;
            if (delQuery != null) {
                log.debug("[%s] Удаляем из кэша устаревшие данные...", reqId);
                log.debug("[%s] Сформирован запрос к кэшу: %s", reqId, delQuery);
                deleted = conn.command(new OCommandSQL(delQuery)).execute();
                log.debug("[%s] Из кэша будет удалено %s записей '%s'", reqId, deleted, className);
            }
            // Баг OrientDB: Не сохраняет значения полей, если имя поля в кириллице.
            // Обходится явным определением схемы класса перед сохранением объекта (см. метод createOrMergeClass()).
            docs.forEach(ODocument::save);

            conn.commit();

            // Запрос считается выполненным только после успешного обновления кэша
            performedRequests.add(request);
            log.debug("[%s] Кэш успешно обновлен: удалено %s, помещено %s записей '%s'", reqId, deleted, data.size(), className);
        } catch (OException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (OException e1) {
                    log.error("При обновлении кэша:", e1);
                }
            }
            RuntimeException ex = new CacheRuntimeException(e);
            log.error("При обновлении кэша:", ex);
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (OException e) {
                    log.error("При закрытии подключения к OrientDB:", e);
                }
            }
        }
    }

    @Override
    public Set<Field> getDeclaredProperties(String name) {
        Set<Field> result = new LinkedHashSet<>();
        ODatabaseDocumentTx conn = null;
        try {
            conn = getConnection();
            OSchema schema = conn.getMetadata().getSchema();
            if (schema.existsClass(name)) {
                List<OProperty> declaredProperties = new ArrayList<>(schema.getClass(name).declaredProperties());
                Collections.sort(declaredProperties);
                for (OProperty p : declaredProperties) {
                    String fieldName = p.getName();
                    FieldType fieldType;
                    if (fieldName.endsWith("_Key")) {
                        fieldType = FieldType.GUID;
                    } else if (fieldName.endsWith("_Base64Data")) {
                        fieldType = FieldType.BINARY;
                    } else {
                        fieldType = FieldType.getByOrientType(p.getType());
                    }
                    result.add(new Field(fieldName, fieldType));
                }
            }
        } catch (OException e) {
            RuntimeException ex = new CacheRuntimeException(e);
            log.error("При получении метаданных кэша:", ex);
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (OException e) {
                    log.error("При закрытии подключения к OrientDB:", e);
                }
            }
        }
        return result;
    }

    @Override
    public boolean hasCachedResult(DataServiceRequest request) {
        return performedRequests.contains(request);
    }

    /**
     * Если класс с именем <code>name</code> не существует, он будет создан.
     * Если существует, в его схему будут добавлены отсутствующие поля из набора <code>fields</code>.
     * <p>
     * В случае добавления новых полей данные класса полностью очищаются
     * (иначе существующие записи не будут содержать новых полей, что может привести к дублированию данных при отборе по этим полям).
     * Запросы, результаты выполнения которых были удалены из кэша, при их последующих выполнениях всегда будут дополняться новыми полями,
     * т.е. будут отличаться от их первоначальных вариантов, и их результаты будут повторно закэшированы.
     * Не относится к классам виртуальных таблиц.
     * <p>
     * Примечание: OrientDB не требует определения схемы базы данных для сохранения документов, но был обнаружен баг,
     * не позволяющий сохранять данные в поля с кириллическими наименованиями. Явное создание полей позволяет обойти
     * эту ошибку.
     *
     * @return ссылка на класс
     */
    private static OClass createOrMergeClass(ODatabaseDocumentTx conn, DataServiceRequest request) {
        final int reqId = request.hashCode();
        final String className = request.getClassName();
        final String superClassName = request.getSuperclassName();
        final LinkedHashSet<Field> fields = request.getFields();
        final boolean isVirtualTable = request.isVirtual();

        OSchema schema = conn.getMetadata().getSchema();
        OClass cls;
        if (schema.existsClass(className)) {
            cls = schema.getClass(className);
            if (isVirtualTable) {
                // Классы виртуальных таблиц полностью определяются при создании.
                return cls;
            }
        } else {
            cls = schema.createClass(className, schema.getClass(superClassName));
            log.debug("[%s] В базе данных кэша '%s' создан класс '%s'", reqId, conn.getName(), className);
            // Определяем индексные поля
            if (!isVirtualTable) {
                Set<Field> keyFields = request.getKeyFields();
                if (!keyFields.isEmpty()) {
                    String[] indexFields = new String[keyFields.size()];
                    int i = 0;
                    for (Field f : keyFields) {
                        indexFields[i++] = f.getName();
                    }
                    cls.createIndex(className + "_Index", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, indexFields);
                }
            }
        }
        // Добавляем новые поля в схему класса.
        boolean clean = false;
        for (Field field : fields) {
            if (!cls.existsProperty(field.getName())) {
                cls.createProperty(field.getName(), field.getFieldType().getOrientType());
                log.debug("[%s] В схему класса '%s' базы данных кэша '%s' добавлено поле %s", reqId, className, conn.getName(), field);
                clean = true;
            }
        }
        if (clean && !isVirtualTable) {
            int deleted = conn.command(new OCommandSQL("DELETE FROM " + className)).execute();
            log.debug("[%s] Схема класса '%s' базы данных кэша '%s' изменена, данные класса очищены (%s записей)",
                    reqId, className, conn.getName(), deleted);
        }
        schema.reload();
        return cls;
    }

    private void createCacheDatabase() {
        switch (engine) {
            case REMOTE:
                OServerAdmin serverAdmin = null;
                try {
                    serverAdmin = remoteServerAdmin.connect(orientdbUser, orientdbPassword);
                    if (serverAdmin.existsDatabase()) {
                        RuntimeException ex = new CacheRuntimeException("База данных кэша уже существует: " + databaseUrl);
                        log.error("При создании базы данных кэша '%s':", databaseUrl, ex);
                        throw ex;
                    }
                    // iDatabaseType - 'document' or 'graph'
                    // iStorageMode - local or memory
                    serverAdmin.createDatabase("document", "memory");
                } catch (IOException | OException e) {
                    RuntimeException ex = new CacheRuntimeException(e);
                    log.error("При создании базы данных кэша '%s':", databaseUrl, ex);
                    throw ex;
                } finally {
                    if (serverAdmin != null) {
                        try {
                            serverAdmin.close();
                        } catch (OException e) {
                            log.error("При закрытии подключения к OrientDB:", e);
                        }
                    }
                }
                break;
            case MEMORY:
                ODatabaseDocumentTx db = null;
                try {
                    db = new ODatabaseDocumentTx(databaseUrl);
                    if (db.exists()) {
                        RuntimeException ex = new CacheRuntimeException("База данных кэша уже существует: " + databaseUrl);
                        log.error("При создании базы данных кэша '%s':", databaseUrl, ex);
                        throw ex;
                    }
                    db.create();
                } catch (OException e) {
                    RuntimeException ex = new CacheRuntimeException(e);
                    log.error("При создании базы данных кэша '%s':", databaseUrl, ex);
                    throw ex;
                } finally {
                    if (db != null) {
                        try {
                            db.close();
                        } catch (OException e) {
                            log.error("При закрытии подключения к OrientDB:", e);
                        }
                    }
                }
                break;
            case PLOCAL:
                throw new UnsupportedOperationException();
        }
        log.debug("Создана база данных '%s'", databaseUrl);

        ODatabaseDocumentTx conn = null;
        try {
            conn = getConnection();

            createSuperClass(Constant.SUPERCLASS_NAME, Constant.KEY_FIELDS, conn);
            createSuperClass(Catalog.SUPERCLASS_NAME, Catalog.KEY_FIELDS, conn);
            createSuperClass(Document.SUPERCLASS_NAME, Document.KEY_FIELDS, conn);
            createSuperClass(DocumentJournal.SUPERCLASS_NAME, DocumentJournal.KEY_FIELDS, conn);
            createSuperClass(ChartOfCharacteristicTypes.SUPERCLASS_NAME, ChartOfCharacteristicTypes.KEY_FIELDS, conn);
            createSuperClass(ChartOfAccounts.SUPERCLASS_NAME, ChartOfAccounts.KEY_FIELDS, conn);
            createSuperClass(ChartOfCalculationTypes.SUPERCLASS_NAME, ChartOfCalculationTypes.KEY_FIELDS, conn);
            createSuperClass(InformationRegister.SUPERCLASS_NAME, InformationRegister.KEY_FIELDS, conn);
            createSuperClass(AccumulationRegister.SUPERCLASS_NAME, AccumulationRegister.KEY_FIELDS, conn);
            createSuperClass(AccountingRegister.SUPERCLASS_NAME, AccountingRegister.KEY_FIELDS, conn);
            createSuperClass(CalculationRegister.SUPERCLASS_NAME, CalculationRegister.KEY_FIELDS, conn);
            createSuperClass(ExchangePlan.SUPERCLASS_NAME, ExchangePlan.KEY_FIELDS, conn);
            createSuperClass(BusinessProcess.SUPERCLASS_NAME, BusinessProcess.KEY_FIELDS, conn);
            createSuperClass(Task.SUPERCLASS_NAME, Task.KEY_FIELDS, conn);
            createSuperClass(TabularSection.SUPERCLASS_NAME, TabularSection.KEY_FIELDS, conn);

            createSuperClassForVirtualTable(InformationRegisterSliceLast.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(InformationRegisterSliceFirst.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(AccumulationRegisterBalance.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(AccumulationRegisterTurnovers.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(AccumulationRegisterBalanceAndTurnovers.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(AccountingRegisterBalance.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(AccountingRegisterTurnovers.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(AccountingRegisterBalanceAndTurnovers.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(AccountingRegisterExtDimensions.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(AccountingRegisterRecordsWithExtDimensions.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(AccountingRegisterDrCrTurnovers.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(CalculationRegisterScheduleData.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(CalculationRegisterActualActionPeriod.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(CalculationRegisterRecalculation.SUPERCLASS_NAME, conn);
            createSuperClassForVirtualTable(CalculationRegisterBaseRegister.SUPERCLASS_NAME, conn);

            // Устанавливаем прежнюю версию SQL-парсера для обхода бага, не позволяющего использовать кириллицу в SQL-запросах
            conn.command(new OCommandSQL("ALTER DATABASE custom strictSql=false")).execute();

            log.debug("Структура базы данных '%s' определена", databaseUrl);
        } catch (OException e) {
            RuntimeException ex = new CacheRuntimeException(e);
            log.error("При определении структуры базы данных '%s':", databaseUrl, ex);
            close();
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (OException e) {
                    log.error("При закрытии подключения к OrientDB:", e);
                }
            }
        }
    }

    private void createSuperClass(String superClassName, Set<Field> keyFields, ODatabaseDocumentTx db) {
        OClass oClass = db.getMetadata().getSchema().createAbstractClass(superClassName);
        for (Field field : keyFields) {
            oClass.createProperty(field.getName(), field.getFieldType().getOrientType()).setMandatory(true).setNotNull(true).setReadonly(true);
        }
        oClass.createProperty(CACHING_TIMESTAMP_FIELD, OType.LONG).setMandatory(true).setNotNull(true).setReadonly(true);
        oClass.setStrictMode(true);
        log.debug("В базе данных '%s' создан суперкласс '%s'", databaseUrl, superClassName);
    }

    // Класс виртуальной таблицы в кэше отличается тем, что результаты каждого уникального запроса к ней сохраняются
    // независимо от результатов других запросов (в отличие от других сущностей, например, Справочников,
    // данные результатов запросов к которым в кэше могут пересекаться).
    private void createSuperClassForVirtualTable(String superClassName, ODatabaseDocumentTx db) {
        OClass oClass = db.getMetadata().getSchema().createAbstractClass(superClassName);
        oClass.createProperty(CACHING_TIMESTAMP_FIELD, OType.LONG).setMandatory(true).setNotNull(true).setReadonly(true);
        oClass.createProperty(VIRTUAL_TABLE_REQUEST_ID, OType.INTEGER).setMandatory(true).setNotNull(true).setReadonly(true);
        log.debug("В базе данных '%s' создан суперкласс '%s'", databaseUrl, superClassName);
    }

    private ODatabaseDocumentTx getConnection() {
        return poolFactory.get(databaseUrl, orientdbUser, orientdbPassword).acquire();
    }

    private OServerAdmin getServerAdmin() {
        try {
            return new OServerAdmin(databaseUrl);
        } catch (IOException e) {
            RuntimeException ex = new CacheRuntimeException(e);
            log.error("При подключении к серверу OrientDB:", ex);
            throw ex;
        }
    }

    private String getDatabaseUrl(String databaseName) {
        String engineType = engine.getType();
        switch (engine) {
            case MEMORY:
                return engineType + ":" + databaseName;
            case REMOTE:
                return engineType + ":" + orientdbServer + "/" + databaseName;
            case PLOCAL:
                //return engineType + ":" + applicationParams.get("orientdb-path") + databaseName;
                throw new UnsupportedOperationException();
            default:
                throw new CacheRuntimeException();
        }
    }

    private enum Engine {
        MEMORY("memory"), REMOTE("remote"), PLOCAL("plocal");

        private final String type;

        Engine(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    @Override
    public void close() {
        switch (engine) {
            case REMOTE:
                OServerAdmin serverAdmin = null;
                try {
                    serverAdmin = remoteServerAdmin.connect(orientdbUser, orientdbPassword);
                    if (serverAdmin.existsDatabase()) {
                        // storageType - Storage type between "plocal" or "memory".
                        serverAdmin.dropDatabase("memory");
                        log.debug("База данных кэша '%s' удалена", databaseUrl);
                    }
                } catch (IOException | OException e) {
                    RuntimeException ex = new CacheRuntimeException(e);
                    log.error("При удалении базы данных кэша '%s':", databaseUrl, ex);
                    throw ex;
                } finally {
                    if (serverAdmin != null) {
                        try {
                            serverAdmin.close();
                        } catch (OException e) {
                            log.error("При закрытии подключения к OrientDB:", e);
                        }
                    }
                }
                break;
            case MEMORY:
                ODatabaseDocumentTx conn = null;
                try {
                    conn = getConnection();
                    if (conn.exists()) {
                        conn.drop();
                    }
                    log.debug("База данных кэша '%s' удалена", databaseUrl);
                } catch (OException e) {
                    RuntimeException ex = new CacheRuntimeException(e);
                    log.error("При удалении базы данных кэша '%s':", databaseUrl, ex);
                    throw ex;
                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (OException e) {
                            log.error("При закрытии подключения к OrientDB:", e);
                        }
                    }
                }
                break;
            case PLOCAL:
                throw new UnsupportedOperationException();
        }
        poolFactory.close();
    }

}
