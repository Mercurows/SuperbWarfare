package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.ShootParameters
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData
import com.atsuishio.superbwarfare.data.vehicle.VehicleData
import com.atsuishio.superbwarfare.data.vehicle.subdata.DestroyInfo
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Math

open class Tom6Entity(type: EntityType<Tom6Entity>, world: Level) : VehicleEntity(type, world) {
    val hasMelon
        get() = weaponData?.hasEnoughAmmoToShoot(this) ?: false

    override fun computeProperties(data: VehicleData, rawData: DefaultVehicleData): DefaultVehicleData {
        if (hasMelon) {
            rawData.destroyInfo = DestroyInfo(
                rawData.destroyInfo.crashPassengers,
                rawData.destroyInfo.explodePassengers,
                rawData.destroyInfo.explodeBlocks,
                this.melonExplosionDamage,
                this.melonExplosionRadius
            )
        }
        return super.computeProperties(data, rawData)
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val data = weaponData ?: return super.interact(player, hand)

        if (hasMelon) {
            return super.interact(player, hand)
        } else {
            val stack = player.mainHandItem
            if (!data.selectedAmmoConsumer().isAmmoItem(stack)) {
                return super.interact(player, hand)
            }

            val level = level()
            if (level is ServerLevel) {
                modifyGunData("MelonBomb") { data -> data.reloadAmmo(player) }
                level.playSound(player, this.onPos, SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1f, 1f)
            }

            return InteractionResult.SUCCESS
        }
    }

    override fun baseTick() {
        super.baseTick()
        val passenger = getFirstPassenger()
        // 空格投掷西瓜炸弹
        if (upInputDown && !onGround() && hasMelon && passenger is Player) {
            val transform = getVehicleTransform(1f)
            val worldPosition = transformPosition(transform, 0.0, 0.3, 0.0)

            val level = this.level() as? ServerLevel ?: return

            modifyGunData("MelonBomb") { data ->
                data.shoot(
                    ShootParameters(
                        this,
                        passenger,
                        level,
                        Vec3(worldPosition.x, worldPosition.y, worldPosition.z),
                        deltaMovement,
                        data,
                        0.0,
                        false,
                        null,
                        null
                    )
                )
            }

            this.level().playSound(null, onPos, SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1f, 1f)
            upInputDown = false
        }
    }

    val weaponData
        get() = getGunData("MelonBomb")

    val melonExplosionDamage
        get() = weaponData?.get(GunProp.EXPLOSION_DAMAGE)?.toFloat() ?: 0f

    val melonExplosionRadius
        get() = weaponData?.get(GunProp.EXPLOSION_RADIUS)?.toFloat() ?: 0f

    override fun engineRunning() =
        getFirstPassenger() != null && Math.abs(deltaMovement.length()) > 0

    override fun getEngineSoundVolume() =
        deltaMovement.length().toFloat()

    override fun getSensitivity(original: Double, zoom: Boolean, seatIndex: Int, isOnGround: Boolean) =
        if (ModKeyMappings.FREE_CAMERA.isDown) 0.0 else 0.6

    override fun useAircraftCamera(seatIndex: Int) =
        ModKeyMappings.FREE_CAMERA.isDown && !ClientEventHandler.zoom

    override val mouseSensitivity
        get() = if (ModKeyMappings.FREE_CAMERA.isDown) 0.3 else 0.0

}
