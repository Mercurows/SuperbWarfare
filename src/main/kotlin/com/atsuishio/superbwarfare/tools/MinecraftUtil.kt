@file:JvmName("MinecraftUtil")

package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod.Companion.queueClientWork
import com.atsuishio.superbwarfare.network.NetworkRegistry
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import net.minecraft.client.Minecraft
import net.minecraft.client.Options
import net.minecraft.client.gui.Font
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.registries.RegistryObject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@get:OnlyIn(Dist.CLIENT)
val mc: Minecraft get() = Minecraft.getInstance()

@get:OnlyIn(Dist.CLIENT)
val localPlayer get() = mc.player

@get:OnlyIn(Dist.CLIENT)
val clientLevel get() = mc.level

@get:OnlyIn(Dist.CLIENT)
val font: Font get() = mc.font

@get:OnlyIn(Dist.CLIENT)
val options: Options get() = mc.options

@get:OnlyIn(Dist.CLIENT)
val notInGame: Boolean
    get() {
        if (mc.player == null) return true
        if (mc.overlay != null) return true
        if (mc.screen != null) return true
        if (!mc.mouseHandler.isMouseGrabbed) return true
        return !mc.isWindowActive
    }

operator fun BlockPos.component1() = this.x
operator fun BlockPos.component2() = this.y
operator fun BlockPos.component3() = this.z

operator fun MutableComponent.plus(other: Component): MutableComponent = this.append(other)
operator fun MutableComponent.plus(other: String): MutableComponent = this.append(Component.literal(other))

@OptIn(ExperimentalContracts::class)
fun Player?.isNullOrSpector(): Boolean {
    contract {
        returns(false) implies (this@isNullOrSpector != null)
    }

    return this == null || this.isSpectator
}

fun Vec3?.toFormattedString(): String {
    if (this == null) return "[ ---, ---, --- ]"
    return "[ " + format0D(x) + ", " + format0D(y) + ", " + format0D(z) + " ]"
}

fun isSameItemStack(a: ItemStack, b: ItemStack) = a sameWith b

// 为空tag添加特判后的比较，专治乱用getOrCreateTag（恼）
infix fun ItemStack.sameWith(that: ItemStack?): Boolean {
    if (that == null) return false
    if (this.tag == null && that.hasEmptyTag() || that.tag == null && this.hasEmptyTag()) {
        val a = this.copy().apply { tag = null }
        val b = that.copy().apply { tag = null }

        return ItemStack.isSameItemSameTags(a, b)
    }

    return ItemStack.isSameItemSameTags(this, that)
}

// 判断是否tag不为null且内容为空
private fun ItemStack.hasEmptyTag() = this.tag?.isEmpty ?: false

// Network
fun Player.sendPacket(packet: Any) = sendPacketTo(this, packet)

fun sendPacketTo(player: Player, packet: Any) {
    if (player !is ServerPlayer) return
    if (packet is Packet<*>) {
        player.connection.send(packet)
    } else {
        NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with { player }, packet)
    }
}

fun sendPacketTo(target: PacketDistributor.PacketTarget, packet: Any) {
    NetworkRegistry.PACKET_HANDLER.send(target, packet)
}

fun sendPacketToAll(packet: Any) {
    NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), packet)
}

fun sendPacketToServer(packet: Any) {
    NetworkRegistry.PACKET_HANDLER.sendToServer(packet)
}

fun sendPacketToTrackingEntity(entity: Entity, packet: Any) {
    NetworkRegistry.PACKET_HANDLER.send(
        PacketDistributor.TRACKING_ENTITY.with { entity },
        packet
    )
}

fun Entity.sendPacketToTrackingThis(packet: Any) {
    sendPacketToTrackingEntity(this, packet)
}

fun <T : Event> postEvent(event: T) = MinecraftForge.EVENT_BUS.post(event)

inline fun queueClientWorkIfDelayed(delay: Int, crossinline block: () -> Unit) {
    if (delay > 0) {
        queueClientWork(delay) { block() }
    } else {
        block()
    }
}

fun ItemStack.`is`(vararg itemsRegistry: RegistryObject<Item>): Boolean {
    return itemsRegistry.any { `is`(it.get()) }
}

fun ItemStack.`is`(vararg items: Item): Boolean {
    return items.any { `is`(it) }
}