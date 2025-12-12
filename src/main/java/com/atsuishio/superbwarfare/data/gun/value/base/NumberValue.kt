package com.atsuishio.superbwarfare.data.gun.value.base

interface NumberValue<T : Number> : TagValue<T> {
    fun add(value: T)
}