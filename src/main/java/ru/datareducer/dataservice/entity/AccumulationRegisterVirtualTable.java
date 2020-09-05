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

import java.time.Instant;
import java.util.LinkedHashSet;

/**
 * Виртуальная таблица регистра накопления (Остатков, Оборотов или Остатков и оборотов)
 *
 * @author Kirill Mikhaylov
 * @see AccumulationRegisterBalance
 * @see AccumulationRegisterTurnovers
 * @see AccumulationRegisterBalanceAndTurnovers
 */
public interface AccumulationRegisterVirtualTable extends DataServiceRequest {
    /**
     * Возвращает набор всех измерений виртуальной таблицы.
     *
     * @return Набор всех измерений виртуальной таблицы.
     */
    LinkedHashSet<Field> getDimensions();

    /**
     * Возвращает набор всех ресурсов виртуальной таблицы.
     *
     * @return Набор всех ресурсов виртуальной таблицы.
     */
    LinkedHashSet<Field> getResources();

    /**
     * Возвращает набор измерений, в разрезе которых будут получены остатки или обороты.
     *
     * @return Набор измерений, в разрезе которых будут получены остатки или обороты.
     */
    LinkedHashSet<Field> getRequestedDimensions();

    /**
     * Возвращает набор представлений полей.
     *
     * @return Набор представлений полей.
     */
    LinkedHashSet<Field> getPresentationFields();

    /**
     * Возвращает дату, на которую будут получены остатки регистра накопления.
     *
     * @return Дата, на которую будут получены остатки регистра накопления.
     */
    default Instant getPeriod() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает начало периода расчета итогов (для виртуальных таблиц Оборотов и Остатков и оборотов)
     *
     * @return Начало периода расчета итогов.
     */
    default Instant getStartPeriod() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает конец периода расчета итогов (для виртуальных таблиц Оборотов и Остатков и оборотов)
     *
     * @return Конец периода расчета итогов.
     */
    default Instant getEndPeriod() {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isVirtual() {
        return true;
    }
}
