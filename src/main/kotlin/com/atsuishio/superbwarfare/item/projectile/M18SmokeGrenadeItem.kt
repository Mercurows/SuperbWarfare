package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.M18SmokeGrenadeEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import com.atsuishio.superbwarfare.tools.NBTTool
import net.minecraft.ChatFormatting
import net.minecraft.core.Position
import net.minecraft.core.dispenser.BlockSource
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
import javax.annotation.ParametersAreNonnullByDefault
import kotlin.math.min

open class M18SmokeGrenadeItem : Item(Properties().rarity(Rarity.UNCOMMON)), DispenserLaunchable {
    fun setColor(stack: ItemStack, color: Int) {
        val tag = NBTTool.getTag(stack)
        tag.putInt(TAG_COLOR, color)
        NBTTool.saveTag(stack, tag)
    }

    fun getColor(stack: ItemStack): Int {
        val tag = NBTTool.getTag(stack)
        return if (tag.contains(TAG_COLOR)) tag.getInt(TAG_COLOR) else 0xFFFFFF
    }

    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.m18_smoke_grenade").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(
                    Component.literal("#" + Integer.toHexString(this.getColor(stack)))
                        .withStyle(Style.EMPTY.withColor(this.getColor(stack)))
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
            val usingTime = this.getUseDuration(stack, living) - timeLeft
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

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int {
        return 80
    }

    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : AbstractProjectileDispenseBehavior() {
            override fun getProjectile(level: Level, position: Position, stack: ItemStack): Projectile {
                val color = this@M18SmokeGrenadeItem.getColor(stack)
                return M18SmokeGrenadeEntity(
                    ModEntities.M18_SMOKE_GRENADE.get(),
                    position.x(),
                    position.y(),
                    position.z(),
                    level
                ).setColor((color shr 16 and 255) / 255f, ((color shr 8) and 255) / 255f, (color and 255) / 255f)
            }

            override fun playSound(source: BlockSource) {
                source.level.playSound(null, source.pos, ModSounds.GRENADE_THROW.get(), SoundSource.BLOCKS, 1f, 1f)
            }
        }
    }

    companion object {
        const val TAG_COLOR: String = "Color"
    }
}

