package com.atsuishio.superbwarfare.item.gun.handgun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.AureliaSceptreRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModEnumExtensions;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AureliaSceptre extends GunItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AureliaSceptre() {
        super(new Properties().stacksTo(1).rarity(ModEnumExtensions.getLegendary()));
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return AureliaSceptreRenderer::new;
    }

    private PlayState fireAnimPredicate(AnimationState<AureliaSceptre> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        // TODO 装填动画
//        if (GunData.from(stack).reload.empty()) {
//            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.aurelia_sceptre.reload_empty"));
//        }
//
//        if (GunData.from(stack).reload.normal()) {
//            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.aurelia_sceptre.reload_normal"));
//        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));
    }

    private PlayState idlePredicate(AnimationState<AureliaSceptre> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        // TODO 冲刺动画
//        if (player.isSprinting() && player.onGround()
//                && ClientEventHandler.cantSprint == 0
//                && !(GunData.from(stack).reload.normal() || GunData.from(stack).reload.empty()) && ClientEventHandler.drawTime < 0.01) {
//            if (ClientEventHandler.tacticalSprint) {
//                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.run_fast"));
//            } else {
//                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.run"));
//            }
//        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 1, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idleController = new AnimationController<>(this, "idleController", 2, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        // TODO 音效
        return Set.of(ModSounds.GLOCK_17_RELOAD_EMPTY.get(), ModSounds.GLOCK_17_RELOAD_NORMAL.get());
    }

    @Override
    public ResourceLocation getGunIcon() {
        // TODO 图标
        return Mod.loc("textures/gun_icon/aurelia_sceptre_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "AURELIA SCEPTRE";
    }

    @Override
    public void addReloadTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
        super.addReloadTimeBehavior(behaviors);
    }
}