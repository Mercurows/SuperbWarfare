package com.atsuishio.superbwarfare.capability

import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.network.message.receive.PlayerVariablesSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilitySerializable
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber

@EventBusSubscriber
object CapabilityHandler {
    @SubscribeEvent
    fun registerCapabilities(event: AttachCapabilitiesEvent<Entity>) {
        val entity = event.getObject()
        if (entity is LivingEntity) {
            event.addCapability(
                PhosphorusFireCapability.ID,
                createProvider(
                    LazyOptional.of { PhosphorusFireCapability() },
                    ModCapabilities.PHOSPHORUS_FIRE_CAPABILITY
                )
            )
        }

        if (entity !is Player) return

        if (entity !is FakePlayer) {
            event.addCapability(
                PlayerVariable.ID,
                createProvider(
                    LazyOptional.of { PlayerVariable() },
                    ModCapabilities.PLAYER_VARIABLE
                )
            )
        }
    }

    @SubscribeEvent
    fun onPlayerLoggedIn(event: PlayerLoggedInEvent) {
        val player = event.entity
        if (player !is ServerPlayer) return

        player.sendPacket(PlayerVariablesSyncMessage(player.id, PlayerVariable.getOrDefault(player).compareAndUpdate()))
    }

    @SubscribeEvent
    fun onPlayerRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        val player = event.entity
        if (player !is ServerPlayer) return

        player.sendPacket(PlayerVariablesSyncMessage(player.id, PlayerVariable.getOrDefault(player).compareAndUpdate()))
    }

    @SubscribeEvent
    fun onPlayerChangeDimension(event: PlayerChangedDimensionEvent) {
        val player = event.entity
        if (player !is ServerPlayer) return

        player.sendPacket(PlayerVariablesSyncMessage(player.id, PlayerVariable.getOrDefault(player).forceUpdate()))
    }

    @SubscribeEvent
    fun clonePlayer(event: PlayerEvent.Clone) {
        event.original.revive()
        if (event.entity.level().isClientSide()) return

        val original = PlayerVariable.getOrDefault(event.original)
        val clone = event.entity.getCapability(ModCapabilities.PLAYER_VARIABLE, null)
            .orElse(PlayerVariable())

        for (type in Ammo.entries) {
            type.set(clone, type.get(original))
        }

        clone.activeThermalImaging = original.activeThermalImaging

        val player = event.entity
        if (player.level().isClientSide()) return

        player.getCapability(ModCapabilities.PLAYER_VARIABLE, null)
            .orElse(PlayerVariable())
            .sync(player)
    }

    fun <T : INBTSerializable<CompoundTag>> createProvider(
        instance: LazyOptional<T>,
        capability: Capability<T>,
    ): ICapabilitySerializable<CompoundTag> {
        return object : ICapabilitySerializable<CompoundTag> {
            override fun <C> getCapability(cap: Capability<C>, side: Direction?) =
                capability.orEmpty(cap, instance.cast())

            override fun serializeNBT(): CompoundTag {
                return instance.orElseThrow { NullPointerException() }
                    .serializeNBT()
            }

            override fun deserializeNBT(nbt: CompoundTag) {
                instance.orElseThrow { NullPointerException() }
                    .deserializeNBT(nbt)
            }
        }
    }
}
