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
package com.datareducer.dataservice.entity;

import javax.xml.bind.annotation.XmlEnum;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.datareducer.dataservice.client.DataServiceClient.DATE_TIME_FORMATTER;

/**
 * Тип поля ресурса REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
@XmlEnum
public enum FieldType {
    STRING("Edm.String"),
    DATETIME("Edm.DateTime"),
    SHORT("Edm.Int16"),
    INTEGER("Edm.Int32"),
    LONG("Edm.Int64"),
    DOUBLE("Edm.Double"),
    BOOLEAN("Edm.Boolean"),
    GUID("Edm.Guid"),
    BINARY("Edm.Binary"),
    STREAM("Edm.Stream");

    private final String edmType;

    private static final Map<String, FieldType> edmTypeLookup = new HashMap<>();

    static {
        for (FieldType type : FieldType.values()) {
            edmTypeLookup.put(type.getEdmType(), type);
        }
    }

    FieldType(String edmType) {
        this.edmType = edmType;
    }

    public static FieldType getByEdmType(String edmType) {
        if (edmTypeLookup.containsKey(edmType)) {
            return edmTypeLookup.get(edmType);
        } else {
            throw new IllegalArgumentException("Неизвестный тип данных: " + edmType);
        }
    }

    public Object parseValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Значение параметра 'value': null");
        }
        switch (this) {
            case STRING:
                return value;
            case DATETIME:
                LocalDateTime ldt = LocalDateTime.parse(value, DATE_TIME_FORMATTER);
                return ldt.atZone(DATE_TIME_FORMATTER.getZone()).toInstant();
            case SHORT:
                return Short.parseShort(value);
            case INTEGER:
                return Integer.parseInt(value);
            case LONG:
                return Long.parseLong(value);
            case DOUBLE:
                return Double.parseDouble(value);
            case BOOLEAN:
                return Boolean.parseBoolean(value);
            case GUID:
                return UUID.fromString(value);
            case BINARY:
                return value;
            case STREAM:
                return value;
            default:
                // Недостижимо.
                throw new IllegalArgumentException(value);
        }
    }

    public String getEdmType() {
        return edmType;
    }

}
