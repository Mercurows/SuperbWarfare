package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.IDyeableSmokeItem
import com.atsuishio.superbwarfare.item.IDyeableSmokeItem.Companion.TAG_COLOR
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockSource
import net.minecraft.core.Position
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class SmokeMortarShellItem : MortarShellItem(), IDyeableSmokeItem {
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

    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : AbstractProjectileDispenseBehavior() {
            override fun getPower(): Float {
                return 0.5f
            }

            override fun getProjectile(pLevel: Level, pPosition: Position, pStack: ItemStack): Projectile {
                val color = this@SmokeMortarShellItem.getColor(pStack)
                val shell = MortarShellEntity(
                    ModEntities.MORTAR_SHELL.get(),
                    pPosition.x(),
                    pPosition.y(),
                    pPosition.z(),
                    pLevel,
                    0.13f
                )
                shell.setType(MortarShellEntity.Type.SMOKE)
                shell.setRGB(
                    floatArrayOf(
                        ((color shr 16) and 255).toFloat(),
                        ((color shr 8) and 255).toFloat(),
                        (color and 255).toFloat()
                    )
                )
                return shell
            }

            override fun playSound(pSource: BlockSource) {
                pSource.level.playSound(null, pSource.pos, ModSounds.MORTAR_FIRE.get(), SoundSource.BLOCKS, 1f, 1f)
            }
        }
    }
}
