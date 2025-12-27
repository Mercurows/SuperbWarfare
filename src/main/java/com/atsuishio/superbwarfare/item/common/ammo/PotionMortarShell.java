package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.DispenserLaunchable;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PotionMortarShell extends MortarShell implements DispenserLaunchable {

    public PotionMortarShell(MortarShellEntity.Type type) {
        super(type);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        return PotionUtils.setPotion(super.getDefaultInstance(), Potions.POISON);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        PotionUtils.addPotionTooltip(pStack, pTooltip, 0.125F);
    }

    @SubscribeEvent
    public static void onRegisterColorHandlers(final RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 1 ? PotionUtils.getColor(stack) : -1, ModItems.POTION_MORTAR_SHELL.get());
    }

    @Override
    public DispenseItemBehavior getLaunchBehavior() {
        return new AbstractProjectileDispenseBehavior() {
            @Override
            protected float getPower() {
                return 0.5F;
            }

            @Override
            @ParametersAreNonnullByDefault
            protected @NotNull Projectile getProjectile(Level pLevel, Position pPosition, ItemStack pStack) {
                var shell = new MortarShellEntity(ModEntities.MORTAR_SHELL.get(), pPosition.x(), pPosition.y(), pPosition.z(), pLevel, 0.13f);
                shell.setEffectsFromItem(pStack);
                return shell;
            }

            @Override
            protected void playSound(@NotNull BlockSource pSource) {
                pSource.getLevel().playSound(null, pSource.getPos(), ModSounds.MORTAR_FIRE.get(), SoundSource.BLOCKS, 1, 1);
            }
        };
    }
}
