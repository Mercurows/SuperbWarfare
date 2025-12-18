package com.atsuishio.superbwarfare.data;

public interface PropertyModifier<DATA extends DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA> {
    default DEFAULT_DATA computeProperties(DATA data, DEFAULT_DATA rawData) {
        return rawData;
    }
}
