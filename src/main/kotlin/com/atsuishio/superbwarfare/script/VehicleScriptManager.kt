package com.atsuishio.superbwarfare.script

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.mojang.blaze3d.vertex.PoseStack
import org.mozillaa.javascript.Context
import org.mozillaa.javascript.Script
import org.mozillaa.javascript.Scriptable
import org.mozillaa.javascript.ScriptableObject

typealias JSFunction = org.mozillaa.javascript.Function

// TODO 测试用，以后不这样写
object VehicleScriptManager {
    val RHINO_CONTEXT: Context = Context.enter()
    val SHARED_SCOPE: ScriptableObject = RHINO_CONTEXT.initStandardObjects()

    class ScriptFunction(val script: Script, val scope: Scriptable)

    fun invokeTransform(
        scriptFunc: ScriptFunction,
        vehicle: Any,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        renderer: Any
    ) {
        val ctx = RHINO_CONTEXT
        val scope = scriptFunc.scope

        val func = scope.get("transformCustomModelPart", scope)
        if (func is JSFunction) {
            func.call(ctx, scope, scope, arrayOf(vehicle, model, poseStack, entityYaw, partialTicks, renderer))
        }
    }
}