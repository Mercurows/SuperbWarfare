package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.PropertyModifier
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.perk.js.PmcProxy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttachmentDefinition(
    @SerialName("Slot")
    val slot: AttachmentType = AttachmentType.SCOPE,

    @SerialName("Level")
    val level: Int = 0,

    @SerialName("Bone")
    val bone: String? = null,

    @SerialName("Icon")
    val icon: String? = null,

    @SerialName("Modifiers")
    val modifiers: List<AttachmentModifier> = emptyList(),

    @SerialName("Zoom")
    val zoom: AttachmentZoom? = null,
) : IDBasedData<AttachmentDefinition>, PropertyModifier<GunData, DefaultGunData> {

    @kotlinx.serialization.Transient
    private var attachmentId: String = ""

    override fun getId(): String = attachmentId

    override fun setId(id: String) {
        attachmentId = id
    }

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        val pmc = PmcProxy(modifier)
        modifiers.forEach { it.apply(pmc) }

        val scopeZoom = zoom ?: return
        val current = modifier.data.attachment.getZoom(slot) ?: scopeZoom.default
        pmc.set("DefaultZoom", current)
        pmc.set("MinZoom", scopeZoom.min)
        pmc.set("MaxZoom", scopeZoom.max)
    }
}

@Serializable
data class AttachmentModifier(
    @SerialName("Prop")
    val prop: String,

    @SerialName("Op")
    val op: AttachmentModifierOp = AttachmentModifierOp.ADD,

    @SerialName("Value")
    val value: Double = 0.0,
) {
    fun apply(pmc: PmcProxy) {
        when (op) {
            AttachmentModifierOp.ADD -> pmc.add(prop, value)
            AttachmentModifierOp.MUL -> pmc.mul(prop, value)
            AttachmentModifierOp.SET -> pmc.set(prop, value)
            AttachmentModifierOp.CLAMP_MIN -> pmc.clampMin(prop, value)
            AttachmentModifierOp.CLAMP_MAX -> pmc.clampMax(prop, value)
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

    @SerialName("Step")
    val step: Double = 1.0,
)
