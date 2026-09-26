package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots.EDIT_ORDER
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots.declaredConflicts
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots.mountOf
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType

/**
 * 槽位的挂载骨骼从哪来。
 *
 * @see AttachmentSlot.mountBone
 */
sealed interface AttachmentMountBone {
    /**
     * 约定骨骼：这个槽位固定挂在枪模型的这个名字上，与配件自身怎么写无关。
     * 目前只有刺刀（`bayonet_pos`）走这条。
     */
    data class Fixed(val name: String) : AttachmentMountBone

    /**
     * 用配件自己在 [AttachmentDefinition.bone] 里声明的骨骼；没声明时退回 [fallback]
     * （为 `null` 表示"没声明就不渲染"）。枪口槽就是这么用的。
     */
    data class FromDefinition(val fallback: String? = null) : AttachmentMountBone

    /**
     * 不是"往枪上挂一个模型"，而是**切换枪模型自带的骨骼**（弹匣这类：
     * `magazine_standard` / `magazine_extend` / `magazine_extend_pro` 都是枪模型里的骨骼）。
     */
    data object GunModel : AttachmentMountBone
}

/**
 * 槽位的渲染分派方式。
 */
enum class AttachmentRenderMode {
    /**
     * 由 `GeoGunRenderer` 里该槽位的专属代码渲染。
     *
     * 现有五个槽位都有各自的特殊表现（瞄具要分划/模板、枪托要适配器、握把要护木、枪口要枪口焰），
     * 单独写比塞进通用流程更清楚。
     */
    CUSTOM,

    /**
     * 走注册表驱动的通用渲染：按 [AttachmentDefinition.model] / [AttachmentDefinition.texture]
     * 取模型与贴图，挂到 [AttachmentSlot.mountBone] 上画。**新增槽位默认走这条**。
     */
    GENERIC,
}

/**
 * 一个配件槽位的全部元数据。
 *
 * 「槽位」原先散落在 8 处硬编码（渲染分派、编辑界面按钮、焦点骨骼、标签桶、挂点组……），
 * 这里收成一条登记项：**新增一个槽位类型 = 加一个 [AttachmentType] 枚举常量 + 在这里登记一条**，
 * 其余环节（挂点互斥、物品 tag、datagen、改装界面按钮、调试聚焦、通用渲染）都从这张表读。
 *
 * @param type 对应的槽位枚举。
 * @param mount 挂点组名。**登记到同一个 [mount] 的两个槽位互斥**（同时只能装一个），
 *   例如刺刀与枪口配件都挂在 `muzzle_device` 上；不同 [mount] 的槽位可以共存且同时生效。
 *   配件可以用 [AttachmentDefinition.mount] 覆盖自己所在槽位的默认挂点组。
 * @param conflictsWith 额外互斥的槽位（默认空）。**挂点组是传递的等价关系**，表达不了
 *   "A 与 B 互斥、A 与 C 互斥，但 B 与 C 可以共存"这种非传递组合，
 *   所以这类规则写在这里（见 [conflicts] 与 `AttachmentType.SUBWEAPON` 的登记项）。
 *   配件可以用 [AttachmentDefinition.conflictsWith] 追加自己的名单，
 *   用 [AttachmentDefinition.allowSharedMount] 整体放行。
 * @param tagBucket 物品 tag 的桶名（`superbwarfare:attachment/<tagBucket>`），
 *   `null` 表示这个槽位不生成 tag。生成逻辑见 `ModTags` / `ModItemTagProvider`。
 * @param icon 改装界面上的槽位图标（`textures/gui/attachment/<icon>.png`）。
 *   **当前界面（`WeaponEditScreen`）本期不改、还没读它**，登记在这里是为了让界面重写时
 *   "槽位 → 图标"不必再散在界面代码里。
 * @param mountBone 挂载骨骼来源，只被 [AttachmentRenderMode.GENERIC] 的渲染用到。
 * @param focusBone 改装界面聚焦到该槽位时用的骨骼；`null` 表示该槽位不聚焦。
 * @param renderMode 渲染分派方式。
 * @param withdrawAmmoOnChange 换这个槽位的配件前，是否需要先把已装填的弹药退还给玩家
 *   （只有弹匣槽位需要：配件会改变弹匣容量）。
 */
