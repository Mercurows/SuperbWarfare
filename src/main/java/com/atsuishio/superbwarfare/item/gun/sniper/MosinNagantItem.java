package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.MosinNagantItemRenderer;
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

public class MosinNagantItem extends GunItem {

    public MosinNagantItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return MosinNagantItemRenderer::new;
    }

    private PlayState fireAnimPredicate(AnimationState<MosinNagantItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mosin_nagant.idle"));

        var data = GunData.from(stack);

        if (data.bolt.actionTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.shift"));
        }

        if (data.reload.stage() == 1 && GunData.from(stack).ammo.get() == 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.prepare_empty"));
        }

        if (data.reload.stage() == 1 && GunData.from(stack).ammo.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.prepare"));
        }

        if (data.loadIndex.get() == 0 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.iterativeload"));
        }

        if (data.loadIndex.get() == 1 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.iterativeload2"));
        }

        if (data.reload.stage() == 3) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.finish"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mosin_nagant.idle"));
    }

    private PlayState idlePredicate(AnimationState<MosinNagantItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mosin_nagant.idle"));

        var data = GunData.from(stack);

        if (player.isSprinting() && player.onGround()
                && ClientEventHandler.cantSprint == 0
                && !(GunData.from(stack).reload.empty())
                && data.reload.stage() != 1
                && data.reload.stage() != 2
                && data.reload.stage() != 3
                && ClientEventHandler.drawTime < 0.01
                && !data.reloading()
        ) {
            if (ClientEventHandler.tacticalSprint && GunData.from(stack).bolt.actionTimer.get() == 0) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mosin_nagant.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mosin_nagant.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mosin_nagant.idle"));
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
                ModSounds.MOSIN_NAGANT_BOLT.get(),
                ModSounds.MOSIN_NAGANT_PREPARE.get(),
                ModSounds.MOSIN_NAGANT_PREPARE_EMPTY.get(),
                ModSounds.MOSIN_NAGANT_LOOP.get(),
                ModSounds.MOSIN_NAGANT_END.get()
        );
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/mosin_nagant_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "MOSIN NAGANT";
    }
}