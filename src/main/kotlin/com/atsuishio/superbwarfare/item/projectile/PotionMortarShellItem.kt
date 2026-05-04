package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import net.minecraft.core.Position
import net.minecraft.core.component.DataComponents
import net.minecraft.core.dispenser.BlockSource
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.util.FastColor
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent

class PotionMortarShellItem : MortarShellItem(), DispenserLaunchable {
    override fun getDefaultInstance(): ItemStack {
        val stack = super.getDefaultInstance()
        stack.set(DataComponents.POTION_CONTENTS, PotionContents(Potions.POISON))
        return stack
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        stack.get(DataComponents.POTION_CONTENTS)?.addPotionTooltip(
            { e -> tooltipComponents.add(e) },
            0.125f,
            context.tickRate()
        )
    }

    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : AbstractProjectileDispenseBehavior() {
            override fun getPower(): Float {
                return 0.5f
            }

            override fun getProjectile(level: Level, position: Position, stack: ItemStack): Projectile {
                val shell = MortarShellEntity(
                    ModEntities.MORTAR_SHELL.get(),
                    position.x(),
                    position.y(),
                    position.z(),
                    level,
                    0.13f
                )
                shell.setEffectsFromItem(stack)
                return shell
            }

            override fun playSound(source: BlockSource) {
                source.level
                    .playSound(null, source.pos, ModSounds.MORTAR_FIRE.get(), SoundSource.BLOCKS, 1f, 1f)
            }
        }
    }

    @EventBusSubscriber(Dist.CLIENT)
    companion object {
        @SubscribeEvent
        fun onRegisterColorHandlers(event: RegisterColorHandlersEvent.Item) {
            event.register(
                { stack, layer ->
                    if (layer == 1) FastColor.ARGB32.opaque(
                        stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).color
                    ) else -1
                },
                ModItems.POTION_MORTAR_SHELL.get()
            )
        }
    }
}
