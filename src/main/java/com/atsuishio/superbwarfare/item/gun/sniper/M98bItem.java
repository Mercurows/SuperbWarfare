package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.renderer.gun.M98bItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.GunsTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
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

public class M98bItem extends GunItem {

    public M98bItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return M98bItemRenderer::new;
    }

    private PlayState fireAnimPredicate(AnimationState<M98bItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_98b.idle"));

        var data = GunData.from(stack);
        if (data.bolt.actionTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_98b.shift"));
        }

        if (data.reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_98b.reload_empty"));
        }

        if (data.reload.normal()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_98b.reload_normal"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_98b.idle"));
    }

    private PlayState idlePredicate(AnimationState<M98bItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_98b.idle"));

        var data = GunData.from(stack);
        if (player.isSprinting() && player.onGround()
                && ClientEventHandler.cantSprint == 0
                && !(data.reload.normal() || GunData.from(stack).reload.empty()) && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint && GunData.from(stack).bolt.actionTimer.get() == 0) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_98b.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_98b.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_98b.idle"));
    }

    private PlayState editPredicate(AnimationState<M98bItem> event) {
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_98b.idle"));

        if (ClickHandler.isEditing) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_98b.edit"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_98b.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 1, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idleController = new AnimationController<>(this, "idleController", 6, this::idlePredicate);
        data.add(idleController);
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        data.add(editController);
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.M_98B_RELOAD_EMPTY.get(), ModSounds.M_98B_RELOAD_NORMAL.get(), ModSounds.M_98B_BOLT.get());
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/m_98b_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "M98-B";
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        int magType = GunData.from(stack).attachment.get(AttachmentType.MAGAZINE);
        if (magType == 2) {
            CompoundTag tag = GunData.from(stack).attachment();
            tag.putInt("Magazine", 0);
        }
    }

    @Override
    public boolean canSwitchScope(ItemStack stack) {
        return GunData.from(stack).attachment.get(AttachmentType.SCOPE) == 2;
    }

    @Override
    public int getCustomMagazine(ItemStack stack) {
        int magType = GunData.from(stack).attachment.get(AttachmentType.MAGAZINE);
        return magType == 1 ? 5 : 0;
    }

    @Override
    public double getCustomZoom(ItemStack stack) {
        var data = GunData.from(stack);
        int scopeType = data.attachment.get(AttachmentType.SCOPE);
        return switch (scopeType) {
            case 2 -> data.tag.getBoolean("ScopeAlt") ? 0 : 2.75;
            case 3 -> GunsTool.getGunDoubleTag(data.tag, "CustomZoom");
            default -> 0;
        };
    }

    @Override
    public boolean canAdjustZoom(ItemStack stack) {
        return GunData.from(stack).attachment.get(AttachmentType.SCOPE) == 3;
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
    public boolean hasCustomScope(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomMagazine(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isOpenBolt(ItemStack stack) {
        return true;
    }

    public boolean hasBipod(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasBulletInBarrel(ItemStack stack) {
        return true;
    }
}