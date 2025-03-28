package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.client.renderer.item.K98ItemRenderer;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
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

public class K98Item extends GunItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public K98Item() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new K98ItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState fireAnimPredicate(AnimationState<K98Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (GunsTool.getGunIntTag(stack, "BoltActionTick") > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k98.shift"));
        }

        if (NBTTool.getTag(stack).getBoolean("is_empty_reloading")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k98.reload_empty"));
        }

        if (NBTTool.getTag(stack).getInt("reload_stage") == 1 && NBTTool.getTag(stack).getDouble("prepare") > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k98.prepare"));
        }

        if (NBTTool.getTag(stack).getDouble("load_index") == 0 && NBTTool.getTag(stack).getInt("reload_stage") == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k98.iterativeload"));
        }

        if (NBTTool.getTag(stack).getDouble("load_index") == 1 && NBTTool.getTag(stack).getInt("reload_stage") == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k98.iterativeload2"));
        }

        if (NBTTool.getTag(stack).getInt("reload_stage") == 3) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.k98.finish"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.k98.idle"));
    }

    private PlayState idlePredicate(AnimationState<K98Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (player.isSprinting() && player.onGround()
                && player.getPersistentData().getDouble("noRun") == 0
                && !(NBTTool.getTag(stack).getBoolean("is_empty_reloading"))
                && NBTTool.getTag(stack).getInt("reload_stage") != 1
                && NBTTool.getTag(stack).getInt("reload_stage") != 2
                && NBTTool.getTag(stack).getInt("reload_stage") != 3
                && ClientEventHandler.drawTime < 0.01
                && !GunsTool.getGunBooleanTag(stack, "Reloading")) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED) && GunsTool.getGunIntTag(stack, "BoltActionTick") == 0) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.k98.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.k98.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.k98.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 1, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(
                ModSounds.K_98_RELOAD_EMPTY.get(),
                ModSounds.K_98_BOLT.get(),
                ModSounds.K_98_PREPARE.get(),
                ModSounds.K_98_LOOP.get(),
                ModSounds.K_98_END.get()
        );
    }

    public static ItemStack getGunInstance() {
        ItemStack stack = new ItemStack(ModItems.K_98.get());
        GunsTool.initCreativeGun(stack, ModItems.K_98.getId().getPath());
        return stack;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return ModUtils.loc("textures/gun_icon/k98_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "KAR-98K";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return PerkHelper.SNIPER_RIFLE_PERKS.test(perk);
    }

    @Override
    public boolean isClipReload(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isIterativeReload(ItemStack stack) {
        return true;
    }

    @Override
    public int getAvailableFireModes() {
        return FireMode.SEMI.flag;
    }
}