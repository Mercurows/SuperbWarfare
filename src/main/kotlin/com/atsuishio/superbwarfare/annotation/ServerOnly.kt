package com.atsuishio.superbwarfare.annotation;

/**
 * 将指定字段标记为仅服务端启用，不会同步给客户端
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ServerOnly