data class AttachmentSlot(
    val type: AttachmentType,
    val mount: String,
    val tagBucket: String?,
    val icon: String,
    val mountBone: AttachmentMountBone,
    val conflictsWith: Set<AttachmentType> = emptySet(),
    val focusBone: String? = null,
    val renderMode: AttachmentRenderMode = AttachmentRenderMode.GENERIC,
    val withdrawAmmoOnChange: Boolean = false,
) {
    /** 槽位的物品 tag 名，例如 `attachment/bayonet`。 */
    val tagName: String? get() = tagBucket?.let { "attachment/$it" }
}

/**
 * 改装界面里的一个可编辑项：某个槽位，或者"弹药类型"这种非槽位项。
 */
sealed interface AttachmentEditTarget {
    data class Slot(val slot: AttachmentSlot) : AttachmentEditTarget

    /** 弹种切换（对应 `GunProp.AMMO_CONSUMER` 列表），不是配件槽位。 */
    data object AmmoType : AttachmentEditTarget
}

/**
 * 配件槽位注册表。
 *
 * 「槽位」原先散落在 8 处硬编码（渲染分派、编辑界面按钮、焦点骨骼、标签桶、挂点组……），
 * 这里收成一条登记项。**新增一个槽位类型 = 加一个 [AttachmentType] 枚举常量 + 在这里登记一条**，
 * 其余环节（挂点互斥、物品 tag、datagen、调试聚焦、通用渲染）都从这张表读；
 * 剩下要手写的只有：物品注册、`sbw/attachments/<id>.json`、模型/贴图、语言文件。
 *
 * **不含改装界面**：`WeaponEditScreen` 暂不接入注册表，它的按钮顺序与 [EDIT_ORDER] 对齐；
 * 追加在 [EDIT_ORDER] 末尾的新槽位没有界面按钮，用 `/sbw attachment` 指令安装。
 */
object AttachmentSlots {

    /**
     * 枪模型里的约定骨骼名。
     *
     * 只新增了刺刀的 `bayonet_pos` 一个约定骨骼；其它槽位/配件一律走配件自己的
     * `AttachmentDefinition.Bone`（枪口槽一直是这么用的）。
     */
    object Bones {
        const val MUZZLE = "muzzle_pos"
        const val SCOPE = "scope_pos"
        const val GRIP = "grip_pos"
        const val STOCK = "stock_pos"
        const val MAGAZINE = "magazine_pos"
        const val BAYONET = "bayonet_pos"
        const val SUBWEAPON = "sub_weapon_pos"
    }

