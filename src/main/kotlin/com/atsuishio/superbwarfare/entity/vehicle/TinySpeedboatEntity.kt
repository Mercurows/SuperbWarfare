package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeItem
import net.minecraft.world.level.Level

open class TinySpeedboatEntity(type: EntityType<TinySpeedboatEntity>, world: Level) : VehicleEntity(type, world) {
    companion object {
        @JvmField
        val COLOR_ID: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(TinySpeedboatEntity::class.java, EntityDataSerializers.INT)
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(COLOR_ID, 0)
    }

    var colorId by COLOR_ID

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putInt("ColorId", colorId)
    }

    public override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        colorId = compound.getInt("ColorId")
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.mainHandItem
        if (stack.item is DyeItem) {
            if (customName != null && customName!!.string == "jeb_") return InteractionResult.PASS
            val stackColor = (stack.item as DyeItem).dyeColor.id

            if (colorId == stackColor) {
                return super.interact(player, hand)
            }

            colorId = stackColor

            if (!player.isCreative) {
                stack.shrink(1)
            }
            this.level().playSound(null, this, SoundEvents.BONE_MEAL_USE, this.soundSource, 1f, 1f)
            return InteractionResult.sidedSuccess(this.level().isClientSide())
        }
        return super.interact(player, hand)
    }
}
