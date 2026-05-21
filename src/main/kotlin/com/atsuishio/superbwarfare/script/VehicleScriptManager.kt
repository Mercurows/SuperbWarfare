package com.atsuishio.superbwarfare.script

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.resources.ResourceLocation
import org.mozillaa.javascript.*

typealias JSFunction = org.mozillaa.javascript.Function

// TODO 测试用，以后不这样写
object VehicleScriptManager {
    val cache: MutableMap<String, ScriptFunction> = hashMapOf()

    class ScriptFunction(val script: Script, val scope: Scriptable)

    private var rhinoContext: Context = Context.enter()
    private var sharedScope: ScriptableObject = rhinoContext.initSafeStandardObjects()

    fun loadScript(vehicleId: String): ScriptFunction? {
        if (cache.containsKey(vehicleId)) {
            return cache[vehicleId]
        }

        val resourceLocation = ResourceLocation("superbwarfare", "scripts/vehicles/$vehicleId.js")
        return try {
            val resource = mc.resourceManager.getResource(resourceLocation)
            if (resource.isEmpty) return null

            val source = resource.get().openAsReader().use { it.readText().trim() }
            if (!rhinoContext.stringIsCompilableUnit(source)) {
                println("[VehicleScriptManager] Invalid script syntax: $resourceLocation")
                return null
            }

            val script = rhinoContext.compileString(source, resourceLocation.toString(), 1, null)

            val scope = rhinoContext.newObject(sharedScope)
            scope.parentScope = sharedScope

            ScriptableObject.putProperty(scope, "Mth", NativeJavaClass(scope, net.minecraft.util.Mth::class.java))
            ScriptableObject.putProperty(scope, "Quaterniond", NativeJavaClass(scope, org.joml.Quaterniond::class.java))
            ScriptableObject.putProperty(scope, "Quaternionf", NativeJavaClass(scope, org.joml.Quaternionf::class.java))
            ScriptableObject.putProperty(scope, "Axis", NativeJavaClass(scope, com.mojang.math.Axis::class.java))

            script.exec(rhinoContext, scope, scope)

            val func = ScriptFunction(script, scope)
            cache[vehicleId] = func
            func
        } catch (e: Exception) {
            println("[VehicleScriptManager] Failed to load script $resourceLocation: ${e.message}")
            null
        }
    }

    fun invokeTransform(
        scriptFunc: ScriptFunction,
        vehicle: Any,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        renderer: Any
    ) {
        val ctx = rhinoContext
        val scope = scriptFunc.scope

        val func = scope.get("transformCustomModelPart", scope)
        if (func is JSFunction) {
            func.call(ctx, scope, scope, arrayOf(vehicle, model, poseStack, entityYaw, partialTicks, renderer))
        }
    }
}