package com.atsuishio.superbwarfare.item.curio

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.type.capability.ICurioItem

class ParachuteItem : Item(Properties().stacksTo(1).durability(600)), ICurioItem {
    override fun isValidRepairItem(pStack: ItemStack, pRepairCandidate: ItemStack): Boolean {
        return pRepairCandidate.`is`(Items.PHANTOM_MEMBRANE)
    }

    override fun canEquip(slotContext: SlotContext, stack: ItemStack?): Boolean {
        return CuriosApi.getCuriosInventory(slotContext.entity)
            .map { it.findFirstCurio(this).isEmpty }
            .orElse(false)
    }

    override fun curioTick(slotContext: SlotContext, stack: ItemStack) {
        val entity = slotContext.entity()
        if (entity !is Player) {
            val tag = stack.getOrCreateTag()
            if (!tag.getBoolean(TAG_OPEN) && entity.deltaMovement.y < -0.6 && entity.fallDistance > 4) {
                tag.putBoolean(TAG_OPEN, true)
                entity.level().playSound(
                    null,
                    entity.x,
                    entity.y,
                    entity.z,
                    ModSounds.PARACHUTE_OPEN.get(),
                    SoundSource.PLAYERS,
                    1f,
                    1f
                )
            }
        }

        if (stack.getOrCreateTag().getBoolean(TAG_OPEN)) {
            if ((entity.onGround() || entity.isInWater) || entity.isFallFlying || entity.vehicle != null || (entity is Player && entity.abilities.flying)) {
                stack.orCreateTag.putBoolean(TAG_OPEN, false)
                entity.level().playSound(
                    null,
                    entity.x,
                    entity.y,
                    entity.z,
                    ModSounds.PARACHUTE_CLOSE.get(),
                    SoundSource.PLAYERS,
                    1f,
                    1f
                )
            }
            if (entity is Player) {
                if (entity.level().isClientSide) {
                    entity.addDeltaMovement(
                        Vec3(entity.lookAngle.x, 0.0, entity.lookAngle.z).normalize().scale(0.05)
                    )
                    entity.deltaMovement = entity.deltaMovement.multiply(1.03, 0.75, 1.03)
                }
            } else {
                if (!entity.level().isClientSide) {
                    entity.addDeltaMovement(
                        Vec3(entity.lookAngle.x, 0.0, entity.lookAngle.z).normalize().scale(0.05)
                    )
                    entity.deltaMovement = entity.deltaMovement.multiply(1.03, 0.75, 1.03)
                }
            }

            if (entity.tickCount % 40 == 0) {
                stack.hurtAndBreak(1, entity) { }
            }
            entity.resetFallDistance()
        }
    }

    companion object {
        const val TAG_OPEN: String = "Open"

        @JvmStatic
        fun isParachuteOpen(entity: LivingEntity?): Boolean {
            return CuriosApi.getCuriosInventory(entity)
                .map {
                    it.findFirstCurio(ModItems.PARACHUTE.get())
                        .map { c ->
                            c.stack.orCreateTag.getBoolean(TAG_OPEN)
                        }.orElse(false)
                }.orElse(false)
        }

        fun isParachuteVisible(entity: LivingEntity?): Boolean {
            return CuriosApi.getCuriosInventory(entity)
                .map {
                    it.findFirstCurio(ModItems.PARACHUTE.get()).map { c ->
                        c.slotContext().visible()
                    }.orElse(false)
                }.orElse(false)
        }
    }
}
