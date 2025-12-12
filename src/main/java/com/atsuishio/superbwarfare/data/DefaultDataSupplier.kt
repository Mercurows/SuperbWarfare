package com.atsuishio.superbwarfare.data

interface DefaultDataSupplier<T> {
    fun getDefault(): T
}
