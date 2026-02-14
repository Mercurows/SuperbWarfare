package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.capability.energy.ItemEnergyProvider
import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tiers.ModItemTier
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
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
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.capabilities.ICapabilityProvider
import org.joml.Math
import java.util.*

class ElectricBatonItem : SwordItem(ModItemTier.STEEL, 2, -2.5f, Properties().durability(1114)) {
    private val energyCapacity: () -> Int = { MAX_ENERGY }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component?>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.electric_baton").withStyle(ChatFormatting.AQUA)
        )
        if (pStack.tag != null && pStack.tag!!.getBoolean(TAG_OPEN)) {
            pTooltipComponents.add(
                Component.translatable("des.superbwarfare.electric_baton.open").withStyle(ChatFormatting.GRAY)
            )
        }
    }

    override fun initCapabilities(stack: ItemStack, tag: CompoundTag?): ICapabilityProvider {
        return ItemEnergyProvider(stack, energyCapacity())
    }

    override fun use(pLevel: Level, pPlayer: Player, pUsedHand: InteractionHand): InteractionResultHolder<ItemStack?> {
        val stack = pPlayer.getItemInHand(pUsedHand)
        if (pPlayer.isShiftKeyDown) {
            stack.getOrCreateTag().putBoolean(TAG_OPEN, !stack.getOrCreateTag().getBoolean(TAG_OPEN))
            pPlayer.displayClientMessage(
                Component.translatable(
                    "des.superbwarfare.electric_baton." + (if (stack.getOrCreateTag().getBoolean(
                            TAG_OPEN
                        )
                    ) "open" else "close")
                ), true
            )
        }
        return InteractionResultHolder.fail<ItemStack?>(stack)
    }

    override fun isBarVisible(pStack: ItemStack): Boolean {
        return pStack.getOrCreateTag().getBoolean(TAG_OPEN) || super.isBarVisible(pStack)
    }

    override fun getBarWidth(pStack: ItemStack): Int {
        if (pStack.getOrCreateTag().getBoolean(TAG_OPEN)) {
            val energy = pStack.getCapability(ForgeCapabilities.ENERGY)
                .map { it.energyStored }
                .orElse(0)

            return Math.round(energy * 13f / MAX_ENERGY)
        } else {
            return super.getBarWidth(pStack)
        }
    }

    override fun getBarColor(pStack: ItemStack): Int {
        return if (pStack.getOrCreateTag().getBoolean(TAG_OPEN)) 0xFFFF00 else super.getBarColor(pStack)
    }

    override fun hurtEnemy(pStack: ItemStack, pTarget: LivingEntity, pAttacker: LivingEntity): Boolean {
        pAttacker.level().playSound(
            null,
            pTarget.onPos,
            ModSounds.MELEE_HIT.get(),
            SoundSource.PLAYERS,
            1f,
            ((2 * Math.random() - 1) * 0.1f + 1.0f).toFloat()
        )
        if (pStack.getOrCreateTag().getBoolean(TAG_OPEN)) {
            val energy = pStack.getCapability(ForgeCapabilities.ENERGY)
                .map { it.energyStored }.orElse(0)
            if (energy >= ENERGY_COST) {
                pStack.getCapability(ForgeCapabilities.ENERGY).ifPresent { it.extractEnergy(ENERGY_COST, false) }
                if (!pTarget.level().isClientSide) {
                    pTarget.addEffect(MobEffectInstance(ModMobEffects.SHOCK.get(), 30, 2), pAttacker)
                }
            }
        }
        return super.hurtEnemy(pStack, pTarget, pAttacker)
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
            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent { it.receiveEnergy(MAX_ENERGY, false) }
            stack.getOrCreateTag().putBoolean(TAG_OPEN, true)
            return stack
        }
    }
}