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

import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rosuda.REngine.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * Результат выполнения скрипта на языке R.
 *
 * @author Kirill Mikhaylov
 */
@XmlRootElement(name = "Result")
@XmlType(propOrder = {"parameters", "dataFrame", "output"})
public final class ScriptResult {
    private REXP dataRexp;
    private ArrayList<REXP> imageRexp;

    private List<ScriptParameter> parameters;

    private List<Map<String, Object>> dataFrame;
    private String output;
    private ArrayList<Image> images;

    private final static Logger log = LogManager.getFormatterLogger(ScriptResult.class);

    public ScriptResult() {
    }

    ScriptResult(REXP dataRexp, ArrayList<REXP> imageRexp, List<ScriptParameter> parameters) {
        this.dataRexp = dataRexp;
        this.imageRexp = imageRexp;
        this.parameters = parameters;
    }

    @XmlElementWrapper(name = "Parameters")
    @XmlElement(name = "Parameter")
    public List<ScriptParameter> getParameters() {
        return parameters;
    }

    @XmlElement(name = "DataFrame")
    @XmlJavaTypeAdapter(DataFrameMarshaller.class)
    public List<Map<String, Object>> getDataFrame() {
        if (dataFrame == null) {
            dataFrame = new ArrayList<>();
            try {
                if (dataRexp instanceof REXPGenericVector) {
                    REXP names = dataRexp.getAttribute("names");
                    if (names != null) {
                        String[] colNames = names.asStrings();
                        Map<String, Object> blankRow = new LinkedHashMap<>();
                        for (String colName : colNames) {
                            if (blankRow.containsKey(colName)) {
                                throw new DuplicateColumnException(colName);
                            }
                            blankRow.put(colName, null);
                        }
                        // Столбцы могут быть разной длины. Определяем наибольшую.
                        int max = 0;
                        for (int s = 0; s < dataRexp.asList().size(); s++) {
                            if (!(dataRexp.asList().at(s) instanceof REXPNull)) {
                                int l = dataRexp.asList().at(s).length();
                                max = l > max ? l : max;
                            }
                        }
                        for (int i = 0; i < max; i++) {
                            dataFrame.add(new LinkedHashMap<>(blankRow));
                        }
                        Object obj = dataRexp.asNativeJavaObject();
                        for (Object o : ((HashMap) obj).entrySet()) {
                            Map.Entry pair = (Map.Entry) o;
                            String colName = (String) pair.getKey();
                            Object value = pair.getValue();
                            if (value instanceof String[]) {
                                String[] column = (String[]) value;
                                for (int i = 0; i < column.length; i++) {
                                    dataFrame.get(i).put(colName, column[i]);
                                }
                            } else if (value instanceof double[]) {
                                double[] column = (double[]) value;
                                for (int i = 0; i < column.length; i++) {
                                    dataFrame.get(i).put(colName, column[i]);
                                }
                            } else if (value instanceof int[]) {
                                int[] column = (int[]) value;
                                for (int i = 0; i < column.length; i++) {
                                    dataFrame.get(i).put(colName, column[i]);
                                }
                            } else if (value instanceof byte[]) {
                                // Булево
                                byte[] column = (byte[]) value;
                                for (int i = 0; i < column.length; i++) {
                                    dataFrame.get(i).put(colName, column[i] == 1);
                                }
                            }
                        }
                    }
                } else if (dataRexp instanceof REXPInteger) {
                    for (int val : (int[]) dataRexp.asNativeJavaObject()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("value", val);
                        dataFrame.add(row);
                    }
                } else if (dataRexp instanceof REXPDouble) {
                    for (double val : (double[]) dataRexp.asNativeJavaObject()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("value", val);
                        dataFrame.add(row);
                    }
                } else if (dataRexp instanceof REXPLogical) {
                    for (byte val : (byte[]) dataRexp.asNativeJavaObject()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("value", val);
                        dataFrame.add(row);
                    }
                } else if (dataRexp instanceof REXPString) {
                    for (String val : (String[]) dataRexp.asNativeJavaObject()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("value", val);
                        dataFrame.add(row);
                    }
                } else if (dataRexp instanceof REXPNull) {
                    return dataFrame;
                } else {
                    RuntimeException e = new UnsupportedOperationException(dataRexp.getClass().toString());
                    log.error(e);
                    throw e;
                }
            } catch (REXPMismatchException e) {
                log.error(e);
                throw new ReducerRuntimeException(e);
            }
        }
        return dataFrame;
    }

    @XmlElement(name = "Output")
    public String getOutput() {
        if (output == null && dataRexp instanceof REXPString) {
            StringBuilder sb = new StringBuilder();
            try {
                String[] strArr = (String[]) dataRexp.asNativeJavaObject();
                for (String str : strArr) {
                    sb.append(str);
                }
            } catch (REXPMismatchException e) {
                log.error(e);
                throw new ReducerRuntimeException(e);
            }
            output = sb.toString();
        }
        return output;
    }

    public ArrayList<Image> getImages() {
        if (images == null) {
            images = new ArrayList<>();
            for (REXP rexp : imageRexp) {
                byte[] bytes;
                try {
                    bytes = rexp.asBytes();
                } catch (REXPMismatchException e) {
                    throw new ReducerRuntimeException(e);
                }
                images.add(new Image(new ByteArrayInputStream(bytes)));
            }
        }
        return images;
    }
}
