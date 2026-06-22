package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.FormatTool.format1DZ
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.unaryMinus
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Math
import java.util.*

class TowEntity(type: EntityType<TowEntity>, world: Level) : VehicleEntity(type, world) {

    override fun defineSynchedData() {
        super.defineSynchedData()

        with(entityData) {
            define(LOADED, false)
            define(RELOAD_COOLDOWN, 0)
        }
    }

    var loaded by LOADED
    var reloadCooldown by RELOAD_COOLDOWN

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putBoolean("State", loaded)
        compound.putInt("ReloadCoolDown", reloadCooldown)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        loaded = compound.getBoolean("State")
        reloadCooldown = compound.getInt("ReloadCoolDown")
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val gunData = getGunData(0) ?: return InteractionResult.SUCCESS

        val coolDown = Math.ceil(20f / (vehicleWeaponRpm(0).toFloat() / 60)).toInt()

        val stack = player.mainHandItem
        if (gunData.hasEnoughAmmoToShoot(player)) {
            loaded = true
            return super.interact(player, hand)
        }

        if (!loaded) {
            if (!gunData.selectedAmmoConsumer().isAmmoItem(stack)) {
                return super.interact(player, hand)
            }

            val level = level()
            if (level is ServerLevel && reloadCooldown == 0) {
                modifyGunData(0) { data -> data.reloadAmmo(player) }

                loaded = true
                level.playSound(
                    null,
                    onPos,
                    ModSounds.TYPE_63_RELOAD.get(),
                    SoundSource.PLAYERS,
                    1f,
                    random.nextFloat() * 0.1f + 0.9f
                )
            } else {
                player.displayClientMessage(
                    Component.literal(
                        format1DZ((coolDown - reloadCooldown).toDouble() / 20) + " / " + format1DZ(
                            coolDown.toDouble() / 20
                        )
                    ), true
                )
            }
        } else {
            loaded = false
        }
        return InteractionResult.SUCCESS
    }

    override fun baseTick() {
        super.baseTick()
        if (reloadCooldown > 0) {
            reloadCooldown--
        }
    }

    override fun vehicleShoot(living: LivingEntity?, uuid: UUID?, targetPos: Vec3?) {
        super.vehicleShoot(living, uuid, targetPos)

        val barrelVector = getBarrelVector(1f)
        val pos = getShootPos(living, 1f).add(barrelVector.scale(-0.5))
        val ab = AABB(pos, pos).inflate(0.75).move(barrelVector.scale(-2.0)).expandTowards(barrelVector.scale(-5.0))
        val coolDown = Math.ceil(20f / (vehicleWeaponRpm(0).toFloat() / 60)).toInt()
        reloadCooldown = coolDown

        // 尾焰伤害
        for (entity in level().getEntities(
            EntityTypeTest.forClass(Entity::class.java),
            ab
        ) { target -> target !== this && target !== getFirstPassenger() && target.vehicle == null }
        ) {
            entity.hurt(
                ModDamageTypes.causeBurnDamage(entity.level().registryAccess(), living),
                30 - 2 * entity.distanceTo(this)
            )
            val force = 4 - 0.7 * entity.distanceTo(this)
            entity.push(-force * barrelVector.x, -force * barrelVector.y, -force * barrelVector.z)
        }

        // 粒子效果
        val level = this.level()
        if (level is ServerLevel) {
            ParticleTool.spawnMediumCannonMuzzleParticles(-barrelVector, pos, level, this)
            ParticleTool.spawnMediumCannonMuzzleParticles(barrelVector, pos, level, this)
        }
    }

    override fun destroy() {
        val level = this.level()
        if (level is ServerLevel) {
            val x = this.x
            val y = this.y
            val z = this.z
            level.explode(null, x, y, z, 0f, Level.ExplosionInteraction.NONE)
            val mortar = ItemEntity(level, x, (y + 1), z, ItemStack(ModItems.MORTAR_BARREL.get()))
            mortar.setPickUpDelay(10)
            level.addFreshEntity(mortar)
        }
        super.destroy()
        this.discard()
    }

    companion object {
        // 是否已装填弹药
        @JvmField
        val LOADED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(TowEntity::class.java, EntityDataSerializers.BOOLEAN)
        val RELOAD_COOLDOWN: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(TowEntity::class.java, EntityDataSerializers.INT)
    }
}
