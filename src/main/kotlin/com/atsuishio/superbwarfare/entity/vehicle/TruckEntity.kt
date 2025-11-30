package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

class TruckEntity(type: EntityType<TruckEntity>, world: Level) : GeoVehicleEntity(type, world) {

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(GREEN, false)
    }

    var isGreen by GREEN

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putBoolean("Green", isGreen)
    }

    public override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        isGreen = compound.getBoolean("Green")
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.mainHandItem
        if (stack.item === Items.LIME_DYE && !isGreen) {
            isGreen = true
            if (!player.isCreative) {
                stack.shrink(1)
            }
            this.level().playSound(null, this, SoundEvents.BONE_MEAL_USE, this.soundSource, 2f, 1f)
            return InteractionResult.sidedSuccess(this.level().isClientSide())
        }

        if (stack.item === Items.RED_DYE && isGreen) {
            isGreen = false
            if (!player.isCreative) {
                stack.shrink(1)
            }
            this.level().playSound(null, this, SoundEvents.BONE_MEAL_USE, this.soundSource, 2f, 1f)
            return InteractionResult.sidedSuccess(this.level().isClientSide())
        }
        return super.interact(player, hand)
    }

    override fun getDamageModifier() = super.getDamageModifier()
        .custom { source, damage -> getSourceAngle(source, 0.25f) * damage }

    override fun baseTick() {
        super.baseTick()
        if (decoyInputDown) {
            horn()
        }
    }

    companion object {
        @JvmField
        val GREEN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(TruckEntity::class.java, EntityDataSerializers.BOOLEAN)
    }
}
