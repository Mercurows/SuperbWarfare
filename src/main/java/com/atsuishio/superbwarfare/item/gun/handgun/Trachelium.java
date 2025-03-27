package com.atsuishio.superbwarfare.item.gun.handgun;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.client.renderer.item.TracheliumItemRenderer;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

public class Trachelium extends GunItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public Trachelium() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.TRACHELIUM_RELOAD_EMPTY.get());
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new TracheliumItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState fireAnimPredicate(AnimationState<Trachelium> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        boolean stock = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.STOCK) == 2;
        boolean grip = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.GRIP) > 0 || GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE) > 0;

        if (ClientEventHandler.firePosTimer > 0 && ClientEventHandler.firePosTimer < 1.7) {
            if (stock) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.fire_stock_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.fire_stock"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.fire_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.fire"));
                }
            }
        }

        if (stock) {
            if (grip) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_stock_grip"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_stock"));
            }
        } else {
            if (grip) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_stock_grip"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));
            }
        }
    }

    private PlayState idlePredicate(AnimationState<Trachelium> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        boolean stock = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.STOCK) == 2;
        boolean grip = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.GRIP) > 0 || GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE) > 0;

        if (GunsTool.getGunIntTag(stack, "BoltActionTick") > 0) {
            if (stock) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.action_stock_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.action_stock"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.action_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.action"));
                }
            }
        }

        if (NBTTool.getOrCreateTag(stack).getBoolean("is_empty_reloading")) {
            if (stock) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.reload_stock_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.reload_stock"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.reload_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.reload"));
                }
            }
        }

        if (player.isSprinting() && player.onGround() && player.getPersistentData().getDouble("noRun") == 0 && ClientEventHandler.drawTime < 0.01) {
            if (stock) {
                if (grip) {
                    if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_fast_stock"));
                    } else {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_stock_grip"));
                    }
                } else {
                    if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_fast_stock"));
                    } else {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_stock"));
                    }
                }
            } else {
                if (grip) {
                    if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_fast"));
                    } else {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_grip"));
                    }
                } else {
                    if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_fast"));
                    } else {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run"));
                    }
                }
            }
        }

        if (stock) {
            if (grip) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_stock_grip"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_stock"));
            }
        } else {
            if (grip) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_grip"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));
            }
        }
    }

    private PlayState editPredicate(AnimationState<Trachelium> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap != null && cap.edit) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 0, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idlePredicate = new AnimationController<>(this, "idlePredicate", 3, this::idlePredicate);
        data.add(idlePredicate);
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        data.add(editController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        list.add(Component.literal(""));
        list.add(Component.translatable("des.superbwarfare.trachelium_1").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        list.add(Component.translatable("des.superbwarfare.trachelium_2").withStyle(ChatFormatting.GRAY));

        TooltipTool.addHideText(list, Component.literal(""));
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.trachelium_3").withStyle(ChatFormatting.WHITE));
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.trachelium_4").withStyle(Style.EMPTY.withColor(0xF4F0FF)));
    }


    public static ItemStack getGunInstance() {
        ItemStack stack = new ItemStack(ModItems.TRACHELIUM.get());
        GunsTool.initCreativeGun(stack, ModItems.TRACHELIUM.getId().getPath());
        return stack;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        var tag = NBTTool.getOrCreateTag(stack);
        GunsTool.setGunIntTag(stack, "BoltActionTime", tag.getBoolean("DA") ? 12 : 0);

        int scopeType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE);
        int gripType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.GRIP);
        int stockType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.STOCK);
        CompoundTag tags = NBTTool.getOrCreateTag(stack).getCompound("Attachments");

        if (stockType == 1) {
            tags.putInt("Stock", 2);
        }

        if (scopeType == 3) {
            tags.putInt("Scope", 0);
        }

        if (scopeType > 0 || gripType > 0) {
            GunsTool.setGunDoubleTag(stack, "CustomVelocity", 15);
            GunsTool.setGunDoubleTag(stack, "BypassesArmor", 0.4);
            GunsTool.setGunDoubleTag(stack, "Damage", 21);
            GunsTool.setGunDoubleTag(stack, "Headshot", 2.5);
        } else {
            GunsTool.setGunDoubleTag(stack, "CustomVelocity", 0);
            GunsTool.setGunDoubleTag(stack, "BypassesArmor", 0.3);
            GunsTool.setGunDoubleTag(stack, "Damage", 19);
            GunsTool.setGunDoubleTag(stack, "Headshot", 2);
        }

        double customZoom = switch (scopeType) {
            case 0, 1 -> 0;
            case 2 -> NBTTool.getOrCreateTag(stack).getBoolean("ScopeAlt") ? 0 : 2.75;
            default -> 1;
        };

        GunsTool.setGunBooleanTag(stack, "CanSwitchScope", scopeType == 2);
        GunsTool.setGunDoubleTag(stack, "CustomZoom", customZoom);
    }

    @Override
    public ResourceLocation getGunIcon() {
        return ModUtils.loc("textures/gun_icon/trachelium_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "TRACHELIUM";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return false;
        // todo perk
//        return PerkHelper.HANDGUN_PERKS.test(perk);
    }

    @Override
    public boolean isMagazineReload(ItemStack stack) {
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
    public boolean hasCustomScope(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomStock(ItemStack stack) {
        return true;
    }

    @Override
    public int getAvailableFireModes() {
        return FireMode.SEMI.flag;
    }
}