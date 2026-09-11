package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.*
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.perk.js.PmcProxy
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedGsonObject
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation

@Serializable
data class AttachmentDefinition(
    @SerialName("Slot")
    val slot: AttachmentType = AttachmentType.SCOPE,

    @SerialName("Level")
    val level: Int = 0,

    @SerialName("Bone")
    val bone: String? = null,

    // 安装该瞄准镜时是否需要导轨桥架；部分专用瞄准镜通过燕尾槽直装在枪身上
    @SerialName("RequiresRail")
    val requiresRail: Boolean = true,

    @SerialName("UsesGunStock")
    val usesGunStock: Boolean = false,

    // 安装该枪托时是否需要适配器；部分枪托（如泽宁特 PT-1）可直接安装在枪身上
    @SerialName("RequiresAdapter")
    val requiresAdapter: Boolean = true,

    @SerialName("Icon")
    val icon: String? = null,

    @SerialName("Model")
    val model: SerializedResourceLocation? = null,

    @SerialName("Texture")
    val texture: SerializedResourceLocation? = null,

    @SerialName("MuzzleFlashScale")
    val muzzleFlashScale: Float = 1.0f,

    @SerialName("SoundRadiusMultiplier")
    val soundRadiusMultiplier: Double = 1.0,

    @SerialName("IsSilenced")
    val isSilenced: Boolean = false,

    @SerialName("Modifiers")
    val modifiers: List<AttachmentModifier> = emptyList(),

    @SerialName("Override")
    val override: SerializedGsonObject? = null,

    @SerialName("Zoom")
    val zoom: AttachmentZoom? = null,

    @SerialName("ScopeInfo")
    val scopeInfo: ScopeInfo? = null,
) : IDBasedData<AttachmentDefinition>, PropertyModifier<GunData, DefaultGunData> {

    @kotlinx.serialization.Transient
    private var attachmentId: String = ""

    @kotlinx.serialization.Transient
    private val jsonPropModifier = JsonPropertyModifier(GunProp.entries)

    override fun getId(): String = attachmentId

    override fun setId(id: String) {
        attachmentId = id
    }

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        val input = captureInputSnapshot(modifier)
        val pmc = PmcProxy(modifier)
        modifiers.forEach { it.apply(pmc, input) }

        override?.let {
            jsonPropModifier.update(it)
            jsonPropModifier.modifyProperty(modifier)
        }

        val scopeInfo = scopeInfo
        val scopeZoom = if (scopeInfo != null && scopeInfo.modes.isNotEmpty()) {
            scopeInfo.mode(modifier.data.attachment.scopeMode(slot)).zoom ?: zoom
        } else {
            zoom
        } ?: return
        val current = modifier.data.attachment.getZoom(slot) ?: scopeZoom.default
        pmc.set("DefaultZoom", current)
        pmc.set("MinZoom", scopeZoom.min)
        pmc.set("MaxZoom", scopeZoom.max)
    }

    private fun captureInputSnapshot(modifier: PMC<GunData, DefaultGunData>): Map<String, Number> {
        val refs = modifiers.mapNotNull {
            (it.value as? AttachmentModifierValue.Reference)?.ref
        }.toSet()
        if (refs.isEmpty()) return emptyMap()

        return buildMap {
            refs.forEach { ref ->
                val prop = GunProp.entries.firstOrNull { it.serializationName == ref }
                if (prop == null) {
                    Mod.LOGGER.error("Unknown attachment modifier reference: {}", ref)
                    return@forEach
                }

                val value = modifier.getUnchecked(prop)
                if (value is Number) {
                    put(ref, value)
                } else {
                    Mod.LOGGER.error(
                        "Attachment modifier reference '{}' must point to a numeric property, got {}",
                        ref,
                        value?.javaClass?.name ?: "null",
                    )
                }
            }
        }
    }

    fun scopeMode(index: Int): ScopeMode? = scopeInfo?.mode(index)

    fun scopeZoom(index: Int): AttachmentZoom? {
        val info = scopeInfo
        return if (info != null && info.modes.isNotEmpty()) {
            info.mode(index).zoom ?: zoom
        } else {
            zoom
        }
    }

    fun supportsScopeSwitching(): Boolean = scopeInfo?.supportsModeSwitching() ?: false

    companion object {
        @JvmStatic
        fun from(id: String): AttachmentDefinition? = CustomData.ATTACHMENTS[id]

        @JvmStatic
        fun from(id: ResourceLocation): AttachmentDefinition? = CustomData.ATTACHMENTS[id.toString()]
    }
}

@Serializable
data class AttachmentModifier(
    @SerialName("Prop")
    val prop: String,

    @SerialName("Op")
    val op: AttachmentModifierOp = AttachmentModifierOp.ADD,

    @SerialName("Value")
    val value: AttachmentModifierValue = AttachmentModifierValue.Constant(0.0),
) {
    fun apply(pmc: PmcProxy, input: Map<String, Number>) {
        val resolved = value.resolve(input) ?: return
        when (op) {
            AttachmentModifierOp.ADD -> pmc.add(prop, resolved)
            AttachmentModifierOp.MUL -> pmc.mul(prop, resolved)
            AttachmentModifierOp.SET -> pmc.set(prop, resolved)
            AttachmentModifierOp.CLAMP_MIN -> pmc.clampMin(prop, resolved)
            AttachmentModifierOp.CLAMP_MAX -> pmc.clampMax(prop, resolved)
        }
    }

    private fun AttachmentModifierValue.resolve(input: Map<String, Number>): Double? {
        return when (this) {
            is AttachmentModifierValue.Constant -> value
            is AttachmentModifierValue.Reference -> {
                val source = input[ref]
                if (source == null) {
                    Mod.LOGGER.error("Unable to resolve attachment modifier reference: {}", ref)
                    return null
                }
                source.toDouble() * scale + offset
            }
        }
    }
}

@Serializable
enum class AttachmentModifierOp {
    @SerialName("Add")
    ADD,

    @SerialName("Mul")
    MUL,

    @SerialName("Set")
    SET,

    @SerialName("ClampMin")
    CLAMP_MIN,

    @SerialName("ClampMax")
    CLAMP_MAX,
}

@Serializable
data class AttachmentZoom(
    @SerialName("Min")
    val min: Double = 1.25,

    @SerialName("Max")
    val max: Double = 1.25,

    @SerialName("Default")
    val default: Double = 1.25,

    // 每滚一格变化的相对倍率，按当前倍率等比缩放，低倍率时变化小而高倍率时变化大
    @SerialName("Step")
    val step: Double = 0.15,
)
