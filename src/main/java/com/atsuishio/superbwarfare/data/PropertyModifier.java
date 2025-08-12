package com.atsuishio.superbwarfare.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public interface PropertyModifier<DATA extends DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA> {
    @NotNull
    <P extends Prop<DATA, DEFAULT_DATA, ?>> Map<P, Prop.PropModifyContext<DATA, ?>> getPropModifiers();

    @Nullable
    @SuppressWarnings("unchecked")
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> Prop.PropModifyContext<DATA, T> getModifier(P prop) {
        return (Prop.PropModifyContext<DATA, T>) getPropModifiers().get(prop);
    }

    /**
     * 直接修改某个属性的值
     */
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> void setProperty(P prop, @Nullable Function<T, T> modifier) {
        if (modifier == null) return;
        setProperty(prop, (data, value) -> modifier.apply(value));
    }

    /**
     * 直接修改某个属性的值
     */
    @SuppressWarnings("unchecked")
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> void setProperty(P prop, @Nullable Prop.PropModifyContext<DATA, T> modifier) {
        if (modifier == null) return;
        getPropModifiers().put(prop, (data, value) -> modifier.apply(data, (T) value));
    }


    /**
     * 在先前修改的基础上继续修改某个属性的值
     */
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> void appendModification(P prop, @Nullable Function<T, T> modifier) {
        if (modifier == null) return;
        appendModification(prop, (data, value) -> modifier.apply(value));
    }

    /**
     * 在先前修改的基础上继续修改某个属性的值
     */
    @SuppressWarnings("unchecked")
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> void appendModification(P prop, @Nullable Prop.PropModifyContext<DATA, T> modifier) {
        if (modifier == null) return;

        var modifiers = getPropModifiers();
        var current = (Prop.PropModifyContext<DATA, T>) modifiers.get(prop);

        if (current == null) {
            setProperty(prop, modifier);
        } else {
            modifiers.put(prop, (data, v) -> {
                var value = current.apply(data, (T) v);
                return modifier.apply(data, value);
            });
        }
    }
}
