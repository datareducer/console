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

import com.datareducer.dataservice.client.ClientRuntimeException;
import com.datareducer.dataservice.jaxb.FieldAdapter;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Поле ресурса REST-сервиса 1С.
 *
 * @author Kirill Mikhaylov
 */
@XmlJavaTypeAdapter(FieldAdapter.class)
public class Field implements DataServiceEntity, Comparable<Field> {
    private final String name;
    private final FieldType fieldType;
    private final int order;

    // Тип объекта конфигурации, на который указывает ссылка (если fieldType == FieldType.GUID).
    private String reference;

    // Поле, относящееся к реквизиту составного типа. Имеет парное поле, которое содержит имя типа и оканчивается суффиксом "_Type".
    // Как правило, хранит значение уникального идентификатора, но не имеет суффикса "_Key". Тип поля - FieldType.STRING.
    private boolean isComposite;

    // Если Истина - в запрос включается представление реквизита.
    private boolean isPresentation;

    /**
     * Создаёт новое поле ресурса REST-сервиса 1С.
     *
     * @param name           Имя поля.
     * @param fieldType      Тип поля.
     * @param order          Порядок следования в метаданных объекта. 0, если порядок неизвестен.
     * @param reference      Тип объекта конфигурации, на который указывает ссылка (если fieldType == FieldType.GUID).
     * @param isComposite    Поле относится к реквизиту составного типа.
     * @param isPresentation Включить в запрос представление реквизита.
     */
    public Field(String name, FieldType fieldType, int order, String reference, boolean isComposite, boolean isPresentation) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Параметр 'name' не задан: " + name);
        }
        if (fieldType == null) {
            throw new IllegalArgumentException("Параметр 'fieldType' равен null");
        }
        if (reference != null && fieldType != FieldType.GUID) {
            throw new IllegalStateException("Реквизит не является ссылочным");
        }
        this.name = name;
        this.fieldType = fieldType;
        this.order = order;
        this.reference = reference;
        this.isComposite = isComposite;
        this.isPresentation = isPresentation;
    }

    public Field(String name, FieldType fieldType, int order) {
        this(name, fieldType, order, null, false, false);
    }

    public Field(String name, FieldType fieldType) {
        this(name, fieldType, 0);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Если имя поля оканчивается суффиксом ("_Key", "_Type" или "_Base64Data")
     * возвращает имя без суффикса.
     *
     * @return Имя поля без суффикса
     */
    public String getOriginalName() {
        if (name.equals("Ref_Key")) {
            throw new ClientRuntimeException("Для поля ссылки на текущую сущность" +
                    " не может быть получено имя без суффикса: " + name);
        }
        if (name.endsWith("_Key")) {
            return name.substring(0, name.indexOf("_Key"));
        } else if (name.endsWith("_Type")) {
            return name.substring(0, name.indexOf("_Type"));
        } else if (name.endsWith("_Base64Data")) {
            return name.substring(0, name.indexOf("_Base64Data"));
        }
        return name;
    }

    /**
     * Возвращает имя поля для получения его представления
     *
     * @return Имя поля для получения его представления
     */
    public String getPresentationName() {
        if (name.equals("Ref_Key")) {
            return "Presentation";
        }
        return getOriginalName().concat("____Presentation");
    }

    @Override
    public String getType() {
        return fieldType.name().toLowerCase();
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public int getOrder() {
        return order;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        if (fieldType != FieldType.GUID) {
            throw new IllegalStateException("Реквизит не является ссылочным");
        }
        this.reference = reference;
    }

    public boolean isComposite() {
        return isComposite;
    }

    public void setComposite(boolean composite) {
        isComposite = composite;
    }

    public boolean isPresentation() {
        return isPresentation;
    }

    public void setPresentation(boolean presentation) {
        this.isPresentation = presentation;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Field)) {
            return false;
        }
        Field that = (Field) o;
        return that.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name + " [" + getType() + (isComposite ? ", c" : "") + "]";
    }

    @Override
    public int compareTo(Field f) {
        if (name.equals(f.name)) {
            return 0;
        }
        if (order < f.order) {
            return -1;
        }
        if (order > f.order) {
            return 1;
        }
        return name.compareTo(f.name);
    }

}
