package com.atsuishio.superbwarfare.item.gun.smg;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.Mp5ItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
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

public class Mp5Item extends GunItem {

    public Mp5Item() {
        super(new Properties().rarity(Rarity.RARE));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return Mp5ItemRenderer::new;
    }

    private PlayState idlePredicate(AnimationState<Mp5Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mp5.idle"));

        boolean drum = GunData.from(stack).attachment.get(AttachmentType.MAGAZINE) == 2;

        if (GunData.from(stack).reload.empty()) {
            if (drum) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mp5.reload_empty_drum"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mp5.reload_empty"));
            }
        }

        if (GunData.from(stack).reload.normal()) {
            if (drum) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mp5.reload_normal_drum"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mp5.reload_normal"));
            }
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mp5.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mp5.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mp5.idle"));
    }

    private PlayState editPredicate(AnimationState<Mp5Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mp5.idle"));

        if (ClientEventHandler.isEditing) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mp5.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mp5.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        AnimationController<Mp5Item> idleController = new AnimationController<>(this, "idleController", 2, this::idlePredicate);
        data.add(idleController);
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        data.add(editController);
    }

    @Override
    public int[] getValidMagazines() {
        return new int[]{0, 2};
    }

    @Override
    public int[] getValidScopes() {
        return new int[]{0, 1, 2};
    }

    @Override
    public int[] getValidGrips() {
        return new int[]{0, 1, 2};
    }

    @Override
    public double getCustomZoom(ItemStack stack) {
        int scopeType = GunData.from(stack).attachment.get(AttachmentType.SCOPE);
        return scopeType == 2 ? 2.75 : 0;
    }

    @Override
    public int getCustomMagazine(ItemStack stack) {
        int magType = GunData.from(stack).attachment.get(AttachmentType.MAGAZINE);
        return magType == 2 ? 20 : 0;
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.VECTOR_RELOAD_NORMAL.get(), ModSounds.VECTOR_RELOAD_EMPTY.get());
    }

    @Override
    public ResourceLocation getGunIcon(ItemStack stack) {
        return Mod.loc("textures/gun_icon/mp_5_icon.png");
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

    @Override
    public boolean canEditAttachments(ItemStack stack) {
        return true;
    }
}