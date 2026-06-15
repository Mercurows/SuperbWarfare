package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.AreaEffectCloud
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries
import java.util.*
import kotlin.math.max

open class MortarShellEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    enum class Type {
        NORMAL, WP, SMOKE
    }

    private var type: Type? = Type.NORMAL
    private var smokeCount: Int = 12
    private var potion: Potion = Potions.EMPTY
    var red: Float = 1.0f
        private set
    var green: Float = 1.0f
        private set
    var blue: Float = 1.0f
        private set

    init {
        this.damageValue = 60f
        this.explosionDamageValue = 100f
        this.explosionRadiusValue = 8f
    }

    constructor(type: EntityType<out MortarShellEntity>, level: Level) : super(type, level)

    constructor(
        type: EntityType<out MortarShellEntity>,
        x: Double,
        y: Double,
        z: Double,
        level: Level,
        gravity: Float
    ) : super(type, x, y, z, level) {
        this.gravityValue = gravity
    }

    constructor(
        entity: LivingEntity?,
        level: Level,
        damage: Float,
        explosionDamage: Float,
        explosionRadius: Float
    ) : super(ModEntities.MORTAR_SHELL.get(), entity, level) {
        this.damageValue = damage
        this.explosionDamageValue = explosionDamage
        this.explosionRadiusValue = explosionRadius
    }

    override fun setRGB(rgb: FloatArray) {
        this.red = rgb[0] / 255f
        this.green = rgb[1] / 255f
        this.blue = rgb[2] / 255f
    }

    fun setEffectsFromItem(pStack: ItemStack) {
        if (pStack.`is`(ModItems.POTION_MORTAR_SHELL.get())) {
            this.potion = PotionUtils.getPotion(pStack)
            val collection = PotionUtils.getCustomEffects(pStack)
            if (!collection.isEmpty()) {
                for (instance in collection) {
                    this.effectsValue.add(MobEffectInstance(instance))
                }
            }
        } else {
            this.potion = Potions.EMPTY
            this.effectsValue.clear()
        }
    }

    open fun setType(type: Type?) {
        this.type = type
        if (type == Type.SMOKE) {
            this.damageValue /= 10f
            this.explosionDamageValue /= 10f
        }
    }

    override fun getDefaultItem(): Item {
        return ModItems.MORTAR_SHELL.get()
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)

        if (this.potion !== Potions.EMPTY) {
            compound.putString(
                "Potion",
                Objects.requireNonNullElse<Comparable<out Comparable<*>?>?>(
                    ForgeRegistries.POTIONS.getKey(this.potion),
                    "empty"
                ).toString()
            )
        }

        compound.putFloat("RColor", this.red)
        compound.putFloat("GColor", this.green)
        compound.putFloat("BColor", this.blue)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)

        if (compound.contains("Potion", 8)) {
            this.potion = PotionUtils.getPotion(compound)
        }

        if (compound.contains("RColor")) {
            this.red = compound.getFloat("RColor")
        }
        if (compound.contains("GColor")) {
            this.green = compound.getFloat("GColor")
        }
        if (compound.contains("BColor")) {
            this.blue = compound.getFloat("BColor")
        }
    }

    override fun afterHitEntity(result: EntityHitResult) {
        if (this.tickCount <= 1) return
        if (this.owner == null) return

        if (!this.level().isClientSide()) {
            when (type) {
                Type.WP -> this.causeWPEffect(result.getLocation(), this.owner!!)
                Type.SMOKE -> this.releaseSmoke()
                else -> {}
            }

            this.causeExplode(result.getLocation())
            this.createAreaCloud(this.level(), result.getLocation())
        }
        this.discard()
    }

    override fun afterHitBlock(result: BlockHitResult) {
        if (!this.level().isClientSide() && this.owner != null) {
            when (type) {
                Type.WP -> causeWPEffect(result.getLocation(), this.owner!!)
                Type.SMOKE -> releaseSmoke()
                else -> {}
            }
        }

        if (!this.level().isClientSide()) {
            if (this.tickCount > 1) {
                causeExplode(result.getLocation())
                this.createAreaCloud(this.level(), result.getLocation())
            }
        }
        this.discard()
    }

    open fun causeWPEffect(pos: Vec3, shooter: Entity) {
        if (this.level() is ServerLevel) {
            val entities = SeekTool.Builder(shooter)
                .withinRange(pos, explosionRadiusValue.toDouble())
                .notItsVehicle()
                .baseFilter()
                .noVehicle()
                .build()

            entities.asSequence()
                .filter { it is LivingEntity && !(it is Player && it.isCreative) }
                .forEach {
                    val dis = pos.distanceTo(it.position())
                    if (!checkNoClip(it, pos)) return@forEach
                    (it as LivingEntity).addEffect(
                        MobEffectInstance(
                            ModMobEffects.PHOSPHORUS_FIRE.get(),
                            (300 - 30 * dis).toInt(),
                            max(explosionRadiusValue - dis, 0.0).toInt()
                        ), this.owner
                    )
                }
        }
    }

    open fun releaseSmoke() {
        val level = this.level()
        if (level is ServerLevel) {
            val vec3 = Vec3(1.0, 0.05, 0.0)
            for (i in 0..<this.smokeCount) {
                val decoy = SmokeDecoyEntity(ModEntities.SMOKE_DECOY.get(), level, true)
                    .setColor(this.red, this.green, this.blue)
                decoy.setPos(this.x, this.y + bbHeight, this.z)
                decoy.decoyShoot(this, vec3.yRot(i * (360f / this.smokeCount) * Mth.DEG_TO_RAD), 2f, 5f)
                level.addFreshEntity(decoy)
            }
        }
    }

    override fun tick() {
        val level = this.level()
        if (tickCount > this.getLife()) {
            if (level is ServerLevel) {
                this.createAreaCloud(level, position())
            }
        }

        super.tick()
        if (deltaMovement.lengthSqr() > 25) {
            mediumTrail()
        }

        if (type == Type.WP) {
            val hitResult = level().clip(
                ClipContext(
                    position(),
                    position().add(deltaMovement.scale(8.0)),
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.ANY,
                    this
                )
            )

            if (hitResult.type == HitResult.Type.BLOCK) {
                releaseWp(owner)
            }
        }
    }

    private fun releaseWp(shooter: Entity?) {
        val level = this.level()
        if (level is ServerLevel) {
            ParticleTool.spawnMediumExplosionParticles(level, position())
            repeat(31) {
                val whitePhosphorusProjectileEntity = WhitePhosphorusProjectileEntity(shooter, level)

                whitePhosphorusProjectileEntity.setPos(position().x, position().y, position().z)
                whitePhosphorusProjectileEntity.shoot(
                    deltaMovement.x,
                    deltaMovement.y,
                    deltaMovement.z,
                    (random.nextFloat() * 0.05f + 0.1f * deltaMovement.length()).toFloat(),
                    35f
                )
                level.addFreshEntity(whitePhosphorusProjectileEntity)
            }
            discard()
        }
    }

    open fun createAreaCloud(level: Level, pos: Vec3) {
        if (this.potion === Potions.EMPTY && this.getEffects().isEmpty()) return

        val cloud = AreaEffectCloud(level, pos.x, pos.y, pos.z)
        cloud.setPotion(this.potion)
        if (this.getEffects().isNotEmpty()) {
            this.getEffects().forEach { cloud.addEffect(it) }
        }

        cloud.duration = this.explosionDamageValue.toInt()
        cloud.radius = this.explosionRadiusValue
        val owner = this.owner
        if (owner is LivingEntity) {
            cloud.setOwner(owner)
        }
        level.addFreshEntity(cloud)
    }

    override fun getSound(): SoundEvent {
        return ModSounds.SHELL_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.06f
    }

    override fun forceLoadChunk(): Boolean {
        return true
    }
}
