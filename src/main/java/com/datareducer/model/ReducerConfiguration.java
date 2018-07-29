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

import com.datareducer.ui.LoadConfigurationException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Конфигурация приложения.
 *
 * @author Kirill Mikhaylov
 */
@XmlRootElement(name = "ReducerConfiguration")
@XmlType(name = "ReducerConfiguration", propOrder = {"infoBaseSequenceValue", "scriptSequenceValue",
        "dataServiceResourceSequenceValue", "infoBases", "scripts"})
public final class ReducerConfiguration {
    private final ObservableList<InfoBase> infoBases = FXCollections.observableArrayList();
    private final ObservableList<Script> scripts = FXCollections.observableArrayList();

    private final AtomicInteger infoBaseSequence = new AtomicInteger(0);
    private final AtomicInteger scriptSequence = new AtomicInteger(0);
    private final AtomicInteger dataServiceResourceSequence = new AtomicInteger(0);

    private Map<String, String> applicationParams;

    private static JAXBContext jaxbContext;
    private static MessageBodyWriter<ReducerConfiguration> configBodyWriter;
    private static MessageBodyReader<ReducerConfiguration> configBodyReader;

    @XmlElement(name = "InfoBase")
    public ObservableList<InfoBase> getInfoBases() {
        return infoBases;
    }

    @XmlElement(name = "Script")
    public ObservableList<Script> getScripts() {
        return scripts;
    }

    @XmlElement(name = "InfoBaseSequenceValue")
    public int getInfoBaseSequenceValue() {
        return infoBaseSequence.get();
    }

    public void setInfoBaseSequenceValue(int value) {
        infoBaseSequence.set(value);
    }

    public AtomicInteger getInfoBaseSequence() {
        return infoBaseSequence;
    }

    @XmlElement(name = "ScriptSequenceValue")
    public int getScriptSequenceValue() {
        return scriptSequence.get();
    }

    public void setScriptSequenceValue(int value) {
        scriptSequence.set(value);
    }

    public AtomicInteger getScriptSequence() {
        return scriptSequence;
    }

    @XmlElement(name = "DataServiceResourceSequenceValue")
    public int getDataServiceResourceSequenceValue() {
        return dataServiceResourceSequence.get();
    }

    public void setDataServiceResourceSequenceValue(int value) {
        dataServiceResourceSequence.set(value);
    }

    public AtomicInteger getDataServiceResourceSequence() {
        return dataServiceResourceSequence;
    }

    /**
     * Добавляет в конфигурацию новую информационную базу 1С.
     *
     * @param infoBase Информационная база 1С к добавлению.
     */
    public void addInfoBase(InfoBase infoBase) {
        if (infoBase == null) {
            throw new IllegalArgumentException("Значение параметра 'infoBase': null");
        }
        infoBase.setApplicationParams(applicationParams);
        infoBases.add(infoBase);
    }

    /**
     * Удаляет информационную базу 1С из конфигурации.
     *
     * @param infoBase Информационная база 1С к удалению.
     */
    public void removeInfoBase(InfoBase infoBase) {
        if (infoBase == null) {
            throw new IllegalArgumentException("Значение параметра 'infoBase': null");
        }
        infoBase.close();
        infoBases.remove(infoBase);
        scripts.removeAll(getInfoBaseDependentScriptList(infoBase));
    }

    /**
     * Возвращает список скриптов, которые включают наборы данных заданной информационной базы.
     *
     * @param infoBase Информационная база.
     * @return Список скриптов.
     */
    private List<Script> getInfoBaseDependentScriptList(InfoBase infoBase) {
        List<Script> result = new ArrayList<>();
        for (Script script : scripts) {
            for (DataServiceResource resource : script.getDataServiceResources()) {
                if (resource.getInfoBase().equals(infoBase)) {
                    result.add(script);
                }
            }
        }
        return result;
    }

    /**
     * Добавляет в конфигурацию новый скрипт.
     *
     * @param script Скрипт к добавлению.
     */
    public void addScript(Script script) {
        if (script == null) {
            throw new IllegalArgumentException("Значение параметра 'script': null");
        }
        script.setApplicationParams(applicationParams);
        scripts.add(script);
    }

    /**
     * Удаляет скрипт из конфигурации.
     *
     * @param script Скрипт к удалению.
     */
    public void removeScript(Script script) {
        if (script == null) {
            throw new IllegalArgumentException("Значение параметра 'script': null");
        }
        scripts.remove(script);
    }

    public void setApplicationParams(Map<String, String> applicationParams) {
        if (applicationParams == null) {
            throw new IllegalArgumentException("Значение параметра 'applicationParams': null");
        }
        this.applicationParams = applicationParams;
        for (InfoBase infoBase : getInfoBases()) {
            infoBase.setApplicationParams(applicationParams);
        }
        for (Script script : getScripts()) {
            script.setApplicationParams(applicationParams);
        }
    }

    public void close() {
        infoBases.forEach(InfoBase::close);
    }

