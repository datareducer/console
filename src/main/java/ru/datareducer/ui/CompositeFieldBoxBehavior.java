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

import com.sun.javafx.scene.control.behavior.ComboBoxBaseBehavior;
import com.sun.javafx.scene.control.behavior.KeyBinding;

import java.util.ArrayList;
import java.util.List;

public class CompositeFieldBoxBehavior extends ComboBoxBaseBehavior<String> {
    private static final List<KeyBinding> COMPOSITE_FIELD_BOX_BINDINGS = new ArrayList<>();

    static {
        COMPOSITE_FIELD_BOX_BINDINGS.addAll(COMBO_BOX_BASE_BINDINGS);
    }

    public CompositeFieldBoxBehavior(CompositeFieldBox compositeFieldBox) {
        super(compositeFieldBox, COMPOSITE_FIELD_BOX_BINDINGS);
    }
}
