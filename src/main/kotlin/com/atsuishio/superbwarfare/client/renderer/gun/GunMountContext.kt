package com.atsuishio.superbwarfare.client.renderer.gun

object GunMountContext {
    private var depth = 0

    val active: Boolean
        get() = depth > 0

    fun enter() {
        depth++
    }

    fun exit() {
        depth--
    }
}
