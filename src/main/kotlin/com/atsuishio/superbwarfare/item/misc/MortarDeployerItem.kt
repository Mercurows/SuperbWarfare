package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.tools.ProgressBarTool
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.UseAnim
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import javax.annotation.ParametersAreNonnullByDefault

class MortarDeployerItem : AbstractDeployerItem(Properties().rarity(Rarity.RARE)) {
    override fun spawnDeployedEntity(
        level: Level,
        player: Player
    ): Entity {
        return MortarEntity(level, player.yRot)
    }

    // PJM: миномёт разворачивается с задержкой, прогресс — в actionbar
    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        player.startUsingItem(context.hand)
        return InteractionResult.CONSUME
    }

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity) = DEPLOY_TICKS

    override fun getUseAnimation(stack: ItemStack) = UseAnim.BOW

    override fun onUseTick(level: Level, living: LivingEntity, stack: ItemStack, remainingUseDuration: Int) {
        if (living is Player) {
            ProgressBarTool.show(
                living,
                "tips.superbwarfare.mortar.deploying",
                (DEPLOY_TICKS - remainingUseDuration) / DEPLOY_TICKS.toFloat()
            )
        }
    }

    override fun finishUsingItem(stack: ItemStack, level: Level, living: LivingEntity): ItemStack {
        if (living !is Player) return stack
        val hitResult = getPlayerPOVHitResult(level, living, ClipContext.Fluid.NONE)
        if (hitResult.type == HitResult.Type.BLOCK) {
            super.useOn(UseOnContext(living, living.usedItemHand, hitResult))
        }
        return stack
    }

    // PJM: миномёт тяжёлый — замедляет, пока лежит в инвентаре
    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slot: Int, selected: Boolean) {
        if (level.isClientSide || entity !is LivingEntity) return
        entity.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false, false))
    }

    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.mortar_deployer").withStyle(ChatFormatting.GRAY)
        )
    }

    companion object {
        const val DEPLOY_TICKS = 60
    }
}
