package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData
import com.atsuishio.superbwarfare.data.vehicle.VehicleData
import com.atsuishio.superbwarfare.data.vehicle.subdata.DestroyInfo
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import org.joml.Math

class Tom6Entity(type: EntityType<Tom6Entity>, world: Level) : GeoVehicleEntity(type, world) {

    var melon by MELON

    override fun computeProperties(data: VehicleData, rawData: DefaultVehicleData): DefaultVehicleData {
        if (melon) {
            rawData.destroyInfo = DestroyInfo(
                rawData.destroyInfo.crashPassengers,
                rawData.destroyInfo.explodePassengers,
                rawData.destroyInfo.explodeBlocks,
                this.melonExplosionDamage,
                this.melonExplosionRadius,
                ParticleTool.ParticleType.HUGE
            )
        }
        return super.computeProperties(data, rawData)
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(MELON, false)
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putBoolean("Melon", melon)
    }

    public override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        melon = compound.getBoolean("Melon")
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (player.mainHandItem.`is`(Items.MELON) && !melon) {
            melon = true
            player.mainHandItem.shrink(1)
            player.level().playSound(player, this.onPos, SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1f, 1f)
            return InteractionResult.SUCCESS
        }
        return super.interact(player, hand)
    }

    override fun baseTick() {
        super.baseTick()
        val passenger = getFirstPassenger()
        // 空格投掷西瓜炸弹
        if (upInputDown() && !onGround() && melon && passenger is Player) {
            melon = false

            val transform = getVehicleTransform(1f)
            val worldPosition = transformPosition(transform, 0.0, 0.3, 0.0)

            val melonBomb = MelonBombEntity(passenger, passenger.level())
            melonBomb.setExplosionDamage(this.melonExplosionDamage)
            melonBomb.setExplosionRadius(this.melonExplosionRadius)
            melonBomb.setPos(worldPosition.x, worldPosition.y, worldPosition.z)
            melonBomb.shoot(
                deltaMovement.x,
                deltaMovement.y,
                deltaMovement.z,
                deltaMovement.length().toFloat(),
                0f
            )
            passenger.level().addFreshEntity(melonBomb)

            this.level().playSound(null, onPos, SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1f, 1f)
            setUpInputDown(false)
        }
    }

    val melonExplosionDamage: Float
        get() {
            val gunData = getGunData("MelonBomb")
            return if (gunData != null) {
                gunData.compute().explosionDamage.toFloat()
            } else {
                0f
            }
        }

    val melonExplosionRadius: Float
        get() {
            val gunData = getGunData("MelonBomb")
            return if (gunData != null) {
                gunData.compute().explosionRadius.toFloat()
            } else {
                0f
            }
        }

    override fun engineRunning(): Boolean {
        return (getFirstPassenger() != null && Math.abs(deltaMovement.length()) > 0)
    }

    override fun getEngineSoundVolume(): Float {
        return deltaMovement.length().toFloat()
    }

    override fun getSensitivity(original: Double, zoom: Boolean, seatIndex: Int, isOnGround: Boolean): Double {
        return if (ModKeyMappings.FREE_CAMERA.isDown()) 0.0 else 0.6
    }

    override fun useAircraftCamera(seatIndex: Int): Boolean {
        return ModKeyMappings.FREE_CAMERA.isDown() && !ClientEventHandler.zoom
    }

    override fun getMouseSensitivity(): Double {
        return if (ModKeyMappings.FREE_CAMERA.isDown()) 0.3 else 0.0
    }

    companion object {
        @JvmField
        val MELON: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(Tom6Entity::class.java, EntityDataSerializers.BOOLEAN)
    }
}
