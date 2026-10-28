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
import com.atsuishio.superbwarfare.network.message.send.UseActionMessage
import com.atsuishio.superbwarfare.perk.MeleeAttackContext
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
import net.minecraftforge.event.entity.living.LivingAttackEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.living.LivingSwapItemsEvent
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import kotlin.math.ceil

@Mod.EventBusSubscriber
object PlayerEventHandler {
    private const val MELEE_RESPAWN_UNTIL = "sbwMeleeRespawnUntil"
    private const val MELEE_RESPAWN_GRACE_TICKS = 5L

    /** 服务器世界总 tick，不受立即复活、昼夜规则或玩家实体计数重置影响。 */
    fun isMeleeBlockedAfterRespawn(player: Player): Boolean {
        if (player !is ServerPlayer) return false
        val tag = player.persistentData
        if (!tag.contains(MELEE_RESPAWN_UNTIL)) return false
        if (player.server.overworld().gameTime < tag.getLong(MELEE_RESPAWN_UNTIL)) return true
        tag.remove(MELEE_RESPAWN_UNTIL)
        return false
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun blockRespawnMeleeAttack(event: AttackEntityEvent) {
        val player = event.entity
        if (!isMeleeBlockedAfterRespawn(player)) return
        if (player.mainHandItem.item is GunItem || MeleeAttackContext.get(player.uuid) != null) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun blockRespawnMeleeDamage(event: LivingAttackEvent) {
        val attacker = event.source.entity as? ServerPlayer ?: return
        // 同一近战调用栈里的 Perk 伤害也受限；普通射击不走这个上下文。
        if (MeleeAttackContext.get(attacker.uuid) != null && isMeleeBlockedAfterRespawn(attacker)) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onPlayerDeath(event: LivingDeathEvent) {
        val player = event.entity as? ServerPlayer ?: return

        ChargeWindowHandler.clear(player)
        UseActionMessage.clearChargeCooldown(player)
    }

    // Hands are unusable in this seat, so a swap would only shuffle items (e.g. controller X while locking on)
    @SubscribeEvent
    fun onSwapHands(event: LivingSwapItemsEvent.Hands) {
        val vehicle = event.entity.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(event.entity)) event.isCanceled = true
    }

    @SubscribeEvent
    fun onPlayerRespawned(event: PlayerEvent.PlayerRespawnEvent) {
        val player = event.entity ?: return

        if (player is ServerPlayer && !event.isEndConquered) {
            player.persistentData.putLong(MELEE_RESPAWN_UNTIL,
                player.server.overworld().gameTime + MELEE_RESPAWN_GRACE_TICKS)
        }

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

            DeployedWeaponHandler.serverTick(player)
        }
    }

    @SubscribeEvent
    fun onPlayerChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        DeployedWeaponHandler.undeploy(event.entity)
    }

    private fun handleSpecialWeaponAmmo(player: Player) {
        val stack = player.mainHandItem
        val data = GunData.from(stack)

        if (data.hasEnoughAmmoToShoot(player)) {
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
