package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.compat.CompatHolder;
import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper;
import com.atsuishio.superbwarfare.config.client.ReloadConfig;
import com.atsuishio.superbwarfare.data.gun.FireMode;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.send.*;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.*;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClickHandler {
    public static boolean isEditing = false;
    public static boolean switchZoom = false;

    private static boolean notInGame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return true;
        if (mc.getOverlay() != null) return true;
        if (mc.screen != null) return true;
        if (!mc.mouseHandler.isMouseGrabbed()) return true;
        return !mc.isWindowActive();
    }

    @SubscribeEvent
    public static void onButtonReleased(InputEvent.MouseButton.Pre event) {
        if (notInGame()) return;
        if (event.getAction() != InputConstants.RELEASE) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        if (player.hasEffect(ModMobEffects.SHOCK)) return;

        int button = event.getButton();
        if (button == ModKeyMappings.FIRE.getKey().getValue()) {
            handleWeaponFireRelease();
        }
        if (button == ModKeyMappings.HOLD_ZOOM.getKey().getValue()) {
            handleWeaponZoomRelease();
            return;
        }

        if (button == ModKeyMappings.SWITCH_ZOOM.getKey().getValue() && !switchZoom) {
            handleWeaponZoomRelease();
        }
    }

    @SubscribeEvent
    public static void onButtonPressed(InputEvent.MouseButton.Pre event) {
        if (notInGame()) return;
        if (event.getAction() != InputConstants.PRESS) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.isSpectator()) return;

        ItemStack stack = player.getMainHandItem();

        int button = event.getButton();

        if (stack.getItem() instanceof GunItem || stack.is(ModItems.MONITOR.get()) || stack.is(ModItems.LUNGE_MINE.get()) || player.hasEffect(ModMobEffects.SHOCK)
                || (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                event.setCanceled(true);
            }
        }

        if (player.hasEffect(ModMobEffects.SHOCK)) return;

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (stack.getItem() instanceof GunItem
                    || (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.isDriver(player) && stack.get(DataComponents.FOOD) != null)) {
                event.setCanceled(true);
            }
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            if (player.hasEffect(ModMobEffects.SHOCK)) {
                event.setCanceled(true);
            }
        }

        if (stack.getItem() instanceof GunItem
                || stack.is(ModItems.MONITOR.get())
                || stack.is(ModItems.LUNGE_MINE.get())
                || (player.getVehicle() instanceof ArmedVehicleEntity)
                || (stack.is(Items.SPYGLASS) && player.isScoping() && player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get()))) {
            if (button == ModKeyMappings.FIRE.getKey().getValue()) {
                handleWeaponFirePress(player, stack);
            }

            if (button == ModKeyMappings.HOLD_ZOOM.getKey().getValue()) {
                handleWeaponZoomPress(player, stack);
                switchZoom = false;
                return;
            }

            if (button == ModKeyMappings.SWITCH_ZOOM.getKey().getValue()) {
                handleWeaponZoomPress(player, stack);
                switchZoom = !switchZoom;
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;

        if (notInGame()) return;
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();

        if (player.hasEffect(ModMobEffects.SHOCK)) return;

        double scroll = event.getScrollDeltaY();

        // 按下自由视角键时，为载具调整相机距离
        if (player.getVehicle() instanceof VehicleEntity vehicle && player == vehicle.getFirstPassenger() && ModKeyMappings.FREE_CAMERA.isDown()) {
            ClientMouseHandler.custom3pDistance = Mth.clamp(ClientMouseHandler.custom3pDistance - event.getScrollDeltaY(), -3, 8);
            event.setCanceled(true);
            return;
        }

        // 未按下shift时，为有武器的载具切换武器
        if (!Screen.hasShiftDown()
                && player.getVehicle() instanceof VehicleEntity vehicle
                && vehicle instanceof WeaponVehicleEntity weaponVehicle
                && weaponVehicle.hasWeapon(vehicle.getSeatIndex(player))
                && weaponVehicle.banHand(player)
        ) {
            int index = vehicle.getSeatIndex(player);
            PacketDistributor.sendToServer(new SwitchVehicleWeaponMessage(index, -scroll, true));
            event.setCanceled(true);
        }

        var tag = NBTTool.getTag(stack);

        if (stack.getItem() instanceof GunItem && ClientEventHandler.zoom) {
            var data = GunData.from(stack);
            if (data.canSwitchScope()) {
                PacketDistributor.sendToServer(new SwitchScopeMessage(scroll));
            } else if (data.canAdjustZoom() || stack.is(ModItems.MINIGUN.get())) {
                PacketDistributor.sendToServer(new AdjustZoomFovMessage(scroll));
            }
            event.setCanceled(true);
        }

        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            ClientEventHandler.droneFov = Mth.clamp(ClientEventHandler.droneFov + 0.4 * scroll, 1, 6);
            event.setCanceled(true);
        }

        Entity looking = TraceTool.findLookingEntity(player, 6);
        if (looking == null) return;
        if (looking instanceof MortarEntity && player.isShiftKeyDown()) {
            PacketDistributor.sendToServer(new AdjustMortarAngleMessage(scroll));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        if (notInGame()) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.isSpectator()) return;

        ItemStack stack = player.getMainHandItem();

        int key = event.getKey();
        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (player.hasEffect(ModMobEffects.SHOCK)) return;

            if (key == Minecraft.getInstance().options.keyJump.getKey().getValue()) {
                handleDoubleJump(player);
            }

            if (key == ModKeyMappings.CONFIG.getKey().getValue() && ModKeyMappings.CONFIG.getKeyModifier().isActive(KeyConflictContext.IN_GAME)) {
                handleConfigScreen(player);
            }

            if (key == ModKeyMappings.RELOAD.getKey().getValue()) {
                ClientEventHandler.burstFireAmount = 0;
                ClickHandler.isEditing = false;
                PacketDistributor.sendToServer(new ReloadMessage(0));
            }
            if (key == ModKeyMappings.FIRE_MODE.getKey().getValue()) {
                PacketDistributor.sendToServer(new FireModeMessage(0));
            }
            if (key == ModKeyMappings.INTERACT.getKey().getValue()) {
                PacketDistributor.sendToServer(new InteractMessage(0));
            }
            if (key == ModKeyMappings.DISMOUNT.getKey().getValue()) {
                handleDismountPress(player);
            }
            if (key == ModKeyMappings.EDIT_MODE.getKey().getValue() && ClientEventHandler.burstFireAmount == 0
                    && stack.getItem() instanceof GunItem gunItem && gunItem.isCustomizable(stack)) {
                ClientEventHandler.holdFire = false;

                if (!isEditing) {
                    player.playSound(ModSounds.EDIT_MODE.get(), 1, 1);
                }
                isEditing = !isEditing;
            }

            if (key == ModKeyMappings.BREATH.getKey().getValue() && !exhaustion && zoom) {
                breath = true;
            }

            if (isEditing) {
                if (!(stack.getItem() instanceof GunItem gunItem)) return;
                if (ModKeyMappings.EDIT_GRIP.getKeyModifier().isActive(KeyConflictContext.IN_GAME)) {
                    if (key == ModKeyMappings.EDIT_GRIP.getKey().getValue() && gunItem.hasCustomGrip(stack)) {
                        PacketDistributor.sendToServer(new EditMessage(4));
                        editModelShake();
                    }
                } else {
                    if (key == ModKeyMappings.EDIT_SCOPE.getKey().getValue() && gunItem.hasCustomScope(stack)) {
                        PacketDistributor.sendToServer(new EditMessage(0));
                        editModelShake();
                    } else if (key == ModKeyMappings.EDIT_BARREL.getKey().getValue() && gunItem.hasCustomBarrel(stack)) {
                        PacketDistributor.sendToServer(new EditMessage(1));
                        editModelShake();
                    } else if (key == ModKeyMappings.EDIT_MAGAZINE.getKey().getValue() && gunItem.hasCustomMagazine(stack)) {
                        PacketDistributor.sendToServer(new EditMessage(2));
                        editModelShake();
                    } else if (key == ModKeyMappings.EDIT_STOCK.getKey().getValue() && gunItem.hasCustomStock(stack)) {
                        PacketDistributor.sendToServer(new EditMessage(3));
                        editModelShake();
                    }
                }
            }
            if (key == ModKeyMappings.SENSITIVITY_INCREASE.getKey().getValue()) {
                PacketDistributor.sendToServer(new SensitivityMessage(true));
            }
            if (key == ModKeyMappings.SENSITIVITY_REDUCE.getKey().getValue()) {
                PacketDistributor.sendToServer(new SensitivityMessage(false));
            }

            if (stack.getItem() instanceof GunItem
                    || stack.is(ModItems.MONITOR.get())
                    || (player.getVehicle() instanceof ArmedVehicleEntity iVehicle && iVehicle.isDriver(player))
                    || (stack.is(Items.SPYGLASS) && player.isScoping() && player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get()))) {
                if (key == ModKeyMappings.FIRE.getKey().getValue()) {
                    handleWeaponFirePress(player, stack);
                }

                if (key == ModKeyMappings.HOLD_ZOOM.getKey().getValue()) {
                    handleWeaponZoomPress(player, stack);
                    switchZoom = false;
                    return;
                }

                if (key == ModKeyMappings.SWITCH_ZOOM.getKey().getValue()) {
                    handleWeaponZoomPress(player, stack);
                    switchZoom = !switchZoom;
                }

                if (event.getAction() == GLFW.GLFW_RELEASE) {
                    if (key == ModKeyMappings.BREATH.getKey().getValue()) {
                        breath = false;
                    }
                }
            }

        } else {
            if (player.hasEffect(ModMobEffects.SHOCK)) return;

            if (key == ModKeyMappings.FIRE.getKey().getValue()) {
                handleWeaponFireRelease();
            }
            if (key == ModKeyMappings.HOLD_ZOOM.getKey().getValue()) {
                handleWeaponZoomRelease();
                return;
            }

            if (key == ModKeyMappings.SWITCH_ZOOM.getKey().getValue() && !switchZoom) {
                handleWeaponZoomRelease();
            }
        }
    }

    public static void handleWeaponFirePress(Player player, ItemStack stack) {
        isEditing = false;
        if (player.hasEffect(ModMobEffects.SHOCK)) return;

        if (stack.is(Items.SPYGLASS) && player.isScoping() && player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get())) {
            PacketDistributor.sendToServer(new SetFiringParametersMessage(0));
        }

        if (stack.is(ModItems.MONITOR.get())) {
            PacketDistributor.sendToServer(new DroneFireMessage(0));
        }


        if (player.getVehicle() instanceof WeaponVehicleEntity iVehicle && iVehicle.banHand(player)) {
            if (player.getVehicle() instanceof VehicleEntity pVehicle && iVehicle.hasWeapon(pVehicle.getSeatIndex(player))) {
                ClientEventHandler.holdFireVehicle = true;
            }
            return;
        }

        if (stack.is(ModItems.LUNGE_MINE.get())) {
            ClientEventHandler.holdFire = true;
        }

        if (stack.getItem() instanceof GunItem && !(player.getVehicle() != null
                && player.getVehicle() instanceof CannonEntity)
                && clientTimer.getProgress() == 0
                && !notInGame()
        ) {
            var data = GunData.from(stack);
            if (!(stack.is(ModItems.BOCEK.get()) || stack.is(ModItems.AURELIA_SCEPTRE.get()))) {
                player.playSound(ModSounds.TRIGGER_CLICK.get(), 1, 1);
            } else {
                bowPower = 0;
                holdFire = true;
                player.setSprinting(false);
                if (data.ammo.get() > 0) {
                    return;
                }
            }

            if (!data.useBackpackAmmo() && data.ammo.get() <= 0 && data.reload.time() == 0) {
                if (ReloadConfig.LEFT_CLICK_RELOAD.get()) {
                    PacketDistributor.sendToServer(new ReloadMessage(0));
                    ClientEventHandler.burstFireAmount = 0;
                }
            } else {
                PacketDistributor.sendToServer(new FireKeyMessage(0, bowPower, zoom));
                if ((!(data.reload.normal() || data.reload.empty())
                        && !data.reloading()
                        && !data.charging()
                        && !data.bolt.needed.get())
                        && drawTime < 0.01
                ) {
                    if (data.fireMode.get() == FireMode.BURST) {
                        if (ClientEventHandler.burstFireAmount == 0) {
                            ClientEventHandler.burstFireAmount = data.burstAmount();
                        }
                    } else {
                        ClientEventHandler.holdFire = true;
                        player.setSprinting(false);
                    }
                }
            }
        }
    }

    public static void handleWeaponFireRelease() {
        PacketDistributor.sendToServer(new FireKeyMessage(1, bowPower, zoom));
        bowPull = false;
        holdFire = false;
        holdFireVehicle = false;
        isEditing = false;
        customRpm = 0;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.isSpectator()) return;

        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModItems.BOCEK.get())) {
            PacketDistributor.sendToServer(new ReloadMessage(0));
        }
    }

    public static void handleWeaponZoomPress(Player player, ItemStack stack) {
        PacketDistributor.sendToServer(new ZoomMessage(0));

        ClickHandler.isEditing = false;

        if (player.getVehicle() instanceof VehicleEntity pVehicle && player.getVehicle() instanceof WeaponVehicleEntity iVehicle && iVehicle.hasWeapon(pVehicle.getSeatIndex(player)) && iVehicle.banHand(player)) {
            ClientEventHandler.zoomVehicle = true;
            return;
        }

        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        ClientEventHandler.zoom = true;
        int level = data.perk.getLevel(ModPerks.INTELLIGENT_CHIP);
        if (level > 0) {
            if (ClientEventHandler.entity == null) {
                ClientEventHandler.entity = SeekTool.seekLivingEntity(player, player.level(), 32 + 8 * (level - 1), 20);
            }
        }
    }

    public static void handleWeaponZoomRelease() {
        PacketDistributor.sendToServer(new ZoomMessage(1));
        ClientEventHandler.zoom = false;
        ClientEventHandler.zoomVehicle = false;
        ClientEventHandler.entity = null;
        breath = false;
    }

    private static void editModelShake() {
        ClientEventHandler.movePosY = -0.8;
        ClientEventHandler.fireRotTimer = 0.4;
    }

    private static void handleDoubleJump(Player player) {
        Level level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (!level.isLoaded(player.blockPosition())) {
            return;
        }

        if (canDoubleJump) {
            player.setDeltaMovement(new Vec3(player.getLookAngle().x, 0.8, player.getLookAngle().z));
            level.playLocalSound(x, y, z, ModSounds.DOUBLE_JUMP.get(), SoundSource.BLOCKS, 1, 1, false);
            PacketDistributor.sendToServer(new DoubleJumpMessage(0));
            canDoubleJump = false;
        }
    }

    private static void handleConfigScreen(Player player) {
        if (ModList.get().isLoaded(CompatHolder.CLOTH_CONFIG)) {
            CompatHolder.hasMod(CompatHolder.CLOTH_CONFIG, () -> Minecraft.getInstance().setScreen(ClothConfigHelper.getConfigScreen(null)));
        } else {
            player.displayClientMessage(Component.translatable("tips.superbwarfare.no_cloth_config").withStyle(ChatFormatting.RED), true);
        }
    }

    private static void handleDismountPress(Player player) {
        var vehicle = player.getVehicle();
        if (!(vehicle instanceof VehicleEntity)) return;

        if ((!vehicle.onGround() || vehicle.getDeltaMovement().length() >= 0.1) && ClientEventHandler.dismountCountdown <= 0) {
            player.displayClientMessage(Component.translatable("mount.onboard", ModKeyMappings.DISMOUNT.getTranslatedKeyMessage()), true);
            ClientEventHandler.dismountCountdown = 20;
            return;
        }
        PacketDistributor.sendToServer(new PlayerStopRidingMessage(0));
    }
}