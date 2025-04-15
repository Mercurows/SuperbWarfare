package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.common.ammo.AmmoSupplierItem;
import com.atsuishio.superbwarfare.tools.Ammo;
import com.atsuishio.superbwarfare.tools.animation.AnimationCurves;
import com.atsuishio.superbwarfare.tools.animation.AnimationTimer;
import com.atsuishio.superbwarfare.tools.animation.ValueAnimator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
public class AmmoCountOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("ammo_count");

    private static final AnimationTimer ammoInfoTimer = new AnimationTimer(500, 2000)
            .forwardAnimation(AnimationCurves.EASE_OUT_EXPO)
            .backwardAnimation(AnimationCurves.EASE_IN_EXPO);
    private static final AnimationTimer ammoBoxTimer = new AnimationTimer(500)
            .forwardAnimation(AnimationCurves.EASE_OUT_EXPO)
            .backwardAnimation(AnimationCurves.EASE_IN_EXPO);

    private static final ValueAnimator<Integer>[] ammoCountAnimators = ValueAnimator.create(
            Ammo.values().length, 800, 0
    );
    private static final ValueAnimator<Integer>[] ammoBoxAnimators = ValueAnimator.create(
            Ammo.values().length, 800, 0
    );

    /**
     * 在手持弹药或弹药盒时，渲染玩家弹药总量信息
     */
    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        boolean startRenderingAmmoInfo = false;
        Player player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) return;

        boolean isAmmoBox = false;

        // 动画计算
        var currentTime = System.currentTimeMillis();
        ItemStack stack = player.getMainHandItem();
        if ((stack.getItem() instanceof AmmoSupplierItem || stack.getItem() == ModItems.AMMO_BOX.get())
                && !(player.getVehicle() instanceof ArmedVehicleEntity vehicle && vehicle.banHand(player))
        ) {
            // 刚拿出弹药物品时，视为开始弹药信息渲染
            startRenderingAmmoInfo = ammoInfoTimer.getProgress(currentTime) == 0;
            ammoInfoTimer.forward(currentTime);

            if (stack.getItem() == ModItems.AMMO_BOX.get()) {
                isAmmoBox = true;
                ammoBoxTimer.forward(currentTime);
            } else {
                ammoBoxTimer.backward(currentTime);
            }
        } else {
            ammoInfoTimer.backward(currentTime);
            ammoBoxTimer.backward(currentTime);
        }
        if (!ammoInfoTimer.isForward() && ammoInfoTimer.finished(currentTime)) return;

        var poseStack = guiGraphics.pose();
        poseStack.pushPose();

        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();

        var ammoX = ammoInfoTimer.lerp(w + 120, (float) w / 2 + 40, currentTime);
        final int fontHeight = 15;
        var yOffset = (-h - Ammo.values().length * fontHeight) / 2f;

        // 渲染总弹药数量
        var cap = player.getData(ModAttachments.PLAYER_VARIABLE);
        var font = Minecraft.getInstance().font;

        for (var type : Ammo.values()) {
            var index = type.ordinal();
            var ammoCount = type.get(cap);
            var animator = ammoCountAnimators[index];

            var boxAnimator = ammoBoxAnimators[index];
            var boxAmmoCount = boxAnimator.newValue();
            boolean boxAmmoSelected = false;

            if (isAmmoBox) {
                var data = stack.get(ModDataComponents.AMMO_BOX_INFO);
                var ammoBoxType = data == null ? "All" : data.type();

                boxAmmoCount = type.get(stack);
                if (ammoBoxType.equals("All") || ammoBoxType.equals(type.serializationName)) {
                    boxAnimator.forward(currentTime);
                    boxAmmoSelected = true;
                } else {
                    boxAnimator.reset(boxAmmoCount);
                }
            }

            // 首次开始渲染弹药信息时，记录弹药数量，便于后续播放动画
            if (startRenderingAmmoInfo) {
                animator.reset(ammoCount);
                animator.endForward(currentTime);
                if (isAmmoBox) {
                    boxAnimator.reset(type.get(stack));
                    boxAnimator.endForward(currentTime);
                }
            }

            int ammoAdd = Integer.compare(ammoCount, animator.oldValue());
            // 弹药数量变化时，更新并开始播放弹药数量更改动画
            animator.compareAndUpdate(ammoCount, () -> {
                // 弹药数量变化时，开始播放弹药数量更改动画
                animator.beginForward(currentTime);
            });

            var progress = animator.getProgress(currentTime);
            var ammoCountStr = Integer.toString(
                    Math.round(animator.lerp(animator.oldValue(), ammoCount, currentTime))
            );

            // 弹药增加时，颜色由绿变白，否则由红变白
            var fontColor = FastColor.ARGB32.lerp(progress, switch (ammoAdd) {
                case 1 -> 0xFF00FF00;
                case -1 -> 0xFFFF0000;
                default -> 0xFFFFFFFF;
            }, 0xFFFFFFFF);

            RenderSystem.setShaderColor(1, 1, 1, ammoInfoTimer.lerp(0, 1, currentTime));

            // 弹药数量
            guiGraphics.drawString(
                    font,
                    ammoCountStr,
                    ammoX + (30 - font.width(ammoCountStr)),
                    h + yOffset,
                    fontColor,
                    true
            );

            // 弹药类型
            guiGraphics.drawString(
                    font,
                    Component.translatable(type.translationKey).getString(),
                    ammoX + 35,
                    h + yOffset,
                    fontColor,
                    true
            );

            // 弹药盒信息渲染
            RenderSystem.setShaderColor(1, 1, 1, ammoBoxTimer.lerp(0, 1, currentTime));
            var ammoBoxX = ammoBoxTimer.lerp(-30, (float) w / 2, currentTime);

            int ammoBoxAdd = Integer.compare(boxAmmoCount, boxAnimator.oldValue());
            boxAnimator.compareAndUpdate(boxAmmoCount, () -> boxAnimator.beginForward(currentTime));

            // 选中时显示为黄色，否则为白色
            var targetColor = boxAmmoSelected ? 0xFFFFFF00 : 0xFFFFFFFF;

            var boxFontColor = FastColor.ARGB32.lerp(boxAnimator.getProgress(currentTime),
                    switch (ammoBoxAdd) {
                        case 1 -> 0xFF00FF00;
                        case -1 -> 0xFFFF0000;
                        default -> targetColor;
                    },
                    targetColor
            );

            // 弹药盒内弹药数量
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Integer.toString(
                            Math.round(boxAnimator.lerp(boxAnimator.oldValue(), boxAmmoCount, currentTime))
                    ),
                    ammoBoxX - 70,
                    h + yOffset,
                    boxFontColor,
                    true
            );

            yOffset += fontHeight;
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        poseStack.popPose();
    }
}
