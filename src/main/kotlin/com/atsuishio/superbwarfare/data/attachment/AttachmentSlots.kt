package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots.Bones.CHARM_CHARM
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots.Bones.CHARM_FIXED
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots.Bones.CHARM_STRING
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots.EDIT_ORDER
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots.declaredConflicts
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots.mountOf
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots.registeredIds
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.ConcurrentHashMap

/** 槽位的挂载骨骼从哪来，见 [AttachmentSlot.mountBone] */
sealed interface AttachmentMountBone {
    /** 约定骨骼：这个槽位固定挂在枪模型的这个名字上，配件自己怎么写都不看，目前只有握把走这条 */
    data class Fixed(val name: String) : AttachmentMountBone

    /** 用配件在 [AttachmentDefinition.bone] 里声明的骨骼，没声明时退回 [fallback]（null = 不渲染） */
    data class FromDefinition(val fallback: String? = null) : AttachmentMountBone

    /** 不挂模型，而是切换枪模型自带的骨骼（弹匣的 `magazine_standard` / `magazine_extend` 等） */
    data object GunModel : AttachmentMountBone
}

/** 槽位的渲染分派方式 */
enum class AttachmentRenderMode {
    /** 由 `GeoGunRenderer` 里该槽位的专属代码渲染（瞄具分划、枪托适配器、握把护木、枪口焰） */
    CUSTOM,

    /**
     * 走注册表驱动的通用渲染：按 [AttachmentDefinition.model] / [AttachmentDefinition.texture]
     * 取模型与贴图，挂到 [AttachmentSlot.mountBone] 上画，新增槽位默认走这条
     */
    GENERIC,
}

/**
 * 一个配件槽位的全部元数据
 *
 * 槽位相关的规则都从这张表读：挂点互斥、物品 tag、datagen、改装界面按钮、调试聚焦、通用渲染，
 * 新增一种槽位 = 加一个 [AttachmentType] 枚举常量 + 在这里登记一条
 *
 * @param mount 挂点组名，登记到同一组的槽位互斥（例如刺刀与枪口配件都在 `muzzle_device`），
 *   不同组的槽位可以共存，配件可以用 [AttachmentDefinition.mount] 覆盖所在槽位的默认值
 * @param conflictsWith 额外互斥的槽位，挂点组是传递的等价关系，表达不了"副武器排斥刺刀与握把、
 *   但刺刀与握把可以共存"这种非传递组合，所以这类规则写在这里
 * @param tagBucket 物品 tag 的桶名（`superbwarfare:attachment/<tagBucket>`），null 表示不生成 tag
 * @param icon 改装界面上的槽位图标（`textures/gui/attachment/<icon>.png`），界面目前还自己硬编码贴图，
 *   这里是给界面重写预留的槽位 → 图标映射
 * @param mountBone 挂载骨骼来源，只有 [AttachmentRenderMode.GENERIC] 的渲染会用到
 * @param focusBone 改装界面聚焦到该槽位时用的骨骼，null 表示不聚焦
 * @param withdrawAmmoOnChange 换这个槽位的配件前是否要先把已装填的弹药退给玩家（只有弹匣槽位需要）
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
    val researchable: Boolean = true,
) {
    /** 槽位的物品 tag 名，例如 `attachment/bayonet` */
    val tagName: String? get() = tagBucket?.let { "attachment/$it" }
}

/** 改装界面里的一个可编辑项：某个槽位，或者"弹药类型"这种非槽位项 */
sealed interface AttachmentEditTarget {
    data class Slot(val slot: AttachmentSlot) : AttachmentEditTarget

    /** 弹种切换（对应 `GunProp.AMMO_CONSUMER` 列表），不是配件槽位 */
    data object AmmoType : AttachmentEditTarget
}

/**
 * 配件槽位注册表，见 [AttachmentSlot]
 *
 * 新增槽位后还要手写：物品注册、`sbw/attachments/<id>.json`、模型与贴图、语言文件
 * 改装界面暂未接入这张表，追加在 [EDIT_ORDER] 末尾的槽位没有界面按钮，只能用 `/sbw attachment` 安装
 */
object AttachmentSlots {

    /** 枪模型与配件模型里的约定骨骼名 */
    object Bones {
        const val MUZZLE = "muzzle_pos"
        const val SCOPE = "scope_pos"
        const val GRIP = "grip_pos"
        const val STOCK = "stock_pos"
        const val MAGAZINE = "magazine_pos"
        const val BAYONET = "bayonet_pos"
        const val SUBWEAPON = "sub_weapon_pos"
        const val CHARM = "charm_pos"

