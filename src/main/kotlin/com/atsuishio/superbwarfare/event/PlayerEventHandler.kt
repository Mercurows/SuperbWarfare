package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.config.common.GameplayConfig
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.InventoryTool
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.TraceTool
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.AnvilUpdateEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import kotlin.math.ceil

@Mod.EventBusSubscriber
object PlayerEventHandler {
    @SubscribeEvent
    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity
        val stack = player.mainHandItem
        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")) {
            stack.getOrCreateTag().putBoolean("Using", false)
        }
    }

    @SubscribeEvent
    fun onPlayerRespawned(event: PlayerEvent.PlayerRespawnEvent) {
        val player = event.entity ?: return

        handleRespawnReload(player)
        handleRespawnAutoArmor(player)
    }

    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        val player = event.player ?: return
        val stack = player.mainHandItem

        if (event.phase == TickEvent.Phase.END) {
            if (stack.item is GunItem) {
                handleSpecialWeaponAmmo(player)
            }
        }
    }

    private fun handleSpecialWeaponAmmo(player: Player) {
        val stack = player.mainHandItem
        val data = GunData.from(stack)

        if ((stack.`is`(ModItems.RPG.get()) || stack.`is`(ModItems.BOCEK.get())) && data.hasEnoughAmmoToShoot(player)) {
            data.isEmpty.set(false)
        }
    }

    private fun handleRespawnReload(player: Player) {
        if (!GameplayConfig.RESPAWN_RELOAD.get()) return

        for (stack in player.inventory.items) {
            if (stack.item is GunItem) {
                val data = GunData.from(stack)
                if (!InventoryTool.hasCreativeAmmoBox(player)) {
                    data.reloadAmmo(player)
                } else {
                    data.ammo.set(data.get(GunProp.MAGAZINE))
                }
                data.holdOpen.set(false)
                data.save()
            }
        }
    }

    private fun handleRespawnAutoArmor(player: Player) {
        if (!GameplayConfig.RESPAWN_AUTO_ARMOR.get()) return

        val armor = player.getItemBySlot(EquipmentSlot.CHEST)
        if (armor == ItemStack.EMPTY) return

        val armorPlate = armor.getOrCreateTag().getDouble("ArmorPlate")

        var armorLevel = MiscConfig.DEFAULT_ARMOR_LEVEL.get()
        if (armor.`is`(ModTags.Items.MILITARY_ARMOR)) {
            armorLevel = MiscConfig.MILITARY_ARMOR_LEVEL.get()
        } else if (armor.`is`(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
            armorLevel = MiscConfig.HEAVY_MILITARY_ARMOR_LEVEL.get()
        }

        if (armorPlate < armorLevel * MiscConfig.ARMOR_POINT_PER_LEVEL.get()) {
            for (stack in player.inventory.items) {
                if (stack.`is`(ModItems.ARMOR_PLATE.get())) {
                    val tag = stack.tag
                    if (tag != null && tag.getBoolean("Infinite")) {
                        armor.getOrCreateTag()
                            .putDouble("ArmorPlate", armorLevel * MiscConfig.ARMOR_POINT_PER_LEVEL.get().toDouble())

                        if (player is ServerPlayer) {
                            player.level().playSound(
                                null,
                                player.onPos,
                                SoundEvents.ARMOR_EQUIP_IRON,
                                SoundSource.PLAYERS,
                                0.5f,
                                1f
                            )
                        }
                    } else {
                        repeat(
                            ceil(((armorLevel * MiscConfig.ARMOR_POINT_PER_LEVEL.get()) - armorPlate) / MiscConfig.ARMOR_POINT_PER_LEVEL.get()).toInt()
                        ) {
                            stack.finishUsingItem(player.level(), player)
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onAnvilUpdate(event: AnvilUpdateEvent) {
        val left = event.left
        val right = event.right

        if (left.item is GunItem && right.item == ModItems.SHORTCUT_PACK.get()) {
            val output = left.copy()

            val data = GunData.from(output)
            data.level.add(1)
            data.save()

            event.output = output
            event.cost = 10
            event.materialCost = 1
        }
    }

    @SubscribeEvent
    fun onAttackEntity(event: AttackEntityEvent) {
        val target = event.target as? VehicleEntity ?: return
        val position = TraceTool.playerFindLookingPos(event.entity, target, event.entity.getEntityReach()) ?: return

        if (target.shouldSendHitSounds()) {
            target.level()
                .playSound(null, BlockPos.containing(position), ModSounds.HIT.get(), SoundSource.PLAYERS, 1f, 1f)
        }

        val level = target.level()
        if (target.shouldSendHitParticles() && level is ServerLevel) {
            ParticleTool.sendParticle(
                level, ModParticleTypes.FIRE_STAR.get(), position.x, position.y, position.z,
                2, 0.0, 0.0, 0.0, 0.2, false
            )
        }
    }
}