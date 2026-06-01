package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.tools.DamageHandler
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.TraceTool
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Math
import java.util.*

class AnnihilatorEntity(type: EntityType<AnnihilatorEntity?>, world: Level) : ArtilleryEntity(type, world),
    BasicGeoVehicleEntity {
    init {
        this.noCulling = true
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(LASER_LEFT_LENGTH, 0f)
            .define(LASER_MIDDLE_LENGTH, 0f)
            .define(LASER_RIGHT_LENGTH, 0f)
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val mainStack = player.mainHandItem
        val offStack = player.offhandItem

        if (mainStack.item === ModItems.FIRING_PARAMETERS.get() && player.isCrouching) {
            setTarget(mainStack)
            return InteractionResult.SUCCESS
        }
        if (offStack.item === ModItems.FIRING_PARAMETERS.get() && player.isCrouching) {
            setTarget(offStack)
            return InteractionResult.SUCCESS
        }

        if (mainStack.`is`(ModTags.Items.TOOLS_CROWBAR) && !player.isCrouching) {
            if (chargeProgress >= 1) {
                vehicleShoot(player, "Main")
            }
            return InteractionResult.SUCCESS
        }
        return super.interact(player, hand)
    }

    fun setTarget(stack: ItemStack) {
        if (this.isWreck) return
        val parameters = stack.firingParameters
        val pos = parameters.pos
        targetPos = pos
    }

    override fun baseTick() {
        super.baseTick()
        if (this.isWreck) return
        val weaponName = "Main"
        val data = getGunData(weaponName)
        if (data != null) {
            val projectileInfo = data.get(GunProp.PROJECTILE)
            val projectileType = projectileInfo.getId()
            val projectileTypeStr = projectileType.trim { it <= ' ' }.lowercase()
            val rpm = Math.ceil(20f / (vehicleWeaponRpm(weaponName).toFloat() / 60)).toInt()

            if (projectileTypeStr == "ray" && chargeProgress < 1 && energy > data.get(GunProp.AMMO_COST_PER_SHOOT)) {
                val chargeSpeed = 1f / rpm
                chargeProgress = Mth.clamp(chargeProgress + chargeSpeed, 0f, 1f)
            }
        }
    }

    private fun laserLength(pos: Vec3, living: LivingEntity?, data: GunData): Float {
        val result = level().clip(
            ClipContext(
                pos, pos.add(getBarrelVector(1f).scale(512.0)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
            )
        )

        val hitPos = result.getLocation()
        val blockPos = result.blockPos

        val hardness = this.level().getBlockState(blockPos).block.defaultDestroyTime()

        if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get() && hardness != -1f) {
            Block.dropResources(this.level().getBlockState(blockPos), this.level(), blockPos, null)
            this.level().destroyBlock(blockPos, true)
        }

        causeLaserExplode(hitPos, data, living)
        this.level().explode(
            living,
            hitPos.x,
            hitPos.y,
            hitPos.z,
            (data.get(GunProp.EXPLOSION_RADIUS) * 0.5f).toFloat(),
            if (ExplosionConfig.EXPLOSION_DESTROY.get()) Level.ExplosionInteraction.BLOCK else Level.ExplosionInteraction.NONE
        )

        return pos.distanceTo(hitPos).toFloat()
    }

    private fun laserLengthEntity(pos: Vec3, living: LivingEntity?, data: GunData): Float {
        if (this.level() is ServerLevel) {
            var distance = (512 * 512).toDouble()
            var hitResult = TraceTool.pickNew(pos, 512.0, getBarrelVector(1f), this)
            if (hitResult!!.type != HitResult.Type.MISS) {
                distance = hitResult.getLocation().distanceToSqr(pos)
                val blockReach = 5.0
                if (distance > blockReach * blockReach) {
                    val posB = hitResult.getLocation()
                    hitResult =
                        BlockHitResult.miss(posB, Direction.getNearest(pos.x, pos.y, pos.z), BlockPos.containing(posB))
                }
            }
            val viewVec = getBarrelVector(1f)
            val toVec = pos.add(viewVec.x * 512, viewVec.y * 512, viewVec.z * 512)
            val aabb = this.boundingBox.expandTowards(viewVec.scale(512.0)).inflate(1.0)
            val result = ProjectileUtil.getEntityHitResult(
                this,
                pos,
                toVec,
                aabb,
                { !it.isSpectator },
                distance
            )
            if (result != null) {
                val targetPos = result.getLocation()
                val distanceToTarget = pos.distanceToSqr(targetPos)
                if (distanceToTarget > distance || distanceToTarget > 512 * 512) {
                    hitResult = BlockHitResult.miss(
                        targetPos,
                        Direction.getNearest(viewVec.x, viewVec.y, viewVec.z),
                        BlockPos.containing(targetPos)
                    )
                } else if (distanceToTarget < distance) {
                    hitResult = result
                }
                if (hitResult!!.type == HitResult.Type.ENTITY) {
                    val passenger = this.getFirstPassenger()
                    val target = (hitResult as EntityHitResult).entity

                    DamageHandler.doDamage(
                        target,
                        ModDamageTypes.causeLaserDamage(this.level().registryAccess(), this, passenger),
                        data.get(GunProp.DAMAGE).toFloat()
                    )
                    target.invulnerableTime = 0
                    causeLaserExplode(targetPos, data, living)
                    return pos.distanceTo(hitResult.getLocation()).toFloat()
                }
            }
        }
        return 512f
    }

    private fun causeLaserExplode(vec3: Vec3, gunData: GunData, living: Entity?) {
        val radius = gunData.get(GunProp.EXPLOSION_RADIUS).toFloat()

        val particleType = if (radius <= 4.0) {
            ParticleTool.ParticleType.SMALL
        } else if (radius > 4.0 && radius < 10.0) {
            ParticleTool.ParticleType.MEDIUM
        } else if (radius in 10.0..<20.0) {
            ParticleTool.ParticleType.HUGE
        } else {
            ParticleTool.ParticleType.GIANT
        }

        createCustomExplosion()
            .damage(gunData.get(GunProp.EXPLOSION_DAMAGE).toFloat())
            .radius(radius)
            .attacker(living)
            .position(vec3)
            .withParticleType(particleType)
            .explode()
    }

    override fun vehicleShoot(living: LivingEntity?, weaponName: String) {
        if (this.isWreck) return
        val data = getGunData(weaponName)
        shoot(living, data)
    }

    override fun vehicleShoot(living: LivingEntity?, uuid: UUID?, targetPos: Vec3?) {
        if (this.isWreck) return
        val data = getGunData(living)
        shoot(living, data)
    }

    fun shoot(living: LivingEntity?, gunData: GunData?) {
        if (gunData == null) return
        if (level() is ServerLevel) {
            chargeProgress = 0f
            this.consumeEnergy(gunData.get(GunProp.AMMO_COST_PER_SHOOT))

            val transform = getBarrelTransform(1f)
            val worldPosition1 = transformPosition(transform, 2.703, -0.045, 15.75)
            val worldPosition2 = transformPosition(transform, 0.0, -0.045, 15.75)
            val worldPosition3 = transformPosition(transform, -2.703, -0.045, 15.75)
            val barrelLeftPos = Vec3(worldPosition1.x, worldPosition1.y, worldPosition1.z)
            val barrelMiddlePos = Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)
            val barrelRightPos = Vec3(worldPosition3.x, worldPosition3.y, worldPosition3.z)

            for (i in 0..9) {
                Mod.queueServerWork(i) {
                    this.entityData.set(
                        LASER_LEFT_LENGTH,
                        Math.min(
                            laserLength(barrelLeftPos, living, gunData),
                            laserLengthEntity(barrelLeftPos, living, gunData)
                        )
                    )
                    this.entityData.set(
                        LASER_MIDDLE_LENGTH,
                        Math.min(
                            laserLength(barrelMiddlePos, living, gunData),
                            laserLengthEntity(barrelMiddlePos, living, gunData)
                        )
                    )
                    this.entityData.set(
                        LASER_RIGHT_LENGTH,
                        Math.min(
                            laserLength(barrelRightPos, living, gunData),
                            laserLengthEntity(barrelRightPos, living, gunData)
                        )
                    )
                }
            }

            val reloadTime =
                Mth.clamp(20 / (Math.max(vehicleWeaponRpm("Main"), 1).toFloat() / 60), 1f, 2.1474836E9f).toInt()

            Mod.queueServerWork(reloadTime - 20) {
                if (this.isAlive) {
                    this.level().playSound(
                        null,
                        this.onPos,
                        gunData.get(GunProp.SOUND_INFO).vehicleReload,
                        SoundSource.PLAYERS,
                        1f,
                        1f
                    )
                }
            }

            gunData.shakePlayers(this)
            playShootSound3p(living, gunData, barrelMiddlePos)
        }
    }

    override fun canShoot(living: LivingEntity?): Boolean {
        val gunData = getGunData(getSeatIndex(living))
        return gunData != null && gunData.canShoot(ammoSupplier) && this.canConsume(gunData.get(GunProp.AMMO_COST_PER_SHOOT)) && !isWreck
    }

    override fun getAnimation(): ResourceLocation? {
        return null
    }

    override fun getAnimationInstance(): VehicleAnimationInstance<*>? {
        return null
    }

    companion object {
        @JvmField
        val LASER_LEFT_LENGTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(AnnihilatorEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val LASER_MIDDLE_LENGTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(AnnihilatorEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val LASER_RIGHT_LENGTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(AnnihilatorEntity::class.java, EntityDataSerializers.FLOAT)
    }
}