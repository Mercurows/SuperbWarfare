package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.attachment.AttachmentProvider
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.playLocalSound
import kotlinx.serialization.Serializable
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

/**
 * 改装界面的一次"点选"：把 [attachmentId] 装到主手枪械的 [slot] 槽位，或卸下该槽位。
 *
 * [attachmentId] 为空串表示卸下（界面上那个画了斜杠的"留空"格）。
 *
 * 与旧界面走 [EditMessage] 的"在可用列表里循环"不同，这里是**按件交换**：
 * 生存模式上装时从背包扣掉一个目标配件，同时把原来那件还给玩家；卸下时把配件还给玩家。
 * 创造模式无需背包持有配件，安装不消耗物品，卸下或替换也不返还物品。
 * 完全自由改装只放宽配件兼容范围，生存模式仍需持有并消耗配件。所有校验都在服务端重做一遍 ——
 * 客户端传来的 id 只是"想装哪个"，能不能装由 `GunData` + `AttachmentSlots` 说了算。
 */
@RegisterPacket
@Serializable
data class AttachmentInstallMessage(
    val slot: String,
    val attachmentId: String,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return

        val type = AttachmentType.entries.firstOrNull { it.attachmentName == slot } ?: return
        val data = GunData.from(stack)

        if (attachmentId.isEmpty()) {
            uninstall(player, data, type)
        } else {
            val id = ResourceLocation.tryParse(attachmentId) ?: return
            install(player, data, type, id)
        }
    }
}

/** 卸下 [type] 槽位的配件，仅在非创造模式返还物品。槽位本来就是空的时什么都不做。 */
private fun uninstall(player: ServerPlayer, data: GunData, type: AttachmentType) {
    val current = data.attachment.id(type) ?: return

    withdrawAmmoIfNeeded(player, data, type)
    data.attachment.remove(type)
    data.save()

    returnItem(player, current)
    player.playLocalSound(ModSounds.ATTACHMENT_REMOVE.get(), 1f, 1f)
}

/**
 * 把 [id] 装到 [type] 槽位。
 *
 * 校验与 `/sbw attachment set` 同源（[GunData.canInstall] 要求配件数据存在、槽位匹配、
 * 不与已装的其它配件抢挂点，并且出现在这把枪的 `AvailableAttachments` 里），
 * 非创造模式另外要求**背包里确实有这么一件**：装上时扣掉一件，原来那件还给玩家。
 */
private fun install(player: ServerPlayer, data: GunData, type: AttachmentType, id: ResourceLocation) {
    val stacking = data.attachment.acceptsStacked(type)
    if (!stacking && data.attachment.id(type) == id) return
    if (!data.canInstall(type, id)) return
    // 先扣背包再动枪：扣不出来（背包里没有 / 已经用完）就整笔取消，不留半成品状态。
    // 创造模式可直接安装，生存模式在任何兼容配置下都需要背包中的配件。
    if (!player.isCreative && !consumeOneAttachment(player, id)) return

    val previous = data.attachment.id(type)
    withdrawAmmoIfNeeded(player, data, type)
    val stacked = data.attachment.set(type, id)
    data.save()

    if (!stacked) previous?.let { returnItem(player, it) }
    player.playLocalSound(ModSounds.ATTACHMENT_SET.get(), 1f, 1f)
}

/** 换这个槽位的配件前先把已装填的弹药退还给玩家（目前只有弹匣槽位需要）。 */
private fun withdrawAmmoIfNeeded(player: ServerPlayer, data: GunData, type: AttachmentType) {
    if (AttachmentSlots.ofOrNull(type)?.withdrawAmmoOnChange == true) {
        data.withdrawAmmo(player)
    }
}

/** 从玩家背包（含快捷栏与副手）扣掉一件 [id] 配件；没有则返回 false，调用方据此取消整笔操作。 */
private fun consumeOneAttachment(player: ServerPlayer, id: ResourceLocation): Boolean {
    val inventory = player.inventory

    for (stack in inventory.items) {
        if (!isAttachment(stack, id)) continue
        stack.shrink(1)
        return true
    }
    for (stack in inventory.offhand) {
        if (!isAttachment(stack, id)) continue
        stack.shrink(1)
        return true
    }
    return false
}

private fun isAttachment(stack: ItemStack, id: ResourceLocation): Boolean {
    if (stack.isEmpty) return false
    val provider = stack.item as? AttachmentProvider ?: return false
    return ResourceLocation.tryParse(provider.attachmentId) == id
}

/** 非创造模式返还配件物品；背包塞不下就掉在脚下。 */
private fun returnItem(player: ServerPlayer, id: ResourceLocation) {
    if (player.isCreative) return
    val item = BuiltInRegistries.ITEM.get(id)
    val stack = ItemStack(item)
    if (!player.addItem(stack)) {
        player.drop(stack, false)
    }
}
