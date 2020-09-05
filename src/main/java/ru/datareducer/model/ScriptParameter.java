/*
 * Copyright (c) 2017-2020 Kirill Mikhaylov <admin@datareducer.ru>
 *
 * Этот файл — часть программы DataReducer Console <http://datareducer.ru>.
 *
 * Программа DataReducer Console является свободным программным обеспечением.
 * Вы вправе распространять ее и/или модифицировать в соответствии с условиями
 * версии 3 либо, по вашему выбору, с условиями более поздней версии
 * Стандартной Общественной Лицензии GNU, опубликованной Free Software Foundation.
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
@XmlType(name = "ScriptParameter", propOrder = {"name", "value", "httpParameter"})
public class ScriptParameter {
    public final static Pattern PARAM_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

    public final static Pattern PARAM_PATTERN = Pattern.compile("\\$\\{[a-zA-Z0-9_]+}");

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
    // Включать параметр в доступные параметры http-запроса
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
        if (!PARAM_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Недопустимое имя параметра: " + name);
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
        if (!PARAM_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Недопустимое имя параметра: " + name);
        }
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

    public boolean isPredefined() {
        return isPredefinedParam(getName());
    }

    public static boolean isPredefinedParam(String paramName) {
        String name = removeBraces(paramName);
        return name.equals(NAME_PARAM) || name.equals(DESCRIPTION_PARAM)
                || name.equals(RESOURCE_NAME_PARAM) || name.equals(REQUEST_ID_PARAM);
    }

    public static String removeBraces(String paramName) {
        if (PARAM_PATTERN.matcher(paramName).matches()) {
            return paramName.substring(2, paramName.length() - 1);
        }
        return paramName;
    }

    public static String addBraces(String paramName) {
        if (!PARAM_PATTERN.matcher(paramName).matches()) {
            return "${" + paramName + "}";
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
