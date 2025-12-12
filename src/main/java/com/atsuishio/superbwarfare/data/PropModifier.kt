package com.atsuishio.superbwarfare.data

class PropModifier<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA, RESULT> internal constructor(
    private val prop: Prop<DATA, DEFAULT_DATA, *, RESULT, *>,
    private val data: DATA
) {

    private val props =
        mutableMapOf<Prop<DATA, DEFAULT_DATA, *, *, *>, Any>()

    fun <F> get(prop: Prop<DATA, DEFAULT_DATA, *, F, *>): F {
        return props.getOrPut(prop) {
            prop.getDefault(data.getDefault()) as Any
        } as F
    }

    fun compute(rawData: DEFAULT_DATA): RESULT {
//        var result = get(prop);
//
//        if (limiter != null) {
//            result = limiter.apply(this, data, result);
//        }
//
//        return (FIELD) DataLoader.processValue(result);

        return prop.getDefault(rawData)
    }
}
