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

import com.datareducer.dataservice.jaxb.DataServiceEntityAdapter;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Описание объекта конфигурации 1С.
 *
 * @author Kirill Mikhaylov
 */
@XmlJavaTypeAdapter(DataServiceEntityAdapter.class)
public interface DataServiceEntity {
    /**
     * Возвращает имя объекта конфигурации, как оно задано в конфигураторе.
     *
     * @return Имя объекта конфигурации.
     */
    String getName();

    /**
     * Возвращает тип объекта конфигурации или поля.
     *
     * @return Тип объекта конфигурации или поля.
     */
    default String getType() {
        return getResourceName();
    }

    /**
     * Возвращает копию набора полей ресурса.
     *
     * @return Набор полей ресурса.
     */
    default LinkedHashSet<Field> getFields() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает поле ресурса по его имени.
     * Если поле с заданным именем отсутствует, возвращает null.
     *
     * @param name Имя поле
     * @return Поле ресурса.
     */
    default Field getFieldByName(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает описание табличных частей объекта конфигурации.
     *
     * @return описание табличных частей объекта конфигурации.
     */
    default Set<TabularSection> getTabularSections() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает табличную часть объекта конфигурации по имени табличной части.
     * Если табличная часть с заданным именем отсутствует, возвращает null.
     *
     * @param name Имя табличной части.
     * @return Табличная часть объекта конфигурации.
     */
    default TabularSection getTabularSectionByName(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает имя ресурса для обращения к REST-сервису 1С.
     *
     * @return Имя ресурса.
     */
    default String getResourceName() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает признак того, что сущность является виртуальной таблицей.
     *
     * @return Признак виртуальной таблицы.
     */
    default boolean isVirtual() {
        return false;
    }

    /**
     * Возвращает имя объекта в дереве метаданных.
     *
     * @return Имя объекта в дереве метаданных.
     */
    default String getMetadataName() {
        return getName();
    }

    /**
     * Возвращает мнемоническое имя объекта.
     *
     * @return Мнемоническое имя объекта.
     */
    default String getMnemonicName() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает время хранения объекта в кэше по умолчанию.
     *
     * @return Время хранения объекта в кэше по умолчанию.
     */
    default Duration getDefaultCacheLifetime() {
        return Duration.ofHours(1);
    }

}
