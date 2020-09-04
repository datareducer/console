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
package ru.datareducer.dataservice.entity;

import javax.xml.bind.annotation.*;
import java.util.LinkedHashSet;

/**
 * Класс для маршалинга неизменяемых объектов AccountingRegisterBalanceAndTurnovers
 *
 * @author Kirill Mikhaylov
 */
@XmlRootElement(name = "AccountingRegisterBalanceAndTurnovers")
@XmlType(name = "AccountingRegisterBalanceAndTurnovers")
public class AdaptedAccountingRegisterBalanceAndTurnovers {
    private String name;
    private LinkedHashSet<Field> properties = new LinkedHashSet<>();
    private LinkedHashSet<Field> resources = new LinkedHashSet<>();

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElementWrapper(name = "Dimensions")
    @XmlElement(name = "Field")
    public LinkedHashSet<Field> getProperties() {
        return properties;
    }

    public void setProperties(LinkedHashSet<Field> properties) {
        this.properties = properties;
    }

    @XmlElementWrapper(name = "Resources")
    @XmlElement(name = "Field")
    public LinkedHashSet<Field> getResources() {
        return resources;
    }

    public void setResources(LinkedHashSet<Field> resources) {
        this.resources = resources;
    }
}
