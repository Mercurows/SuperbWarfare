package com.atsuishio.superbwarfare.item.gun.rifle;

import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.model.item.MarlinItemModel;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MarlinItem extends GunGeoItem {

    public MarlinItem() {
        super(new Item.Properties().rarity(Rarity.COMMON));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return GunRendererBuilder.simple(MarlinItemModel::new);
    }

    private PlayState fireAnimPredicate(AnimationState<MarlinItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.marlin.idle"));

        var data = GunData.from(stack);

        if (GunData.from(stack).bolt.actionTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.marlin.shift"));
        }

        if (data.reload.stage() == 1 && GunData.from(stack).reload.prepareTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.marlin.prepare"));
        }

        if (GunData.from(stack).loadIndex.get() == 0 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.marlin.iterativeload"));
        }

        if (GunData.from(stack).loadIndex.get() == 1 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.marlin.iterativeload2"));
        }

        if (data.reload.stage() == 3) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.marlin.finish"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.marlin.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 1, this::fireAnimPredicate);
        data.add(fireAnimController);
    }

    @Override
    public void whenNoAmmo(GunData data) {
        data.closeStrike.set(true);
    }

    @Override
    public void addBoltTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
        super.addBoltTimeBehavior(behaviors);
        behaviors.put(7, data -> data.closeStrike.set(false));
    }
}