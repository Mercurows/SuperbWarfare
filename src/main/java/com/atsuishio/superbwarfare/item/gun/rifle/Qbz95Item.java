package com.atsuishio.superbwarfare.item.gun.rifle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.client.renderer.item.Qbz95ItemRenderer;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
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
        var data = GunData.from(stack);
        final var tag = data.tag();

        boolean drum = GunsTool.getAttachmentType(tag, GunsTool.AttachmentType.MAGAZINE) == 2;
        boolean grip = GunsTool.getAttachmentType(tag, GunsTool.AttachmentType.GRIP) == 1 || GunsTool.getAttachmentType(tag, GunsTool.AttachmentType.GRIP) == 2;

        if (data.reload.empty()) {
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

        if (data.reload.normal()) {
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

        int magType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.MAGAZINE);
        if (magType == 1) {
            CompoundTag tag = NBTTool.getTag(stack).getCompound("Attachments");
            tag.putInt("Magazine", 2);
        }
    }

    @Override
    public int getCustomMagazine(ItemStack stack) {
        int magType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.MAGAZINE);
        return magType == 2 ? 30 : 0;
    }

    @Override
    public double getCustomZoom(ItemStack stack) {
        int scopeType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE);
        return switch (scopeType) {
            case 2 -> 2.15;
            case 3 -> GunsTool.getGunDoubleTag(NBTTool.getTag(stack), "CustomZoom");
            default -> 0;
        };
    }

    @Override
    public boolean canAdjustZoom(ItemStack stack) {
        return GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE) == 3;
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.QBZ_95_RELOAD_EMPTY.get(), ModSounds.QBZ_95_RELOAD_NORMAL.get());
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