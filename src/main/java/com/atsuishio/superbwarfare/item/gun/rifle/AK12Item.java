package com.atsuishio.superbwarfare.item.gun.rifle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.renderer.gun.AK12ItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
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

public class AK12Item extends GunItem {

    public AK12Item() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.AK_12_RELOAD_EMPTY.get(), ModSounds.AK_12_RELOAD_NORMAL.get());
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return AK12ItemRenderer::new;
    }

    private PlayState idlePredicate(AnimationState<AK12Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_12.idle"));

        var data = GunData.from(stack);
        boolean drum = data.attachment.get(AttachmentType.MAGAZINE) == 2;
        boolean grip = data.attachment.get(AttachmentType.GRIP) == 1 || data.attachment.get(AttachmentType.GRIP) == 2;

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

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_12.run_fast"));
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ak_12.run_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ak_12.run"));
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

        if (ClickHandler.isEditing) {
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
    public boolean canAdjustZoom(ItemStack stack) {
        return GunData.from(stack).attachment.get(AttachmentType.SCOPE) == 3;
    }

    @Override
    public double getCustomZoom(ItemStack stack) {
        int scopeType = GunData.from(stack).attachment.get(AttachmentType.SCOPE);
        return switch (scopeType) {
            case 2 -> 2.15;
            case 3 -> GunsTool.getGunDoubleTag(NBTTool.getTag(stack), "CustomZoom");
            default -> 0;
        };
    }

    @Override
    public int getCustomMagazine(ItemStack stack) {
        int magType = GunData.from(stack).attachment.get(AttachmentType.MAGAZINE);
        return switch (magType) {
            case 1 -> 15;
            case 2 -> 45;
            default -> 0;
        };
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/ak_12_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "AK-12";
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
    public boolean isCustomizable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomBarrel(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomGrip(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomMagazine(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomScope(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomStock(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEjectShell(ItemStack stack) {
        return true;
    }
}