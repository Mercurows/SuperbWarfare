package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModDamageTypes.causeProjectileHitDamage
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
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
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries
import java.util.*
import kotlin.math.max

open class MortarShellEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    enum class Type {
        NORMAL, WP
    }

    private var type: Type? = Type.NORMAL
    private var potion: Potion = Potions.EMPTY
    private val effects: MutableSet<MobEffectInstance> = hashSetOf()

    constructor(type: EntityType<out MortarShellEntity>, level: Level) : super(type, level) {
        this.noCulling = true
        this.damage = 60f
        this.explosionDamage = 100f
        this.explosionRadius = 8f
    }

    constructor(
        type: EntityType<out MortarShellEntity>,
        x: Double,
        y: Double,
        z: Double,
        level: Level,
        gravity: Float
    ) : super(type, x, y, z, level) {
        this.noCulling = true
        this.damage = 60f
        this.explosionDamage = 100f
        this.explosionRadius = 8f
        this.gravity = gravity
    }

    constructor(
        entity: LivingEntity?,
        level: Level,
        damage: Float,
        explosionDamage: Float,
        explosionRadius: Float
    ) : super(
        ModEntities.MORTAR_SHELL.get(), entity, level
    ) {
        this.noCulling = true
        this.damage = damage
        this.explosionDamage = explosionDamage
        this.explosionRadius = explosionRadius
    }

    fun setEffectsFromItem(pStack: ItemStack) {
        if (pStack.`is`(ModItems.POTION_MORTAR_SHELL.get())) {
            this.potion = PotionUtils.getPotion(pStack)
            val collection = PotionUtils.getCustomEffects(pStack)
            if (!collection.isEmpty()) {
                for (instance in collection) {
                    this.effects.add(MobEffectInstance(instance))
                }
            }
        } else if (pStack.`is`(ModItems.MORTAR_SHELL.get())) {
            this.potion = Potions.EMPTY
            this.effects.clear()
        }
    }

    fun setType(type: Type?) {
        this.type = type
    }

    override fun addAdditionalSaveData(pCompound: CompoundTag) {
        super.addAdditionalSaveData(pCompound)

        if (this.potion !== Potions.EMPTY) {
            pCompound.putString(
                "Potion",
                Objects.requireNonNullElse<Comparable<out Comparable<*>?>?>(
                    ForgeRegistries.POTIONS.getKey(this.potion),
                    "empty"
                ).toString()
            )
        }

        if (!this.effects.isEmpty()) {
            val list = ListTag()
            for (instance in this.effects) {
                list.add(instance.save(CompoundTag()))
            }
            pCompound.put("CustomPotionEffects", list)
        }
    }

    override fun readAdditionalSaveData(pCompound: CompoundTag) {
        super.readAdditionalSaveData(pCompound)

        if (pCompound.contains("Potion", 8)) {
            this.potion = PotionUtils.getPotion(pCompound)
        }

        this.effects.addAll(PotionUtils.getCustomEffects(pCompound))
    }

    override fun getDefaultItem(): Item {
        return ModItems.MORTAR_SHELL.get()
    }

    public override fun onHitEntity(entityHitResult: EntityHitResult) {
        super.onHitEntity(entityHitResult)
        val entity = entityHitResult.entity
        val owner = this.owner
        if (owner != null && owner.vehicle != null && entity == owner.vehicle) return
        if (this.level() is ServerLevel && this.tickCount > 1) {
            if (owner is ServerPlayer) {
                owner.level()
                    .playSound(null, owner.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1f, 1f)
                sendPacketTo(owner, ClientIndicatorMessage(0, 5))
            }

            entity.forceHurt(
                causeProjectileHitDamage(this.level().registryAccess(), this, owner),
                this.damage
            )

            if (type == Type.WP) {
                findNearEntity(entityHitResult.getLocation(), getOwner()!!)
            }

            if (this.level() is ServerLevel) {
                causeExplode(entityHitResult.getLocation())
                this.createAreaCloud(this.level(), entityHitResult.getLocation())
            }
            this.discard()
        }
    }

    public override fun onHitBlock(blockHitResult: BlockHitResult) {
        super.onHitBlock(blockHitResult)
        val resultPos = blockHitResult.blockPos
        val state = this.level().getBlockState(resultPos)
        val block = state.block

        if (block is BellBlock) {
            block.attemptToRing(this.level(), resultPos, blockHitResult.direction)
        }

        if (type == Type.WP && this.owner != null) {
            findNearEntity(blockHitResult.getLocation(), this.owner!!)
        }

        if (!this.level().isClientSide()) {
            if (this.tickCount > 1) {
                causeExplode(blockHitResult.getLocation())
                this.createAreaCloud(this.level(), blockHitResult.getLocation())
            }
        }
        this.discard()
    }

    fun findNearEntity(pos: Vec3, shooter: Entity) {
        if (this.level() is ServerLevel) {
            val entities = SeekTool.Builder(shooter)
                .withinRange(pos, explosionRadius.toDouble())
                .notItsVehicle()
                .baseFilter()
                .noVehicle()
                .build()

            for (e in entities) {
                val dis = pos.distanceTo(e.position())

                if (e is LivingEntity && checkNoClip(e, pos)) {
                    if (e is Player && e.isCreative) {
                        return
                    }
                    if (!e.level().isClientSide()) {
                        e.addEffect(
                            MobEffectInstance(
                                ModMobEffects.PHOSPHORUS_FIRE.get(),
                                (300 - 30 * dis).toInt(),
                                max(explosionRadius - dis, 0.0).toInt()
                            ), this.owner
                        )
                    }
                }
            }
        }
    }

    override fun tick() {
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

    override fun buildExplosion(vec3: Vec3?): CustomExplosion.Builder {
        return super.buildExplosion(vec3).damageMultiplier(1.25f)
    }

    fun createAreaCloud(level: Level, pos: Vec3) {
        if (this.potion === Potions.EMPTY) return

        val cloud = AreaEffectCloud(level, pos.x, pos.y, pos.z)
        cloud.setPotion(this.potion)
        cloud.duration = this.explosionDamage.toInt()
        cloud.radius = this.explosionRadius
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

    override fun getModel() = BedrockModelLoader.MORTAR_SHELL_MODEL
}
