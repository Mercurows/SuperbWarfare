@file:JvmName("MinecraftUtil")

package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod.Companion.queueClientWork
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import net.minecraft.client.Minecraft
import net.minecraft.client.Options
import net.minecraft.client.gui.Font
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.Event
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.registries.DeferredHolder
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
infix fun ItemStack.sameWith(that: ItemStack?): Boolean {
    return if (that == null) false
    else ItemStack.isSameItemSameComponents(this, that)
}

fun Player.sendPacket(packet: CustomPacketPayload) = sendPacketTo(this, packet)
fun Player.sendPacket(packet: Packet<*>) = sendPacketTo(this, packet)

fun sendPacketTo(player: Player, packet: Packet<*>) {
    if (player !is ServerPlayer) return
    player.connection.send(packet)
}

fun sendPacketTo(player: Player, packet: CustomPacketPayload) {
    if (player !is ServerPlayer) return

    PacketDistributor.sendToPlayer(player, packet)
}

fun sendPacketToAll(packet: CustomPacketPayload) {
    PacketDistributor.sendToAllPlayers(packet)
}

fun sendPacketToServer(packet: CustomPacketPayload) {
    PacketDistributor.sendToServer(packet)
}

fun sendPacketToTrackingEntity(entity: Entity, packet: CustomPacketPayload) {
    PacketDistributor.sendToPlayersTrackingEntity(entity, packet)
}

fun Entity.sendPacketToTrackingThis(packet: CustomPacketPayload) {
    sendPacketToTrackingEntity(this, packet)
}

fun <T : Event> postEvent(event: T): T = NeoForge.EVENT_BUS.post(event)

inline fun queueClientWorkIfDelayed(delay: Int, crossinline block: () -> Unit) {
    if (delay > 0) {
        queueClientWork(delay) { block() }
    } else {
        block()
    }
}

fun ItemStack.`is`(vararg itemsRegistry: DeferredHolder<Item, out Item>): Boolean {
    return itemsRegistry.any { `is`(it.value()) }
}

fun ItemStack.`is`(vararg items: Item): Boolean {
    return items.any { `is`(it) }
}

// 1.20 compat

fun ItemStack.getOrCreateTag(): CompoundTag = NBTTool.getTag(this)
var ItemStack.tag
    get() = get(DataComponents.CUSTOM_DATA)?.copyTag()
    set(value) {
        if (value == null) {
            remove(DataComponents.CUSTOM_DATA)
        } else {
            set(DataComponents.CUSTOM_DATA, CustomData.of(value))
        }
    }

fun LivingEntity.hasEffect(effect: MobEffect) = hasEffect(Holder.direct(effect))
val ItemStack.isEdible get() = this.get(DataComponents.FOOD) != null
fun Player.getEntityReach() = entityInteractionRange()
fun Player.getBlockReach() = blockInteractionRange()
val Minecraft.deltaFrameTime get() = timer.gameTimeDeltaTicks

fun ItemStack.hasCustomHoverName() = this.has(DataComponents.CUSTOM_NAME)