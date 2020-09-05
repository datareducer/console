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
import java.util.*;

/**
 * Виртуальная таблица регистра бухгалтерии.
 *
 * @author Kirill Mikhaylov
 * @see AccountingRegisterBalance
 * @see AccountingRegisterTurnovers
 * @see AccountingRegisterBalanceAndTurnovers
 * @see AccountingRegisterExtDimensions
 * @see AccountingRegisterRecordsWithExtDimensions
 * @see AccountingRegisterDrCrTurnovers
 */
public interface AccountingRegisterVirtualTable extends DataServiceRequest {
    /**
     * Возвращает набор всех измерений, субконто и полей счетов виртуальной таблицы регистра бухгалтерии.
     *
     * @return Набор всех измерений, субконто и полей счетов виртуальной таблицы регистра бухгалтерии.
     */
    default LinkedHashSet<Field> getProperties() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает набор всех измерений виртуальной таблицы регистра бухгалтерии.
     *
     * @return Набор всех измерений виртуальной таблицы регистра бухгалтерии.
     */
    default LinkedHashSet<Field> getDimensions() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает набор всех субконто виртуальной таблицы регистра бухгалтерии.
     *
     * @return Набор всех субконто виртуальной таблицы регистра бухгалтерии.
     */
    default LinkedHashSet<Field> getExtDimensions() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает поле счета виртуальной таблицы регистра бухгалтерии.
     *
     * @return Поле счета виртуальной таблицы регистра бухгалтерии.
     */
    default Field getAccountField() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает поле корреспондирующего счета виртуальной таблицы регистра бухгалтерии.
     *
     * @return Поле корреспондирующего счета виртуальной таблицы регистра бухгалтерии.
     */
    default Field getBalancedAccountField() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает набор всех ресурсов виртуальной таблицы регистра бухгалтерии.
     *
     * @return Набор всех ресурсов виртуальной таблицы регистра бухгалтерии.
     */
    default LinkedHashSet<Field> getResources() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает набор измерений, в разрезе которых будут получены остатки или обороты.
     *
     * @return Набор измерений, в разрезе которых будут получены остатки или обороты.
     */
    default LinkedHashSet<Field> getRequestedDimensions() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает набор представлений полей.
     *
     * @return Набор представлений полей.
     */
    LinkedHashSet<Field> getPresentationFields();

    /**
     * Возвращает дату, на которую будут получены остатки регистра бухгалтерии.
     *
     * @return Дата, на которую будут получены остатки регистра бухгалтерии.
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

    /**
     * Возвращает условие отбора по счетам.
     *
     * @return Условие отбора по счетам.
     */
    default Condition getAccountCondition() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает условие отбора по корреспондирующим счетам.
     *
     * @return Условие отбора по корреспондирующим счетам.
     */
    default Condition getBalancedAccountCondition() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает список уникальных идентификаторов видов субконто.
     *
     * @return Список уникальных идентификаторов видов субконто.
     */
    default ArrayList<UUID> getExtraDimensions() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает список уникальных идентификаторов корреспондирующих видов субконто.
     *
     * @return Список уникальных идентификаторов корреспондирующих видов субконто.
     */
    default ArrayList<UUID> getBalancedExtraDimensions() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает набор всех полей виртуальной таблицы.
     *
     * @return Набор всех полей виртуальной таблицы.
     */
    default LinkedHashSet<Field> getVirtualTableFields() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает параметр ограничения максимального количества записей.
     * (для виртуальных таблиц движений с субконто регистров бухгалтерии).
     *
     * @return Параметр ограничения максимального количества записей.
     */
    default int getTop() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает список имён колонок, по которым будет выполняться сортировка проводок.
     * (для виртуальных таблиц движений с субконто регистров бухгалтерии).
     *
     * @return Список имён колонок, по которым будет выполняться сортировка проводок.
     */
    default List<String> getOrderBy() {
        throw new UnsupportedOperationException();
    }

    /**
     * Возвращает уникальные идентификаторы видов субконто через запятую.
     *
     * @return уникальные идентификаторы видов субконто через запятую.
     */
    default String getExtraDimensionsString() {
        StringBuilder sb = new StringBuilder();
        Iterator<UUID> it = getExtraDimensions().iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Возвращает уникальные идентификаторы корреспондирующих видов субконто через запятую.
     *
     * @return уникальные идентификаторы корреспондирующих видов субконто через запятую.
     */
    default String getBalancedExtraDimensionsString() {
        StringBuilder sb = new StringBuilder();
        Iterator<UUID> it = getBalancedExtraDimensions().iterator();
        while (it.hasNext()) {
            sb.append(it.next().toString());
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    @Override
    default boolean isVirtual() {
        return true;
    }

}
