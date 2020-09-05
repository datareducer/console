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

package ru.datareducer.dataservice.client;

/**
 * Настройки подключения к REST-сервису 1С
 *
 * @author Kirill Mikhaylov
 */
public final class ConnectionParams {
    private final String host;
    private final String base;
    private final String user;
    private final String password;

    /**
     * Создаёт настройки подключения к REST-сервису 1С
     *
     * @param host     Адрес сервера 1С
     * @param base     Имя информационной базы
     * @param user     Имя пользователя REST-сервиса
     * @param password Пароль пользователя REST-сервиса
     */
    public ConnectionParams(String host, String base, String user, String password) {
        if (host == null || base == null || user == null || password == null
                || host.isEmpty() || base.isEmpty() || user.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("host=" + host + ", base=" + base + ", user=" + user + ", password=" + password);
        }
        this.host = host;
        this.base = base;
        this.user = user;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public String getBase() {
        return base;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
