package com.atsuishio.superbwarfare.item.gun.smg;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.renderer.gun.VectorItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.function.Supplier;

public class VectorItem extends GunItem {

    public VectorItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    private PlayState idlePredicate(AnimationState<VectorItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.vector.idle"));

        var data = GunData.from(stack);
        boolean drum = data.attachment.get(AttachmentType.MAGAZINE) == 2;

        if (data.reload.empty()) {
            if (drum) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.vector.reload_empty_drum"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.vector.reload_empty"));
            }
        }

        if (data.reload.normal()) {
            if (drum) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.vector.reload_normal_drum"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.vector.reload_normal"));
            }
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.vector.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.vector.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.vector.idle"));
    }

    private PlayState editPredicate(AnimationState<VectorItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.vector.idle"));

        if (ClickHandler.isEditing) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.vector.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.vector.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        AnimationController<VectorItem> idleController = new AnimationController<>(this, "idleController", 2, this::idlePredicate);
        data.add(idleController);
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        data.add(editController);
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return VectorItemRenderer::new;
    }

    // TODO 移除inventoryTick
    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        var data = GunData.from(stack);
        int scopeType = data.attachment.get(AttachmentType.SCOPE);
        int gripType = data.attachment.get(AttachmentType.GRIP);

        if (scopeType == 3) {
            data.attachment.set(AttachmentType.SCOPE, 0);
            data.save();
        }
        if (gripType == 3) {
            data.attachment.set(AttachmentType.GRIP, 0);
            data.save();
        }
    }

    @Override
    public double getCustomZoom(ItemStack stack) {
        int scopeType = GunData.from(stack).attachment.get(AttachmentType.SCOPE);
        return scopeType == 2 ? 0.75 : 0;
    }

    @Override
    public int getCustomMagazine(ItemStack stack) {
        int magType = GunData.from(stack).attachment.get(AttachmentType.MAGAZINE);
        return switch (magType) {
            case 1 -> 20;
            case 2 -> 57;
            default -> 0;
        };
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.VECTOR_RELOAD_NORMAL.get(), ModSounds.VECTOR_RELOAD_EMPTY.get());
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/vector_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "VECTOR";
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