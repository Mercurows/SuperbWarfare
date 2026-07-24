package com.atsuishio.superbwarfare.script

import org.mozillaa.javascript.Context
import org.mozillaa.javascript.Function
import org.mozillaa.javascript.Script
import org.mozillaa.javascript.ScriptableObject

/**
 * Rhino JS 脚本管理器。
 *
 * Rhino 的 Context 是与线程绑定的（通过 ThreadLocal 实现），一个 Context 只能在其 enter 时所在的
 * 线程上使用。因此，本管理器不缓存 Context，而是在每次编译和执行时通过 [Context.enter] /
 * [Context.exit] 在当前线程上创建和释放 Context。
 *
 * [Script] 和 [ScriptableObject] 是不绑定 Context 的 Java 对象，可以在不同 Context 之间安全复用，
 * 所以 [CustomScript] 只持有编译后的脚本和作用域，每次执行时动态 enter 一个 Context。
 */
object ScriptManager {

    /**
     * 资源重载在 ForkJoinPool worker 线程上并行执行，这些线程的 contextClassLoader 默认是
     * JVM 的系统/平台类加载器，而不是加载本模组（及其 jarJar 的 Rhino）的 TRANSFORMER 层
     * 模块类加载器。Rhino 的 [Context] 静态初始化通过 ServiceLoader（依赖 contextClassLoader）
     * 查找 RegExpLoader 实现，加载器不一致会导致 NoClassDefFoundError /
     * "not a subtype" 报错。这里在调用 Rhino 前临时切换成本类的类加载器，用后恢复。
     */
    private inline fun <T> withCorrectClassLoader(block: () -> T): T {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = ScriptManager::class.java.classLoader
        try {
            return block()
        } finally {
            thread.contextClassLoader = previous
        }
    }

    @JvmRecord
    data class CustomScript(
        val name: String,
        val scope: ScriptableObject,
        val script: Script
    ) {
        /**
         * 执行脚本，在调用线程上自动管理 Context 生命周期。
         */
        fun exec(): Any = ScriptManager.withCorrectClassLoader {
            val cx = Context.enter()
            try {
                script.exec(cx, scope, scope)
            } finally {
                Context.exit()
            }
        }

        /**
         * 向脚本作用域注入属性。
         */
        fun putProperty(name: String, value: Any) {
            ScriptableObject.putProperty(scope, name, value)
        }

        /**
         * 向脚本作用域注入常量。
         */
        fun putConstant(name: String, value: Any) {
            ScriptableObject.putConstProperty(scope, name, value)
        }

        /**
         * 调用作用域中指定名称的 JS 函数，在调用线程上自动管理 Context 生命周期。
         *
         * 若函数不存在或返回值非 Number，返回 null。
         */
        fun callFunction(functionName: String, vararg args: Any): Any? = ScriptManager.withCorrectClassLoader {
            val cx = Context.enter()
            try {
                val func = scope.get(functionName, scope) as? Function ?: return@withCorrectClassLoader null
                func.call(cx, scope, scope, args.toList().toTypedArray())
            } finally {
                Context.exit()
            }
        }
    }

    /**
     * 创建安全脚本（不允许 Java 互操作）。
     */
    fun createSafeScript(name: String, source: String): CustomScript? = withCorrectClassLoader {
        val cx = Context.enter()
        try {
            if (!cx.stringIsCompilableUnit(source)) return@withCorrectClassLoader null
            val scope = cx.initSafeStandardObjects()
            val script = cx.compileString(source, name, 1, null)
            CustomScript(name, scope, script)
        } finally {
            Context.exit()
        }
    }

    /**
     * 创建标准脚本（允许 Java 互操作）。
     */
    fun createScript(name: String, source: String): CustomScript? = withCorrectClassLoader {
        val cx = Context.enter()
        try {
            if (!cx.stringIsCompilableUnit(source)) return@withCorrectClassLoader null
            val scope = cx.initStandardObjects()
            val script = cx.compileString(source, name, 1, null)
            CustomScript(name, scope, script)
        } finally {
            Context.exit()
        }
    }
}
