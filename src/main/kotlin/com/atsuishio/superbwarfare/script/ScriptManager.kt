package com.atsuishio.superbwarfare.script

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.mozillaa.javascript.Context
import org.mozillaa.javascript.Script
import org.mozillaa.javascript.ScriptableObject

object ScriptManager {
    private val CONTEXT: LoadingCache<Thread, Context> = CacheBuilder.newBuilder()
        .weakKeys()
        .build(
            object : CacheLoader<Thread, Context>() {
                override fun load(key: Thread): Context {
                    return Context.enter()
                }
            }
        )

    @JvmStatic
    fun getContext(): Context = CONTEXT.getUnchecked(Thread.currentThread())

    @JvmRecord
    data class CustomScript(val name: String, val context: Context, val scope: ScriptableObject, val script: Script) {
        fun exec(): Any {
            return script.exec(context, scope, scope)
        }

        fun putProperty(name: String, value: Any) {
            ScriptableObject.putProperty(scope, name, value)
        }

        fun putConstant(name: String, value: Any) {
            ScriptableObject.putConstProperty(scope, name, value)
        }
    }

    fun createSafeScript(name: String, source: String): CustomScript? {
        val ctx = getContext()
        if (!ctx.stringIsCompilableUnit(source)) return null
        val scope = ctx.initSafeStandardObjects()
        val script = ctx.compileString(source, name, 1, null)
        return CustomScript(name, ctx, scope, script)
    }

    fun createScript(name: String, source: String): CustomScript? {
        val ctx = getContext()
        if (!ctx.stringIsCompilableUnit(source)) return null
        val scope = ctx.initStandardObjects()
        val script = ctx.compileString(source, name, 1, null)
        return CustomScript(name, ctx, scope, script)
    }
}