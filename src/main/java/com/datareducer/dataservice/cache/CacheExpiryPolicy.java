/*
 * Copyright (c) 2017-2020 Kirill Mikhaylov <admin@datareducer.ru>
 *
 * Этот файл — часть программы DataReducer <http://datareducer.ru>.
 *
 * Программа DataReducer является свободным программным обеспечением.
 * Вы вправе распространять ее и/или модифицировать только в соответствии с условиями
 * версии 2 Стандартной Общественной Лицензии GNU, опубликованной Free Software Foundation.
 *
 * Программа DataReducer распространяется в надежде, что она будет полезной,
 * но БЕЗО ВСЯКИХ ГАРАНТИЙ, в том числе ГАРАНТИИ ТОВАРНОГО СОСТОЯНИЯ ПРИ ПРОДАЖЕ
 * и ПРИГОДНОСТИ ДЛЯ ИСПОЛЬЗОВАНИЯ В КОНКРЕТНЫХ ЦЕЛЯХ.
 * Подробнее см. в Стандартной Общественной Лицензии GNU.
 *
 * Вы должны были получить копию Стандартной Общественной Лицензии GNU
 * вместе с этой программой. Если это не так, см. <https://www.gnu.org/licenses/>.
 */
package com.datareducer.dataservice.cache;

import com.datareducer.dataservice.entity.DataServiceRequest;
import com.datareducer.dataservice.entity.DataServiceResponse;
import org.ehcache.expiry.ExpiryPolicy;

import java.time.Duration;
import java.util.function.Supplier;

public class CacheExpiryPolicy implements ExpiryPolicy<DataServiceRequest, DataServiceResponse> {

    @Override
    public Duration getExpiryForCreation(DataServiceRequest dataServiceRequest, DataServiceResponse dataServiceResponse) {
        return dataServiceRequest.getCacheLifetime();
    }

    @Override
    public Duration getExpiryForAccess(DataServiceRequest dataServiceRequest, Supplier<? extends DataServiceResponse> supplier) {
        return null;
    }

    @Override
    public Duration getExpiryForUpdate(DataServiceRequest dataServiceRequest, Supplier<? extends DataServiceResponse> supplier,
                                       DataServiceResponse dataServiceResponse) {
        return dataServiceRequest.getCacheLifetime();
    }
}
