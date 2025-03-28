package com.atsuishio.superbwarfare.item.gun.handgun;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.client.renderer.item.Glock17ItemRenderer;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;

public class Glock17Item extends GunItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public Glock17Item() {
        super(new Properties().stacksTo(1).rarity(Rarity.COMMON));
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new Glock17ItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState fireAnimPredicate(AnimationState<Glock17Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (NBTTool.getTag(stack).getBoolean("is_empty_reloading")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.glock.reload_empty"));
        }

        if (NBTTool.getTag(stack).getBoolean("is_normal_reloading")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.glock.reload_normal"));
        }


        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.glock.idle"));
    }

    private PlayState idlePredicate(AnimationState<Glock17Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (player.isSprinting() && player.onGround()
                && player.getPersistentData().getDouble("noRun") == 0
                && !(NBTTool.getTag(stack).getBoolean("is_normal_reloading") || NBTTool.getTag(stack).getBoolean("is_empty_reloading")) && ClientEventHandler.drawTime < 0.01) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.glock.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.glock.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.glock.idle"));
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 1, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idleController = new AnimationController<>(this, "idleController", 2, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.GLOCK_17_RELOAD_EMPTY.get(), ModSounds.GLOCK_17_RELOAD_NORMAL.get());
    }

    public static ItemStack getGunInstance() {
        ItemStack stack = new ItemStack(ModItems.GLOCK_17.get());
        GunsTool.initCreativeGun(stack, ModItems.GLOCK_17.getId().getPath());
        return stack;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return ModUtils.loc("textures/gun_icon/glock_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "GLOCK-17";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return false;
        // TODO perk
//        return PerkHelper.HANDGUN_PERKS.test(perk) || PerkHelper.MAGAZINE_PERKS.test(perk);
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
    public boolean canEjectShell(ItemStack stack) {
        return true;
    }

    @Override
    public int getAvailableFireModes() {
        return FireMode.SEMI.flag;
    }
}