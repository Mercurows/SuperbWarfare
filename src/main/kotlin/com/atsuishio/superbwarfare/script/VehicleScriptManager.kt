package com.atsuishio.superbwarfare.script

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance
import com.mojang.blaze3d.vertex.PoseStack
import org.mozillaa.javascript.Context
import org.mozillaa.javascript.Script
import org.mozillaa.javascript.Scriptable
import org.mozillaa.javascript.ScriptableObject

typealias JSFunction = org.mozillaa.javascript.Function

object VehicleScriptManager {
    val SHARED_SCOPE: ScriptableObject = Context.enter().use { it.initStandardObjects() }

    class ScriptFunction(val script: Script, val scope: Scriptable)

    fun invokeTransform(
        scriptFunc: ScriptFunction,
        vehicle: Any,
        model: BakedModelInstance,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        renderer: Any
    ) {

        val scope = scriptFunc.scope

        val func = scope.get("transformCustomModelPart", scope)
        if (func is JSFunction) {
            Context.enter().use { cx ->
                func.call(cx, scope, scope, arrayOf(vehicle, model, poseStack, entityYaw, partialTicks, renderer))
            }
        }
    }
}
