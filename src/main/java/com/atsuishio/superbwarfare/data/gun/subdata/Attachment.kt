package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.config.server.AttachmentConfig
import com.atsuishio.superbwarfare.data.attachment.AttachmentAdjustments
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.subdata.Attachment.Companion.NO_LASER_COLOR
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation

/**
 * Manages attachment slot data for a single [GunData] instance.
 *
 * Every write through [set] increments structural version so that
 * PMC calculations are invalidated and rebuilt lazily on demand.
 *
 * @param gun the owning [GunData] instance.
 */
class Attachment(private val gun: GunData) {

    private val attachment = gun.attachment()

    /**
     * Legacy integer slot accessor kept for existing renderers and item code.
     *
     * @param type the attachment slot to query.
     * @return slot index, or 0 if empty.
     */
    fun get(type: AttachmentType): Int = attachment.getInt(type.attachmentName)

    /**
     * Legacy integer writer kept for code that still uses old attachment ids.
     *
     * @param type slot to modify.
     * @param value legacy slot index.
     */
    fun set(type: AttachmentType, value: Int) {
        if (attachment.getInt(type.attachmentName) == value) return  // no-op: unchanged
        attachment.putInt(type.attachmentName, value)
        gun.invalidateProperties()
    }

    /**
     * Returns the registered attachment id installed in [type].
     */
    fun id(type: AttachmentType): ResourceLocation? {
        if (type == AttachmentType.SCOPE) {
            val stacked = scopeStack()
            if (stacked.isNotEmpty()) return stacked.last().id
        }
        val tag = attachment.get(type.attachmentName) ?: return null
        return when (tag.id) {
            Tag.TAG_STRING -> ResourceLocation.tryParse(attachment.getString(type.attachmentName))
            Tag.TAG_COMPOUND -> ResourceLocation.tryParse(
                attachment.getCompound(type.attachmentName).readId()
            )

            else -> null
        }
    }

    fun scopeStack(): List<ScopeStackEntry> {
        val root = getTag(AttachmentType.SCOPE) ?: return emptyList()
        val entries = ArrayList<ScopeStackEntry>()
        var tag: CompoundTag? = root
        while (tag != null && entries.size < MAX_STACK_DEPTH) {
            val id = tag.readId().takeIf { it.isNotBlank() }?.let(ResourceLocation::tryParse) ?: break
            entries += ScopeStackEntry(
                id,
                tag,
                AttachmentDefinition.from(id),
                tag.getString(AttachmentDefinition.CURRENT_SKIN_ID).trim()
            )
            tag = tag.getCompound(NESTED_KEY).takeIf { it.contains("Id") || it.contains("Name") }
        }
        return entries
    }

    fun acceptsStacked(type: AttachmentType): Boolean =
        type == AttachmentType.SCOPE && scopeStack().lastOrNull()?.definition?.acceptsScope == true

    private fun effectiveTag(type: AttachmentType): CompoundTag? {
        if (type == AttachmentType.SCOPE) {
            val stacked = scopeStack()
            if (stacked.isNotEmpty()) return stacked.last().tag
        }
        return getTag(type)
    }

    private fun getOrCreateEffectiveTag(type: AttachmentType): CompoundTag {
        if (type == AttachmentType.SCOPE) {
            val root = getOrCreateTag(type)
            return scopeStack().lastOrNull()?.tag ?: root
        }
        return getOrCreateTag(type)
    }

    /**
     * Returns the persisted per-instance NBT tag for [type], if present.
     */
    private fun getTag(type: AttachmentType): CompoundTag? {
        val tag = attachment.get(type.attachmentName) ?: return null
        return when (tag.id) {
            Tag.TAG_COMPOUND -> attachment.getCompound(type.attachmentName)
            Tag.TAG_STRING -> CompoundTag().apply {
                putString("Id", attachment.getString(type.attachmentName))
            }

            else -> null
        }
    }

