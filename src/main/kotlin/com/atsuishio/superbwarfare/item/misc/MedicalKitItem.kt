package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.entity.projectile.MedicalKitEntity
import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import org.joml.Math

open class MedicalKitItem : Item(Properties().stacksTo(16)) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component?>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.medical_kit").withStyle(ChatFormatting.GRAY))
    }

    override fun use(level: Level, player: Player, handIn: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(handIn)

        if (player.isShiftKeyDown) {
            if (!MiscConfig.THROW_MEDICAL_KIT.get()) {
                return InteractionResultHolder.fail(stack)
            }

            if (!level.isClientSide) {
                val randomRot = ((2 * Math.random() - 1) * 180).coerceIn(-180.0, 180.0).toFloat()
                val entity = MedicalKitEntity(ModEntities.MEDICAL_KIT.get(), level)
                entity.moveTo(player.x, player.eyeY - 0.25, player.z, randomRot, 0f)
                entity.setYBodyRot(randomRot)
                entity.setYHeadRot(randomRot)
                entity.setDeltaMovement(
                    0.8 * player.lookAngle.x,
                    0.8 * player.lookAngle.y,
                    0.8 * player.lookAngle.z
                )
                level.addFreshEntity(entity)
            }

            if (!player.isCreative) {
                stack.shrink(1)
            }

            player.cooldowns.addCooldown(this, 25)
            return InteractionResultHolder.success(stack)
        } else if (player.health < player.maxHealth) {
            player.startUsingItem(handIn)
            return InteractionResultHolder.success(stack)
        }

        return InteractionResultHolder.fail(stack)
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim {
        return UseAnim.BOW
    }

    override fun finishUsingItem(pStack: ItemStack, pLevel: Level, pLivingEntity: LivingEntity): ItemStack {
        if (!pLevel.isClientSide) {
            treat(pLivingEntity)

            if (pLivingEntity is ServerPlayer) {
                pLivingEntity.level().playSound(
                    null,
                    pLivingEntity.onPos,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    SoundSource.PLAYERS,
                    0.5f,
                    1f
                )
            }

            if (pLivingEntity is Player) {
                pLivingEntity.cooldowns.addCooldown(pStack.item, 25)
            }

            if (pLivingEntity !is Player || !pLivingEntity.isCreative) {
                pStack.shrink(1)
            }
        }

        return super.finishUsingItem(pStack, pLevel, pLivingEntity)
    }

    override fun getUseDuration(stack: ItemStack): Int {
        return 40
    }

    open fun treat(living: LivingEntity) {
        val value =
            MiscConfig.MEDICAL_KIT_HEAL_AMOUNT.get() + MiscConfig.MEDICAL_KIT_HEAL_PERCENTAGE.get() * living.maxHealth
        living.heal(value.toFloat())
        living.addEffect(MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false), living)
    }
}