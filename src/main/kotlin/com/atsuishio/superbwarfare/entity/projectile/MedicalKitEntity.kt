package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraftforge.items.ItemHandlerHelper

open class MedicalKitEntity(type: EntityType<MedicalKitEntity>, level: Level) : Entity(type, level) {
    override fun defineSynchedData() {
    }

    override fun isPickable(): Boolean {
        return true
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (player.isShiftKeyDown) {
            if (!this.level().isClientSide()) {
                this.discard()
            }

            if (!player.abilities.instabuild) {
                ItemHandlerHelper.giveItemToPlayer(player, ItemStack(ModItems.MEDICAL_KIT.get()))
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide())
    }

    override fun tick() {
        super.tick()

        this.deltaMovement = this.deltaMovement.add(0.0, -0.05, 0.0)

        if (!this.level().noCollision(this.boundingBox)) {
            this.moveTowardsClosestSpace(
                this.x,
                (this.boundingBox.minY + this.boundingBox.maxY) / 2.0,
                this.z
            )
        }

        this.move(MoverType.SELF, this.deltaMovement)
        var f = 0.98f
        if (this.onGround()) {
            this.xRot = -90f
            val pos = this.blockPosBelowThatAffectsMyMovement
            f = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98f
        } else {
            this.updateRotation()
        }

        this.deltaMovement = this.deltaMovement.multiply(f.toDouble(), 0.98, f.toDouble())
        if (this.onGround()) {
            this.deltaMovement = this.deltaMovement.multiply(1.0, -0.9, 1.0)
        }

        if (isInFluidType) {
            deltaMovement = deltaMovement.scale(0.75)
        }

        if (this.tickCount >= 10) {
            touchEntity()
        }

        this.refreshDimensions()
    }

    fun touchEntity() {
        val level = this.level()
        if (level is ServerLevel) {
            val frontBox = boundingBox.inflate(0.3)

            val entities = level.getEntities(
                EntityTypeTest.forClass(LivingEntity::class.java),
                frontBox
            ) { it.health < it.maxHealth }

            for (entity in entities) {
                if (entity != null) {
                    treat(entity)
                    level.playSound(
                        null,
                        position().x,
                        position().y,
                        position().z,
                        SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS,
                        0.5f,
                        1f
                    )
                    this.discard()
                    break
                }
            }
        }
    }

    override fun readAdditionalSaveData(pCompound: CompoundTag) {
    }

    override fun addAdditionalSaveData(pCompound: CompoundTag) {
    }

    protected fun updateRotation() {
        if (deltaMovement.length() > 0.05) {
            val vec3 = this.deltaMovement
            val d0 = vec3.horizontalDistance()
            this.xRot = lerpRotation(
                this.xRotO,
                (Mth.atan2(vec3.y, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
            )
            this.yRot = lerpRotation(
                this.yRotO,
                (Mth.atan2(vec3.x, vec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
            )
        }
    }

    open fun treat(living: LivingEntity) {
        val value =
            MiscConfig.MEDICAL_KIT_ENTITY_HEAL_AMOUNT.get() + MiscConfig.MEDICAL_KIT_ENTITY_HEAL_PERCENTAGE.get() * living.maxHealth
        living.heal(value.toFloat())
        living.addEffect(MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false), living)
    }

    companion object {
        @JvmStatic
        protected fun lerpRotation(pCurrentRotation: Float, pTargetRotation: Float): Float {
            var pCurrentRotation = pCurrentRotation
            while (pTargetRotation - pCurrentRotation < -180f) {
                pCurrentRotation -= 360f
            }

            while (pTargetRotation - pCurrentRotation >= 180f) {
                pCurrentRotation += 360f
            }

            return Mth.lerp(0.2f, pCurrentRotation, pTargetRotation)
        }
    }
}