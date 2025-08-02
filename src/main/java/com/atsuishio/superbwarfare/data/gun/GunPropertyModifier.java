package com.atsuishio.superbwarfare.data.gun;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;

public interface GunPropertyModifier {
    @NotNull
    Map<GunProp<?>, BiFunction<GunData, ?, ?>> getPropModifiers();

    @Nullable
    @SuppressWarnings("unchecked")
    default <T> BiFunction<GunData, T, T> getModifier(GunProp<T> prop) {
        return (BiFunction<GunData, T, T>) getPropModifiers().get(prop);
    }

    /**
     * 直接修改某个属性的值
     */
    default <T> void modifyProperty(GunProp<T> prop, BiFunction<GunData, T, T> modifier) {
        getPropModifiers().put(prop, modifier);
    }

    /**
     * 在先前修改的基础上继续修改某个属性的值
     */
    @SuppressWarnings("unchecked")
    default <T> void appendModification(GunProp<T> prop, @Nullable BiFunction<GunData, T, T> modifier) {
        if (modifier == null) return;

        var modifiers = getPropModifiers();
        var current = (BiFunction<GunData, T, T>) modifiers.get(prop);

        if (current == null) {
            modifiers.put(prop, modifier);
        } else {
            modifiers.put(prop, (data, v) -> {
                var value = current.apply(data, (T) v);
                return modifier.apply(data, value);
            });
        }
    }
}