    fun getOrCreateTag(type: AttachmentType): CompoundTag {
        val name = type.attachmentName
        val current = attachment.get(name)
        val tag = when (current?.id) {
            Tag.TAG_COMPOUND -> attachment.getCompound(name)
            Tag.TAG_STRING -> CompoundTag().apply {
                putString("Id", attachment.getString(name))
            }

            else -> CompoundTag()
        }
        attachment.put(name, tag)
        return tag
    }

    /**
     * **只读**版：槽位里本来就是 compound 时返回它（活引用），否则返回 `null`。
     *
     * 与 [getOrCreateTag] 的区别是它**绝不物化**字符串形式的槽位、也**不写回**任何东西。
     *
     * 副武器运行时需要这个：客户端的主手物品每个 tick 都会被换成一份**新的栈对象**，
     * 而那份栈上的槽位内容在没被实体化之前一直是字符串 —— 用 [getOrCreateTag] 就会
     * **每帧 materialize 出一个游离的空 compound**，拿它去折进缓存那份就等于每帧清空一次
     * 副武器状态（四期踩过的坑，见 `SubWeaponRuntime.installed` 的注释）。
     * 既然我们手里那份已经是完整的活引用，就没有理由每帧去枪上重新造一个。
     */
    fun getTagIfCompound(type: AttachmentType): CompoundTag? =
        if (attachment.get(type.attachmentName)?.id == Tag.TAG_COMPOUND) {
            attachment.getCompound(type.attachmentName)
        } else {
            null
        }

    /**
     * 把 [type] 槽位的内容直接换成 [tag]（**同一个对象**，不复制）。
     *
     * 只有副武器运行时用得上：它的枪械状态住在附件子 tag 上，而 `GunData` 构造时会把根 tag
     * 与它的子 compound 全部捕获成 `val`。主武器同步为新栈后，槽位 compound 是新的副本，
     * 副武器运行时则可能仍持有原先的 compound。
     *
     * 所以装配副武器时把手里那份重新挂回槽位，让 tag 引用全程不变 ——
     * 详情见 `SubWeaponRuntime.installed`。
     */
    fun setTag(type: AttachmentType, tag: CompoundTag) {
        if (attachment.get(type.attachmentName) === tag) return
        attachment.put(type.attachmentName, tag)
    }

    fun has(type: AttachmentType): Boolean = id(type) != null

    /**
     * [type] 装到这把枪上时，是否与**已安装的其它槽位**互斥（[AttachmentSlots.conflicts]）。
     *
     * 互斥有两个来源：**挂点组相同**（槽位在 `AttachmentSlots` 里登记，可用
     * [AttachmentDefinition.mount] 覆盖），或**任一方显式点名**了对方
     * （[AttachmentSlot.conflictsWith] / [AttachmentDefinition.conflictsWith]）。
     * 后者用来表达挂点组表达不了的**非传递**规则：副武器排斥刺刀与握把，
     * 而刺刀与握把可以共存（三者共用前段导轨，但只有那两对装不到一起）。
     *
     * 任一方声明了 `AllowSharedMount` 就放行 —— 那个字段本来就是为"转接座"这类叠装件准备的。
     *
     * 服务端配置开了「自由改装模式」（`AttachmentConfig.FREE_ATTACHMENT_MODE`，
     * 或蕴含它的「完全自由改装模式」）时**一律返回 `null`**：挂点组与 `ConflictsWith` 都不再拦人，
     * 同一根枪口挂点上可以同时挂刺刀与消音器，握把 / 刺刀 / 副武器也可以共存。
     *
     * 判定只写在**这一处**：`/sbw attachment`、[com.atsuishio.superbwarfare.data.gun.GunData.availableAttachments]
     * （进而改装界面、指令补全）与 [cycle] 全都经过这个查询，所以配置一开它们自动同步。
     *
     * @return 与它互斥的已装槽位，没有冲突时返回 `null`。
     */
    fun conflict(type: AttachmentType, definition: AttachmentDefinition?): AttachmentType? {
        if (AttachmentConfig.freeAttachmentMode) return null

        for (other in AttachmentType.entries) {
            if (other == type) continue

            val otherId = id(other) ?: continue
            val otherDefinition = AttachmentDefinition.from(otherId) ?: continue
            if (AttachmentSlots.conflicts(type, definition, other, otherDefinition, gun)) return other
        }
        return null
    }

