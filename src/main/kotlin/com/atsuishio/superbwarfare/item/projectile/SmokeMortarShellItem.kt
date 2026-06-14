package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.IDyeableSmokeItem
import com.atsuishio.superbwarfare.item.IDyeableSmokeItem.Companion.TAG_COLOR
import com.atsuishio.superbwarfare.tools.getOrCreateTag
import com.atsuishio.superbwarfare.tools.tag
import net.minecraft.ChatFormatting
import net.minecraft.core.Position
import net.minecraft.core.dispenser.BlockSource
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

    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : AbstractProjectileDispenseBehavior() {
            override fun getPower(): Float {
                return 0.5f
            }

            override fun getProjectile(level: Level, position: Position, stack: ItemStack): Projectile {
                val color = this@SmokeMortarShellItem.getColor(stack)
                val shell = MortarShellEntity(
                    ModEntities.MORTAR_SHELL.get(),
                    position.x(),
                    position.y(),
                    position.z(),
                    level,
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

            override fun playSound(source: BlockSource) {
                source.level.playSound(null, source.pos, ModSounds.MORTAR_FIRE.get(), SoundSource.BLOCKS, 1f, 1f)
            }
        }
    }
}
