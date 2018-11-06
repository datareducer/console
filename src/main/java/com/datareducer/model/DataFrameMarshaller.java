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

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.List;
import java.util.Map;

public class DataFrameMarshaller extends XmlAdapter<AdaptedDataFrame, List<Map<String, Object>>> {
    @Override
    public AdaptedDataFrame marshal(List<Map<String, Object>> v) throws Exception {
        AdaptedDataFrame result = new AdaptedDataFrame();
        for (Map<String, Object> map : v) {
            AdaptedDataFrameRecord record = new AdaptedDataFrameRecord();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                AdaptedDataFrameField field = new AdaptedDataFrameField();
                field.setName(entry.getKey());
                field.setValue(entry.getValue().toString());
                field.setType(entry.getValue().getClass().getSimpleName());
                record.getFields().add(field);
            }
            result.getRecords().add(record);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> unmarshal(AdaptedDataFrame v) throws Exception {
        throw new UnsupportedOperationException();
    }

}