    /**
     * Checks whether any installed attachment declares a built-in bipod.
     *
     * Not limited to the grip slot: any slot may host an attachment with the
     * `Bipod` flag set in its definition.
     *
     * @return `true` when at least one installed attachment has a bipod.
     */
    fun hasBipod(): Boolean = AttachmentType.entries.any { type ->
        val id = id(type) ?: return@any false
        AttachmentDefinition.from(id)?.hasBipod == true
    }

    fun getRotation(type: AttachmentType): Double = getRotation(type, effectiveTag(type))

    fun getRotation(type: AttachmentType, tag: CompoundTag?): Double {
        if (type == AttachmentType.MAGAZINE || type == AttachmentType.CHARM) return 0.0
        if (tag == null || !tag.contains("Rotation")) return 0.0
        val rotation = tag.getDouble("Rotation")
        if (!rotation.isFinite()) return 0.0
        // 兼容旧的 0..360 存档：270° 在新滑块里显示为 -90°，保持原来的朝向。
        val wrapped = rotation % 360.0
        return when {
            wrapped > 180.0 -> wrapped - 360.0
            wrapped < -180.0 -> wrapped + 360.0
            else -> wrapped
        }
    }

    fun setRotation(type: AttachmentType, rotation: Double) {
        if (type == AttachmentType.MAGAZINE || type == AttachmentType.CHARM || !has(type) || !rotation.isFinite()) return

        val value = rotation.coerceIn(AttachmentAdjustments.MIN_ROTATION, AttachmentAdjustments.MAX_ROTATION)
        if (getRotation(type) == value) return

        getOrCreateEffectiveTag(type).putDouble("Rotation", value)
        gun.invalidateProperties()
    }

    fun getOffset(type: AttachmentType): Double = getOffset(type, effectiveTag(type))

    fun getOffset(type: AttachmentType, tag: CompoundTag?): Double {
        if (type == AttachmentType.MAGAZINE) return 0.0
        if (tag == null || !tag.contains("Offset")) return 0.0
        val offset = tag.getDouble("Offset").takeIf { it.isFinite() } ?: 0.0
        val range = AttachmentAdjustments.offsetRange(gun, type)
        return offset.coerceIn(range.start, range.endInclusive)
    }

    fun setOffset(type: AttachmentType, offset: Double) {
        if (type == AttachmentType.MAGAZINE || !has(type) || !offset.isFinite()) return
        val range = AttachmentAdjustments.offsetRange(gun, type)
        val value = offset.coerceIn(range.start, range.endInclusive)
        // 比较原始存档值：读侧钳制后的相等不能阻止把旧的越界值真正修正回 NBT。
        if ((effectiveTag(type)?.getDouble("Offset") ?: 0.0) == value) return

        getOrCreateEffectiveTag(type).putDouble("Offset", value)
        gun.invalidateProperties()
    }

    fun getSkin(type: AttachmentType): String {
        val tag = effectiveTag(type) ?: return ""
        return tag.getString(AttachmentDefinition.CURRENT_SKIN_ID).trim()
    }

    fun setSkin(type: AttachmentType, skinId: String?) {
        if (!has(type)) return

        val value = skinId?.trim().orEmpty()
        if (getSkin(type) == value) return

        getOrCreateEffectiveTag(type).putString(AttachmentDefinition.CURRENT_SKIN_ID, value)
        gun.invalidateProperties()
    }

