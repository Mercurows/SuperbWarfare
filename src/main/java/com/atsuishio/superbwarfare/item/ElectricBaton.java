package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tiers.ModItemTier;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.ChatFormatting;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

public class ElectricBaton extends SwordItem implements EnergyStorageItem {

    public static final int MAX_ENERGY = 30000;
    public static final int ENERGY_COST = 2000;
    public static final String TAG_OPEN = "Open";

    public ElectricBaton() {
        super(ModItemTier.STEEL, new Properties()
                .durability(1114)
                .attributes(SwordItem.createAttributes(ModItemTier.STEEL, 2, -2.5f))
        );
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.electric_baton").withStyle(ChatFormatting.AQUA));

        if (NBTTool.getTag(stack).getBoolean(TAG_OPEN)) {
            tooltipComponents.add(Component.translatable("des.superbwarfare.electric_baton.open").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public int getMaxEnergy() {
        return MAX_ENERGY;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (player.isShiftKeyDown()) {
            var tag = NBTTool.getTag(stack);
            tag.putBoolean(TAG_OPEN, !tag.getBoolean(TAG_OPEN));
            NBTTool.saveTag(stack, tag);

            player.displayClientMessage(Component.translatable("des.superbwarfare.electric_baton." + (tag.getBoolean(TAG_OPEN) ? "open" : "close")), true);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return NBTTool.getTag(stack).getBoolean(TAG_OPEN) || super.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        if (NBTTool.getTag(stack).getBoolean(TAG_OPEN)) {
            var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (cap == null) return 0;

            return Math.round((float) cap.getEnergyStored() * 13F / MAX_ENERGY);
        } else {
            return super.getBarWidth(stack);
        }
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return NBTTool.getTag(stack).getBoolean(TAG_OPEN) ? 0xFFFF00 : super.getBarColor(stack);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        attacker.level().playSound(null, target.getOnPos(), ModSounds.MELEE_HIT.get(), SoundSource.PLAYERS, 1, (float) ((2 * org.joml.Math.random() - 1) * 0.1f + 1));

        if (NBTTool.getTag(stack).getBoolean(TAG_OPEN)) {
            var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (cap != null && cap.getEnergyStored() >= ENERGY_COST) {
                cap.extractEnergy(ENERGY_COST, false);

                if (!target.level().isClientSide) {
                    target.addEffect(new MobEffectInstance(ModMobEffects.SHOCK, 30, 2), attacker);
                }
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new CellImageComponent(pStack));
    }

    public static ItemStack makeFullEnergyStack() {
        ItemStack stack = new ItemStack(ModItems.ELECTRIC_BATON.get());

        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap != null) {
            cap.receiveEnergy(MAX_ENERGY, false);
        }

        var tag = NBTTool.getTag(stack);
        tag.putBoolean(TAG_OPEN, true);
        NBTTool.saveTag(stack, tag);

        return stack;
    }
}
