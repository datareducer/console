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
package com.datareducer.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.regex.Pattern;

/**
 * Параметр скрипта на языке R.
 *
 * @author Kirill Mikhaylov
 */
@XmlRootElement(name = "ScriptParameter")
@XmlType(name = "ScriptParameter")
public class ScriptParameter {
    public final static String PARAM_PREFIX = "&";

    public final static Pattern PARAM_PATTERN = Pattern.compile(PARAM_PREFIX + "[a-zA-Z0-9_]*");

    // Имена предопределённых параметров
    // Наименование скрипта
    public final static String NAME_PARAM = "name";
    // Описание скрипта
    public final static String DESCRIPTION_PARAM = "description";
    // Имя HTTP-ресурса
    public final static String RESOURCE_NAME_PARAM = "resourceName";
    // Случайный идентификатор запроса к HTTP-ресурсу
    public final static String REQUEST_ID_PARAM = "requestId";

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty value = new SimpleStringProperty("");
    // Включать параметр в доступные пераметры http-запроса
    private final BooleanProperty httpParameter = new SimpleBooleanProperty();

    public ScriptParameter() {
    }

    public ScriptParameter(String name, String value, boolean isHttpParameter) {
        if (name == null) {
            throw new IllegalArgumentException("Значение параметра 'name': null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Значение параметра 'value': null");
        }
        setName(name);
        setValue(value);
        setHttpParameter(isHttpParameter);
    }

    public StringProperty nameProperty() {
        return name;
    }

    @XmlAttribute(name = "paramName")
    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        nameProperty().set(name);
    }

    public StringProperty valueProperty() {
        return value;
    }

    @XmlAttribute(name = "paramValue")
    public String getValue() {
        return value.get();
    }

    public void setValue(String name) {
        valueProperty().set(name);
    }

    public BooleanProperty httpParameterProperty() {
        return httpParameter;
    }

    @XmlAttribute(name = "isHttpParameter")
    public boolean isHttpParameter() {
        return httpParameter.get();
    }

    public void setHttpParameter(boolean isHttpParameter) {
        httpParameterProperty().set(isHttpParameter);
    }

    public static boolean isPredefinedParam(String paramName) {
        String name = removePrefix(paramName);
        return name.equals(NAME_PARAM) || name.equals(DESCRIPTION_PARAM)
                || name.equals(RESOURCE_NAME_PARAM) || name.equals(REQUEST_ID_PARAM);
    }

    public static String removePrefix(String paramName) {
        if (paramName.startsWith(PARAM_PREFIX)) {
            return paramName.substring(PARAM_PREFIX.length(), paramName.length());
        }
        return paramName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ScriptParameter)) {
            return false;
        }
        ScriptParameter that = (ScriptParameter) o;
        return that.getName().equals(getName()) && that.getValue().equals(getValue());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + getName().hashCode();
        result = 31 * result + getValue().hashCode();
        return result;
    }
}
