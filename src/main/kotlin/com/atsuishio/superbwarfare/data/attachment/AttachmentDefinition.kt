package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.*
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.perk.js.PmcProxy
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import net.minecraft.resources.ResourceLocation

/**
 * 配件数据（`sbw/attachments/<id>.json`）
 *
 * 说明配件装在哪个槽位、用哪套模型贴图，以及它对枪械属性的修改，槽位规则见 [AttachmentSlots]
 */
@Serializable
data class AttachmentDefinition(
    @SerialName("Slot")
    val slot: AttachmentType = AttachmentType.SCOPE,

    /**
     * 还能装进哪些槽位
     */
    @SerialName("ExtraSlots")
    val extraSlots: List<AttachmentType> = emptyList(),

    /** 配件等级，如果类型是弹匣则用于弹匣等级，`GunData.magazineLevel()` 靠它从 `DrumLevels` 与分级换弹时间里取值 */
    @SerialName("Level")
    val level: Int = 0,

    /** 挂在枪模型的哪根骨骼上，不写就用槽位登记的约定骨骼 */
    @SerialName("Bone")
    val bone: String? = null,

    // 安装该瞄准镜时是否需要导轨桥架；部分专用瞄准镜通过燕尾槽直装在枪身上
    @SerialName("RequiresRail")
    val requiresRail: Boolean = true,

    @SerialName("UsesGunStock")
    val usesGunStock: Boolean = false,

    // 挂点组名，覆盖所在槽位的默认值，登记到同一挂点组的槽位互斥
    @SerialName("Mount")
    val mount: String? = null,

    // 追加到所在槽位默认互斥名单上的槽位，用来表达挂点组表达不了的非传递互斥
    @SerialName("ConflictsWith")
    val conflictsWith: List<AttachmentType> = emptyList(),

    // 允许与互斥的槽位共存，给"转接座"这类本来就是用来叠装的配件留的口子
    @SerialName("AllowSharedMount")
    val allowSharedMount: Boolean = false,

    // 安装该枪托时是否需要适配器，部分枪托可直接装在枪身上
    @SerialName("RequiresAdapter")
    val requiresAdapter: Boolean = true,

    @SerialName("Model")
    val model: SerializedResourceLocation? = null,

    @SerialName("Texture")
    val texture: SerializedResourceLocation? = null,

    /**
     * 配件的基础三轴旋转（度）
     */
    @SerialName("Rotation")
    val rotation: AttachmentRotation? = null,

    @SerialName("MuzzleFlashScale")
    val muzzleFlashScale: Float = 1.0f,

    @SerialName("SoundRadiusMultiplier")
    val soundRadiusMultiplier: Double = 1.0,

    @SerialName("IsSilenced")
    val isSilenced: Boolean = false,

    // 该配件是否自带脚架（不限于握把，未来其他槽位的配件也可以声明）
    @SerialName("Bipod")
    val hasBipod: Boolean = false,

    /**
     * 刺刀型枪口配件
     */
    @SerialName("IsBayonet")
    val isBayonet: Boolean = false,

    @SerialName("Modifiers")
    val modifiers: List<AttachmentModifier> = emptyList(),

    @SerialName("Override")
    val override: JsonObject? = null,

    /** 弹药条骨骼，任意槽位的配件都可以声明 */
    @SerialName("AmmoBar")
    val ammoBar: List<AmmoBarEntry> = emptyList(),

    /** 弹药文字锚点骨骼 */
    @SerialName("TextShow")
    val textShow: List<AmmoTextEntry> = emptyList(),

    @SerialName("ScopeInfo")
    val scopeInfo: ScopeInfo? = null,

    /** 副武器定义：带上它就是副武器，与槽位无关，刺刀这类只改近战动作的配件不带它 */
    @SerialName("SubWeapon")
    val subWeapon: SubWeaponInfo? = null,

    /** 吊坠摆动参数，不写就用 [CharmInfo.DEFAULT] */
    @SerialName("Charm")
    val charm: CharmInfo? = null,

    /**
     * 激光瞄准器定义：**带上它就说明这件配件会发光束**
     */
    @SerialName("Laser")
    val laser: LaserInfo? = null,

    /**
     * 枪盾定义（见 [ShieldInfo]）：**带上它就说明这件配件会挡投射物**
     */
    @SerialName("Shield")
    val shield: ShieldInfo? = null,
) : IDBasedData<AttachmentDefinition>, PropertyModifier<GunData, DefaultGunData> {

    /**
     * 这件配件能装进的全部槽位
     */
    @kotlinx.serialization.Transient
    val acceptedSlots: Set<AttachmentType> =
        if (extraSlots.isEmpty()) setOf(slot) else extraSlots.toSet() + slot

    @kotlinx.serialization.Transient
    private var attachmentId: String = ""

    @kotlinx.serialization.Transient
    private val jsonPropModifier = JsonOverrideApplier(GunProp.entries)

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

        val scopeZoom = scopeZoom(modifier.data.attachment.scopeMode(slot)) ?: return
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
        val info = scopeInfo ?: return null

        return if (info.modes.isNotEmpty()) {
            info.mode(index).zoom ?: info.zoom
        } else {
            info.zoom
        }
    }

    fun supportsScopeSwitching(): Boolean = scopeInfo?.supportsModeSwitching() ?: false

    /** 吊坠参数，没配置时退回 [CharmInfo.DEFAULT] */
    fun charmInfo(): CharmInfo = charm ?: CharmInfo.DEFAULT

    companion object {
        @JvmStatic
        fun from(id: String): AttachmentDefinition? = CustomData.ATTACHMENTS[id]

        @JvmStatic
        fun from(id: ResourceLocation): AttachmentDefinition? = CustomData.ATTACHMENTS[id.toString()]

        /**
         * 列出该枪上全部会发光束的配件。
         *
         * 返回槽位是为了让调用方按槽位读颜色覆盖（`Attachment.getLaserColor(slot)`）。
         * 必须是复数：四条导轨互不互斥，一把枪可以同时装好几件，那几束都要画。
         */
        @JvmStatic
        fun findLaserEmitters(gun: GunData?): List<LaserEmitter> {
            if (gun == null) return emptyList()

            val result = mutableListOf<LaserEmitter>()
            for (slot in AttachmentSlots.ALL) {
                val attachmentId = gun.attachment.id(slot.type) ?: continue
                val definition = from(attachmentId) ?: continue
                val info = definition.laser ?: continue
                result += LaserEmitter(slot, definition, info)
            }
            return result
        }

        /** 列出该枪上全部装了枪盾的槽位，按 [AttachmentType.entries] 的顺序，也就是吸收伤害的顺序 */
        @JvmStatic
        fun findShields(gun: GunData?): List<ShieldEmitter> {
            if (gun == null) return emptyList()

            val result = mutableListOf<ShieldEmitter>()
            for (slot in AttachmentSlots.ALL) {
                val attachmentId = gun.attachment.id(slot.type) ?: continue
                val definition = from(attachmentId) ?: continue
                val info = definition.shield ?: continue
                result += ShieldEmitter(slot, definition, info)
            }
            return result
        }
    }
}

/**
 * 一件装着的激光配件
 */
data class LaserEmitter(
    val slot: AttachmentSlot,
    val definition: AttachmentDefinition,
    val info: LaserInfo,
)

/**
 * 一件装着的枪盾
 */
data class ShieldEmitter(
    val slot: AttachmentSlot,
    val definition: AttachmentDefinition,
    val info: ShieldInfo,
)

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

/**
 * 配件的基础三轴旋转，单位是度
 */
@Serializable
data class AttachmentRotation(
    @SerialName("X")
    val x: Float = 0f,

    @SerialName("Y")
    val y: Float = 0f,

    @SerialName("Z")
    val z: Float = 0f,
) {
    /** 三个分量都是 `0`，等价于不写（没有背衬字段，本来就不会被序列化） */
    val isIdentity: Boolean get() = x == 0f && y == 0f && z == 0f
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
