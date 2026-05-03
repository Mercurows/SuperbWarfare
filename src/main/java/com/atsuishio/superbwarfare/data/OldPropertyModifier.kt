package com.atsuishio.superbwarfare.data

interface OldPropertyModifier<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA> {
    fun computeProperties(data: DATA, rawData: DEFAULT_DATA): DEFAULT_DATA {
        return rawData
    }
}
