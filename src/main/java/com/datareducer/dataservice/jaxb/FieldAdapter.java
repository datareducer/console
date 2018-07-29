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
package com.datareducer.dataservice.jaxb;

import com.datareducer.dataservice.entity.AdaptedField;
import com.datareducer.dataservice.entity.Field;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class FieldAdapter extends XmlAdapter<AdaptedField, Field> {
    @Override
    public Field unmarshal(AdaptedField v) throws Exception {
        return new Field(v.getName(), v.getFieldType(), v.getOrder(), null, v.isComposite(), v.isPresentation());
    }

    @Override
    public AdaptedField marshal(Field v) throws Exception {
        AdaptedField adapted = new AdaptedField();
        adapted.setName(v.getName());
        adapted.setFieldType(v.getFieldType());
        adapted.setOrder(v.getOrder());
        adapted.setComposite(v.isComposite());
        adapted.setPresentation(v.isPresentation());
        return adapted;
    }
}
