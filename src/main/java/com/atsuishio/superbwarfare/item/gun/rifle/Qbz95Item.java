package com.atsuishio.superbwarfare.item.gun.rifle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.client.renderer.item.Qbz95ItemRenderer;
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
import net.minecraft.nbt.CompoundTag;
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

public class Qbz95Item extends GunItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public Qbz95Item() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new Qbz95ItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<Qbz95Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;
        final var tag = NBTTool.getTag(stack);

        boolean drum = GunsTool.getAttachmentType(tag, GunsTool.AttachmentType.MAGAZINE) == 2;
        boolean grip = GunsTool.getAttachmentType(tag, GunsTool.AttachmentType.GRIP) == 1 || GunsTool.getAttachmentType(tag, GunsTool.AttachmentType.GRIP) == 2;

        if (tag.getBoolean("is_empty_reloading")) {
            if (drum) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz95.reload_empty_drum_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz95.reload_empty_drum"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz95.reload_empty_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz95.reload_empty"));
                }
            }
        }

        if (tag.getBoolean("is_normal_reloading")) {
            if (drum) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz95.reload_normal_drum_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz95.reload_normal_drum"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz95.reload_normal_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz95.reload_normal"));
                }
            }
        }

        if (player.isSprinting() && player.onGround() && player.getPersistentData().getDouble("noRun") == 0 && ClientEventHandler.drawTime < 0.01) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz95.run_fast"));
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz95.run_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz95.run"));
                }
            }
        }

        if (grip) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz95.idle_grip"));
        } else {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz95.idle"));
        }
    }

    private PlayState editPredicate(AnimationState<Qbz95Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap != null && cap.edit) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz95.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz95.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        data.add(editController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        final var rootTag = NBTTool.getTag(stack);
        int scopeType = GunsTool.getAttachmentType(rootTag, GunsTool.AttachmentType.SCOPE);
        int magType = GunsTool.getAttachmentType(rootTag, GunsTool.AttachmentType.MAGAZINE);

        if (magType == 1) {
            CompoundTag tag = rootTag.getCompound("Attachments");
            tag.putInt("Magazine", 2);
        }

        int customMag = magType == 2 ? 30 : 0;

        double customZoom = switch (scopeType) {
            case 0, 1 -> 0;
            case 2 -> 2.15;
            default -> GunsTool.getGunDoubleTag(rootTag, "CustomZoom", 0);
        };

        rootTag.putBoolean("CanAdjustZoomFov", scopeType == 3);
        GunsTool.setGunDoubleTag(rootTag, "CustomZoom", customZoom);
        GunsTool.setGunIntTag(rootTag, "CustomMagazine", customMag);
        NBTTool.saveTag(stack, rootTag);
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.QBZ_95_RELOAD_EMPTY.get(), ModSounds.QBZ_95_RELOAD_NORMAL.get());
    }

    public static ItemStack getGunInstance() {
        ItemStack stack = new ItemStack(ModItems.QBZ_95.get());
        GunsTool.initCreativeGun(stack, ModItems.QBZ_95.getId().getPath());
        return stack;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/qbz95_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "QBZ-95-1";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return PerkHelper.RIFLE_PERKS.test(perk) || PerkHelper.MAGAZINE_PERKS.test(perk);
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
    public boolean isAutoWeapon(ItemStack stack) {
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
    public boolean canEjectShell(ItemStack stack) {
        return true;
    }

    @Override
    public int getAvailableFireModes() {
        return FireMode.SEMI.flag + FireMode.AUTO.flag;
    }
}