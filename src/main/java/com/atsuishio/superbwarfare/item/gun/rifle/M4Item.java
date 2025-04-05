package com.atsuishio.superbwarfare.item.gun.rifle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.client.renderer.item.M4ItemRenderer;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunData;
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

public class M4Item extends GunItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public M4Item() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new M4ItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<M4Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;

        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;
        var data = GunData.from(stack);
        final var tag = data.tag();

        boolean drum = GunsTool.getAttachmentType(tag, GunsTool.AttachmentType.MAGAZINE) == 2;
        boolean grip = GunsTool.getAttachmentType(tag, GunsTool.AttachmentType.GRIP) == 1 || GunsTool.getAttachmentType(tag, GunsTool.AttachmentType.GRIP) == 2;

        if (data.emptyReloading()) {
            if (drum) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m4.reload_empty_drum_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m4.reload_empty_drum"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m4.reload_empty_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m4.reload_empty"));
                }
            }
        }

        if (data.normalReloading()) {
            if (drum) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m4.reload_normal_drum_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m4.reload_normal_drum"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m4.reload_normal_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m4.reload_normal"));
                }
            }
        }

        if (player.isSprinting() && player.onGround() && player.getPersistentData().getDouble("noRun") == 0 && ClientEventHandler.drawTime < 0.01) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m4.run_fast"));
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m4.run_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m4.run"));
                }
            }
        }

        if (grip) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m4.idle_grip"));
        } else {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m4.idle"));
        }
    }

    private PlayState editPredicate(AnimationState<M4Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap != null && cap.edit) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m4.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m4.idle"));
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
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.M_4_RELOAD_EMPTY.get(), ModSounds.M_4_RELOAD_NORMAL.get());
    }

    @Override
    public boolean canSwitchScope(ItemStack stack) {
        return GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE) == 2;
    }

    @Override
    public int getCustomMagazine(ItemStack stack) {
        int magType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.MAGAZINE);
        return switch (magType) {
            case 1 -> 15;
            case 2 -> 30;
            default -> 0;
        };
    }

    @Override
    public double getCustomZoom(ItemStack stack) {
        int scopeType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE);
        return switch (scopeType) {
            case 0, 1 -> 0;
            case 2 -> NBTTool.getTag(stack).getBoolean("ScopeAlt") ? 0 : 2.75;
            default -> GunsTool.getGunDoubleTag(NBTTool.getTag(stack), "CustomZoom");
        };
    }

    @Override
    public boolean canAdjustZoom(ItemStack stack) {
        return GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE) == 3;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/m4_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "M4A1";
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
    public boolean hasCustomStock(ItemStack stack) {
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