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
package ru.datareducer.model;

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
                Object value = entry.getValue();
                if (value == null) {
                    field.setValue("NA");
                    field.setType("NA");
                } else {
                    field.setValue(value.toString());
                    field.setType(value.getClass().getSimpleName());
                }
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
