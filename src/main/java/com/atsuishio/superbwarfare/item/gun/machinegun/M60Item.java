package com.atsuishio.superbwarfare.item.gun.machinegun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.M60ItemRenderer;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

public class M60Item extends GunItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public M60Item() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new M60ItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState fireAnimPredicate(AnimationState<M60Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (ClientEventHandler.firePosTimer > 0 && ClientEventHandler.firePosTimer < 0.45) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m60.fire"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m60.idle"));
    }

    private PlayState idlePredicate(AnimationState<M60Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (NBTTool.getTag(stack).getBoolean("is_empty_reloading")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m60.reload"));
        }

        if (NBTTool.getTag(stack).getBoolean("is_normal_reloading")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m60.reload2"));
        }

        if (player.isSprinting() && player.onGround() && player.getPersistentData().getDouble("noRun") == 0 && ClientEventHandler.drawTime < 0.01) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m60.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m60.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m60.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 0, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idleController = new AnimationController<>(this, "idleController", 4, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.M_60_RELOAD_EMPTY.get(), ModSounds.M_60_RELOAD_NORMAL.get());
    }

    public static ItemStack getGunInstance() {
        ItemStack stack = new ItemStack(ModItems.M_60.get());
        GunsTool.initCreativeGun(stack, ModItems.M_60.getId().getPath());
        return stack;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (NBTTool.getTag(stack).getBoolean("draw")) {
            NBTTool.getTag(stack).putBoolean("draw", false);

            if (GunsTool.getGunIntTag(stack, "Ammo", 0) <= 5) {
                GunsTool.setGunBooleanTag(stack, "HideBulletChain", true);
            }
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/m60_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "M60";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return PerkHelper.MACHINE_GUN_PERKS.test(perk) || PerkHelper.MAGAZINE_PERKS.test(perk) || perk == ModPerks.DESPERADO.get();
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
    public boolean isAutoWeapon(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEjectShell(ItemStack stack) {
        return true;
    }

    @Override
    public int getAvailableFireModes() {
        return FireMode.AUTO.flag;
    }
}