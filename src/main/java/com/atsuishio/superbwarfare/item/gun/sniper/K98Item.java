package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.K98ItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Set;
import java.util.function.Supplier;

public class K98Item extends GunItem {

    public K98Item() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return K98ItemRenderer::new;
    }

    private PlayState fireAnimPredicate(AnimationState<K98Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.k_98.idle"));

        var data = GunData.from(stack);

        if (data.bolt.actionTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k_98.shift"));
        }

        if (data.reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k_98.reload_empty"));
        }

        if (data.reload.stage() == 1 && data.reload.prepareTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k_98.prepare"));
        }

        if (data.loadIndex.get() == 0 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k_98.iterativeload"));
        }

        if (data.loadIndex.get() == 1 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k_98.iterativeload2"));
        }

        if (data.reload.stage() == 3) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k_98.finish"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.k_98.idle"));
    }

    private PlayState idlePredicate(AnimationState<K98Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.k_98.idle"));

        var data = GunData.from(stack);

        if (player.isSprinting() && player.onGround()
                && ClientEventHandler.cantSprint == 0
                && !data.reload.empty()
                && data.reload.stage() != 1
                && data.reload.stage() != 2
                && data.reload.stage() != 3
                && ClientEventHandler.drawTime < 0.01
                && !data.reloading()
        ) {
            if (ClientEventHandler.tacticalSprint && data.bolt.actionTimer.get() == 0) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.k_98.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.k_98.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.k_98.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 1, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(
                ModSounds.K_98_RELOAD_EMPTY.get(),
                ModSounds.K_98_BOLT.get(),
                ModSounds.K_98_PREPARE.get(),
                ModSounds.K_98_LOOP.get(),
                ModSounds.K_98_END.get()
        );
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/k_98_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "KAR-98K";
    }
}