    /**
     * 全部槽位。顺序决定配件物品 tag 的排列顺序（仅影响生成文件的可读性）。
     */
    val ALL: List<AttachmentSlot> = listOf(
        AttachmentSlot(
            type = AttachmentType.SCOPE,
            mount = "scope_rail",
            tagBucket = "scope",
            icon = "scope",
            mountBone = AttachmentMountBone.FromDefinition(Bones.SCOPE),
            focusBone = Bones.SCOPE,
            renderMode = AttachmentRenderMode.CUSTOM,
        ),
        AttachmentSlot(
            type = AttachmentType.MAGAZINE,
            mount = "magazine_well",
            tagBucket = "magazine",
            icon = "magazine",
            mountBone = AttachmentMountBone.GunModel,
            focusBone = Bones.MAGAZINE,
            renderMode = AttachmentRenderMode.CUSTOM,
            withdrawAmmoOnChange = true,
        ),
        AttachmentSlot(
            type = AttachmentType.BARREL,
            mount = "muzzle_device",
            tagBucket = "barrel",
            icon = "barrel",
            mountBone = AttachmentMountBone.FromDefinition(),
            focusBone = Bones.MUZZLE,
            renderMode = AttachmentRenderMode.CUSTOM,
        ),
        AttachmentSlot(
            type = AttachmentType.STOCK,
            mount = "stock_interface",
            tagBucket = "stock",
            icon = "stock",
            mountBone = AttachmentMountBone.FromDefinition("custom_stock_adapter"),
            focusBone = Bones.STOCK,
            renderMode = AttachmentRenderMode.CUSTOM,
        ),
        // 握把与将来的下挂（`underbarrel_rail`）物理上是同一根下导轨，但本期**不合并挂点组**：
        // 合并会让"装了垂直握把就装不了下挂榴弹"，那是玩法改动，等三期落地下挂时再单独决定。
        AttachmentSlot(
            type = AttachmentType.GRIP,
            mount = "grip_rail",
            tagBucket = "grip",
            icon = "grip",
            mountBone = AttachmentMountBone.Fixed(Bones.GRIP),
            focusBone = Bones.GRIP,
            renderMode = AttachmentRenderMode.CUSTOM,
        ),
        // 刺刀和枪口配件（消音器/制退器）抢的是**同一个枪口挂点**：装了其中一个就装不了另一个。
        // 物理上刺刀是卡在枪口下方的卡榫上，但真枪上也确实不能同时又挂消音器又上刺刀。
        AttachmentSlot(
            type = AttachmentType.BAYONET,
            mount = "muzzle_device",
            tagBucket = "bayonet",
            icon = "bayonet",
            mountBone = AttachmentMountBone.Fixed(Bones.BAYONET),
            focusBone = Bones.BAYONET,
            renderMode = AttachmentRenderMode.GENERIC,
        ),
        // 副武器（下挂榴弹发射器这类）。挂点组仍与握把（`grip_rail`）分开 ——
        // 挂点组是**传递**的等价关系，把副武器并进 `grip_rail` 会顺带把它和"所有 grip_rail 上的槽位"
        // 绑成一团，将来想再细分就没法表达了。这里要的是**非传递**的互斥：
        //
        //     副武器 ↔ 刺刀、副武器 ↔ 握把，但刺刀 ↔ 握把**不**互斥
        //
        // 三者物理上都挨着前段导轨/枪口，但"刺刀 + 握把"是能同时装的组合，所以用
        // [conflictsWith] 显式点名，而不是合并挂点组。
        //
        // **挂点骨骼走 `FromDefinition` 而不是 `Fixed`**：副武器挂在枪身的哪根骨骼
        // 由配件自己的 `Bone` 说了算（不同的下挂件可以挂在不同位置，将来加"枪托内置发射器"
        // 之类也不用再改代码）；配件没写 `Bone` 时才退回约定骨骼 `sub_weapon_pos`。
        // 用 `Fixed` 的话配件里的 `Bone` 会被**静默忽略**，模型骨骼名一旦和常量差一个字符
        // （`subweapon_pos` vs `sub_weapon_pos`）就什么都不会渲染，且没有任何报错。
        AttachmentSlot(
            type = AttachmentType.SUBWEAPON,
            mount = "subweapon_rail",
            tagBucket = "subweapon",
            icon = "subweapon",
            mountBone = AttachmentMountBone.FromDefinition(Bones.SUBWEAPON),
            conflictsWith = setOf(AttachmentType.BAYONET, AttachmentType.GRIP),
            focusBone = Bones.SUBWEAPON,
            renderMode = AttachmentRenderMode.GENERIC,
        ),
    )

    @JvmField
    val BY_TYPE: Map<AttachmentType, AttachmentSlot> = ALL.associateBy { it.type }

    private val BY_MOUNT: Map<String, List<AttachmentSlot>> = ALL.groupBy { it.mount }

    /**
     * 改装界面 / 报文里的编辑项顺序：**下标就是 `EditMessage.type`**，客户端与服务端共用这一份。
     *
     * 前 6 项是既有改装界面按钮的固定排布（枪口 / 瞄具 / 握把 / 枪托 / 弹匣 / 弹种），顺序不能动；
     * 新增槽位追加在末尾 —— 本期界面不改（要重写），所以追加的槽位暂时没有按钮，
     * 用 `/sbw attachment <entity> set <type> <id>` 安装，重写后的界面按这份顺序布局即可自动带上。
     */
    @JvmField
    val EDIT_ORDER: List<AttachmentEditTarget> = listOf(
        AttachmentEditTarget.Slot(of(AttachmentType.BARREL)),
        AttachmentEditTarget.Slot(of(AttachmentType.SCOPE)),
        AttachmentEditTarget.Slot(of(AttachmentType.GRIP)),
        AttachmentEditTarget.Slot(of(AttachmentType.STOCK)),
        AttachmentEditTarget.Slot(of(AttachmentType.MAGAZINE)),
        AttachmentEditTarget.AmmoType,
        AttachmentEditTarget.Slot(of(AttachmentType.BAYONET)),
        AttachmentEditTarget.Slot(of(AttachmentType.SUBWEAPON)),
    )

    /** 弹药类型那一项在 [EDIT_ORDER] 里的下标（车辆改装界面只支持这一项）。 */
    @JvmField
    val AMMO_TYPE_EDIT_INDEX: Int = EDIT_ORDER.indexOf(AttachmentEditTarget.AmmoType)

