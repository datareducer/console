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

import org.rosuda.REngine.*;
import ru.datareducer.model.ReducerRuntimeException;

import java.util.*;

/**
 * Результат выполнения запроса к REST-сервису 1С
 *
 * @author Kirill Mikhaylov
 */
public final class DataServiceResponse {
    private final DataServiceRequest request;
    private final List<Map<Field, Object>> dataTable;

    private REXP dataFrame;
    private LinkedHashSet<Field> dataTableFields;

    public DataServiceResponse(DataServiceRequest request, List<Map<Field, Object>> dataTable) {
        if (request == null) {
            throw new IllegalArgumentException("Значение параметра 'request': null");
        }
        if (dataTable == null) {
            throw new IllegalArgumentException("Значение параметра 'dataTable': null");
        }
        this.request = request;
        this.dataTable = new ArrayList<>(dataTable);
    }

    public List<Map<Field, Object>> asDataTable() {
        return new ArrayList<>(dataTable);
    }

    /**
     * Возвращает таблицу данных для передачи RServe
     *
     * @return Таблица данных
     */
    public REXP asDataFrame() {
        if (dataFrame != null) {
            return dataFrame;
        }
        // Список имён столбцов
        List<String> colNames = new ArrayList<>();
        // Столбцы таблицы
        Map<Field, Object> cols = new LinkedHashMap<>();
        Set<Field> fields = getDataTableFields();
        for (Field field : fields) {
            colNames.add(field.getName());
            int l = dataTable.size();
            switch (field.getFieldType()) {
                case LONG:
                    cols.put(field, new double[l]);
                    break;
                case SHORT:
                    cols.put(field, new int[l]);
                    break;
                case DOUBLE:
                    cols.put(field, new double[l]);
                    break;
                case BOOLEAN:
                    cols.put(field, new boolean[l]);
                    break;
                default:
                    cols.put(field, new String[l]);
            }
        }
        // Строки таблицы
        for (int rowNum = 0; rowNum < dataTable.size(); rowNum++) {
            Map<Field, Object> row = dataTable.get(rowNum);
            for (Map.Entry<Field, Object> entry : row.entrySet()) {
                Field field = entry.getKey();
                // Набор полей фактического запроса к ресурсу может отличаться от исходного набора.
                if (!fields.contains(field)) {
                    continue;
                }
                Object value = entry.getValue();
                switch (field.getFieldType()) {
                    case GUID:
                        ((String[]) cols.get(field))[rowNum] = value == null ? null : value.toString();
                        break;
                    case LONG:
                        ((double[]) cols.get(field))[rowNum] = (long) value;
                        break;
                    case SHORT:
                        ((int[]) cols.get(field))[rowNum] = (short) value;
                        break;
                    case DOUBLE:
                        ((double[]) cols.get(field))[rowNum] = (double) value;
                        break;
                    case BOOLEAN:
                        ((boolean[]) cols.get(field))[rowNum] = (boolean) value;
                        break;
                    case DATETIME:
                        ((String[]) cols.get(field))[rowNum] = value == null ? null : String.valueOf(value);
                        break;
                    default:
                        ((String[]) cols.get(field))[rowNum] = value == null ? null : (String) value;
                }
            }
        }
        // Столбцы типа REXP
        List<REXP> rCols = new ArrayList<>();
        for (Map.Entry<Field, Object> entry : cols.entrySet()) {
            Field field = entry.getKey();
            Object value = entry.getValue();
            switch (field.getFieldType()) {
                case LONG:
                    rCols.add(new REXPDouble((double[]) value));
                    break;
                case SHORT:
                    rCols.add(new REXPInteger((int[]) value));
                    break;
                case DOUBLE:
                    rCols.add(new REXPDouble((double[]) value));
                    break;
                case BOOLEAN:
                    rCols.add(new REXPLogical((boolean[]) value));
                    break;
                default:
                    rCols.add(new REXPString((String[]) value));
            }
        }
        RList data = new RList(rCols, colNames);
        try {
            dataFrame = REXP.createDataFrame(data);
        } catch (REXPMismatchException e) {
            throw new ReducerRuntimeException(e);
        }

        return dataFrame;
    }

    /**
     * Возвращает поля таблицы результата запроса к ресурсу REST-сервиса 1С.
     *
     * @return Поля таблицы результата запроса к ресурсу REST-сервиса 1С.
     */
    public LinkedHashSet<Field> getDataTableFields() {
        if (dataTableFields == null) {
            dataTableFields = new LinkedHashSet<>();
            for (Field f : request.getRequestedFields()) {
                dataTableFields.add(f);
                if (f.isPresentation()) {
                    dataTableFields.add(new Field(f.getPresentationName(), FieldType.STRING));
                }
            }
            if (request instanceof AccumulationRegisterVirtualTable) {
                dataTableFields.addAll(((AccumulationRegisterVirtualTable) request).getResources());
            } else if (request instanceof AccountingRegisterVirtualTable
                    && !(request instanceof AccountingRegisterExtDimensions)
                    && !(request instanceof AccountingRegisterRecordsWithExtDimensions)) {
                AccountingRegisterVirtualTable vt = (AccountingRegisterVirtualTable) request;
                dataTableFields.addAll(vt.getResources());
                Field af = vt.getAccountField();
                dataTableFields.add(af);
                dataTableFields.add(new Field(af.getPresentationName(), FieldType.STRING));
                if (request instanceof AccountingRegisterTurnovers
                        || request instanceof AccountingRegisterDrCrTurnovers) {
                    Field baf = vt.getBalancedAccountField();
                    dataTableFields.add(baf);
                    dataTableFields.add(new Field(baf.getPresentationName(), FieldType.STRING));
                }
                for (Field f : vt.getExtDimensions()) {
                    dataTableFields.add(f);
                    dataTableFields.add(new Field(f.getPresentationName(), FieldType.STRING));
                }
            }
        }
        return new LinkedHashSet<>(dataTableFields);
    }

    public int size() {
        return dataTable.size();
    }

}
