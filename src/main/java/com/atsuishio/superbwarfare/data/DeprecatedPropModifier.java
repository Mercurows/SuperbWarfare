package com.atsuishio.superbwarfare.data;

import java.util.HashMap;
import java.util.Map;

@Deprecated
public class DeprecatedPropModifier<DATA extends DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA, FIELD> {
    private final Prop<DATA, DEFAULT_DATA, FIELD> prop;
    private final DATA data;

    private final Map<Prop<DATA, DEFAULT_DATA, ?>, Object> props = new HashMap<>();

    DeprecatedPropModifier(Prop<DATA, DEFAULT_DATA, FIELD> prop, DATA data) {
        this.prop = prop;
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public <F> F get(Prop<DATA, DEFAULT_DATA, F> prop) {
        return (F) DataLoader.processValue(props.computeIfAbsent(prop, aa -> aa.getDefault(data.getDefault())));
    }

    public FIELD compute(DEFAULT_DATA rawData) {
//        var result = get(prop);
//
//        if (limiter != null) {
//            result = limiter.apply(this, data, result);
//        }
//
//        return (FIELD) DataLoader.processValue(result);

        return prop.getDefault(rawData);
    }
}
