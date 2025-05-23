package com.atsuishio.superbwarfare.annotation;

import java.lang.annotation.*;

/**
 * 将指定字段标记为仅服务端启用，不会同步给客户端
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ServerOnly {
}
