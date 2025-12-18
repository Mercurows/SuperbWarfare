package com.atsuishio.superbwarfare.item.gun.rifle;

import com.atsuishio.superbwarfare.client.renderer.gun.AK12ItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.GunsTool;
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

import java.util.function.Supplier;

public class AK12Item extends GunGeoItem {

    public AK12Item() {
        super(new Properties().rarity(Rarity.RARE));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return AK12ItemRenderer::new;
    }

    private PlayState idlePredicate(AnimationState<AK12Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_12.idle"));

        boolean drum = GunData.from(stack).attachment.get(AttachmentType.MAGAZINE) == 2;
        boolean grip = GunData.from(stack).attachment.get(AttachmentType.GRIP) == 1 || GunData.from(stack).attachment.get(AttachmentType.GRIP) == 2;

        if (GunData.from(stack).reload.empty()) {
            if (grip) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_12.reload_empty_grip"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_12.reload_empty"));
            }
        }

        if (GunData.from(stack).reload.normal()) {
            if (drum) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_12.reload_normal_drum_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_12.reload_normal_drum"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_12.reload_normal_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_12.reload_normal"));
                }
            }
        }

        if (grip) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_12.idle_grip"));
        } else {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_12.idle"));
        }
    }

    private PlayState editPredicate(AnimationState<AK12Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_12.idle"));

        if (ClientEventHandler.isEditing) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_12.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_12.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
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
            case 2 -> 2.15;
            case 3 -> GunsTool.getGunDoubleTag(data.stack, "CustomZoom");
            default -> 0;
        };
    }

    @Override
    public int getCustomMagazine(GunData data) {
        int magType = data.attachment.get(AttachmentType.MAGAZINE);
        return switch (magType) {
            case 1 -> 15;
            case 2 -> 45;
            default -> 0;
        };
    }

    @Override
    public boolean isOpenBolt(GunData data) {
        return true;
    }

    @Override
    public boolean hasBulletInBarrel(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomBarrel(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomGrip(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomMagazine(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomScope(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomStock(GunData data) {
        return true;
    }

    @Override
    public boolean canEditAttachments(GunData data) {
        return true;
    }
}