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

package ru.datareducer.dataservice.entity;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Отбор данных при запросе REST-сервису 1С
 *
 * @author Kirill Mikhaylov
 */
@XmlRootElement(name = "Condition")
@XmlType(name = "Condition")
public final class Condition implements FilterElement, Cloneable {
    private List<FilterElement> elements;

    public Condition() {
        elements = new ArrayList<>();
    }

    public Condition(FilterElement el) {
        elements = new ArrayList<>();
        elements.add(el);
    }

    @XmlAnyElement(lax = true)
    public List<FilterElement> getElements() {
        return elements;
    }

    /**
     * Добавляет к отбору логическое выражение или логический оператор
     *
     * @param el - Логическое выражение или логический оператор
     * @return ссылка на объект построителя
     */
    public Condition append(FilterElement el) {
        if (el == LogicalOperator.NOT && !elements.isEmpty() && !(elements.get(elements.size() - 1) instanceof LogicalOperator)) {
            throw new IllegalStateException("Оператору NOT должен предшествовать логический оператор: " + this + " NOT");
        }
        elements.add(el);
        return this;
    }

    @Override
    public String getHttpForm() {
        StringBuilder builder = new StringBuilder();
        for (FilterElement f : elements) {
            if (f instanceof Condition) {
                builder.append('(').append(f.getHttpForm()).append(')');
            } else {
                builder.append(f.getHttpForm());
            }
        }
        return builder.toString().replace("  ", " ");
    }

    /**
     * @return <code>true</code>, если отбор не содержит параметров
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Возвращает количество элементов отбора
     *
     * @return Количество элементов отбора
     */
    public int size() {
        return elements.size();
    }

    /**
     * Возвращает набор всех задействованных в отборе полей объекта.
     *
     * @return Набор всех задействованных в отборе полей объекта.
     */
    Set<Field> getFilterFields() {
        Set<Field> result = new HashSet<>();
        addFilterFieldsToSet(this, result);
        return result;
    }

    private static void addFilterFieldsToSet(Condition condition, Set<Field> fields) {
        for (FilterElement f : condition.elements) {
            if (f instanceof RelationalExpression) {
                fields.add(((RelationalExpression) f).getField());
            } else if (f instanceof Condition) {
                addFilterFieldsToSet((Condition) f, fields);
            }
        }
    }

    @Override
    public Condition clone() {
        try {
            Condition result = (Condition) super.clone();
            result.elements = new ArrayList<>();
            for (FilterElement el : elements) {
                if (el instanceof RelationalExpression) {
                    el = ((RelationalExpression) el).clone();
                } else if (el instanceof Condition) {
                    el = ((Condition) el).clone();
                }
                result.elements.add(el);
            }
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     * Два отбора равны, если равны их HTTP-представления. Отборы с разным
     * порядком элементов будут отличаться.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Condition)) {
            return false;
        }
        Condition fc = (Condition) o;
        return fc.getHttpForm().equals(getHttpForm());
    }

    /**
     * Хэш-код HTTP-представления отбора.
     */
    @Override
    public int hashCode() {
        return getHttpForm().hashCode();
    }

    @Override
    public String toString() {
        return getHttpForm();
    }

}
