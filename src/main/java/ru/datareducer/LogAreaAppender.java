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
package ru.datareducer;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Plugin(name = "LogAreaAppender", category = "Core", elementType = "appender", printObject = true)
public class LogAreaAppender extends AbstractAppender {
    private static TextArea logArea;

    private final Lock readLock = new ReentrantReadWriteLock().readLock();

    private LogAreaAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @PluginFactory
    public static LogAreaAppender createAppender(@PluginAttribute("name") String name,
                                                 @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
                                                 @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                 @PluginElement("Filters") Filter filter) {
        if (name == null) {
            LOGGER.error("Не указано имя для LogAreaAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new LogAreaAppender(name, filter, layout, ignoreExceptions, null);
    }

    public static void setLogArea(TextArea logArea) {
        LogAreaAppender.logArea = logArea;
    }

    @Override
    public void append(LogEvent event) {
        if (logArea == null) {
            LOGGER.error("Область вывода сообщений не назначена");
            return;
        }
        readLock.lock();
        try {
            final byte[] bytes = getLayout().toByteArray(event);
            // Добавление текста в TextArea должно выполняться в потоке JavaFX
            Platform.runLater(() -> logArea.appendText(new String(bytes)));
        } catch (Exception e) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException(e);
            }
        } finally {
            readLock.unlock();
        }
    }

}
