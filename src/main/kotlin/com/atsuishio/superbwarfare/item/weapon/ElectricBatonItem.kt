package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.CustomDamageProperty
import com.atsuishio.superbwarfare.item.EnergyStorageItem
import com.atsuishio.superbwarfare.tiers.ModItemTier
import com.atsuishio.superbwarfare.tools.NBTTool
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.neoforged.neoforge.capabilities.Capabilities
import org.joml.Math
import java.util.*
import kotlin.math.roundToInt

class ElectricBatonItem : SwordItem(
    ModItemTier.STEEL, CustomDamageProperty(1114).attributes(createAttributes(ModItemTier.STEEL, 2, -2.5f))
), EnergyStorageItem {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.electric_baton").withStyle(ChatFormatting.AQUA))

        if (NBTTool.getTag(stack).getBoolean(TAG_OPEN)) {
            tooltipComponents.add(
                Component.translatable("des.superbwarfare.electric_baton.open").withStyle(ChatFormatting.GRAY)
            )
        }
    }

    override fun getMaxEnergy(stack: ItemStack) = MAX_ENERGY

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)

        if (player.isShiftKeyDown) {
            val tag = NBTTool.getTag(stack)
            tag.putBoolean(
                TAG_OPEN,
                !tag.getBoolean(TAG_OPEN)
            )
            NBTTool.saveTag(stack, tag)

            player.displayClientMessage(
                Component.translatable(
                    "des.superbwarfare.electric_baton." + (if (tag.getBoolean(TAG_OPEN)) "open" else "close")
                ), true
            )
        }
        return InteractionResultHolder.fail(stack)
    }

    override fun isBarVisible(stack: ItemStack): Boolean {
        return NBTTool.getTag(stack).getBoolean(TAG_OPEN) || super.isBarVisible(stack)
    }

    override fun getBarWidth(stack: ItemStack): Int {
        if (NBTTool.getTag(stack).getBoolean(TAG_OPEN)) {
            val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return 0

            return (cap.energyStored.toFloat() * 13f / MAX_ENERGY).roundToInt()
        } else {
            return super.getBarWidth(stack)
        }
    }

    override fun getBarColor(stack: ItemStack): Int {
        return if (NBTTool.getTag(stack)
                .getBoolean(TAG_OPEN)
        ) 0xFFFF00 else super.getBarColor(stack)
    }

    override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        attacker.level().playSound(
            null,
            target.onPos,
            ModSounds.MELEE_HIT.get(),
            SoundSource.PLAYERS,
            1f,
            ((2 * Math.random() - 1) * 0.1f + 1).toFloat()
        )

        if (NBTTool.getTag(stack).getBoolean(TAG_OPEN)) {
            val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM)
            if (cap != null && cap.energyStored >= ENERGY_COST) {
                cap.extractEnergy(ENERGY_COST, false)

                if (!target.level().isClientSide) {
                    target.addEffect(MobEffectInstance(ModMobEffects.SHOCK, 30, 2), attacker)
                }
            }
        }
        return super.hurtEnemy(stack, target, attacker)
    }

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> {
        return Optional.of(CellImageComponent(pStack))
    }

    companion object {
        const val MAX_ENERGY: Int = 30000
        const val ENERGY_COST: Int = 2000
        const val TAG_OPEN: String = "Open"

        @JvmStatic
        fun makeFullEnergyStack(): ItemStack {
            val stack = ItemStack(ModItems.ELECTRIC_BATON.get())

            val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM)
            cap?.receiveEnergy(MAX_ENERGY, false)

            val tag = NBTTool.getTag(stack)
            tag.putBoolean(TAG_OPEN, true)
            NBTTool.saveTag(stack, tag)

            return stack
        }
    }
}