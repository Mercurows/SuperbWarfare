package com.atsuishio.superbwarfare.data.gun;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface GunPropertyModifier {
    @NotNull
    Map<GunProp<?>, GunProp.GunPropModifyContext<?>> getPropModifiers();

    @Nullable
    @SuppressWarnings("unchecked")
    default <T> GunProp.GunPropModifyContext<T> getModifier(GunProp<T> prop) {
        return (GunProp.GunPropModifyContext<T>) getPropModifiers().get(prop);
    }

    /**
     * 直接修改某个属性的值
     */
    default <T> void modifyProperty(GunProp<T> prop, @Nullable Function<T, T> modifier) {
        if (modifier == null) return;
        modifyProperty(prop, (data, value) -> modifier.apply(value));
    }

    /**
     * 直接修改某个属性的值
     */
    default <T> void modifyProperty(GunProp<T> prop, @Nullable BiFunction<GunData, T, T> modifier) {
        if (modifier == null) return;
        modifyProperty(prop, (data, value, target, source) -> modifier.apply(data, value));
    }

    /**
     * 直接修改某个属性的值
     */
    @SuppressWarnings("unchecked")
    default <T> void modifyProperty(GunProp<T> prop, @Nullable GunProp.GunPropModifyContext<T> modifier) {
        if (modifier == null) return;
        getPropModifiers().put(prop, (data, value, target, source) -> modifier.apply(data, (T) value, target, source));
    }


    /**
     * 在先前修改的基础上继续修改某个属性的值
     */
    default <T> void appendModification(GunProp<T> prop, @Nullable Function<T, T> modifier) {
        if (modifier == null) return;
        appendModification(prop, (data, value) -> modifier.apply(value));
    }

    default <T> void appendModification(GunProp<T> prop, @Nullable BiFunction<GunData, T, T> modifier) {
        if (modifier == null) return;
        appendModification(prop, (data, value, target, source) -> modifier.apply(data, value));
    }

    /**
     * 在先前修改的基础上继续修改某个属性的值
     */
    @SuppressWarnings("unchecked")
    default <T> void appendModification(GunProp<T> prop, @Nullable GunProp.GunPropModifyContext<T> modifier) {
        if (modifier == null) return;

        var modifiers = getPropModifiers();
        var current = (GunProp.GunPropModifyContext<T>) modifiers.get(prop);

        if (current == null) {
            modifiers.put(prop, modifier);
        } else {
            modifiers.put(prop, (data, v, target, source) -> {
                var value = current.apply(data, (T) v, target, source);
                return modifier.apply(data, value, target, source);
            });
        }
    }
}
