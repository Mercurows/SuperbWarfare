package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import net.minecraft.core.BlockSource
import net.minecraft.core.Position
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterColorHandlersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber

class PotionMortarShellItem : MortarShellItem(), DispenserLaunchable {
    override fun getDefaultInstance(): ItemStack {
        return PotionUtils.setPotion(super.getDefaultInstance(), Potions.POISON)
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltip: MutableList<Component>,
        pFlag: TooltipFlag
    ) {
        PotionUtils.addPotionTooltip(pStack, pTooltip, 0.125f)
    }

    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : AbstractProjectileDispenseBehavior() {
            override fun getPower(): Float {
                return 0.5f
            }

            override fun getProjectile(pLevel: Level, pPosition: Position, pStack: ItemStack): Projectile {
                val shell = MortarShellEntity(
                    ModEntities.MORTAR_SHELL.get(),
                    pPosition.x(),
                    pPosition.y(),
                    pPosition.z(),
                    pLevel,
                    0.13f
                )
                shell.setEffectsFromItem(pStack)
                return shell
            }

            override fun playSound(pSource: BlockSource) {
                pSource.level
                    .playSound(null, pSource.pos, ModSounds.MORTAR_FIRE.get(), SoundSource.BLOCKS, 1f, 1f)
            }
        }
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
    companion object {
        @SubscribeEvent
        fun onRegisterColorHandlers(event: RegisterColorHandlersEvent.Item) {
            event.register(
                { stack, layer -> if (layer == 1) PotionUtils.getColor(stack) else -1 },
                ModItems.POTION_MORTAR_SHELL.get()
            )
        }
    }
}