    /**
     * 读该槽位激光颜色的覆盖值。
     *
     * 与 [getRotation] / [getOffset] 同契约：**只读、不物化** ——
     * 槽位还是旧字符串形式、或没有 `LaserColor` 键时返回 [NO_LASER_COLOR]，绝不 `getOrCreateTag`。
     * 负数与带 alpha 的 ARGB 都当"没设过"，非法值一律在读侧丢弃。
     */
    fun getLaserColor(type: AttachmentType): Int {
        val tag = getTag(type) ?: return NO_LASER_COLOR
        if (!tag.contains("LaserColor")) return NO_LASER_COLOR

        val color = tag.getInt("LaserColor")
        return if (color in 0..LASER_COLOR_MASK) color else NO_LASER_COLOR
    }

    /**
     * 写该槽位激光颜色的覆盖值。
     *
     * 传 `null` 或负数表示清掉覆盖：走 `getTag(...).remove(...)` 让键消失，而不是留一个 `-1`。
     * **不调 `gun.invalidateProperties()`** —— 颜色是纯表现量，不参与 PMC。
     */
    fun setLaserColor(type: AttachmentType, color: Int?) {
        if (!has(type)) return

        if (color == null || color < 0) {
            if (getLaserColor(type) == NO_LASER_COLOR) return
            getTag(type)?.remove("LaserColor")
        } else {
            val value = color and LASER_COLOR_MASK
            if (getLaserColor(type) == value) return
            getOrCreateTag(type).putInt("LaserColor", value)
        }
    }

    fun set(type: AttachmentType, id: ResourceLocation?): Boolean {
        if (id == null) {
            remove(type)
            return false
        }
        if (type == AttachmentType.SCOPE) {
            if (stackOnto(id)) return true
            if (acceptsStacked(type)) return false
        }
        if (id(type) == id) return false

        attachment.put(type.attachmentName, createTag(id))
        gun.invalidateProperties()
        return false
    }

    fun clear(type: AttachmentType) {
        if (!attachment.contains(type.attachmentName)) return
        attachment.remove(type.attachmentName)
        gun.invalidateProperties()
    }

    fun remove(type: AttachmentType) {
        if (type == AttachmentType.SCOPE && pop(type)) return
        clear(type)
    }

    fun pop(type: AttachmentType): Boolean {
        if (type != AttachmentType.SCOPE) return false
        val stacked = scopeStack()
        if (stacked.size <= 1) return false
        stacked[stacked.size - 2].tag.remove(NESTED_KEY)
        gun.invalidateProperties()
        return true
    }

    private fun stackOnto(id: ResourceLocation): Boolean {
        val stacked = scopeStack()
        val top = stacked.lastOrNull() ?: return false
        if (top.definition?.acceptsScope == true) {
            if (AttachmentDefinition.from(id)?.canMountOnRaiser == false) return false
            if (stacked.size >= MAX_STACK_DEPTH) return false
            top.tag.put(NESTED_KEY, createTag(id))
            gun.invalidateProperties()
            return true
        }
        if (stacked.size > 1) {
            if (top.id == id) return false
            stacked[stacked.size - 2].tag.put(NESTED_KEY, createTag(id))
            gun.invalidateProperties()
        }
        return false
    }

    private fun createTag(id: ResourceLocation): CompoundTag =
        CompoundTag().apply {
            putString("Id", id.toString())
            AttachmentDefinition.from(id)?.scopeZoom(0)?.let { putDouble("Zoom", it.default) }
        }

    fun cycle(type: AttachmentType, add: Boolean): Boolean {
        val allowed = gun.availableAttachments(type)
        if (allowed.isEmpty()) return false

        val optionCount = allowed.size + 1
        val currentIndex = allowed.indexOf(id(type)) + 1
        val nextIndex = if (add) {
            (currentIndex + 1) % optionCount
        } else {
            (currentIndex - 1 + optionCount) % optionCount
        }

        if (nextIndex == 0) {
            set(type, null)
        } else {
            set(type, allowed[nextIndex - 1])
        }
        return true
    }

    fun getZoom(type: AttachmentType): Double? {
        val tag = effectiveTag(type) ?: return null
        if (!tag.contains("Zoom")) return null
        return tag.getDouble("Zoom")
    }

