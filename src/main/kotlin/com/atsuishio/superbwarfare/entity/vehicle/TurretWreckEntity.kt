package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.data.loot.WreckageLootData
import com.atsuishio.superbwarfare.data.loot.WreckageLootDataManager
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier.Companion.createDefaultModifier
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.angleTo
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.entity.vehicle.Minecart
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries
import org.joml.Quaterniond
import org.joml.Quaternionf
import kotlin.random.Random

open class TurretWreckEntity(type: EntityType<TurretWreckEntity>, level: Level) : Entity(type, level) {
    companion object {
        @JvmField
        val QUATERNION: EntityDataAccessor<Quaternionf> =
            SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.QUATERNION)

        @JvmField
        val VEHICLE_NAME: EntityDataAccessor<String> =
            SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.STRING)

        @JvmField
        val HEALTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.FLOAT)

        private val DAMAGE_MODIFIER = createDefaultModifier()
            .multiply(0.02f, ModDamageTypes.CUSTOM_EXPLOSION)
            .multiply(0.02f, ModDamageTypes.MINE)
            .multiply(0.02f, ModDamageTypes.PROJECTILE_EXPLOSION)
            .multiply(0.02f, DamageTypes.EXPLOSION)
    }

    open var quaternion by QUATERNION
    open var vehicleName by VEHICLE_NAME
    open var health by HEALTH

    open var qxO = 0f
    open var qyO = 0f
    open var qzO = 0f
    open var qwO = 1f
    open var supportByVehicle = false
    open var lastTickSpeed = 0.0
    open var lastTickVerticalSpeed = 0.0
    open var collisionCoolDown = 0
    open var lastDamageSource: DamageSource? = null
        get() {
            if (this.level().gameTime - this.lastDamageStamp > 40L) {
                this.lastDamageSource = null
            }
            return field
        }
    open var lastDamageStamp: Long = 0

    override fun canBeCollidedWith(): Boolean {
        return true
    }

    override fun canCollideWith(pEntity: Entity): Boolean {
        return true
    }

    override fun isPickable(): Boolean {
        return !this.isRemoved
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        var amount = amount
        amount = DAMAGE_MODIFIER.compute(this, source, amount)
        entityData.set(HEALTH, entityData.get(HEALTH) - amount)
        if (level() is ServerLevel) {
            val serverLevel = level() as ServerLevel
            serverLevel.playSound(
                null,
                BlockPos.containing(position()),
                ModSounds.HIT.get(),
                SoundSource.PLAYERS,
                1f,
                1f
            )
            ParticleTool.sendParticle(
                serverLevel,
                ModParticleTypes.FIRE_STAR.get(),
                position().x,
                eyeY,
                position().z,
                2,
                0.0,
                0.0,
                0.0,
                0.2,
                false
            )
            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.SMOKE,
                position().x,
                eyeY,
                position().z,
                2,
                0.0,
                0.0,
                0.0,
                0.01,
                false
            )

            this.lastDamageSource = source
            this.lastDamageStamp = level().gameTime
        }
        return super.hurt(source, amount)
    }

    override fun defineSynchedData() {
        entityData.define(QUATERNION, Quaternionf())
        entityData.define(VEHICLE_NAME, "GunMu")
        entityData.define(HEALTH, 100f)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        entityData.set(
            QUATERNION,
            Quaternionf(compound.getFloat("Qx"), compound.getFloat("Qy"), compound.getFloat("Qz"), compound.getFloat("Qw"))
        )
        entityData.set(VEHICLE_NAME, compound.getString("VehicleName"))
        entityData.set(HEALTH, compound.getFloat("Health"))
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        val q = entityData.get(QUATERNION)
        compound.putFloat("Qx", q.x())
        compound.putFloat("Qy", q.y())
        compound.putFloat("Qz", q.z())
        compound.putFloat("Qw", q.w())
        compound.putString("VehicleName", entityData.get(VEHICLE_NAME))
        compound.putFloat("Health", entityData.get(HEALTH))
    }

    override fun baseTick() {
        qxO = quaternion.x()
        qyO = quaternion.y()
        qzO = quaternion.z()
        qwO = quaternion.w()

        lastTickSpeed = Vec3(this.deltaMovement.x, this.deltaMovement.y + 0.04, this.deltaMovement.z).length()
        lastTickVerticalSpeed = this.deltaMovement.y + 0.04
        if (collisionCoolDown > 0) {
            collisionCoolDown--
        }

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

            setQuaternion(Quaterniond(getQuaternion(1f).rotateX(rot * getFrontVec(1f).y.toFloat())))
            setQuaternion(Quaterniond(getQuaternion(1f).rotateZ(rot * getRightVec(1f).y.toFloat())))
            supportByVehicle = false
        } else {
            setQuaternion(Quaterniond(getQuaternion(1f).rotateX(0.015f + 0.002f * deltaMovement.y.toFloat())))
        }

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
                    cooldown = true,
                    light = true
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
                    cooldown = false,
                    light = true
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
        health -= 0.05f

        if (health <= 0) {
            this.discard()

            CustomExplosion.Builder(this).attacker(null)
                .radius(4f)
                .damage(1f)
                .keepBlock().explode()

            this.generateWreckageLoot()
        }

        crushEntities()
    }

    open fun generateWreckageLoot() {
        val data = WreckageLootDataManager.getLootData(ResourceLocation(this.vehicleName)) ?: return
        val pools = data.pools
        if (pools.isEmpty()) return
        pools.forEach { pool ->
            val type = pool.type
            if (type == WreckageLootData.Pool.Type.VEHICLE_ONLY || type == WreckageLootData.Pool.Type.COMPLETE) return@forEach

            val entries = pool.entries
            if (entries.isEmpty()) return@forEach
            val source = pool.source
            if (source != "@Default") {
                val lastSource = this.lastDamageSource ?: return@forEach
                val parsedLoc = ResourceLocation.tryParse(source) ?: return@forEach
                val damageType = ResourceKey.create(Registries.DAMAGE_TYPE, parsedLoc)
                if (!lastSource.`is`(damageType)) return@forEach
            } else if (this.lastDamageSource?.`is`(ModDamageTypes.REPAIR_TOOL) == true) {
                return@forEach
            }

            repeat(pool.rolls) {
                entries.forEach { entry ->
                    val random = Random.nextDouble()
                    val chance = if (type == WreckageLootData.Pool.Type.DEFAULT) {
                        entry.chance * VehicleConfig.TURRET_WRECKAGE_LOOT_RATE.get()
                    } else {
                        entry.chance
                    }
                    if (random > chance) return@forEach

                    val name = entry.name
                    val item = ForgeRegistries.ITEMS.getValue(ResourceLocation(name)) ?: return@forEach
                    val count = entry.count
                    val entity = ItemEntity(level(), x, (y + 1), z, ItemStack(item, count))
                    entity.setPickUpDelay(10)
                    level().addFreshEntity(entity)
                }
            }
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

    private fun lerpRotationToTarget(targetRotation: Quaternionf, lerpFactor: Float) {
        val currentRotation: Quaternionf = this.getQuaternion(1f)
        currentRotation.slerp(targetRotation, lerpFactor)
        this.setQuaternion(Quaterniond(currentRotation))
    }

    open fun setQuaternion0(quaternion: Quaterniond) {
        qxO = quaternion.x.toFloat()
        qyO = quaternion.y.toFloat()
        qzO = quaternion.z.toFloat()
        qwO = quaternion.w.toFloat()
    }

    open fun setQuaternion(quaternion: Quaterniond) {
        this.quaternion = Quaternionf(quaternion.x.toFloat(), quaternion.y.toFloat(), quaternion.z.toFloat(), quaternion.w.toFloat())
    }

    open fun getQuaternion(tickDelta: Float) = Quaternionf(
        Mth.lerp(tickDelta, qxO, quaternion.x()),
        Mth.lerp(tickDelta, qyO, quaternion.y()),
        Mth.lerp(tickDelta, qzO, quaternion.z()),
        Mth.lerp(tickDelta, qwO, quaternion.w())
    )

    open fun getUpVec(ticks: Float) = VehicleVecUtils.getUpVec(getQuaternion(ticks))

    open fun getFrontVec(ticks: Float) = VehicleVecUtils.getFrontVec(getQuaternion(ticks))

    open fun getRightVec(ticks: Float) = VehicleVecUtils.getRightVec(getQuaternion(ticks))

    override fun move(movementType: MoverType, movement: Vec3) {
        super.move(movementType, movement)

        if (lastTickSpeed < 0.3 || collisionCoolDown > 0) return

        if (verticalCollision) {
            if (Mth.abs(lastTickVerticalSpeed.toFloat()) > 0.4) {
                if (!this.level().isClientSide) {
                    this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.soundSource, 1f, 1f)
                }
                this.bounceVertical(
                    Direction.getNearest(
                        this.deltaMovement.x(),
                        this.deltaMovement.y(),
                        this.deltaMovement.z()
                    ).opposite
                )
            }
        }

        if (this.horizontalCollision) {
            this.bounceHorizontal(
                Direction.getNearest(
                    this.deltaMovement.x(),
                    this.deltaMovement.y(),
                    this.deltaMovement.z()
                ).opposite
            )
            if (!this.level().isClientSide) {
                this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.soundSource, 1f, 1f)
            }
        }
    }

    fun bounceHorizontal(direction: Direction) {
        when (direction.axis) {
            Direction.Axis.X -> deltaMovement = deltaMovement.multiply(0.8, 0.99, 0.99)
            Direction.Axis.Z -> deltaMovement = deltaMovement.multiply(0.99, 0.99, 0.8)
            else -> {}
        }
    }

    fun bounceVertical(direction: Direction) {
        collisionCoolDown = 4
        if (direction.axis === Direction.Axis.Y) {
            deltaMovement = deltaMovement.multiply(0.9, -0.8, 0.9)
        }
    }

    @Suppress("DEPRECATION")
    fun crushEntities() {
        if (this.isRemoved) return
        val vec3 = this.deltaMovement
        val entities: List<Entity>?

        val frontBox = this.boundingBox.move(vec3)
        entities = this.level().getEntities(
            EntityTypeTest.forClass(Entity::class.java),
            frontBox
        ) { entity -> entity !== this && entity!!.vehicle == null }
            .filter { entity ->
                if (entity.isAlive) {
                    val type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)
                    return@filter (entity is VehicleEntity || entity is Boat || entity is Minecart || (entity is TurretWreckEntity && entity.tickCount > 5)
                            || (entity is LivingEntity && !(entity is Player && entity.isSpectator)))
                            || VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString())
                }
                false
            }
            .toList()

        for (entity in entities) {
            val entitySize = entity.boundingBox.getSize()
            val thisSize = this.boundingBox.getSize()
            val f: Double
            val f1: Double

            val v0 = vec3.subtract(entity.deltaMovement)
            if (v0.angleTo(this.position().vectorTo(entity.position())) > 90) return

            if (this.deltaMovement.lengthSqr() < 0.09) return

            if (entity is LivingEntity && entity.hasEffect(ModMobEffects.STRIKE_PROTECTION.get())) {
                continue
            }

            if (entity is VehicleEntity) {
                f = Mth.clamp((entity.mass / 3).toDouble(), 0.25, 4.0)
                f1 = Mth.clamp((3 / entity.mass).toDouble(), 0.25, 4.0)
            } else {
                f = Mth.clamp(entitySize / thisSize, 0.25, 4.0)
                f1 = Mth.clamp(thisSize / entitySize, 0.25, 4.0)
            }

            val length = v0.length().toFloat()
            val velAdd = v0.normalize().scale(0.8 * length)

            if (length <= 0.3) {
                continue
            }

            this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.soundSource, 1f, 1f)

            entity.forceHurt(
                ModDamageTypes.causeVehicleStrikeDamage(
                    this.level().registryAccess(),
                    this, this
                ),
                (f1 * 80 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)).toFloat()
            )

            this.pushNew(-0.3f * f * velAdd.x, -0.3f * f * velAdd.y, -0.3f * f * velAdd.z)

            if (entity is VehicleEntity) {
                val vec31 = this.deltaMovement.normalize().scale(velAdd.length())
                entity.pushNew(f1 * vec31.x, f1 * vec31.y, f1 * vec31.z)
            } else {
                val vec31 = this.deltaMovement.normalize().scale(velAdd.length())
                entity.push(f1 * vec31.x, f1 * vec31.y, f1 * vec31.z)
            }
        }
    }

    open fun pushNew(pX: Double, pY: Double, pZ: Double) {
        this.deltaMovement = this.deltaMovement.add(pX, pY, pZ)
    }
}
