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
package com.datareducer.dataservice.entity;

import com.orientechnologies.orient.core.metadata.schema.OType;

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
    STRING("Edm.String", OType.STRING),
    DATETIME("Edm.DateTime", OType.DATETIME),
    SHORT("Edm.Int16", OType.SHORT),
    INTEGER("Edm.Int32", OType.INTEGER),
    LONG("Edm.Int64", OType.LONG),
    DOUBLE("Edm.Double", OType.DOUBLE),
    BOOLEAN("Edm.Boolean", OType.BOOLEAN),
    GUID("Edm.Guid", OType.STRING),
    BINARY("Edm.Binary", OType.STRING),
    STREAM("Edm.Stream", OType.STRING);

    private final String edmType;
    private final OType orientType;

    private static final Map<String, FieldType> edmTypeLookup = new HashMap<>();
    private static final Map<OType, FieldType> orientTypeLookup = new HashMap<>();

    static {
        for (FieldType type : FieldType.values()) {
            edmTypeLookup.put(type.getEdmType(), type);
            if (!type.equals(GUID)) {
                orientTypeLookup.put(type.getOrientType(), type);
            }
        }
    }

    FieldType(String edmType, OType orientType) {
        this.edmType = edmType;
        this.orientType = orientType;
    }

    public static FieldType getByEdmType(String edmType) {
        if (edmTypeLookup.containsKey(edmType)) {
            return edmTypeLookup.get(edmType);
        } else {
            throw new IllegalArgumentException("Неизвестный тип данных: " + edmType);
        }
    }

    /**
     * OrientDB не содержит специального типа данных для GUID,
     * поэтому эти объекты сохраняются с типом данных "Строка".
     * Перед вызовом этого метода нужно проверить, что имя поля не заканчивается на "_Key".
     * Это будет указывать на то, что тип поля - FieldType.GUID.
     * <p>
     * Аналогично для полей BINARY, имена которых заканчиваются на "_Base64Data"
     */
    public static FieldType getByOrientType(OType orientType) {
        if (orientTypeLookup.containsKey(orientType)) {
            return orientTypeLookup.get(orientType);
        } else {
            throw new IllegalArgumentException("Неизвестный тип данных: " + orientType);
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

    public OType getOrientType() {
        return orientType;
    }

}
