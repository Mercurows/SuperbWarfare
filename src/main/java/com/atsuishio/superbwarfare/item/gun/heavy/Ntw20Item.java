package com.atsuishio.superbwarfare.item.gun.heavy;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.client.renderer.item.Ntw20Renderer;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;

public class Ntw20Item extends GunItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public Ntw20Item() {
        super(new Properties().stacksTo(1)
                // TODO rarity
//                .rarity(ModRarity.getLegendary())
        );
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.NTW_20_RELOAD_EMPTY.get(), ModSounds.NTW_20_RELOAD_NORMAL.get(), ModSounds.NTW_20_BOLT.get());
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new Ntw20Renderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState fireAnimPredicate(AnimationState<Ntw20Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (GunsTool.getGunIntTag(stack, "BoltActionTick") > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ntw_20.shift"));
        }

        if (NBTTool.getTag(stack).getBoolean("is_empty_reloading")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ntw_20.reload_empty"));
        }

        if (NBTTool.getTag(stack).getBoolean("is_normal_reloading")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ntw_20.reload_normal"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ntw_20.idle"));
    }

    private PlayState idlePredicate(AnimationState<Ntw20Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (player.isSprinting() && player.onGround()
                && player.getPersistentData().getDouble("noRun") == 0
                && !(NBTTool.getTag(stack).getBoolean("is_normal_reloading") || NBTTool.getTag(stack).getBoolean("is_empty_reloading"))
                && ClientEventHandler.drawTime < 0.01) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED) && GunsTool.getGunIntTag(stack, "BoltActionTick") == 0) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ntw_20.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ntw_20.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ntw_20.idle"));
    }

    private PlayState editPredicate(AnimationState<Ntw20Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap != null && cap.edit) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ntw_20.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ntw_20.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 0, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idleController = new AnimationController<>(this, "idleController", 4, this::idlePredicate);
        data.add(idleController);
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        data.add(editController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        int scopeType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE);
        int magType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.MAGAZINE);

        int customMag = switch (magType) {
            case 1 -> 3;
            case 2 -> 6;
            default -> 0;
        };

        double customZoom = switch (scopeType) {
            case 0, 1 -> 0;
            case 2 -> 2.25;
            default -> GunsTool.getGunDoubleTag(stack, "CustomZoom", 0);
        };

        NBTTool.getTag(stack).putBoolean("CanAdjustZoomFov", scopeType == 3);
        GunsTool.setGunDoubleTag(stack, "CustomZoom", customZoom);
        GunsTool.setGunIntTag(stack, "CustomMagazine", customMag);
    }

    public static ItemStack getGunInstance() {
        ItemStack stack = new ItemStack(ModItems.NTW_20.get());
        GunsTool.initCreativeGun(stack, ModItems.NTW_20.getId().getPath());
        return stack;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return ModUtils.loc("textures/gun_icon/ntw_20_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "NTW-20";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return false;
        // todo perk
//        return PerkHelper.SNIPER_RIFLE_PERKS.test(perk) || PerkHelper.MAGAZINE_PERKS.test(perk);
    }

    @Override
    public boolean isMagazineReload(ItemStack stack) {
        return true;
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
    public boolean hasCustomMagazine(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomScope(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasBipod(ItemStack stack) {
        return true;
    }

    @Override
    public int getAvailableFireModes() {
        return FireMode.SEMI.flag;
    }
}