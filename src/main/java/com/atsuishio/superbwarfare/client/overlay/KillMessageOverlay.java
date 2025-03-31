package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.config.client.KillMessageConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.KillMessageHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.PlayerKillRecord;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class KillMessageOverlay {

    private static final ResourceLocation BEAST = ModUtils.loc("textures/screens/damage_types/beast.png");

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (!KillMessageConfig.SHOW_KILL_MESSAGE.get()) {
            return;
        }

        Player player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        if (KillMessageHandler.QUEUE.isEmpty()) {
            return;
        }

        float totalTop = 5;

        var arr = KillMessageHandler.QUEUE.toArray(new PlayerKillRecord[0]);
        var record = arr[0];

        if (record.freeze) {
            for (var playerKillRecord : arr) {
                playerKillRecord.freeze = false;
            }
        }

        if (record.tick >= 80) {
            if (arr.length > 1 && record.tick - arr[1].tick < (record.fastRemove ? 2 : 20)) {
                arr[1].fastRemove = true;
                record.fastRemove = true;
                for (int j = 1; j < arr.length; j++) {
                    arr[j].freeze = true;
                }
            }
        }

        for (PlayerKillRecord r : KillMessageHandler.QUEUE) {
            totalTop = renderKillMessages(r, event, totalTop);
        }
    }

    private static float renderKillMessages(PlayerKillRecord record, RenderGuiEvent.Pre event, float baseTop) {
        int w = event.getWindow().getGuiScaledWidth();
        float top = baseTop;

        Font font = Minecraft.getInstance().font;

        AtomicReference<String> targetName = new AtomicReference<>(record.target.getDisplayName().getString());
        if (record.target instanceof Player targetPlayer) {
            targetName.set(targetPlayer.getName().getString() + " Senpai");
        }

        int targetNameWidth = font.width(targetName.get());

        var gui = event.getGuiGraphics();
        gui.pose().pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE
        );

        // 入场效果
        if (record.tick < 3) {
            gui.pose().translate((3 - record.tick - event.getPartialTick()) * 33, 0, 0);
        }

        // 4s后开始消失
        if (record.tick >= 80) {
            int animationTickCount = record.fastRemove ? 2 : 20;
            float rate = (float) Math.pow((record.tick + event.getPartialTick() - 80) / animationTickCount, 5);
            gui.pose().translate(rate * 100, 0, 0);
            gui.setColor(1, 1, 1, 1 - rate);
            baseTop += 10 * (1 - rate);
        } else {
            baseTop += 10;
        }

        // 击杀提示是右对齐的，这里从右向左渲染

        // 渲染被击杀者名称
        gui.drawString(
                Minecraft.getInstance().font,
                targetName.get(),
                w - targetNameWidth - 10f,
                top,
                record.target.getTeamColor(),
                false
        );

        // 第一个图标：爆头/爆炸/近战等图标
        int damageTypeIconW = w - targetNameWidth - 28;

        ResourceLocation damageTypeIcon = getDamageTypeIcon(record);

        if (damageTypeIcon != null) {
            preciseBlit(gui,
                    damageTypeIcon,
                    damageTypeIconW,
                    top - 2,
                    0,
                    0,
                    12,
                    12,
                    12,
                    12
            );
        }

        Player player = record.attacker;
        boolean renderItem = false;
        int itemIconW = damageTypeIcon != null ? w - targetNameWidth - 64 : w - targetNameWidth - 46;

        if (player != null && player.getVehicle() instanceof VehicleEntity vehicleEntity) {
            // 载具图标
            if ((vehicleEntity instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player)) || record.damageType == ModDamageTypes.VEHICLE_STRIKE) {
                renderItem = true;

                ResourceLocation resourceLocation = vehicleEntity.getVehicleIcon();

                preciseBlit(gui,
                        resourceLocation,
                        itemIconW,
                        top,
                        0,
                        0,
                        32,
                        8,
                        -32,
                        8
                );
            } else {
                if (record.stack.getItem() instanceof GunItem gunItem) {
                    renderItem = true;

                    ResourceLocation resourceLocation = gunItem.getGunIcon();

                    preciseBlit(gui,
                            resourceLocation,
                            itemIconW,
                            top,
                            0,
                            0,
                            32,
                            8,
                            -32,
                            8
                    );
                }
            }
        } else {
            // 如果是枪械击杀，则渲染枪械图标
            if (record.stack.getItem() instanceof GunItem gunItem) {
                renderItem = true;

                ResourceLocation resourceLocation = gunItem.getGunIcon();

                preciseBlit(gui,
                        resourceLocation,
                        itemIconW,
                        top,
                        0,
                        0,
                        32,
                        8,
                        -32,
                        8
                );
            }
        }

        // 渲染击杀者名称
        AtomicReference<String> attackerName = new AtomicReference<>("Senpai");
        if (record.attacker != null) {
            attackerName.set(record.attacker.getName().getString() + " Senpai");
        }

        int attackerNameWidth = font.width(attackerName.get());
        int nameW = w - targetNameWidth - 16 - attackerNameWidth;
        if (renderItem) {
            nameW -= 32;
        }
        if (damageTypeIcon != null) {
            nameW -= 18;
        }

        gui.drawString(
                Minecraft.getInstance().font,
                attackerName.get(),
                nameW,
                top,
                record.attacker.getTeamColor(),
                false
        );

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        gui.setColor(1, 1, 1, 1);
        gui.pose().popPose();

        return baseTop;
    }

    @Nullable
    private static ResourceLocation getDamageTypeIcon(PlayerKillRecord record) {
        return BEAST;
    }
}
