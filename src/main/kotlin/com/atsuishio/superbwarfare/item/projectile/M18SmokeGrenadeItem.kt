package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.M18SmokeGrenadeEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import com.atsuishio.superbwarfare.item.IDyeableSmokeItem
import com.atsuishio.superbwarfare.item.IDyeableSmokeItem.Companion.TAG_COLOR
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockSource
import net.minecraft.core.Position
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.*
import net.minecraft.world.level.Level
import kotlin.math.min

open class M18SmokeGrenadeItem : Item(Properties().rarity(Rarity.UNCOMMON)), DispenserLaunchable, IDyeableSmokeItem {
    override fun setColor(stack: ItemStack, color: Int) {
        stack.getOrCreateTag().putInt(TAG_COLOR, color)
    }

    override fun getColor(stack: ItemStack): Int {
        return if (stack.tag != null && stack.tag!!.contains(TAG_COLOR)) stack.tag!!.getInt(TAG_COLOR) else 0xFFFFFF
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.m18_smoke_grenade").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(
                    Component.literal("#" + Integer.toHexString(this.getColor(pStack)))
                        .withStyle(Style.EMPTY.withColor(this.getColor(pStack)))
                )
        )
    }

    override fun use(worldIn: Level, playerIn: Player, handIn: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = playerIn.getItemInHand(handIn)
        playerIn.startUsingItem(handIn)
        if (playerIn is ServerPlayer) {
            playerIn.level().playSound(null, playerIn.onPos, ModSounds.GRENADE_PULL.get(), SoundSource.PLAYERS, 1f, 1f)
        }
        return InteractionResultHolder.consume(stack)
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim {
        return UseAnim.SPEAR
    }

    override fun releaseUsing(stack: ItemStack, level: Level, living: LivingEntity, timeLeft: Int) {
        if (!level.isClientSide && living is Player) {
            val usingTime = this.getUseDuration(stack) - timeLeft
            if (usingTime > 3) {
                living.cooldowns.addCooldown(stack.item, 20)
                val power = min(usingTime / 8f, 1.8f)

                val color = this.getColor(stack)

                val grenade = M18SmokeGrenadeEntity(living, level, 80 - usingTime)
                    .setColor((color shr 16 and 255) / 255f, ((color shr 8) and 255) / 255f, (color and 255) / 255f)
                grenade.shootFromRotation(living, living.xRot, living.yRot, 0f, power, 0f)
                level.addFreshEntity(grenade)

                if (level is ServerLevel) {
                    level.playSound(null, living.onPos, ModSounds.GRENADE_THROW.get(), SoundSource.PLAYERS, 1f, 1f)
                }

                if (!living.isCreative) {
                    stack.shrink(1)
                }
            }
        }
    }

    override fun finishUsingItem(pStack: ItemStack, pLevel: Level, pLivingEntity: LivingEntity): ItemStack {
        if (!pLevel.isClientSide) {
            val color = this.getColor(pStack)
            val grenade = M18SmokeGrenadeEntity(pLivingEntity, pLevel, 2)
                .setColor((color shr 16 and 255) / 255f, ((color shr 8) and 255) / 255f, (color and 255) / 255f)
            pLevel.addFreshEntity(grenade)

            if (pLivingEntity is Player) {
                pLivingEntity.cooldowns.addCooldown(pStack.item, 20)
            }

            if (pLivingEntity is Player && !pLivingEntity.isCreative) {
                pStack.shrink(1)
            }
        }

        return super.finishUsingItem(pStack, pLevel, pLivingEntity)
    }

    override fun getUseDuration(stack: ItemStack): Int {
        return 80
    }

    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : AbstractProjectileDispenseBehavior() {
            override fun getProjectile(pLevel: Level, pPosition: Position, pStack: ItemStack): Projectile {
                val color = this@M18SmokeGrenadeItem.getColor(pStack)
                return M18SmokeGrenadeEntity(
                    ModEntities.M18_SMOKE_GRENADE.get(),
                    pPosition.x(),
                    pPosition.y(),
                    pPosition.z(),
                    pLevel
                ).setColor((color shr 16 and 255) / 255f, ((color shr 8) and 255) / 255f, (color and 255) / 255f)
            }

            override fun playSound(pSource: BlockSource) {
                pSource.level.playSound(null, pSource.pos, ModSounds.GRENADE_THROW.get(), SoundSource.BLOCKS, 1f, 1f)
            }
        }
    }
}

