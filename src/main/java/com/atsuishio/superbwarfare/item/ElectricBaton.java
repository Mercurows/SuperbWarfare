package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.capability.energy.ItemEnergyProvider;
import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tiers.ModItemTier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ElectricBaton extends SwordItem {

    public static final int MAX_ENERGY = 30000;
    public static final int ENERGY_COST = 2000;
    public static final String TAG_OPEN = "Open";
    private final Supplier<Integer> energyCapacity;

    public ElectricBaton() {
        super(ModItemTier.STEEL, 2, -2.5f, new Properties().durability(1114));
        this.energyCapacity = () -> MAX_ENERGY;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.electric_baton").withStyle(ChatFormatting.AQUA));
        if (pStack.getTag() != null && pStack.getTag().getBoolean(TAG_OPEN)) {
            pTooltipComponents.add(Component.translatable("des.superbwarfare.electric_baton.open").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag tag) {
        return new ItemEnergyProvider(stack, energyCapacity.get());
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack stack = pPlayer.getItemInHand(pUsedHand);
        if (pPlayer.isShiftKeyDown()) {
            stack.getOrCreateTag().putBoolean(TAG_OPEN, !stack.getOrCreateTag().getBoolean(TAG_OPEN));
            pPlayer.displayClientMessage(Component.translatable("des.superbwarfare.electric_baton." + (stack.getOrCreateTag().getBoolean(TAG_OPEN) ? "open" : "close")), true);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack pStack) {
        return pStack.getOrCreateTag().getBoolean(TAG_OPEN) || super.isBarVisible(pStack);
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        if (pStack.getOrCreateTag().getBoolean(TAG_OPEN)) {
            var energy = pStack.getCapability(ForgeCapabilities.ENERGY)
                    .map(IEnergyStorage::getEnergyStored)
                    .orElse(0);

            return Math.round(energy * 13F / MAX_ENERGY);
        } else {
            return super.getBarWidth(pStack);
        }
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        return pStack.getOrCreateTag().getBoolean(TAG_OPEN) ? 0xFFFF00 : super.getBarColor(pStack);
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        pAttacker.level().playSound(null, pTarget.getOnPos(), ModSounds.MELEE_HIT.get(), SoundSource.PLAYERS, 1, (float) ((2 * org.joml.Math.random() - 1) * 0.1f + 1.0f));
        if (pStack.getOrCreateTag().getBoolean(TAG_OPEN)) {
            var energy = pStack.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::getEnergyStored).orElse(0);
            if (energy >= ENERGY_COST) {
                pStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(e -> e.extractEnergy(ENERGY_COST, false));
                if (!pTarget.level().isClientSide) {
                    pTarget.addEffect(new MobEffectInstance(ModMobEffects.SHOCK.get(), 30, 2), pAttacker);
                }
            }
        }
        return super.hurtEnemy(pStack, pTarget, pAttacker);
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new CellImageComponent(pStack));
    }

    public static ItemStack makeFullEnergyStack() {
        ItemStack stack = new ItemStack(ModItems.ELECTRIC_BATON.get());
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                e -> e.receiveEnergy(MAX_ENERGY, false)
        );
        stack.getOrCreateTag().putBoolean(TAG_OPEN, true);
        return stack;
    }
}
