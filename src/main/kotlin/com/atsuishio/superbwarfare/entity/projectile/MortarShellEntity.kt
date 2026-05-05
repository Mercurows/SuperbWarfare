package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModDamageTypes.causeProjectileHitDamage
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.resources.ResourceLocation
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
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.network.PacketDistributor
import java.util.*
import kotlin.math.max

open class MortarShellEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    enum class Type {
        NORMAL, WP
    }

    private var type: Type? = Type.NORMAL
    private var potion: Potion? = Potions.WATER.value()
    private val effects: MutableSet<MobEffectInstance> = hashSetOf()

    constructor(type: EntityType<out MortarShellEntity>, level: Level) : super(type, level) {
        this.noCulling = true
        this.damageValue = 60f
        this.explosionDamageValue = 100f
        this.explosionRadiusValue = 8f
        this.gravityValue = 0.13f
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
        this.damageValue = 60f
        this.explosionDamageValue = 100f
        this.explosionRadiusValue = 8f
        this.gravityValue = gravity
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
        this.damageValue = damage
        this.explosionDamageValue = explosionDamage
        this.explosionRadiusValue = explosionRadius
        this.gravityValue = 0.13f
    }

    fun setEffectsFromItem(stack: ItemStack) {
        if (stack.`is`(ModItems.POTION_MORTAR_SHELL.get())) {
            val potionContents =
                stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
            this.potion = potionContents.potion().orElse(Potions.WATER).value()

            for (instance in potionContents.allEffects) {
                this.effects.add(MobEffectInstance(instance))
            }
        } else if (stack.`is`(ModItems.MORTAR_SHELL.get())) {
            this.potion = Potions.WATER.value()
            this.effects.clear()
        }
    }

    fun setType(type: Type?) {
        this.type = type
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)

        if (this.potion != Potions.WATER.value() && this.potion != null) {
            compound.putString(
                "Potion",
                Objects.requireNonNullElse(
                    BuiltInRegistries.POTION.getKey(this.potion!!),
                    "empty"
                ).toString()
            )
        }

        if (!this.effects.isEmpty()) {
            val listTag = ListTag()
            for (instance in this.effects) {
                listTag.add(instance.save())
            }
            compound.put("CustomPotionEffects", listTag)
        }
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)

        if (compound.contains("Potion", 8)) {
            val tagName = compound.getString("Potion")
            this.potion = BuiltInRegistries.POTION.get(ResourceLocation.parse(tagName))
        }

        val listTag = compound.getList("CustomPotionEffects", 10)
        for (i in listTag.indices) {
            val compoundtag = listTag.getCompound(i)
            val instance = MobEffectInstance.load(compoundtag)
            if (instance != null) {
                this.effects.add(instance)
            }
        }
    }

    override fun getDefaultItem(): Item {
        return ModItems.MORTAR_SHELL.get()
    }

    public override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        val entity = result.entity
        val owner = this.owner
        if (owner != null && owner.vehicle != null && entity == owner.vehicle) return
        if (this.level() is ServerLevel && this.tickCount > 1) {
            if (owner is LivingEntity) {
                if (owner is ServerPlayer) {
                    owner.level()
                        .playSound(null, owner.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1f, 1f)

                    PacketDistributor.sendToPlayer(owner, ClientIndicatorMessage(0, 5))
                }
            }

            entity.forceHurt(
                causeProjectileHitDamage(this.level().registryAccess(), this, owner),
                this.damageValue
            )

            if (type == Type.WP) {
                findNearEntity(result.getLocation(), getOwner()!!)
            }

            if (this.level() is ServerLevel) {
                causeExplode(result.getLocation())
                this.createAreaCloud(this.level(), position())
            }
            this.discard()
        }
    }

    public override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)
        val resultPos = result.blockPos
        val state = this.level().getBlockState(resultPos)

        val block = state.block
        if (block is BellBlock) {
            block.attemptToRing(this.level(), resultPos, result.direction)
        }

        if (type == Type.WP && owner != null) {
            findNearEntity(result.getLocation(), owner!!)
        }

        if (!this.level().isClientSide()) {
            if (this.tickCount > 1) {
                causeExplode(result.getLocation())
                this.createAreaCloud(this.level(), position())
            }
        }
        this.discard()
    }

    fun findNearEntity(pos: Vec3, shooter: Entity) {
        if (this.level() is ServerLevel) {
            val entities = SeekTool.Builder(shooter)
                .withinRange(pos, explosionRadiusValue.toDouble())
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
                                ModMobEffects.PHOSPHORUS_FIRE,
                                (300 - 30 * dis).toInt(),
                                max(explosionRadiusValue - dis, 0.0).toInt()
                            ), this.owner
                        )
                    }
                }
            }
        }
    }

    override fun tick() {
        val level = this.level()
        if (tickCount > getLife()) {
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

    override fun buildExplosion(vec3: Vec3): CustomExplosion.Builder {
        return super.buildExplosion(vec3).damageMultiplier(1.25f)
    }

    fun createAreaCloud(level: Level, pos: Vec3) {
        if (this.potion === Potions.WATER.value()) return

        val cloud = AreaEffectCloud(level, pos.x, pos.y, pos.z)
        for (effect in this.effects) {
            cloud.addEffect(effect)
        }
        cloud.duration = this.explosionDamageValue.toInt()
        cloud.radius = this.explosionRadiusValue
        val owner = this.owner
        if (owner is LivingEntity) {
            cloud.owner = owner
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
