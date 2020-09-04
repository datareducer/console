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
package ru.datareducer.dataservice.jaxb;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import ru.datareducer.dataservice.jaxb.atom.Feed;
import ru.datareducer.dataservice.jaxb.csdl.EdmxType;
import ru.datareducer.dataservice.jaxb.register.Result;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Создает новый экземпляр клиента REST-сервиса 1С. Регистрирует компонент
 * базовой аутентификации. Имя пользователя и пароль должны быть установлены при
 * запросе. Созданный объект должен использоваться повторно.
 *
 * @author Kirill Mikhaylov
 */
public class RestClientBuilder {

    private RestClientBuilder() {
    }

    /**
     * Создает новый экземпляр клиента REST-сервиса 1С.
     *
     * @return клиент REST-сервиса 1С
     */
    public static Client build() {
        return ClientBuilder.newBuilder().register(HttpAuthenticationFeature.basicBuilder().build())
                .register(MetadataReader.class)
                .register(AtomFeedReader.class)
                .register(VirtualRegisterReader.class).build();
    }

    /*
     * MessageBodyReader для описания метаданных
     */
    private static class MetadataReader implements MessageBodyReader<EdmxType> {

        @Override
        public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
            return aClass == EdmxType.class;
        }

        @Override
        public EdmxType readFrom(Class<EdmxType> aClass, Type type, Annotation[] annotations,
                                 MediaType mediaType, MultivaluedMap<String, String> multivaluedMap,
                                 InputStream inputStream) throws IOException, WebApplicationException {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(EdmxType.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                JAXBElement<EdmxType> schema = unmarshaller.unmarshal(new StreamSource(inputStream), EdmxType.class);
                return schema.getValue();
            } catch (JAXBException ex) {
                throw new ProcessingException("Ошибка десериализации описания интерфейса OData", ex);
            }
        }
    }

    /*
     * MessageBodyReader для формата Atom
     */
    private static class AtomFeedReader implements MessageBodyReader<Feed> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == Feed.class;
        }

        @Override
        public Feed readFrom(Class<Feed> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                             MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance("ru.datareducer.dataservice.jaxb.atom");
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                JAXBElement<Feed> feed = unmarshaller.unmarshal(new StreamSource(entityStream), Feed.class);
                return feed.getValue();
            } catch (JAXBException ex) {
                throw new ProcessingException("Ошибка десериализации ленты Atom", ex);
            }
        }
    }

    /*
     * MessageBodyReader для виртуальных таблиц регистров 1С
     */
    private static class VirtualRegisterReader implements MessageBodyReader<Result> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == Result.class;
        }

        @Override
        public Result readFrom(Class<Result> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(Result.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                JAXBElement<Result> result = unmarshaller.unmarshal(new StreamSource(entityStream), Result.class);
                return result.getValue();
            } catch (JAXBException ex) {
                throw new ProcessingException("Ошибка десериализации записей виртуальной таблицы регистра 1С", ex);
            }
        }

    }

}
