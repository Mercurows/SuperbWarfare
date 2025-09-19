package com.atsuishio.superbwarfare.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface PropertyModifier<DATA extends DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA> {
    @NotNull
    <P extends Prop<DATA, DEFAULT_DATA, ?>> Map<P, Prop.PropModifyContext<DATA, DEFAULT_DATA, ?>> getPropModifiers();

    @Nullable
    @SuppressWarnings("unchecked")
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> Prop.PropModifyContext<DATA, DEFAULT_DATA, T> getModifier(P prop) {
        return (Prop.PropModifyContext<DATA, DEFAULT_DATA, T>) getPropModifiers().get(prop);
    }

    /**
     * 直接修改某个属性的值
     */
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> void setProperty(P prop, @Nullable Function<T, T> modifier) {
        if (modifier == null) return;
        setProperty(prop, (pm, data, value) -> modifier.apply(value));
    }

    /**
     * 直接修改某个属性的值
     */
    @SuppressWarnings("unchecked")
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> void setProperty(P prop, @Nullable BiFunction<DATA, T, T> modifier) {
        if (modifier == null) return;
        getPropModifiers().put(prop, (pm, data, value) -> modifier.apply(data, (T) value));
    }

    /**
     * 直接修改某个属性的值
     */
    @SuppressWarnings("unchecked")
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> void setProperty(P prop, @Nullable Prop.PropModifyContext<DATA, DEFAULT_DATA, T> modifier) {
        if (modifier == null) return;
        getPropModifiers().put(prop, (pm, data, value) -> modifier.apply((PropModifier<DATA, DEFAULT_DATA, T>) pm, data, (T) value));
    }


    /**
     * 在先前修改的基础上继续修改某个属性的值
     */
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> void appendModification(P prop, @Nullable Function<T, T> modifier) {
        if (modifier == null) return;
        appendModification(prop, (pm, data, value) -> modifier.apply(value));
    }

    /**
     * 在先前修改的基础上继续修改某个属性的值
     */
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> void appendModification(P prop, @Nullable BiFunction<DATA, T, T> modifier) {
        if (modifier == null) return;
        appendModification(prop, (pm, data, value) -> modifier.apply(data, value));
    }

    /**
     * 在先前修改的基础上继续修改某个属性的值
     */
    @SuppressWarnings("unchecked")
    default <T, P extends Prop<DATA, DEFAULT_DATA, T>> void appendModification(P prop, @Nullable Prop.PropModifyContext<DATA, DEFAULT_DATA, T> modifier) {
        if (modifier == null) return;

        var modifiers = getPropModifiers();
        var current = (Prop.PropModifyContext<DATA, DEFAULT_DATA, T>) modifiers.get(prop);

        if (current == null) {
            setProperty(prop, modifier);
        } else {
            modifiers.put(prop, (pm, data, v) -> {
                var value = current.apply((PropModifier<DATA, DEFAULT_DATA, T>) pm, data, (T) v);
                return modifier.apply((PropModifier<DATA, DEFAULT_DATA, T>) pm, data, value);
            });
        }
    }
}
