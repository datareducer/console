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

import com.datareducer.Reducer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static com.datareducer.model.ScriptParameter.*;

/**
 * Скрипт на языке R.
 *
 * @author Kirill Mikhaylov
 */
@XmlRootElement(name = "Script")
@XmlType(name = "Script")
public final class Script {
    private final static String RSERVE_DEFAULT_PORT = "6311";

    // Идентификатор скрипта
    private final IntegerProperty id = new SimpleIntegerProperty();
    // Наименование скрипта
    private final StringProperty name = new SimpleStringProperty("");
    // Имя HTTP-ресурса
    private final StringProperty resourceName = new SimpleStringProperty("");
    // Описание скрипта
    private final StringProperty description = new SimpleStringProperty("");
    // Текст скрипта
    private final StringProperty scriptBody = new SimpleStringProperty("");
    // Наборы данных скрипта
    private final ObservableList<DataServiceResource> dataServiceResources = FXCollections.observableArrayList();
    // Параметры скрипта по умолчанию
    private final ObservableList<ScriptParameter> defaultParams = FXCollections.observableArrayList();
    // Ширина изображения, генерируемого скриптом
    private final IntegerProperty plotWidth = new SimpleIntegerProperty(700);
    // Высота изображения, генерируемого скриптом
    private final IntegerProperty plotHeight = new SimpleIntegerProperty(350);
    // Признак возможности веб-доступа к результату выполнения скрипта
    private final BooleanProperty webAccess = new SimpleBooleanProperty(false);
    // Использовать стандартный шаблон результата выполнения скрипта
    private final BooleanProperty useDefaultTemplate = new SimpleBooleanProperty(true);
    // Кастомный шаблон результата выполнения скрипта
    private final StringProperty template = new SimpleStringProperty();
    // Роли доступа к HTTP-ресурсу
    private final ObservableList<String> securityRoles = FXCollections.observableArrayList();
    // Стандартный шаблон результата выполнения скрипта
    private final static ReadOnlyStringWrapper defaultTemplate = new ReadOnlyStringWrapper();

    private final static Logger log = LogManager.getFormatterLogger(Script.class);

    private static SecureRandom prng = null;

    private Map<String, String> applicationParams;

    public Script() {
    }

    public Script(int id) {
        String idStr = String.valueOf(id);
        setId(id);
        setName("Script ".concat(idStr));
        setResourceName("resource".concat(idStr));
    }

    /**
     * Выполнить скрипт со значениями параметров по умолчанию.
     *
     * @param executor Сервис-исполнитель
     * @return Результат выполнения скрипта
     */
    public ScriptResult execute(ExecutorService executor) throws UndefinedParameterException {
        refillParameterList();
        for (ScriptParameter param : defaultParams) {
            if (param.getValue() == null || param.getValue().isEmpty()) {
                UndefinedParameterException ex = new UndefinedParameterException(param.getName());
                log.error(ex);
                throw ex;
            }
        }
        return execute(executor, generateRequestId(defaultParams), new ArrayList<>());
    }