        /**
         * 下导轨的约定挂点（护木下方的导轨），目前只有 SVD 的模型上有这根骨骼。
         *
         * 与 [GRIP] 的 `grip_pos` 一样走 [AttachmentMountBone.Fixed]：导轨在枪上的位置由**枪模型**
         * 决定，配件自己声明不了 —— 这正是"导轨"的含义，也让 SVD 之外新支持下导轨的枪只需在
         * 模型里加一根同名骨骼即可
         */
        const val LOWER_RAIL = "lower_rail_pos"

        /**
         * **配件模型内部**的三个分组名（吊坠专用），不是枪模型上的骨骼
         *
         * [CHARM_FIXED] 是固定件（挂环、卡扣），始终静止，[CHARM_STRING] 连接绳与 [CHARM_CHARM] 挂件本体
         * 一起绕摆点旋转，这两组的骨骼枢轴不参与计算：摆点取 `string` 分组包围盒的顶部中心，
         * 摆长取它的高度，见 `CharmRig` 与 `CharmSolver`
         */
        const val CHARM_FIXED = "fixed"
        const val CHARM_STRING = "string"
        const val CHARM_CHARM = "charm"
    }

    /** 全部槽位，顺序决定配件物品 tag 的排列顺序（只影响生成文件的可读性） */
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
            type = AttachmentType.MUZZLE,
            mount = "muzzle_device",
            tagBucket = "muzzle",
            icon = "muzzle",
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
        // 握把与将来的下挂（underbarrel_rail）物理上是同一根下导轨，但这里不合并挂点组：
        // 合并会让"装了垂直握把就装不了下挂榴弹"，那是玩法改动
        AttachmentSlot(
            type = AttachmentType.GRIP,
            mount = "grip_rail",
            tagBucket = "grip",
            icon = "grip",
            mountBone = AttachmentMountBone.Fixed(Bones.GRIP),
            focusBone = Bones.GRIP,
            renderMode = AttachmentRenderMode.CUSTOM,
        ),
        // 刺刀与枪口配件（消音器/制退器）抢同一个枪口挂点，装了其中一个就装不了另一个
        // 挂载骨骼用 FromDefinition：刺刀卡在枪口上，而"枪口"这根骨骼各枪叫法不同
        //（有的叫 bayonet_pos，有的只有 muzzle_pos）
        AttachmentSlot(
            type = AttachmentType.BAYONET,
            mount = "muzzle_device",
            tagBucket = "bayonet",
            icon = "bayonet",
            mountBone = AttachmentMountBone.FromDefinition(Bones.BAYONET),
            focusBone = Bones.BAYONET,
            renderMode = AttachmentRenderMode.GENERIC,
        ),
        // 副武器（下挂榴弹发射器这类），挂点组与握把分开：挂点组是传递的等价关系，
        // 并进 grip_rail 会把它和"所有 grip_rail 上的槽位"绑成一团，这里要的是非传递互斥 ——
        // 副武器排斥刺刀、握把与下导轨，但这三者彼此可以共存，所以用 conflictsWith 显式点名。
        // 下导轨（脚架）算进来是因为它和副武器抢的是护木下方同一块位置，物理上装不下两个；
        // `conflicts` 是对称判定的（任一方点名即互斥），所以只在这一侧登记就够了
        AttachmentSlot(
            type = AttachmentType.SUBWEAPON,
            mount = "subweapon_rail",
            tagBucket = "subweapon",
            icon = "subweapon",
            mountBone = AttachmentMountBone.FromDefinition(Bones.SUBWEAPON),
            conflictsWith = setOf(AttachmentType.BAYONET, AttachmentType.GRIP, AttachmentType.LOWER_RAIL),
            focusBone = Bones.SUBWEAPON,
            renderMode = AttachmentRenderMode.GENERIC,
        ),
        // 吊坠独占挂点组 charm_loop，与其它槽位都不冲突，可以同时装
        // 走通用渲染，但摆动姿态是在通用渲染之前注入的（见 `GeoGunRenderer` 与 `CharmRuntime`）
        AttachmentSlot(
            type = AttachmentType.CHARM,
            mount = "charm_loop",
            tagBucket = "charm",
            icon = "charm",
            mountBone = AttachmentMountBone.FromDefinition(Bones.CHARM),
            focusBone = Bones.CHARM,
            renderMode = AttachmentRenderMode.GENERIC,
            researchable = false,
        ),
        // 下导轨与握把（GRIP）物理上是同一根导轨，但沿用上面那条理由：挂点组分开，
        // 于是两者既不互斥也不共享挂点，装了垂直握把照样能装脚架
        // 与副武器（SUBWEAPON）互斥 —— 由 SUBWEAPON 那一侧的 conflictsWith 点名，不在这里重复
        // 挂载骨骼走 Fixed（导轨由枪模型定，见 [Bones.LOWER_RAIL]），所以配件数据里**不要**写 `Bone`
        AttachmentSlot(
            type = AttachmentType.LOWER_RAIL,
            mount = "lower_rail",
            tagBucket = "lower_rail",
            icon = "lower_rail",
            mountBone = AttachmentMountBone.Fixed(Bones.LOWER_RAIL),
            focusBone = Bones.LOWER_RAIL,
            renderMode = AttachmentRenderMode.GENERIC,
        ),
    )

    @JvmField
    val BY_TYPE: Map<AttachmentType, AttachmentSlot> = ALL.associateBy { it.type }

    /**
     * 改装界面与报文里的编辑项顺序，**下标就是 `EditMessage.type`**，客户端与服务端共用这一份
     *
     * 前 6 项是既有改装界面按钮的固定排布（枪口 / 瞄具 / 握把 / 枪托 / 弹匣 / 弹种），顺序不能动，
     * 新增槽位追加在末尾，界面重写后按这份顺序布局即可自动带上
     */
    @JvmField
    val EDIT_ORDER: List<AttachmentEditTarget> = listOf(
        AttachmentEditTarget.Slot(of(AttachmentType.MUZZLE)),
        AttachmentEditTarget.Slot(of(AttachmentType.SCOPE)),
        AttachmentEditTarget.Slot(of(AttachmentType.GRIP)),
        AttachmentEditTarget.Slot(of(AttachmentType.STOCK)),
        AttachmentEditTarget.Slot(of(AttachmentType.MAGAZINE)),
        AttachmentEditTarget.AmmoType,
        AttachmentEditTarget.Slot(of(AttachmentType.BAYONET)),
        AttachmentEditTarget.Slot(of(AttachmentType.SUBWEAPON)),
        AttachmentEditTarget.Slot(of(AttachmentType.CHARM)),
        AttachmentEditTarget.Slot(of(AttachmentType.LOWER_RAIL)),
    )

    /** 弹药类型那一项在 [EDIT_ORDER] 里的下标（车辆改装界面只支持这一项） */
    @JvmField
    val AMMO_TYPE_EDIT_INDEX: Int = EDIT_ORDER.indexOf(AttachmentEditTarget.AmmoType)

    /** 取出 [type] 的登记项，未登记（新增枚举但忘了登记）时抛异常，早失败好过静默失效 */
    @JvmStatic
    fun of(type: AttachmentType): AttachmentSlot =
        BY_TYPE[type] ?: error("Attachment slot $type is not registered in AttachmentSlots.ALL")

    /** [type] 的登记项，未登记时返回 null，供只读查询使用 */
    @JvmStatic
    fun ofOrNull(type: AttachmentType): AttachmentSlot? = BY_TYPE[type]

    /** [type] 实际使用的挂点组名，配件可以用 [AttachmentDefinition.mount] 覆盖槽位默认值 */
    @JvmStatic
    fun mountOf(type: AttachmentType, definition: AttachmentDefinition? = null): String =
        definition?.mount ?: of(type).mount

    /**
     * [type]（实际装的是 [definition]）显式声明互斥的槽位：槽位登记项与配件声明的并集
     *
     * 与挂点组不同，这份名单不传递，所以"副武器排斥刺刀与握把、但刺刀与握把共存"可以表达
     */
    @JvmStatic
    fun declaredConflicts(type: AttachmentType, definition: AttachmentDefinition? = null): Set<AttachmentType> {
        val slot = ofOrNull(type)?.conflictsWith.orEmpty()
        val declared = definition?.conflictsWith.orEmpty()
        return if (declared.isEmpty()) slot else slot + declared
    }

    /**
     * [gun] 的**枪械数据**里单独声明的槽位互斥（`AttachmentConflicts`）。
     *
     * 与 [declaredConflicts] 同一套语义（单边声明、**对称**生效、不传递），区别只在于规则挂在**某一
     * 把枪**上而不是全局：下导轨与握把抢的是护木下方同一段导轨，护木够长的枪上两者都能装，
     * 但 AK47 / AK12 / MP5 / QBZ191 这类导轨过短的枪同时挂上会互相穿模 —— 这种"只有某些枪"的
     * 限制用挂点组表达不了（挂点组是全体的），只能写进枪械数据。
     *
     * 键与值都按 [AttachmentType.attachmentName] 比对，认不出来的名字当作没写
     * （`DataValidator.validateAttachmentConflicts` 会把它报出来）。
     */
    @JvmStatic
    fun gunConflicts(gun: GunData?, type: AttachmentType, other: AttachmentType): Boolean {
        if (gun == null || type == other) return false

        val declared = gun.getDefault().attachmentConflicts
        if (declared.isEmpty()) return false

        return other.attachmentName in declared[type.attachmentName].orEmpty() ||
                type.attachmentName in declared[other.attachmentName].orEmpty()
    }

    /**
     * [type] 槽位（装的是 [definition]）与 [other] 槽位（装的是 [otherDefinition]）是否互斥
     *
     * 三种情况互斥：这把枪自己声明了这对槽位互斥（[gunConflicts]，见 `AttachmentConflicts`）、
     * 挂点组相同（[mountOf]），或者任一方显式点名了对方（[declaredConflicts]）
     * 任一方声明了 [AttachmentDefinition.allowSharedMount] 就整体放行，那是"转接座"这类
     * 本来就是用来叠装的配件的逃生口，同一个槽位不算冲突
     *
     * 枪械数据那条规则是**物理**限制（这段导轨装不下两个），但 [AttachmentDefinition.allowSharedMount]
     * 依然能放行它：那个字段的含义是"这件配件本身就是设计来叠装的"，优先级高于几何限制。
     *
     * @param gun 装这些配件的枪；不传时只判全局规则（枪械数据那条无从判断）
     */
    @JvmStatic
    @JvmOverloads
    fun conflicts(
        type: AttachmentType,
        definition: AttachmentDefinition?,
        other: AttachmentType,
        otherDefinition: AttachmentDefinition?,
        gun: GunData? = null,
    ): Boolean {
        if (type == other) return false
        if (definition?.allowSharedMount == true || otherDefinition?.allowSharedMount == true) return false

        if (gunConflicts(gun, type, other)) return true

        if (mountOf(type, definition) == mountOf(other, otherDefinition)) return true

        return other in declaredConflicts(type, definition) || type in declaredConflicts(other, otherDefinition)
    }

    /** [registeredIds] 的结果缓存，按 [GunData.DATA_VERSION] 整体失效 */
    private val registeredIdsCache = ConcurrentHashMap<AttachmentType, List<ResourceLocation>>()

    /** 缓存对应的 [GunData.DATA_VERSION]，`Int.MIN_VALUE` = 还没算过 */
    private var registeredIdsVersion = Int.MIN_VALUE

    /**
     * [type] 槽位上已注册的全部配件 id（配件物品与配件数据都在的那些），按 id 排序
     *
     * 只被「完全自由改装模式」用到：那一档要无视枪械数据里的 `AvailableAttachments`，
     * 把一个槽位能装的东西全部放出来，来源是配件物品注册表 [ModItems.ATTACHMENTS]，
     * 槽位以配件数据的 `Slot` 为准，所以数据包改了 `Slot` 之后结果会跟着变
     *
     * 结果按 [GunData.DATA_VERSION] 缓存：它会经 `GunItem.hasCustomAttachment` 被渲染路径每帧查询，
     * 而枚举几十个配件物品再逐条查数据表并不便宜
     */
    @JvmStatic
    fun registeredIds(type: AttachmentType): List<ResourceLocation> {
        if (registeredIdsVersion != GunData.DATA_VERSION) {
            registeredIdsCache.clear()
            registeredIdsVersion = GunData.DATA_VERSION
        }

        return registeredIdsCache.getOrPut(type) {
            ModItems.ATTACHMENTS.entries
                .map { it.id }
                .filter { AttachmentDefinition.from(it)?.slot == type }
                .sorted()
        }
    }

    /**
     * [slot]（装的是 [definition]）实际使用的挂载骨骼名，null 表示这个槽位不往枪模型上挂
     * （[AttachmentMountBone.GunModel] 与"没声明就不渲染"的 `FromDefinition(null)`）
     *
     * 渲染与"配件骨骼在枪模型里的哪个位置"的查询必须走同一个判定
     */
    @JvmStatic
    fun mountBoneOf(slot: AttachmentSlot, definition: AttachmentDefinition?): String? =
        when (val mountBone = slot.mountBone) {
            is AttachmentMountBone.Fixed -> mountBone.name
            is AttachmentMountBone.FromDefinition -> definition?.bone ?: mountBone.fallback
            AttachmentMountBone.GunModel -> null
        }
}