    public static JAXBContext getJaxbContext() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(
                    com.datareducer.dataservice.entity.AdaptedConstant.class,
                    com.datareducer.dataservice.entity.AdaptedCatalog.class,
                    com.datareducer.dataservice.entity.AdaptedDocument.class,
                    com.datareducer.dataservice.entity.AdaptedDocumentJournal.class,
                    com.datareducer.dataservice.entity.AdaptedChartOfCharacteristicTypes.class,
                    com.datareducer.dataservice.entity.AdaptedChartOfAccounts.class,
                    com.datareducer.dataservice.entity.AdaptedChartOfCalculationTypes.class,
                    com.datareducer.dataservice.entity.AdaptedAccumulationRegister.class,
                    com.datareducer.dataservice.entity.AdaptedAccumulationRegisterBalance.class,
                    com.datareducer.dataservice.entity.AdaptedAccumulationRegisterTurnovers.class,
                    com.datareducer.dataservice.entity.AdaptedAccumulationRegisterBalanceAndTurnovers.class,
                    com.datareducer.dataservice.entity.AdaptedAccountingRegister.class,
                    com.datareducer.dataservice.entity.AdaptedAccountingRegisterBalance.class,
                    com.datareducer.dataservice.entity.AdaptedAccountingRegisterTurnovers.class,
                    com.datareducer.dataservice.entity.AdaptedAccountingRegisterBalanceAndTurnovers.class,
                    com.datareducer.dataservice.entity.AdaptedAccountingRegisterExtDimensions.class,
                    com.datareducer.dataservice.entity.AdaptedAccountingRegisterRecordsWithExtDimensions.class,
                    com.datareducer.dataservice.entity.AdaptedAccountingRegisterDrCrTurnovers.class,
                    com.datareducer.dataservice.entity.AdaptedExchangePlan.class,
                    com.datareducer.dataservice.entity.AdaptedInformationRegister.class,
                    com.datareducer.dataservice.entity.AdaptedInformationRegisterSliceLast.class,
                    com.datareducer.dataservice.entity.AdaptedInformationRegisterSliceFirst.class,
                    com.datareducer.dataservice.entity.AdaptedCalculationRegister.class,
                    com.datareducer.dataservice.entity.AdaptedCalculationRegisterScheduleData.class,
                    com.datareducer.dataservice.entity.AdaptedCalculationRegisterActualActionPeriod.class,
                    com.datareducer.dataservice.entity.AdaptedCalculationRegisterRecalculation.class,
                    com.datareducer.dataservice.entity.AdaptedCalculationRegisterBaseRegister.class,
                    com.datareducer.dataservice.entity.AdaptedBusinessProcess.class,
                    com.datareducer.dataservice.entity.AdaptedTask.class,
                    com.datareducer.dataservice.entity.AdaptedTabularSection.class,

                    com.datareducer.model.ReducerConfiguration.class,
                    com.datareducer.model.InfoBase.class,
                    com.datareducer.model.DataServiceResource.class,
                    com.datareducer.model.Script.class,
                    com.datareducer.dataservice.entity.Condition.class,
                    com.datareducer.dataservice.entity.AdaptedRelationalExpression.class,
                    com.datareducer.dataservice.entity.LogicalOperator.class
            );
        }
        return jaxbContext;
    }

    public static MessageBodyWriter<ReducerConfiguration> getConfigBodyWriter() {
        if (configBodyWriter == null) {
            configBodyWriter = new MessageBodyWriter<ReducerConfiguration>() {
                @Override
                public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
                    return type == ReducerConfiguration.class;
                }

                @Override
                public void writeTo(ReducerConfiguration config, Class<?> aClass, Type type, Annotation[] annotations,
                                    MediaType mediaType, MultivaluedMap<String, Object> multivaluedMap,
                                    OutputStream outputStream) throws IOException, WebApplicationException {
                    try {
                        JAXBContext jaxbContext = getJaxbContext();
                        jaxbContext.createMarshaller().marshal(config, outputStream);
                    } catch (JAXBException e) {
                        throw new LoadConfigurationException(e.getCause());
                    }
                }
            };
        }
        return configBodyWriter;
    }

    public static MessageBodyReader<ReducerConfiguration> getConfigBodyReader() {
        if (configBodyReader == null) {
            configBodyReader = new MessageBodyReader<ReducerConfiguration>() {
                @Override
                public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
                    return aClass == ReducerConfiguration.class;
                }

                @Override
                public ReducerConfiguration readFrom(Class<ReducerConfiguration> aClass, Type type, Annotation[] annotations,
                                                     MediaType mediaType, MultivaluedMap<String, String> multivaluedMap,
                                                     InputStream inputStream) throws IOException, WebApplicationException {
                    try {
                        JAXBContext jaxbContext = getJaxbContext();
                        Unmarshaller unm = jaxbContext.createUnmarshaller();
                        JAXBElement<ReducerConfiguration> conf = unm.unmarshal(new StreamSource(inputStream), ReducerConfiguration.class);
                        return conf.getValue();
                    } catch (JAXBException ex) {
                        throw new LoadConfigurationException(ex.getCause());
                    }
                }
            };
        }
        return configBodyReader;
    }

}