    /** 取出 [type] 的登记项；未登记（新增枚举但忘了登记）时抛异常，早失败好过静默失效。 */
    @JvmStatic
    fun of(type: AttachmentType): AttachmentSlot =
        BY_TYPE[type] ?: error("Attachment slot $type is not registered in AttachmentSlots.ALL")

    /** [type] 的登记项，未登记时返回 `null`（数据包侧只读查询用，不要让它把整局游戏炸掉）。 */
    @JvmStatic
    fun ofOrNull(type: AttachmentType): AttachmentSlot? = BY_TYPE[type]

    /**
     * [type] 实际使用的挂点组名：配件可以用 [AttachmentDefinition.mount] 覆盖槽位默认值。
     */
    @JvmStatic
    fun mountOf(type: AttachmentType, definition: AttachmentDefinition? = null): String =
        definition?.mount ?: of(type).mount

    /**
     * [type]（实际装的配件是 [definition]）**显式**声明互斥的槽位：槽位登记项 [AttachmentSlot.conflictsWith]
     * 与配件自己的 [AttachmentDefinition.conflictsWith] 的并集。
     *
     * 与挂点组不同，这份名单**不传递**，所以"副武器排斥刺刀与握把、但刺刀与握把共存"可以表达。
     */
    @JvmStatic
    fun declaredConflicts(type: AttachmentType, definition: AttachmentDefinition? = null): Set<AttachmentType> {
        val slot = ofOrNull(type)?.conflictsWith.orEmpty()
        val declared = definition?.conflictsWith.orEmpty()
        return if (declared.isEmpty()) slot else slot + declared
    }

    /**
     * [type] 槽位（装的是 [definition]）与 [other] 槽位（装的是 [otherDefinition]）**是否互斥**。
     *
     * 互斥有两个来源，任一成立即互斥：
     * 1. **挂点组相同**（[mountOf]）—— 传递的等价关系，例如刺刀与枪口配件都占 `muzzle_device`；
     * 2. **任一方显式点名**了对方（[declaredConflicts]）—— 非传递，例如
     *    副武器 ↔ 刺刀、副武器 ↔ 握把，但刺刀与握把可以共存。
     *
     * 任一方声明了 [AttachmentDefinition.allowSharedMount] 就整体放行 ——
     * 那是"转接座"这类本来就是用来叠装的配件的逃生口，两种互斥都适用。
     *
     * 同一个槽位不算冲突（调用方问的是"两个槽位能不能共存"）。
     */
    @JvmStatic
    fun conflicts(
        type: AttachmentType,
        definition: AttachmentDefinition?,
        other: AttachmentType,
        otherDefinition: AttachmentDefinition?,
    ): Boolean {
        if (type == other) return false
        if (definition?.allowSharedMount == true || otherDefinition?.allowSharedMount == true) return false

        if (mountOf(type, definition) == mountOf(other, otherDefinition)) return true

        return other in declaredConflicts(type, definition) || type in declaredConflicts(other, otherDefinition)
    }

    /** 登记到 [mount] 这个挂点组上的全部槽位。 */
    @JvmStatic
    fun byMount(mount: String): List<AttachmentSlot> = BY_MOUNT[mount].orEmpty()

    /**
     * [slot]（装的是 [definition]）实际使用的**挂点骨骼名**；`null` = 这个槽位不往枪模型上挂
     * （[AttachmentMountBone.GunModel] 与"没声明就不渲染"的 `FromDefinition(null)`）。
     *
     * 渲染（`GeoGunRenderer.renderRegisteredAttachments`）与"配件骨骼在枪模型里的哪个位置"的查询
     * （`GeoGunRenderer.resolveSubWeaponFlareTransform`）必须走**同一个**判定，否则会出现
     * "模型画得出来、枪口焰却找不到挂点"这种只在骨骼名写错时才会暴露的问题。
     */
    @JvmStatic
    fun mountBoneOf(slot: AttachmentSlot, definition: AttachmentDefinition?): String? =
        when (val mountBone = slot.mountBone) {
            is AttachmentMountBone.Fixed -> mountBone.name
            is AttachmentMountBone.FromDefinition -> definition?.bone ?: mountBone.fallback
            AttachmentMountBone.GunModel -> null
        }

    /** 槽位的物品 tag 名（`attachment/<bucket>`）；该槽位不生成 tag 时返回 `null`。 */
    @JvmStatic
    fun tagNameOf(type: AttachmentType): String? = ofOrNull(type)?.tagName
}
