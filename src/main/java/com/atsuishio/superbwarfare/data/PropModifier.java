package com.atsuishio.superbwarfare.data;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PropModifier<DATA extends DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA, FIELD> {
    private final Prop<DATA, DEFAULT_DATA, FIELD> prop;
    private final DATA data;
    private final Prop.PropModifyContext<DATA, DEFAULT_DATA, FIELD> limiter;

    private final Map<Prop<DATA, DEFAULT_DATA, ?>, Object> props = new HashMap<>();

    PropModifier(Prop<DATA, DEFAULT_DATA, FIELD> prop, DATA data, @Nullable Prop.PropModifyContext<DATA, DEFAULT_DATA, FIELD> limiter) {
        this.prop = prop;
        this.data = data;
        this.limiter = limiter;
    }

    @SuppressWarnings("unchecked")
    public PropModifier<DATA, DEFAULT_DATA, FIELD> apply(@Nullable PropertyModifier<DATA, DEFAULT_DATA> modifier) {
        if (modifier == null) return this;

        var a = modifier.getPropModifiers();
        for (var entry : a.entrySet()) {
            var prop = (Prop<DATA, DEFAULT_DATA, FIELD>) entry.getKey();
            var func = (Prop.PropModifyContext<DATA, DEFAULT_DATA, FIELD>) entry.getValue();

            props.put(prop, func.apply(PropModifier.this, data, get(prop)));
        }

        return this;
    }

    @SuppressWarnings("unchecked")
    public void applyMap(@Nullable Map<Prop<DATA, DEFAULT_DATA, ?>, Prop.PropModifyContext<DATA, DEFAULT_DATA, ?>> modifier) {
        if (modifier == null) return;

        for (var entry : modifier.entrySet()) {
            var key = (Prop<DATA, DEFAULT_DATA, Object>) entry.getKey();
            var value = (Prop.PropModifyContext<DATA, DEFAULT_DATA, Object>) entry.getValue();
            props.put(entry.getKey(), value.apply((PropModifier<DATA, DEFAULT_DATA, Object>) this, data, get(key)));
        }
    }

    @SuppressWarnings("unchecked")
    public <F> F get(Prop<DATA, DEFAULT_DATA, F> prop) {
        return (F) DataLoader.processValue(props.computeIfAbsent(prop, aa -> aa.getDefault(data.getDefault())));
    }

    @SuppressWarnings("unchecked")
    public FIELD compute() {
        var result = get(prop);

        if (limiter != null) {
            result = limiter.apply(this, data, result);
        }

        return (FIELD) DataLoader.processValue(result);
    }
}
