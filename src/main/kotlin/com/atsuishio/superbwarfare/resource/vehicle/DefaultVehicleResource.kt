package com.atsuishio.superbwarfare.resource.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.data.ObjectToList
import com.atsuishio.superbwarfare.resource.ModelResource
import com.atsuishio.superbwarfare.script.ScriptMath
import com.atsuishio.superbwarfare.script.VehicleScriptManager
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.tools.mc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.MarkerManager
import org.mozillaa.javascript.NativeJavaClass
import org.mozillaa.javascript.ScriptableObject

@Serializable
class DefaultVehicleResource : IDBasedData<DefaultVehicleResource> {
    @Transient
    @kotlin.jvm.Transient
    private var id = ""

    override fun getId(): String {
        return this.id
    }

    override fun setId(id: String) {
        this.id = id
    }

    @Deprecated("Use models instead", ReplaceWith("getModels()"))
    @SerialName("Model")
    private val model: ModelResource? = null

    @Deprecated("Use models instead", ReplaceWith("getModels()"))
    fun getModel() = this.model ?: ModelResource()

    @Deprecated("Use models instead", ReplaceWith("getModels()"))
    @SerialName("LODDistance")
    var lodDistance: ObjectToList<Double> = ObjectToList(48.0, 96.0)

    @SerialName("Animation")
    val animation: SerializedResourceLocation? = null

    @SerialName("Models")
    private val models: ObjectToList<VehicleModelPojo> = ObjectToList()

    @SerialName("Script")
    private val script: SerializedResourceLocation? = null

    @Transient
    @kotlin.jvm.Transient
    private var scriptCache: VehicleScriptManager.ScriptFunction? = null

    fun getModels(): List<VehicleModelPojo> {
        val list = models.list
        list.sortBy { it.distance }
        return list
    }

    fun getScript(): VehicleScriptManager.ScriptFunction? {
        if (this.scriptCache != null) return this.scriptCache!!
        if (this.script == null) return null
        return try {
            val resource = mc.resourceManager.getResource(script)
            if (resource.isEmpty) return null

            val source = resource.get().openAsReader().use { it.readText().trim() }
            if (!VehicleScriptManager.RHINO_CONTEXT.stringIsCompilableUnit(source)) {
                Mod.LOGGER.error(MARKER, "Failed to compile script: $source")
                return null
            }

            val script = VehicleScriptManager.RHINO_CONTEXT.compileString(source, script.toString(), 1, null)

            val scope = VehicleScriptManager.RHINO_CONTEXT.newObject(VehicleScriptManager.SHARED_SCOPE)
            scope.parentScope = VehicleScriptManager.SHARED_SCOPE

            ScriptableObject.putProperty(scope, "JsMath", ScriptMath)
            ScriptableObject.putProperty(scope, "Quaterniond", NativeJavaClass(scope, org.joml.Quaterniond::class.java))
            ScriptableObject.putProperty(scope, "Quaternionf", NativeJavaClass(scope, org.joml.Quaternionf::class.java))

            script.exec(VehicleScriptManager.RHINO_CONTEXT, scope, scope)

            val func = VehicleScriptManager.ScriptFunction(script, scope)
            scriptCache = func
            func
        } catch (e: Exception) {
            Mod.LOGGER.error(MARKER, "Failed to load script: $script", e)
            null
        }
    }

    companion object {
        private val MARKER: Marker = MarkerManager.getMarker("VehicleResource")
    }
}
