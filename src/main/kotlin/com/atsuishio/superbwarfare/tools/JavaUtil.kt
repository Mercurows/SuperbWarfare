package com.atsuishio.superbwarfare.tools

import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

operator fun <P, R> Function<P, R>.invoke(p: P): R = apply(p)
operator fun <P1, P2, R> BiFunction<P1, P2, R>.invoke(p1: P1, p2: P2): R = apply(p1, p2)

operator fun <C> Consumer<C>.invoke(c: C) = accept(c)
operator fun Runnable.invoke() = run()
operator fun <T> Supplier<T>.invoke() = get()
