package com.atsuishio.superbwarfare.item.gun.shotgun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.Aa12ItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModEnumExtensions;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Set;
import java.util.function.Supplier;

public class Aa12Item extends GunItem {

    public String animationProcedure = "empty";
    public static ItemDisplayContext transformType;

    public Aa12Item() {
        super(new Properties().stacksTo(1).rarity(ModEnumExtensions.getLegendary()));
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return Aa12ItemRenderer::new;
    }

    private PlayState idlePredicate(AnimationState<Aa12Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        if (this.animationProcedure.equals("empty")) {
            if (GunData.from(stack).reload.empty()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.aa_12.reload_empty"));
            }

            if (GunData.from(stack).reload.normal()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.aa_12.reload_normal"));
            }

            if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
                if (ClientEventHandler.tacticalSprint) {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aa_12.run_fast"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aa_12.run"));
                }
            }

            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aa_12.idle"));
        }
        return PlayState.STOP;
    }

    private PlayState procedurePredicate(AnimationState<Aa12Item> event) {
        if (transformType != null && transformType.firstPerson()) {
            if (!this.animationProcedure.equals("empty") && event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationProcedure));
                if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                    this.animationProcedure = "empty";
                    event.getController().forceAnimationReset();
                }
            } else if (this.animationProcedure.equals("empty")) {
                return PlayState.STOP;
            }
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var procedureController = new AnimationController<>(this, "procedureController", 0, this::procedurePredicate);
        data.add(procedureController);
        var idleController = new AnimationController<>(this, "idleController", 4, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.AA_12_RELOAD_EMPTY.get(), ModSounds.AA_12_RELOAD_NORMAL.get());
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/aa_12_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "AA-12";
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
    public boolean canEjectShell(ItemStack stack) {
        return true;
    }
}