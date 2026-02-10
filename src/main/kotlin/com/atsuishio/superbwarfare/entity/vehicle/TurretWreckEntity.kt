package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier.Companion.createDefaultModifier
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.common.ammo.MortarShell
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.VectorUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Snowball
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SnowballItem
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector4d
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.stream.StreamSupport
import kotlin.streams.toList


open class TurretWreckEntity(type: EntityType<TurretWreckEntity>, world: Level) : Entity(type, world), GeoEntity {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
    companion object {
        @JvmField
        val QUATERNIOND_X: EntityDataAccessor<Float> = SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.FLOAT)
        val QUATERNIOND_Y: EntityDataAccessor<Float> = SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.FLOAT)
        val QUATERNIOND_Z: EntityDataAccessor<Float> = SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.FLOAT)
        val QUATERNIOND_W: EntityDataAccessor<Float> = SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.FLOAT)
        val VEHICLE_NAME: EntityDataAccessor<String> = SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.STRING)
        val HEALTH: EntityDataAccessor<Float> = SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.FLOAT)
    }

    open var QuaternionX by QUATERNIOND_X
    open var QuaternionY by QUATERNIOND_Y
    open var QuaternionZ by QUATERNIOND_Z
    open var QuaternionW by QUATERNIOND_W
    open var VehicleName by VEHICLE_NAME
    open var health by HEALTH

    open var qxO = 0f
    open var qyO = 0f
    open var qzO = 0f
    open var qwO = 1f
    open var supportByVehicle = false

    override fun canBeCollidedWith(): Boolean {
        return true
    }

    override fun canCollideWith(pEntity: Entity): Boolean {
        return true
    }

    override fun isPickable(): Boolean {
        return !this.isRemoved
    }

    private val DAMAGE_MODIFIER = createDefaultModifier()
            .multiply(0.02f, ModDamageTypes.CUSTOM_EXPLOSION)
            .multiply(0.02f, ModDamageTypes.MINE)
            .multiply(0.02f, ModDamageTypes.PROJECTILE_EXPLOSION)
            .multiply(0.02f, DamageTypes.EXPLOSION)

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        var amount = amount
        amount = DAMAGE_MODIFIER.compute(source, amount)
        entityData.set(HEALTH, entityData.get(HEALTH) - amount)
        if (level() is ServerLevel) {
            val serverLevel = level() as ServerLevel
            serverLevel.playSound(null, BlockPos.containing(position()), ModSounds.HIT.get(), SoundSource.PLAYERS, 1f, 1f)
            ParticleTool.sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), position().x, eyeY, position().z, 2, 0.0, 0.0, 0.0, 0.2, false)
            ParticleTool.sendParticle(serverLevel, ParticleTypes.SMOKE, position().x, eyeY, position().z, 2, 0.0, 0.0, 0.0, 0.01, false)
        }
        return super.hurt(source, amount)
    }

    override fun defineSynchedData() {
        entityData.define(QUATERNIOND_X, 0f)
        entityData.define(QUATERNIOND_Y, 0f)
        entityData.define(QUATERNIOND_Z, 0f)
        entityData.define(QUATERNIOND_W, 1f)
        entityData.define(VEHICLE_NAME, "GunMu")
        entityData.define(HEALTH, 100f)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        entityData.set(QUATERNIOND_X, compound.getFloat("Qx"))
        entityData.set(QUATERNIOND_Y, compound.getFloat("Qy"))
        entityData.set(QUATERNIOND_Z, compound.getFloat("Qz"))
        entityData.set(QUATERNIOND_W, compound.getFloat("Qw"))
        entityData.set(VEHICLE_NAME, compound.getString("VehicleName"))
        entityData.set(HEALTH, compound.getFloat("Health"))
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        compound.putFloat("Qx", entityData.get(QUATERNIOND_X))
        compound.putFloat("Qy", entityData.get(QUATERNIOND_Y))
        compound.putFloat("Qz", entityData.get(QUATERNIOND_Z))
        compound.putFloat("Qw", entityData.get(QUATERNIOND_W))
        compound.putString("VehicleName", entityData.get(VEHICLE_NAME))
        compound.putFloat("Health", entityData.get(HEALTH))
    }

    fun getPlayer(level: Level?): List<Entity> {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).all.spliterator(), false)
                .filter { e: Entity -> e is Player }
                .toList()
    }

    override fun baseTick() {
        qxO = QuaternionX
        qyO = QuaternionY
        qzO = QuaternionZ
        qwO = QuaternionW
        super.baseTick()

        this.move(MoverType.SELF, this.deltaMovement)
        var f = 0.98f
        if (this.onGround() || supportByVehicle) {
            val pos = this.blockPosBelowThatAffectsMyMovement
            f = level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98f

//            val targetRotation = Quaternionf().rotationXYZ(0f, -yRot * Mth.DEG_TO_RAD, 0f)
//            val lerpFactor = 0.5f
//            this.lerpRotationToTarget(targetRotation, lerpFactor)

            var rot = 0.6f

            if (getUpVec(1f).y < 0) {
                rot = -0.6f
            }

            setQuaternion(Quaterniond(getQuaternion(1f).rotateX( rot * getFrontVec(1f).y.toFloat())))
            setQuaternion(Quaterniond(getQuaternion(1f).rotateZ( rot * getRightVec(1f).y.toFloat())))
            supportByVehicle = false
        } else {
            setQuaternion(Quaterniond(getQuaternion(1f).rotateX(0.015f + 0.002f * deltaMovement.y.toFloat())))
        }
//
//        for (player in getPlayer(level())) {
//            if (player is Player) {
//                player.displayClientMessage(Component.literal(getEulerAngles(getQuaternion(1f)).z.toString()), true)
//            }
//
//        }

//        for (player in getPlayer(level())) {
//            val vehicle = player.vehicle
//            if (vehicle is Lav150Entity) {
//                val quaterniond = VectorTool.combineRotationsTurret(1f, vehicle)
//                setQuaternion(quaterniond)
//            }
//        }

        if (level().isClientSide) {
            val random = 2 * (this.random.nextFloat() - 0.5f)
            addRandomParticle(
                    ParticleTypes.LARGE_SMOKE,
                    Vec3(this.x, this.y + 0.7f * bbHeight, this.z),
                    0.35f * this.bbWidth,
                    level(),
                    0.01f,
                    1
            )
            addRandomParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    Vec3(this.x, this.y + 0.7f * bbHeight, this.z),
                    0.35f * this.bbWidth,
                    level(),
                    0.005f,
                    1
            )
            addRandomParticle(
                    CustomCloudOption(
                            1f,
                            0.1f,
                            0f,
                            (240 + 40 * random).toInt(),
                            2.5f + 0.5f * random,
                            -0.07f,
                            true,
                            true
                    ),
                    Vec3(this.x, this.y + 0.85f * bbHeight, this.z),
                    0.35f * this.bbWidth,
                    level(),
                    0.01f,
                    1
            )
            addRandomParticle(
                    CustomCloudOption(
                            1f,
                            0.35f,
                            0f,
                            (80 + 40 * random).toInt(),
                            1.5f + 0.5f * random,
                            -0.07f,
                            false,
                            true
                    ),
                    Vec3(this.x, this.y + 0.85f * bbHeight, this.z),
                    0.3f * this.bbWidth,
                    level(),
                    0.01f,
                    1
            )
        }
        if (this.tickCount % 15 == 0) {
            this.level().playSound(null, this.onPos, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1f, 1f)
        }

        this.deltaMovement = deltaMovement.multiply(f.toDouble(), 0.98, f.toDouble()).add(0.0, -0.04, 0.0)
        health -= 0.1f

        if (health <= 0) {
            this.discard()

            val explosion = createCustomExplosion()
                    .radius(0f)
                    .damage(0f)
                    .withParticleType(ParticleTool.ParticleType.SMALL)
            explosion.keepBlock()
            explosion.explode()

            val block = ItemEntity(level(), x, (y + 1), z, ItemStack(ModItems.STEEL_BLOCK.get()))
            block.setPickUpDelay(10)
            level().addFreshEntity(block)
        }
    }

    open fun addRandomParticle(
            particleOptions: ParticleOptions,
            pos: Vec3,
            randomPos: Float,
            level: Level,
            speed: Float,
            count: Int
    ) {
        val randomX = 2 * (this.random.nextFloat() - 0.5f)
        val randomY = 2 * (this.random.nextFloat() - 0.5f)
        val randomZ = 2 * (this.random.nextFloat() - 0.5f)
        repeat(count) {
            level.addAlwaysVisibleParticle(
                    particleOptions,
                    true,
                    pos.x + randomPos * randomX,
                    pos.y + randomPos * randomY,
                    pos.z + randomPos * randomZ,
                    (randomX * speed).toDouble(),
                    (randomY * speed).toDouble(),
                    (randomZ * speed).toDouble()
            )
        }
    }
    open fun createCustomExplosion(): CustomExplosion.Builder = CustomExplosion.Builder(this).attacker(null)

    private fun lerpRotationToTarget(targetRotation: Quaternionf, lerpFactor: Float) {
        val currentRotation: Quaternionf = this.getQuaternion(1f)
        currentRotation.slerp(targetRotation, lerpFactor)
        this.setQuaternion(Quaterniond(currentRotation))
    }

    open fun setQuaternion0(quaterniond: Quaterniond) {
        qxO = quaterniond.x.toFloat()
        qyO = quaterniond.y.toFloat()
        qzO = quaterniond.z.toFloat()
        qwO = quaterniond.w.toFloat()
    }
    open fun setQuaternion(quaterniond: Quaterniond) {
        QuaternionX = quaterniond.x.toFloat()
        QuaternionY = quaterniond.y.toFloat()
        QuaternionZ = quaterniond.z.toFloat()
        QuaternionW = quaterniond.w.toFloat()
    }

    open fun getQuaternion(tickDelta: Float) = Quaternionf(Mth.lerp(tickDelta, qxO, QuaternionX),
            Mth.lerp(tickDelta, qyO, QuaternionY),
            Mth.lerp(tickDelta, qzO, QuaternionZ),
            Mth.lerp(tickDelta, qwO, QuaternionW))

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = this.cache

    fun getWreckTransform(partialTicks: Float): Matrix4d {
        val transform = Matrix4d()
        transform.translate(
                Mth.lerp(partialTicks.toDouble(), xo, x),
                Mth.lerp(partialTicks.toDouble(), yo + 0.6, y + 0.6),
                Mth.lerp(partialTicks.toDouble(), zo, z)
        )
        transform.rotate(getQuaternion(partialTicks))
        return transform
    }

    open fun getUpVec(ticks: Float): Vec3 {
        val transform = getWreckTransform(ticks)
        val force0 = transformPosition(transform, 0.0, 0.0, 0.0)
        val force1 = transformPosition(transform, 0.0, 1.0, 0.0)
        return Vec3(force0.x, force0.y, force0.z).vectorTo(Vec3(force1.x, force1.y, force1.z))
    }

    open fun getFrontVec(ticks: Float): Vec3 {
        val transform = getWreckTransform(ticks)
        val force0 = transformPosition(transform, 0.0, 0.0, 0.0)
        val force1 = transformPosition(transform, 0.0, 0.0, 1.0)
        return Vec3(force0.x, force0.y, force0.z).vectorTo(Vec3(force1.x, force1.y, force1.z))
    }

    open fun getRightVec(ticks: Float): Vec3 {
        val transform = getWreckTransform(ticks)
        val force0 = transformPosition(transform, 0.0, 0.0, 0.0)
        val force1 = transformPosition(transform, -1.0, 0.0, 0.0)
        return Vec3(force0.x, force0.y, force0.z).vectorTo(Vec3(force1.x, force1.y, force1.z))
    }

//    open fun getAxisAngle(ticks: Float): FloatArray {
//        val x = -VehicleVecUtils.getXRotFromVector(getUpVec(ticks))
//        val y = -VehicleVecUtils.getZRotFromVector(getUpVec(ticks))
//        return floatArrayOf(x, y, z)
//    }

    open fun transformPosition(transform: Matrix4d, x: Double, y: Double, z: Double): Vector4d {
        return transform.transform(Vector4d(x, y, z, 1.0))
    }
}
