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

package ru.datareducer.ui;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableMap;
import ru.datareducer.model.ReducerConfiguration;

/**
 * Следит, чтобы окна для одних и тех же объектов не открывались повторно.
 * Закрывает окна при удалении объектов или при полной замене модели.
 *
 * @param <E> Объект, которому принадлежит окно.
 * @param <W> Окно объекта.
 * @author Kirill Mikhaylov
 */
class WindowsManager<E, W extends Window<E>> implements ModelReplacedListener {
    private final ObservableMap<E, W> windows = FXCollections.observableHashMap();

    /**
     * Закрывает все окна при замене модели.
     *
     * @param model Новая модель.
     */
    @Override
    public void acceptModel(ReducerConfiguration model) {
        closeAllWindows();
    }

    /**
     * Закрывает все окна
     */
    public void closeAllWindows() {
        windows.forEach((o, w) -> w.close());
        windows.clear();
    }

    /**
     * Возвращает окно заданного объекта, если оно было создано.
     *
     * @param obj Объект.
     * @return Окно заданного объекта или null, если окно не было создано.
     */
    W getWindow(E obj) {
        return windows.get(obj);
    }

    /**
     * Открывает заданное окно.
     *
     * @param window Окно объекта.
     */
    void showWindow(W window) {
        if (window == null) {
            throw new IllegalArgumentException();
        }
        E obj = window.getEntity();
        if (windows.containsKey(obj)) {
            windows.get(obj).show();
            return;
        }
        windows.put(obj, window);
        window.show();
    }

    /**
     * Слушатель изменений состояния модели.
     * При удалении объекта закрывает его окно и удаляет ссылку на него.
     *
     * @param change Изменения состояния модели.
     */
    void onModelChanged(ListChangeListener.Change<?> change) {
        while (change.next()) {
            if (change.wasRemoved()) {
                E obj = (E) change.getRemoved().get(0);
                if (windows.containsKey(obj)) {
                    windows.get(obj).close();
                    windows.remove(obj);
                }
            }
        }
    }

    ObservableMap<E, W> windowsProperty() {
        return windows;
    }
}