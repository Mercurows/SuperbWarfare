package com.atsuishio.superbwarfare.data;

// TODO 取代原PropModifier
public interface NewPropModifier<DATA extends DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA> {
    DEFAULT_DATA compute(DATA data, DEFAULT_DATA rawData);
}
