package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.client.renderer.gun.MosinNagantItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public class MosinNagantItem extends GunGeoItem {

    public MosinNagantItem() {
        super(new Properties().rarity(Rarity.RARE));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return MosinNagantItemRenderer::new;
    }

    @OnlyIn(Dist.CLIENT)
    private PlayState fireAnimPredicate(AnimationState<MosinNagantItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mosin_nagant.idle"));

        var data = GunData.from(stack);

        if (GunData.from(stack).bolt.actionTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.shift"));
        }

        if (data.reload.stage() == 1 && !GunData.from(stack).hasEnoughAmmoToShoot(player)) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.prepare_empty"));
        }

        if (data.reload.stage() == 1 && GunData.from(stack).hasEnoughAmmoToShoot(player)) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.prepare"));
        }

        if (GunData.from(stack).loadIndex.get() == 0 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.iterativeload"));
        }

        if (GunData.from(stack).loadIndex.get() == 1 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.iterativeload2"));
        }

        if (data.reload.stage() == 3) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mosin_nagant.finish"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mosin_nagant.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 1, this::fireAnimPredicate);
        data.add(fireAnimController);
    }
}