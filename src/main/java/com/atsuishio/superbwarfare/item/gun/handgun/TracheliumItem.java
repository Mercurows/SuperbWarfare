package com.atsuishio.superbwarfare.item.gun.handgun;

import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.client.renderer.gun.TracheliumItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModRarities;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Supplier;

public class TracheliumItem extends GunGeoItem {

    public TracheliumItem() {
        super(new Item.Properties().rarity(ModRarities.VIRTUAL));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return TracheliumItemRenderer::new;
    }

    private PlayState fireAnimPredicate(AnimationState<TracheliumItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));

        var data = GunData.from(stack);
        boolean stock = data.attachment.get(AttachmentType.STOCK) == 2;
        boolean grip = data.attachment.get(AttachmentType.GRIP) > 0 || data.attachment.get(AttachmentType.SCOPE) > 0;

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

    private PlayState idlePredicate(AnimationState<TracheliumItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));

        var data = GunData.from(stack);
        boolean stock = data.attachment.get(AttachmentType.STOCK) == 2;
        boolean grip = data.attachment.get(AttachmentType.GRIP) > 0 || data.attachment.get(AttachmentType.SCOPE) > 0;

        if (data.bolt.actionTimer.get() > 0) {
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

        if (GunData.from(stack).reload.empty()) {
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

    private PlayState editPredicate(AnimationState<TracheliumItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));

        if (ClientEventHandler.isEditing) {
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
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        list.add(Component.empty());
        list.add(Component.translatable("des.superbwarfare.trachelium_1").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        list.add(Component.translatable("des.superbwarfare.trachelium_2").withStyle(ChatFormatting.GRAY));

        TooltipTool.addHideText(list, Component.empty());
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.trachelium_3").withStyle(ChatFormatting.WHITE));
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.trachelium_4").withStyle(Style.EMPTY.withColor(0xF4F0FF)));
    }

    @Override
    public int @NotNull [] getValidStocks() {
        return new int[]{0, 2};
    }

    @Override
    public int @NotNull [] getValidScopes() {
        return new int[]{0, 1, 2};
    }

    @Override
    public boolean canSwitchScope(GunData data) {
        return data.attachment.get(AttachmentType.SCOPE) == 2;
    }

    private boolean useSpecialAttributes(GunData data) {
        int scopeType = data.attachment.get(AttachmentType.SCOPE);
        int gripType = data.attachment.get(AttachmentType.GRIP);
        return scopeType > 0 || gripType > 0;
    }

    @Override
    public double getCustomDamage(@NotNull GunData data) {
        if (useSpecialAttributes(data)) {
            return 3;
        }
        return super.getCustomDamage(data);
    }

    @Override
    public double getCustomZoom(GunData data) {
        var stack = data.stack;
        int scopeType = data.attachment.get(AttachmentType.SCOPE);
        return scopeType == 2 ? (NBTTool.getTag(stack).getBoolean("ScopeAlt") ? 0 : 2.75) : 0;
    }

    @Override
    public double getCustomVelocity(@NotNull GunData data) {
        if (useSpecialAttributes(data)) {
            return 15;
        }
        return super.getCustomVelocity(data);
    }

    @Override
    public double getCustomHeadshot(@NotNull GunData data) {
        if (useSpecialAttributes(data)) {
            return 0.5;
        }
        return super.getCustomHeadshot(data);
    }

    @Override
    public double getCustomBypassArmor(@NotNull GunData data) {
        if (useSpecialAttributes(data)) {
            return 0.1;
        }
        return super.getCustomBypassArmor(data);
    }

    @Override
    public boolean hasCustomBarrel(@NotNull GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomGrip(@NotNull GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomScope(@NotNull GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomStock(@NotNull GunData data) {
        return true;
    }

    @Override
    public boolean canEditAttachments(@NotNull GunData data) {
        return true;
    }
}