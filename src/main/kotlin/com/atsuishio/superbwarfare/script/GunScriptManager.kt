package com.atsuishio.superbwarfare.script

import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import net.minecraft.world.item.ItemStack
import org.mozillaa.javascript.Context
import org.mozillaa.javascript.Script
import org.mozillaa.javascript.Scriptable
import org.mozillaa.javascript.ScriptableObject

object GunScriptManager {
    val SHARED_SCOPE: ScriptableObject = Context.enter().use { it.initStandardObjects() }

    class ScriptFunction(val script: Script, val scope: Scriptable)

    fun invokeTransform(
        scriptFunc: ScriptFunction,
        stack: ItemStack,
        model: GeoGunModel,
        transformType: ScriptTransform,
        partialTick: Float,
        renderer: Any
    ) {
        val func = scriptFunc.scope.get("transformCustomModelPart", scriptFunc.scope)
        if (func is JSFunction) {
            Context.enter().use { cx ->
                func.call(cx, scriptFunc.scope, scriptFunc.scope, arrayOf(stack, model, transformType, partialTick, renderer))
            }
        }
    }
}
