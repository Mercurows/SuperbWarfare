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
     * 除 [slot] 之外**还能**装进哪些槽位（`"ExtraSlots": ["LowerRail", "LeftRail", "RightRail"]`）。
     *
     * 名字里的 `Extra` 是要紧的：[slot] 是这件配件**定义在哪个槽位**上，这里列的只是"顺便也能装"，
     * 两者不是平级关系 —— 装上以后一律以**实际安装的槽位**为准（挂载骨骼 [AttachmentSlots.mountBoneOf]、
     * 互斥判定、渲染都取那个槽位），[slot] 同时还是 [scopeMode] / [scopeZoom] 这类
     * **按槽位下标取值**的数据的取值依据。所以多槽位的配件请**不要**带 `ScopeInfo`：
     * 同一份模式表会在不同槽位上被解释成不同的档位。
     *
     * 存在的理由是同一件东西在好几根导轨上都装得下：激光指示器 / 战术手电这类不分上下左右，
     * 而四条导轨在 `AttachmentSlots` 里各占一个槽位（只有这样它们才能互不冲突），
     * 于是"一件配件、多个槽位"只能由配件数据自己表达。
     *
     * 值里不必重复写 [slot]（[acceptedSlots] 会自动并上），写重了也无害。
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
     * 配件的基础三轴旋转（度）：乘在挂点变换**之内**，也就是"挂上去之后再整件转一下"。
     *
     * 三个分量与 geo 里骨骼的 `rotation` **是同一套写法**（换算见 `TreeBedrockModelBaker`：
     * X、Y 取负、Z 不取负，按 `ZYX` 顺序合成），所以美术在 Blockbench 里量到多少度，这里就写多少度。
     * 不写、或三个分量都是 `0`，等于没有这个字段（渲染路径会整段跳过）。
     *
     * 存在的理由是"同一件配件要装在朝向不同的挂点上"：四条导轨的挂点骨骼各自带
     * ±90° / 180° 的 Z 轴旋转（见 [AttachmentSlots.Bones]），而配件模型只可能照其中一根的方向去建模，
     * 换到别的导轨上就用这个字段补正，不必再让美术多出一份模型。
     *
     * **瞄准镜会忽略它**：镜筒的挂点变换还兼着开镜窗口（`ocular`）的定位，旋转光学瞄具本身也没有意义。
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
     * 刺刀型枪口配件。
     *
     * 刺刀已经并入枪口槽（[AttachmentType.MUZZLE]），槽位本身不再区分它——这个标记只负责两件事：
     * 在tooltip里标出"枪刺功能"，以及与副武器（[AttachmentType.SUBWEAPON]）互斥
     * （两者抢的是前段同一处导轨，见 [AttachmentSlots.declaredConflicts]）。
     * 除这两点外，它的外观与行为与普通枪口配件完全一致。
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
     * 激光瞄准器定义（见 [LaserInfo]）：**带上它就说明这件配件会发光束**。
     *
     * 这是"给配件加一个能力"，**不是"新增一类配件"** —— 它不参与 [slot] / [extraSlots] 的槽位声明，
     * 也不影响 [modifiers]：一件普通的导轨配件可以既改属性、又发光束。
     * 装在四条导轨上的任意一条都走同一份配置（方向从枪模型的挂点骨骼继承）。
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
     * [slot] 与 [extraSlots] 的并集：这件配件能装进的全部槽位。
     *
     * 判定装得上与否的地方（`GunData.canInstall`、`Attachment.installed`、指令补全、
     * `AttachmentSlots.registeredIds`）一律走这里，不要再单独比 [slot]。
     *
     * 在构造期算好而不是每次现算：`availableAttachments` 会在改装界面每帧的候选过滤里被反复调用
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
 * 一件装着的激光配件。
 *
 * @param slot 装在哪条导轨上，既是挂点骨骼的依据，也是颜色覆盖的键
 */
data class LaserEmitter(
    val slot: AttachmentSlot,
    val definition: AttachmentDefinition,
    val info: LaserInfo,
)

/**
 * 一件装着的枪盾。
 *
 * @param slot 装在哪个槽位上，也是耐久状态的键
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
 * 配件的基础三轴旋转（见 [AttachmentDefinition.rotation]），单位是度
 *
 * 三个分量分别省略时为 `0`；[isIdentity] 为 `true` 时渲染路径整个跳过，
 * 既不建矩阵也不乘进 `PoseStack` —— 绝大多数配件都不写这个字段。
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
