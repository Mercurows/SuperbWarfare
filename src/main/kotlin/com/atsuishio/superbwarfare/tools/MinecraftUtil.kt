package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import net.minecraft.client.Minecraft
import net.minecraft.client.Options
import net.minecraft.client.gui.Font
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.Event
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.network.PacketDistributor
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@get:OnlyIn(Dist.CLIENT)
val mc: Minecraft get() = Minecraft.getInstance()

@get:OnlyIn(Dist.CLIENT)
val localPlayer get() = mc.player

@get:OnlyIn(Dist.CLIENT)
val font: Font get() = mc.font

@get:OnlyIn(Dist.CLIENT)
val options: Options get() = mc.options

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

fun Player.sendPacket(packet: CustomPacketPayload) = sendPacketTo(this, packet)

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