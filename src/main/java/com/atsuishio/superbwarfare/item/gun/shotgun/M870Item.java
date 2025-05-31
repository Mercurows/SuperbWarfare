package com.atsuishio.superbwarfare.item.gun.shotgun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.M870ItemRenderer;
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

public class M870Item extends GunItem {

    public M870Item() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return M870ItemRenderer::new;
    }

    private PlayState fireAnimPredicate(AnimationState<M870Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_870.idle"));

        var data = GunData.from(stack);

        if (data.bolt.actionTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_870.shift"));
        }

        if (data.reload.stage() == 1 && data.reload.prepareLoadTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_870.preparealt"));
        }

        if (data.reload.stage() == 1 && data.reload.prepareTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_870.prepare"));
        }

        if (data.loadIndex.get() == 0 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_870.iterativeload"));
        }

        if (data.loadIndex.get() == 1 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_870.iterativeload2"));
        }

        if (data.reload.stage() == 3) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_870.finish"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_870.idle"));
    }

    private PlayState idlePredicate(AnimationState<M870Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_870.idle"));

        var data = GunData.from(stack);
        if (player.isSprinting()
                && player.onGround()
                && ClientEventHandler.cantSprint == 0
                && ClientEventHandler.drawTime < 0.01
                && !data.reloading()) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_870.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_870.run"));
            }
        }

        event.getController().setAnimation(RawAnimation.begin().thenLoop("animation.m_870.idle"));
        return PlayState.CONTINUE;
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
        return Set.of(ModSounds.M_870_PREPARE_LOAD.get(),
                ModSounds.M_870_LOOP.get(),
                ModSounds.M_870_BOLT.get());
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/m_870_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "M870 MCS";
    }
}