    fun setZoom(type: AttachmentType, zoom: Double) {
        getOrCreateEffectiveTag(type).putDouble("Zoom", zoom)
        gun.invalidateProperties()
    }

    fun scopeMode(type: AttachmentType): Int {
        val tag = effectiveTag(type) ?: return 0
        return if (tag.contains("Mode")) tag.getInt("Mode").coerceAtLeast(0) else 0
    }

    fun setScopeMode(type: AttachmentType, mode: Int) {
        getOrCreateEffectiveTag(type).putInt("Mode", mode.coerceAtLeast(0))
        gun.invalidateProperties()
    }

    fun cycleScopeMode(type: AttachmentType, scroll: Double): Int {
        val id = id(type) ?: return 0
        val definition = AttachmentDefinition.from(id) ?: return 0
        if (!definition.supportsScopeSwitching()) return scopeMode(type)

        val count = definition.scopeInfo?.modeCount() ?: 1
        val current = scopeMode(type)
        val direction = if (scroll >= 0) 1 else -1
        val next = ((current + direction) % count + count) % count

        val tag = getOrCreateEffectiveTag(type)
        tag.putInt("Mode", next)
        definition.scopeZoom(next)?.let { tag.putDouble("Zoom", it.default) }
        gun.invalidateProperties()
        return next
    }

    /**
     * Advances the installed scope's zoom by [amount] scroll notches.
     *
     * The step is proportional to the current zoom, so zooming is fine-grained at low
     * magnification and accelerates as magnification increases.
     *
     * @param type the attachment slot to adjust.
     * @param amount scroll direction and count (typically ±1 per notch).
     * @return the new zoom value, or null when the scope has no configurable zoom.
     */
    fun cycleZoom(type: AttachmentType, amount: Double): Double? {
        val id = id(type) ?: return null
        val definition = AttachmentDefinition.from(id) ?: return null
        val zoomConfig = definition.scopeZoom(scopeMode(type)) ?: return null

        val current = getZoom(type) ?: zoomConfig.default
        val next = (current * (1.0 + amount * zoomConfig.step)).coerceIn(zoomConfig.min, zoomConfig.max)
        setZoom(type, next)
        return next
    }

    fun installed(): List<AttachmentInstance> {
        val result = mutableListOf<AttachmentInstance>()
        for (type in AttachmentType.entries) {
            if (type == AttachmentType.SCOPE) {
                for (entry in scopeStack()) {
                    val definition = entry.definition ?: continue
                    if (type !in definition.acceptedSlots) continue
                    result += AttachmentInstance(type, entry.id, entry.tag, definition)
                }
                continue
            }
            val id = id(type) ?: continue
            val definition = AttachmentDefinition.from(id) ?: continue
            // 装进来的槽位必须在这件配件声明的可装槽位里（多槽位配件见 `AttachmentDefinition.extraSlots`）
            if (type !in definition.acceptedSlots) continue
            val tag = getTag(type) ?: continue
            result += AttachmentInstance(type, id, tag, definition)
        }
        return result
    }

    companion object {
        /** 没设过颜色的哨兵值，回退配件数据里的 `Laser.Color` */
        const val NO_LASER_COLOR: Int = -1

        /** 颜色合法域上限（`0xRRGGBB`，不含 alpha） */
        const val LASER_COLOR_MASK: Int = 0xFFFFFF

        /** 叠装层挂在下一层槽位 tag 里的键名 */
        const val NESTED_KEY: String = "OnTop"

        /** 叠装层数上限，防止损坏的存档把遍历拖成无底洞 */
        const val MAX_STACK_DEPTH: Int = 32
    }
}

data class AttachmentInstance(
    val slot: AttachmentType,
    val id: ResourceLocation,
    val tag: CompoundTag,
    val definition: AttachmentDefinition,
)

data class ScopeStackEntry(
    val id: ResourceLocation,
    val tag: CompoundTag,
    val definition: AttachmentDefinition?,
    val skinId: String,
)

private fun CompoundTag.readId(): String {
    val id = getString("Id")
    return id.ifBlank { getString("Name") }
}
