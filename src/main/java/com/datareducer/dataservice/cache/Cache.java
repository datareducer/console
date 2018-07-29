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

import com.datareducer.dataservice.entity.DataServiceRequest;
import com.datareducer.dataservice.entity.Field;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Кэш ресурсов REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
public interface Cache {
    /**
     * Время хранения в кэше: один час.
     */
    long ONE_HOUR_CACHE = 3600000;

    /**
     * Время хранения в кэше: одни сутки.
     */
    long ONE_DAY_CACHE = 86400000;

    /**
     * Время хранения в кэше: 0 мс. Ресурс будет получен через REST-сервис 1С
     * вне зависимости от его наличия в кэше. Полученные объекты не кэшируются.
     */
    long NO_CACHE = 0L;

    // Имя поля, содержащего время помещения объекта в кэш.
    String CACHING_TIMESTAMP_FIELD = "CachingTimestamp";

    // Имя поля, содержащего идентификатор запроса к виртуальной таблице.
    String VIRTUAL_TABLE_REQUEST_ID = "VirtualTableRequestId";

    /**
     * Возвращает данные, соответствующие запросу.
     * Если данные в кэше не обнаружены, возвращает пустой список.
     * Если данные в кэше просрочены, возвращает null.
     * Если данные в кэше не согласованы (время версий записей различается), возвращает null.
     * <p>
     * Данные могут оказаться несогласованными, если запрос обновил часть данных другого запроса в кэше.
     *
     * @param request       Параметры запроса.
     * @param cacheLifeTime Время с момента добавления в кэш, по прошествии которого
     *                      данные в кэше считать просроченными (мс).
     * @return данные, соответствующие запросу или null, если данные просрочены или не согласованы.
     * @throws CacheException при получении данных из кэша.
     */
    List<Map<Field, Object>> fetch(DataServiceRequest request, long cacheLifeTime) throws CacheException;

    /**
     * Помещает переданные данные в кэш.
     *
     * @param data    Данные.
     * @param request Параметры запроса.
     * @throws CacheException при кэшировании объектов.
     */
    void store(List<Map<Field, Object>> data, DataServiceRequest request) throws CacheException;

    /**
     * Возвращает набор полей заданного объекта в кэше.
     * Если класс заданного объекта в кэше отсутствует, возвращает пустой набор.
     *
     * @param name Имя объекта.
     * @return Набор полей заданного объекта в кэше.
     * @throws CacheException при получении метаданных.
     */
    Set<Field> getDeclaredProperties(String name) throws CacheException;

    /**
     * Возвращает признак того, что запрос был выполнен ранее и его результаты сохранены в кэше.
     *
     * @param request Запрос к ресурсу REST-сервиса 1С.
     * @return Признак выполнения запроса.
     */
    boolean hasCachedResult(DataServiceRequest request);

    /**
     * Удаляет кэш и закрывает связанные ресурсы.
     */
    void close();
}
