package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.client.renderer.gun.Ntw20Renderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModRarities;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public class Ntw20Item extends GunGeoItem {

    public Ntw20Item() {
        super(new Properties().rarity(ModRarities.LEGENDARY));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return Ntw20Renderer::new;
    }

    private PlayState fireAnimPredicate(AnimationState<Ntw20Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ntw_20.idle"));

        var data = GunData.from(stack);
        if (data.bolt.actionTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ntw_20.shift"));
        }

        if (data.reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ntw_20.reload_empty"));
        }

        if (data.reload.normal()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ntw_20.reload_normal"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ntw_20.idle"));
    }

    private PlayState editPredicate(AnimationState<Ntw20Item> event) {
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ntw_20.idle"));

        if (ClientEventHandler.isEditing) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ntw_20.edit"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ntw_20.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 0, this::fireAnimPredicate);
        data.add(fireAnimController);
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        data.add(editController);
    }

    @Override
    public boolean canAdjustZoom(GunData data) {
        return data.attachment.get(AttachmentType.SCOPE) == 3;
    }

    @Override
    public double getCustomZoom(GunData data) {
        int scopeType = data.attachment.get(AttachmentType.SCOPE);
        return switch (scopeType) {
            case 2 -> 2.25;
            case 3 -> GunsTool.getGunDoubleTag(NBTTool.getTag(data.stack), "CustomZoom");
            default -> 0;
        };
    }

    @Override
    public int getCustomMagazine(GunData data) {
        return switch (data.attachment.get(AttachmentType.MAGAZINE)) {
            case 1 -> 3;
            case 2 -> 6;
            default -> 0;
        };
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
    public boolean hasCustomMagazine(@NotNull GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomScope(@NotNull GunData data) {
        return true;
    }

    @Override
    public boolean hasBipod(@NotNull GunData data) {
        return true;
    }

    @Override
    public boolean canEditAttachments(@NotNull GunData data) {
        return true;
    }
}