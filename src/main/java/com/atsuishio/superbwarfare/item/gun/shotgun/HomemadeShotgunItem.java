package com.atsuishio.superbwarfare.item.gun.shotgun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.HomemadeShotgunItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.ShotgunImageComponent;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.Set;

public class HomemadeShotgunItem extends GunItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.HOMEMADE_SHOTGUN_RELOAD_EMPTY.get(), ModSounds.HOMEMADE_SHOTGUN_NORMAL.get());
    }

    public HomemadeShotgunItem() {
        super(new Properties().durability(24).rarity(Rarity.COMMON));
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new HomemadeShotgunItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<HomemadeShotgunItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (GunData.from(stack).reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ab.reload_empty"));
        }

        if (GunData.from(stack).reload.normal()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ab.reload_normal"));
        }

        if (player.isSprinting() && player.onGround() && player.getPersistentData().getDouble("noRun") == 0 && ClientEventHandler.drawTime < 0.01) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ab.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ab.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ab.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/homemade_shotgun_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "Homemade Shotgun";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return PerkHelper.SHOTGUN_PERKS.test(perk);
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new ShotgunImageComponent(pStack));
    }

    @Override
    public boolean isMagazineReload(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isOpenBolt(ItemStack stack) {
        return true;
    }

    @Override
    public int getAvailableFireModes() {
        return FireMode.SEMI.flag;
    }
}
