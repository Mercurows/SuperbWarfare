package com.atsuishio.superbwarfare.init

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegistryName(val value: String)