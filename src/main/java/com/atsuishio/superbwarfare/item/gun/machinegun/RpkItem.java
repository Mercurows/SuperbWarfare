package com.atsuishio.superbwarfare.item.gun.machinegun;

import com.atsuishio.superbwarfare.client.renderer.gun.RpkItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.GunsTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

public class RpkItem extends GunGeoItem {

    public RpkItem() {
        super(new Properties().rarity(Rarity.EPIC));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return RpkItemRenderer::new;
    }

    private PlayState idlePredicate(AnimationState<RpkItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_47.idle"));

        var data = GunData.from(stack);
        boolean drum = data.attachment.get(AttachmentType.MAGAZINE) == 2;
        boolean grip = data.attachment.get(AttachmentType.GRIP) == 1 || data.attachment.get(AttachmentType.GRIP) == 2;

        if (data.reload.empty()) {
            if (drum) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_47.reload_empty_drum_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_47.reload_empty_drum"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_47.reload_empty_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_47.reload_empty"));
                }
            }
        }

        if (data.reload.normal()) {
            if (drum) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_47.reload_normal_drum_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_47.reload_normal_drum"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_47.reload_normal_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_47.reload_normal"));
                }
            }
        }

        if (grip) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_47.idle_grip"));
        } else {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_47.idle"));
        }
    }

    private PlayState editPredicate(AnimationState<RpkItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_47.idle"));

        if (ClientEventHandler.isEditing) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_47.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_47.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        data.add(editController);
    }

    @Override
    public int getCustomMagazine(GunData data) {
        int magType = data.attachment.get(AttachmentType.MAGAZINE);
        return switch (magType) {
            case 1 -> 20;
            case 2 -> 60;
            default -> 0;
        };
    }

    @Override
    public double getCustomZoom(GunData data) {
        int scopeType = data.attachment.get(AttachmentType.SCOPE);
        return switch (scopeType) {
            case 2 -> 1.55;
            case 3 -> GunsTool.getGunDoubleTag(data.stack, "CustomZoom");
            default -> 0;
        };
    }

    @Override
    public boolean canAdjustZoom(GunData data) {
        return data.attachment.get(AttachmentType.SCOPE) == 3;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        var data = GunData.from(stack);
        int gripType = data.attachment.get(AttachmentType.GRIP);

        if (gripType == 3) {
            data.attachment.set(AttachmentType.GRIP, 0);
            data.save();
        }
    }

    @Override
    public boolean canSwitchScope(GunData data) {
        return data.attachment.get(AttachmentType.SCOPE) == 2;
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
    public boolean hasBipod(GunData data) {
        return true;
    }

    @Override
    public boolean canEditAttachments(GunData data) {
        return true;
    }
}