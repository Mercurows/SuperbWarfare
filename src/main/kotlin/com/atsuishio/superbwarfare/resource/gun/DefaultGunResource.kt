package com.atsuishio.superbwarfare.resource.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.data.ModColor
import com.atsuishio.superbwarfare.data.ObjectToList
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.resource.ModelResource
import com.atsuishio.superbwarfare.resource.gun.pojo.*
import com.atsuishio.superbwarfare.script.GunScriptManager
import com.atsuishio.superbwarfare.script.ScriptMath
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import com.atsuishio.superbwarfare.tools.mc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.MarkerManager
import org.joml.Vector3f
import org.mozillaa.javascript.ScriptableObject

@Serializable
class DefaultGunResource : IDBasedData<DefaultGunResource> {
    @Transient
    @kotlin.jvm.Transient
    private var id = ""

    override fun getId(): String {
        return this.id
    }

    override fun setId(id: String) {
        this.id = id
    }

    @JvmField
    @SerialName("SlotIcon")
    var slotIcon: String = ""

    @JvmField
    @SerialName("ItemDisplay")
    var itemDisplay: MutableMap<String, ItemDisplayInfo> = hashMapOf()

    @JvmField
    @SerialName("Model")
    var modelValue: ModelResource? = ModelResource()

    fun getModel(): ModelResource {
        return if (modelValue == null) ModelResource() else modelValue!!
    }

    @SerialName("Script")
    private val script: SerializedResourceLocation? = null

    @Transient
    @kotlin.jvm.Transient
    private var scriptCache: GunScriptManager.ScriptFunction? = null

    fun getScript(): GunScriptManager.ScriptFunction? {
        if (scriptCache != null) return scriptCache!!
        val script = this.script ?: return null
        return try {
            val resource = mc.resourceManager.getResource(script)
            if (resource.isEmpty) return null

            val source = resource.get().openAsReader().use { it.readText().trim() }
            if (!GunScriptManager.RHINO_CONTEXT.stringIsCompilableUnit(source)) {
                Mod.LOGGER.error(MARKER, "Failed to compile gun script: $source")
                return null
            }

            val compiled = GunScriptManager.RHINO_CONTEXT.compileString(source, script.toString(), 1, null)
            val scope = GunScriptManager.RHINO_CONTEXT.newObject(GunScriptManager.SHARED_SCOPE)
            scope.parentScope = GunScriptManager.SHARED_SCOPE

            ScriptableObject.putProperty(scope, "JsMath", ScriptMath)
            compiled.exec(GunScriptManager.RHINO_CONTEXT, scope, scope)

            val func = GunScriptManager.ScriptFunction(compiled, scope)
            scriptCache = func
            func
        } catch (e: Exception) {
            Mod.LOGGER.error(MARKER, "Failed to load gun script: $script", e)
            null
        }
    }

    @JvmField
    @SerialName("Animation")
    var animation: GunAnimation? = GunAnimation()

    @JvmField
    @SerialName("DrumLevels")
    var drumLevels: ObjectToList<Int> = ObjectToList()

    @JvmField
    @SerialName("UseOldHandRenderer")
    var useOldHandRenderer: Boolean = false

    @JvmField
    @SerialName("FlarePosition")
    var flarePosition: SerializedVec3? = null

    @JvmField
    @SerialName("FlareSize")
    var flareSize: Float = 1f

    @JvmField
    @SerialName("Smoke")
    var smoke: SmokeInfo = SmokeInfo()

    @JvmField
    @SerialName("HasSmoke")
    var hasSmoke: Boolean = false

    @JvmField
    @SerialName("HideCrosshairWhenZoom")
    var hideCrosshairWhenZoom: Boolean = true

    @JvmField
    @SerialName("EnergyBarColor")
    var energyBarColor: ModColor = ModColor(0x95E9FF)

    @JvmField
    @SerialName("TriggerSound")
    var triggerSound: SerializedSoundEvent? = ModSounds.TRIGGER_CLICK.get()

    @JvmField
    @SerialName("DischargeSound")
    var dischargeSound: SerializedSoundEvent? = null

    @JvmField
    @SerialName("EjectShell")
    var ejectShell: Boolean = false

    @JvmField
    @SerialName("ShellEject")
    var shellEject: ShellEjectInfo? = null

    @JvmField
    @SerialName("CanZoom")
    var canZoom: Boolean = true

    @JvmField
    @SerialName("SprintOffset")
    var sprintOffset: SerializedVector3f = Vector3f(0f, 0f, 0f)

    @JvmField
    @SerialName("ShootRecoil")
    var shootRecoil: ShootRecoilInfo = ShootRecoilInfo()

    // 移动时是否出现倾斜动画
    @JvmField
    @SerialName("MovingTilt")
    var movingTilt: Boolean = true

    @JvmField
    @SerialName("Attachments")
    var attachmentInfo: AttachmentInfo = AttachmentInfo()

    companion object {
        private val MARKER: Marker = MarkerManager.getMarker("GunResource")
    }
}
