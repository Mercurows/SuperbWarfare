package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.SentinelItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.SentinelImageComponent;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModRarity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.EnergyStorageItem;
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
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.Set;

public class SentinelItem extends GunItem implements GeoItem, EnergyStorageItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public SentinelItem() {
        super(new Properties().stacksTo(1).rarity(ModRarity.getLegendary()));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.getEnergyStored() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);

        return Math.round((float) (cap != null ? cap.getEnergyStored() : 0) * 13.0F / 24000F);
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        return 0x95E9FF;
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new SentinelItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState fireAnimPredicate(AnimationState<SentinelItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;
        final var tag = NBTTool.getTag(stack);

        if (GunsTool.getGunIntTag(tag, "BoltActionTick") > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.shift"));
        }

        if (tag.getBoolean("is_empty_reloading")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.reload_empty"));
        }

        if (tag.getBoolean("is_normal_reloading")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.reload_normal"));
        }

        if (GunsTool.getGunBooleanTag(tag, "Charging")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sentinel.charge"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.idle"));
    }

    private PlayState idlePredicate(AnimationState<SentinelItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;
        final var tag = NBTTool.getTag(stack);

        if (player.isSprinting() && player.onGround()
                && player.getPersistentData().getDouble("noRun") == 0
                && !(tag.getBoolean("is_normal_reloading") || tag.getBoolean("is_empty_reloading"))
                && !GunsTool.getGunBooleanTag(tag, "Charging") && ClientEventHandler.drawTime < 0.01) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED) && GunsTool.getGunIntTag(tag, "BoltActionTick") == 0) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sentinel.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 1, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idleController = new AnimationController<>(this, "idleController", 4, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        final var tag = NBTTool.getTag(stack);

        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap != null && cap.getEnergyStored() > 0) {
            cap.extractEnergy(1, false);
            GunsTool.setGunDoubleTag(tag, "ChargedDamage", 0.2857142857142857
                    * GunsTool.getGunDoubleTag(tag, "Damage", 0));
        } else {
            GunsTool.setGunDoubleTag(tag, "ChargedDamage", 0);
        }
        NBTTool.saveTag(stack, tag);
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(
                ModSounds.SENTINEL_RELOAD_EMPTY.get(),
                ModSounds.SENTINEL_RELOAD_NORMAL.get(),
                ModSounds.SENTINEL_CHARGE.get(),
                ModSounds.SENTINEL_BOLT.get()
        );
    }

    public static ItemStack getGunInstance() {
        ItemStack stack = new ItemStack(ModItems.SENTINEL.get());
        GunsTool.initCreativeGun(stack, ModItems.SENTINEL.getId().getPath());
        return stack;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/sentinel_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "SENTINEL";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return PerkHelper.SNIPER_RIFLE_PERKS.test(perk) || PerkHelper.MAGAZINE_PERKS.test(perk);
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new SentinelImageComponent(pStack));
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
    public int getAvailableFireModes() {
        return FireMode.SEMI.flag;
    }

    @Override
    public int getMaxEnergy() {
        return 24000;
    }
}