    /**
     * Выполнить скрипт.
     *
     * @param executor       Сервис-исполнитель.
     * @param requestId      Идентификатор запроса к HTTP-ресурсу.
     * @param clientParams   Список пользовательских значений параметров скрипта. Должен содержать только те параметры,
     *                       которые помечены флагом "httpParameter".
     * @return Результат выполнения скрипта
     */
    public ScriptResult execute(ExecutorService executor, String requestId, List<ScriptParameter> clientParams)
            throws UndefinedParameterException {

        final long executionStart = System.currentTimeMillis();

        if (requestId == null) {
            throw new IllegalArgumentException("Значение параметра 'requestId' равно null");
        }

        Map<String, ScriptParameter> clientParamsMap = new HashMap<>();
        for (ScriptParameter param : clientParams) {
            clientParamsMap.put(param.getName(), param);
        }

        Map<String, ScriptParameter> defaultParamsMap = new HashMap<>();
        for (ScriptParameter param : defaultParams) {
            defaultParamsMap.put(param.getName(), new ScriptParameter(param.getName(), param.getValue(), param.isHttpParameter()));
        }

        // Проверяем, что клиент не задал "лишних" параметров
        for (Map.Entry<String, ScriptParameter> entry : clientParamsMap.entrySet()) {
            ScriptParameter param = defaultParamsMap.get(entry.getKey());
            if (param == null || !param.isHttpParameter()) {
                throw new IllegalArgumentException("Недопустимый параметр: " + entry.getKey());
            }
        }

        Map<String, ScriptParameter> paramsLookup = new HashMap<>();
        paramsLookup.putAll(defaultParamsMap); // Последовательность важна
        paramsLookup.putAll(clientParamsMap); // Последовательность важна
        paramsLookup.put(REQUEST_ID_PARAM, new ScriptParameter(REQUEST_ID_PARAM, requestId, false));
        paramsLookup.put(NAME_PARAM, new ScriptParameter(NAME_PARAM, getName(), false));
        paramsLookup.put(DESCRIPTION_PARAM, new ScriptParameter(DESCRIPTION_PARAM, getDescription(), false));
        paramsLookup.put(RESOURCE_NAME_PARAM, new ScriptParameter(RESOURCE_NAME_PARAM, getResourceName(), false));

        List<ScriptParameter> paramsList = new ArrayList<>();
        for (Map.Entry<String, ScriptParameter> entry : paramsLookup.entrySet()) {
            ScriptParameter param = entry.getValue();
            if (param.getValue() == null || (param.getValue().isEmpty() && !ScriptParameter.isPredefinedParam(param.getName()))) {
               throw new UndefinedParameterException(param.getName());
            }
            paramsList.add(entry.getValue());
        }

        // Последовательно создаем базы данных для кэширования ресурсов.
        // (параллельные операции приводят к ошибкам OrientDB).
        for (DataServiceResource resource : dataServiceResources) {
            resource.getInfoBase().createCacheDatabase();
        }

        // Получаем наборы данных.
        Map<String, Future<REXP>> futures = new HashMap<>();
        for (DataServiceResource resource : dataServiceResources) {
            resource.setParametersLookup(paramsLookup);
            futures.put(resource.getName(), executor.submit(resource::getDataFrame));
        }

        String host = applicationParams.get(Reducer.RSERVE_HOST_PARAM_NAME);
        if (host == null || host.isEmpty()) {
            RuntimeException ex = new ReducerRuntimeException("Не задано значение параметра " + Reducer.RSERVE_HOST_PARAM_NAME);
            log.error("При установке соединения с Rserve:", ex);
            throw ex;
        }

        RConnection conn = null;
        try {
            conn = new RConnection(host);

            if (conn.needLogin()) {
                String user = applicationParams.get(Reducer.RSERVE_USER_PARAM_NAME);
                if (user == null || user.isEmpty()) {
                    RuntimeException ex = new ReducerRuntimeException("Не задано значение параметра " + Reducer.RSERVE_USER_PARAM_NAME);
                    log.error("[%s] При установке соединения с Rserve:", hashCode(), ex);
                    throw ex;
                }
                String pwd = applicationParams.get(Reducer.RSERVE_PASSWORD_PARAM_NAME);
                if (pwd == null || pwd.isEmpty()) {
                    RuntimeException ex = new ReducerRuntimeException("Не задано значение параметра" + Reducer.RSERVE_PASSWORD_PARAM_NAME);
                    log.error("[%s] При установке соединения с Rserve:", hashCode(), ex);
                    throw ex;
                }
                conn.login(user, pwd);
            }

            try {
                for (Map.Entry<String, Future<REXP>> future : futures.entrySet()) {
                    // Передаём наборы данных RServe
                    conn.assign(future.getKey(), future.getValue().get());
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error(e);
                throw new ReducerRuntimeException(e);
            }

            // Устанавливаем параметры тела скрипта
            String scriptBody = getScriptBody();
            for (Map.Entry<String, ScriptParameter> entry : paramsLookup.entrySet()) {
                String param = java.util.regex.Pattern.quote(ScriptParameter.addBraces(entry.getKey()));
                scriptBody = scriptBody.replaceAll(param, entry.getValue().getValue());
            }

            // Определяем, требуется ли выводить графику
            int devNum = 0;
            String lines[] = scriptBody.split("\\r?\\n");
            for (String line : lines) {
                int ind = line.indexOf("dev.off()");
                if (ind != -1) {
                    if (!line.substring(0, ind).contains("#")) {
                        devNum++;
                    }
                }
                int ind1 = line.indexOf("CairoPNG(");
                if (ind1 != -1) {
                    if (!line.substring(0, ind1).contains("#")) {
                        // Пользователь сам создаёт устройство вывода изображения
                        devNum--;
                    }
                }

            }

            // Создаём устройства вывода графики.
            // Нужно выполнять ДО выполнения основного скрипта.
            if (devNum > 0) {
                String device = "png";
                if (conn.parseAndEval("suppressWarnings(require('Cairo',quietly=TRUE))").asInteger() > 0)
                    device = "CairoPNG";
                else {
                    log.warn("Установка пакета 'Cairo' может улучшить качество графики");
                }
                for (int i = 0; i < devNum; i++) {
                    REXP xp = conn.parseAndEval("try(" + device + "('plot" + i + ".png', width =" + getPlotWidth()
                            + ", height =" + getPlotHeight() + "))");
                    if (xp.inherits("try-error")) {
                        log.error("Невозможно открыть устройство вывода " + device + " графики: " + xp.asString());
                        break;
                    }
                }
            }

            // Получаем таблицу данных
            REXP data = conn.parseAndEval(scriptBody);

            // Получаем изображения
            ArrayList<REXP> imageRexp = new ArrayList<>();
            for (int i = 0; i < devNum; i++) {
                REXP rexp = conn.parseAndEval("r=readBin('plot" + i + ".png','raw',1024*1024); unlink('plot" + i + ".png'); r");
                imageRexp.add(rexp);
            }

            float duration = (System.currentTimeMillis() - executionStart);
            log.info("[%s] Выполнено за %s с", hashCode(), duration / 1000);

            return new ScriptResult(data, imageRexp, paramsList);

        } catch (RserveException e) {
            log.error("[%s] При установке соединения с Rserve (%s:%s):", hashCode(), host, RSERVE_DEFAULT_PORT, e);
            throw new ReducerRuntimeException(e);
        } catch (REngineException | REXPMismatchException e) {
            String msg = "";
            try {
                // Получаем описание ошибки от R
                msg = conn.parseAndEval("geterrmessage()").asString();
                if (!msg.isEmpty()) {
                    msg = msg.substring(0, msg.length() - 1); // Удаление перевода строки
                }
            } catch (REngineException | REXPMismatchException e1) {
                log.error("[%s] При получении описания ошибки от R:", hashCode(), e1);
            }
            log.error("[%s] %s:", hashCode(), msg, e);
            throw new ReducerRuntimeException(e);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public void refillParameterList() {
        Set<String> paramsNames = new HashSet<>();
        for (DataServiceResource resource : dataServiceResources) {
            paramsNames.addAll(resource.getParameterNamesSet());
        }
        Matcher paramMatcher = PARAM_PATTERN.matcher(getScriptBody());
        while (paramMatcher.find()) {
            String name = paramMatcher.group();
            if (!ScriptParameter.isPredefinedParam(name)) {
                paramsNames.add(ScriptParameter.removeBraces(name));
            }
        }
        Map<String, ScriptParameter> paramsLookup = getDefaultParamsLookup();
        // Добавляем недостающие параметры
        for (String name : paramsNames) {
            if (!paramsLookup.containsKey(name)) {
                ScriptParameter parameter = new ScriptParameter();
                parameter.setName(name);
                defaultParams.add(parameter);
            }
        }
        // Удаляем отсутствующие параметры
        for (Map.Entry<String, ScriptParameter> pair : paramsLookup.entrySet()) {
            if (!paramsNames.contains(pair.getKey())) {
                defaultParams.remove(pair.getValue());
            }
        }
    }

    public Map<String, ScriptParameter> getDefaultParamsLookup() {
        Map<String, ScriptParameter> result = new HashMap<>();
        for (ScriptParameter p : defaultParams) {
            result.put(p.getName(), p);
        }
        return result;
    }

    /**
     * Возвращает URI результата выполнения скрипта с параметрами по умолчанию
     *
     * @return URI результата выполнения скрипта
     */
    public String getUri() {
        UriBuilder builder = UriBuilder.fromPath("/").path("{name}");
        for (ScriptParameter param : defaultParams) {
            if (param.isHttpParameter()) {
                builder = builder.queryParam(param.getName().substring(1), param.getValue());
            }
        }
        URI uri = builder.build(getResourceName());
        return uri.toString();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    @XmlAttribute
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        idProperty().set(id);
    }

    public StringProperty nameProperty() {
        return name;
    }

    @XmlAttribute
    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        nameProperty().set(name);
    }

    public StringProperty resourceNameProperty() {
        return resourceName;
    }

    @XmlAttribute
    public String getResourceName() {
        return resourceName.get();
    }

    public void setResourceName(String resourceName) {
        resourceNameProperty().set(resourceName);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    @XmlElement(name = "Description")
    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        descriptionProperty().set(description);
    }

    public StringProperty scriptBodyProperty() {
        return scriptBody;
    }

    @XmlElement(name = "ScriptBody")
    public String getScriptBody() {
        return scriptBody.get();
    }

    public void setScriptBody(String scriptBody) {
        scriptBodyProperty().set(scriptBody);
    }

    @XmlElement(name = "DataServiceResource")
    public ObservableList<DataServiceResource> getDataServiceResources() {
        return dataServiceResources;
    }

    @XmlElementWrapper(name = "DefaultParameters")
    @XmlElement(name = "Parameter")
    public ObservableList<ScriptParameter> getDefaultParams() {
        return defaultParams;
    }

    public IntegerProperty plotWidthProperty() {
        return plotWidth;
    }

    @XmlElement(name = "PlotWidth")
    public int getPlotWidth() {
        return plotWidth.get();
    }

    public void setPlotWidth(int plotWidth) {
        this.plotWidth.set(plotWidth);
    }

    public IntegerProperty plotHeightProperty() {
        return plotHeight;
    }

    @XmlElement(name = "PlotHeight")
    public int getPlotHeight() {
        return plotHeight.get();
    }

    public void setPlotHeight(int plotHeight) {
        this.plotHeight.set(plotHeight);
    }

    public BooleanProperty webAccessProperty() {
        return webAccess;
    }

    @XmlElement(name = "WebAccess")
    public boolean isWebAccess() {
        return webAccess.get();
    }

    public void setWebAccess(boolean webAccess) {
        this.webAccess.set(webAccess);
    }

    public BooleanProperty useDefaultTemplateProperty() {
        return useDefaultTemplate;
    }

    @XmlElement(name = "UseDefaultTemplate")
    public boolean isUseDefaultTemplate() {
        return useDefaultTemplate.get();
    }

    public void setUseDefaultTemplate(boolean isDefaultTemplate) {
        this.useDefaultTemplate.set(isDefaultTemplate);
    }

    public StringProperty templateProperty() {
        return template;
    }

    @XmlElement(name = "Template")
    public String getTemplate() {
        return template.get();
    }

    public void setTemplate(String template) {
        this.template.set(template);
    }

    @XmlElementWrapper(name = "SecurityRoles")
    @XmlElement(name = "Role")
    public ObservableList<String> getSecurityRoles() {
        return securityRoles;
    }

    public static ReadOnlyStringProperty defaultTemplateProperty() {
        if (defaultTemplate.get() == null) {
            InputStream stream = Script.class.getResourceAsStream("/template.ftl");
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream))) {
                defaultTemplate.set(buffer.lines().collect(Collectors.joining("\n")));
            } catch (IOException e) {
                log.error(e);
            }
        }
        return defaultTemplate;
    }

    public static String getDefaultTemplate() {
        return defaultTemplateProperty().get();
    }

    /**
     * Генерирует случайный идентификатор запроса к HTTP-ресурсу
     *
     * @param params Параметры запроса к HTTP-ресурсу
     * @return Случайный идентификатор запроса
     */
    public static String generateRequestId(List<ScriptParameter> params) {
        String result = null;
        try {
            if (prng == null) {
                prng = SecureRandom.getInstance("SHA1PRNG");
            }
            String randomStr = Integer.toString(prng.nextInt() * params.hashCode());
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            result = bytesToHex(sha.digest(randomStr.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        return result;
    }

    public static String prepareContentString(String content) {
        String result = content.replace("'", "\\'");
        result = result.replace("\\n", "\\\n");
        result = result.replace("\\b", "\\\b");
        result = result.replace("\\f", "\\\f");
        result = result.replace("\\r", "\\\r");
        result = result.replace("\\t", "\\\t");
        result = result.replace("\\0", "\\\0");
        result = result.replace("\\\\", "\\\\\\\\");
        result = result.replace(System.getProperty("line.separator"), "\\n");
        result = result.replace("\n", "\\n");
        result = result.replace("\r", "\\n");
        return result;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void setApplicationParams(Map<String, String> applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Script)) {
            return false;
        }
        Script that = (Script) o;
        return that.getId() == getId();
    }

    @Override
    public int hashCode() {
        return getId();
    }

}
