package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.SentinelItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.SentinelImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.EnergyStorageItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class SentinelItem extends GunItem implements EnergyStorageItem {

    public SentinelItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.getEnergyStored() > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);

        return Math.round((float) (cap != null ? cap.getEnergyStored() : 0) * 13.0F / 24000F);
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        return 0x95E9FF;
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return SentinelItemRenderer::new;
    }

    private PlayState fireAnimPredicate(AnimationState<SentinelItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.idle"));

        var data = GunData.from(stack);
        if (data.bolt.actionTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.shift"));
        }

        if (data.reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.reload_empty"));
        }

        if (data.reload.normal()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.reload_normal"));
        }

        if (data.charging()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.charge"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.idle"));
    }

    private PlayState idlePredicate(AnimationState<SentinelItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.idle"));

        var data = GunData.from(stack);
        if (player.isSprinting() && player.onGround()
                && ClientEventHandler.cantSprint == 0
                && !data.reloading()
                && !data.charging()
                && ClientEventHandler.drawTime < 0.01
        ) {
            if (ClientEventHandler.tacticalSprint && data.bolt.actionTimer.get() == 0) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 1, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idleController = new AnimationController<>(this, "idleController", 4, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public double getCustomDamage(ItemStack stack) {
        var data = GunData.from(stack);
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap != null && cap.getEnergyStored() > 0) {
            return 0.2857142857142857 * data.rawDamage();
        }
        return 0;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap != null && cap.getEnergyStored() > 0) {
            cap.extractEnergy(1, false);
        }
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(
                ModSounds.SENTINEL_RELOAD_EMPTY.get(),
                ModSounds.SENTINEL_RELOAD_NORMAL.get(),
                ModSounds.SENTINEL_CHARGE.get(),
                ModSounds.SENTINEL_BOLT.get()
        );
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/sentinel_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "SENTINEL";
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new SentinelImageComponent(pStack));
    }

    @Override
    public boolean isOpenBolt(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasBulletInBarrel(ItemStack stack) {
        return true;
    }

    @Override
    public void afterShoot(GunData data, Player player) {
        super.afterShoot(data, player);

        var cap = data.stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap != null) {
            cap.extractEnergy(3000, false);
        }
    }

    @Override
    public void playFireSounds(GunData data, Player player, boolean zoom) {
        var cap = data.stack.getCapability(Capabilities.EnergyStorage.ITEM);

        if (cap != null && cap.getEnergyStored() > 0) {
            float soundRadius = (float) data.soundRadius();

            player.playSound(ModSounds.SENTINEL_CHARGE_FAR.get(), soundRadius * 0.7f, 1f);
            player.playSound(ModSounds.SENTINEL_CHARGE_FIRE_3P.get(), soundRadius * 0.4f, 1f);
            player.playSound(ModSounds.SENTINEL_CHARGE_VERYFAR.get(), soundRadius, 1f);
        } else {
            super.playFireSounds(data, player, zoom);
        }
    }

    @Override
    public int getMaxEnergy() {
        return 24000;
    }
}