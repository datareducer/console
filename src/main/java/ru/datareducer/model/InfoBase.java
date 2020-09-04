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
package ru.datareducer.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import ru.datareducer.dataservice.cache.CacheExpiryPolicy;
import ru.datareducer.dataservice.client.ClientException;
import ru.datareducer.dataservice.client.ConnectionParams;
import ru.datareducer.dataservice.client.DataServiceClient;
import ru.datareducer.dataservice.entity.DataServiceEntity;
import ru.datareducer.dataservice.entity.DataServiceRequest;
import ru.datareducer.dataservice.entity.DataServiceResponse;
import ru.datareducer.dataservice.entity.MetadataTree;

import javax.xml.bind.annotation.*;
import java.util.Map;

/**
 * Объект доступа к данным и метаданным информационой базы 1С.
 * <p>
 * Класс реализует интерфейс DataServiceEntity для добавления объекта в корень дерева метаданных.
 *
 * @author Kirill Mikhaylov
 */
@XmlRootElement(name = "InfoBase")
@XmlType(name = "InfoBase", propOrder = {"host", "base", "user", "password"})
public final class InfoBase implements DataServiceEntity {
    // Тип поля (строка) обусловлен требованиями JAXB для XML ID.
    private final StringProperty id = new SimpleStringProperty("");
    private final StringProperty name = new SimpleStringProperty("");     // Наименование подключения к REST-сервису 1С;

    private final StringProperty host = new SimpleStringProperty("");     // Сервер 1С:Предприятия.
    private final StringProperty base = new SimpleStringProperty("");     // Имя информационной базы 1C.
    private final StringProperty user = new SimpleStringProperty("");     // Имя пользователя REST-сервиса 1C.
    private final StringProperty password = new SimpleStringProperty(""); // Пароль пользователя REST-сервиса 1C.

    private Map<String, String> applicationParams;

    private MetadataTree metadataTree;

    private DataServiceClient dataServiceClient;

    private CacheManager cacheManager;
    private Cache<DataServiceRequest, DataServiceResponse> cache;

    private static final Logger log = LogManager.getFormatterLogger(InfoBase.class);

    public InfoBase() {
        addListeners();
    }

    public InfoBase(String name, String host, String base, String user, String password) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Значение параметра 'name' не задано: " + name);
        }
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Значение параметра 'host' не задано: " + name);
        }
        if (base == null || base.isEmpty()) {
            throw new IllegalArgumentException("Значение параметра 'base' не задано: " + base);
        }
        if (user == null || user.isEmpty()) {
            throw new IllegalArgumentException("Значение параметра 'user' не задано: " + user);
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Значение параметра 'password' не задано: " + password);
        }
        setName(name);
        setHost(host);
        setBase(base);
        setUser(user);
        setPassword(password);

        addListeners();
    }

    private void addListeners() {
        host.addListener((observable, oldValue, newValue) -> this.close());
        base.addListener((observable, oldValue, newValue) -> this.close());
        user.addListener((observable, oldValue, newValue) -> this.close());
    }

    /**
     * Выполняет загрузку дерева матаданных 1С, если не было загружено ранее.
     *
     * @return Дерево метаданных 1С
     */
    public synchronized MetadataTree getMetadataTree() throws ClientException {
        if (metadataTree == null) {
            metadataTree = loadMetadataTree();
        }
        return metadataTree;
    }

    /**
     * Выполняет загрузку дерева матаданных 1С.
     *
     * @return Дерево метаданных 1С
     */
    public MetadataTree loadMetadataTree() throws ClientException {
        metadataTree = getDataServiceClient().metadata();
        return metadataTree;
    }

    /**
     * Выполняет GET-запрос к REST-сервису 1С и возвращает полученные данные.
     *
     * @param request Параметры запроса
     * @return Результат выполнения запроса
     * @throws ClientException
     */
    public DataServiceResponse get(DataServiceRequest request) throws ClientException {
        if (request == null) {
            throw new IllegalArgumentException("Значение параметра 'request': null");
        }
        DataServiceResponse response = getCache().get(request);
        if (response == null) {
            response = getDataServiceClient().get(request);
            getCache().put(request, response);
        } else {
            log.info("[%s] Из кэша получено %s записей '%s'", request.hashCode(), response.size(), request.getResourceName());
        }
        return response;
    }

    private synchronized Cache<DataServiceRequest, DataServiceResponse> getCache() {
        if (cache == null) {
            cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
            cacheManager.init();
            cache = cacheManager.createCache(getId(), CacheConfigurationBuilder
                    .newCacheConfigurationBuilder(DataServiceRequest.class, DataServiceResponse.class, ResourcePoolsBuilder.heap(10))
                    .withExpiry(new CacheExpiryPolicy()));
        }
        return cache;
    }

    private synchronized DataServiceClient getDataServiceClient() {
        if (dataServiceClient == null) {
            ConnectionParams connectionParams = new ConnectionParams(getHost(), getBase(), getUser(), getPassword());
            dataServiceClient = new DataServiceClient(connectionParams);
        }
        return dataServiceClient;
    }

    public StringProperty idProperty() {
        return id;
    }

    @XmlAttribute
    @XmlID
    public String getId() {
        return id.get();
    }

    public void setId(String id) {
        idProperty().set(id);
    }

    public StringProperty nameProperty() {
        return name;
    }

    @XmlAttribute
    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        nameProperty().set(name);
    }

    public final StringProperty hostProperty() {
        return host;
    }

    @XmlElement(name = "Host")
    public String getHost() {
        return host.get();
    }

    public void setHost(String host) {
        hostProperty().set(host);
    }

    public StringProperty baseProperty() {
        return base;
    }

    @XmlElement(name = "Base")
    public String getBase() {
        return base.get();
    }

    public void setBase(String base) {
        baseProperty().set(base);
    }

    public StringProperty userProperty() {
        return user;
    }

    @XmlElement(name = "User")
    public String getUser() {
        return user.get();
    }

    public void setUser(String user) {
        userProperty().set(user);
    }

    public StringProperty passwordProperty() {
        return password;
    }

    @XmlElement(name = "Password")
    public String getPassword() {
        return password.get();
    }

    public void setPassword(String password) {
        passwordProperty().set(password);
    }

    public void setApplicationParams(Map<String, String> applicationParams) {
        this.applicationParams = applicationParams;
    }

    public void close() {
        if (dataServiceClient != null) {
            dataServiceClient.close();
            dataServiceClient = null;
        }
        if (cacheManager != null) {
            cacheManager.close();
            cacheManager = null;
        }
        metadataTree = null;
    }

    @Override
    public String getResourceName() {
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof InfoBase)) {
            return false;
        }
        InfoBase that = (InfoBase) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
