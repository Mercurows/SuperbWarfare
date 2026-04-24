package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.client.renderer.gun.SentinelItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.SentinelImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.function.Supplier;

public class SentinelItem extends GunGeoItem {

    public SentinelItem() {
        super(new Item.Properties().rarity(Rarity.EPIC));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return SentinelItemRenderer::new;
    }

    private PlayState fireAnimPredicate(AnimationState<SentinelItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.idle"));

        if (GunData.from(stack).bolt.actionTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.shift"));
        }

        if (GunData.from(stack).reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.reload_empty"));
        }

        if (GunData.from(stack).reload.normal()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.reload_normal"));
        }

        if (GunData.from(stack).charging()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.charge"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 1, this::fireAnimPredicate);
        data.add(fireAnimController);
    }

    @Override
    public double getCustomDamage(GunData data) {
        var stack = data.stack;
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(cap -> cap.getEnergyStored() > 0 ? 0.2857142857142857 * data.getDefault().damage : 0)
                .orElse(0D);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                energy -> {
                    int energyStored = energy.getEnergyStored();
                    if (energyStored > 0) {
                        energy.extractEnergy(1, false);
                    }
                }
        );
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new SentinelImageComponent(pStack));
    }

    @Override
    public boolean isOpenBolt(@NotNull GunData data) {
        return true;
    }

    @Override
    public boolean hasBulletInBarrel(@NotNull GunData data) {
        return true;
    }

    @Override
    public void afterShoot(@NotNull ShootParameters parameters) {
        super.afterShoot(parameters);

        var data = parameters.data;

        data.stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> cap.extractEnergy(3000, false));
    }

    @Override
    public void playFireSounds(GunData data, Entity shooter, boolean zoom) {
        var cap = data.stack.getCapability(ForgeCapabilities.ENERGY);

        if (cap.map(c -> c.getEnergyStored() > 0).orElse(false)) {
            float soundRadius = data.get(GunProp.SOUND_RADIUS).floatValue();

            shooter.playSound(ModSounds.SENTINEL_CHARGE_FAR.get(), soundRadius * 0.7f, 1f);
            shooter.playSound(ModSounds.SENTINEL_CHARGE_FIRE_3P.get(), soundRadius * 0.4f, 1f);
            shooter.playSound(ModSounds.SENTINEL_CHARGE_VERYFAR.get(), soundRadius, 1f);
        } else {
            super.playFireSounds(data, shooter, zoom);
        }
    }
}