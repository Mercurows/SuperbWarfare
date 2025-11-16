package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.client.screens.WeaponEditScreen;
import com.atsuishio.superbwarfare.compat.CompatHolder;
import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper;
import com.atsuishio.superbwarfare.config.client.ReloadConfig;
import com.atsuishio.superbwarfare.data.gun.FireMode;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.ItemScreenProvider;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.send.*;
import com.atsuishio.superbwarfare.resource.gun.GunResource;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
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

    private static boolean cancelFireKey(Player player, ItemStack stack) {
        return stack.getItem() instanceof GunItem || stack.is(ModItems.MONITOR.get()) || stack.is(ModItems.LUNGE_MINE.get()) || stack.is(ModItems.ARTILLERY_INDICATOR.get()) || player.hasEffect(ModMobEffects.SHOCK)
                || (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player));
    }

    private static boolean cancelZoomKey(Player player, ItemStack stack) {
        return stack.getItem() instanceof GunItem
                || (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getFirstPassenger() == player && !stack.has(DataComponents.FOOD));
    }

    @SubscribeEvent
    public static void onButtonPressed(InputEvent.MouseButton.Pre event) {
        if (notInGame()) return;
        if (event.getAction() != InputConstants.PRESS) return;

        var mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        if (player.isSpectator()) return;

//        player.displayClientMessage(Component.literal("Mouse " + event.getButton() + " Pressed"), false);

        ItemStack stack = player.getMainHandItem();

        int button = event.getButton();

        var fireKey = ModKeyMappings.FIRE.getKey();
        if (fireKey.getType() == InputConstants.Type.MOUSE
                && fireKey.getValue() == button
                && cancelFireKey(player, stack)
        ) {
            event.setCanceled(true);
        }

        if (player.hasEffect(ModMobEffects.SHOCK)) return;

        var zoomKey = ModKeyMappings.HOLD_ZOOM.getKey();
        if (zoomKey.getType() == InputConstants.Type.MOUSE
                && zoomKey.getValue() == button
                && cancelZoomKey(player, stack)
        ) {
            event.setCanceled(true);
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            if (player.hasEffect(ModMobEffects.SHOCK)) {
                event.setCanceled(true);
                return;
            }
            if (stack.is(ModItems.ARTILLERY_INDICATOR.get())) {
                event.setCanceled(true);
            }
            if (stack.is(ModItems.MONITOR.get()) && player.getOffhandItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
                event.setCanceled(true);
            }
        }

        if (button == ModKeyMappings.MARK.getKey().getValue()) {
            if (stack.is(ModItems.ARTILLERY_INDICATOR.get())) {
                PacketDistributor.sendToServer(SetFiringParametersMessage.INSTANCE);
            }
            if (stack.is(ModItems.MONITOR.get()) && player.getOffhandItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
                droneLeftClick(stack, player);
            }
        }

        if (stack.getItem() instanceof GunItem
                || stack.is(ModItems.MONITOR.get())
                || stack.is(ModItems.LUNGE_MINE.get())
                || player.getVehicle() instanceof VehicleEntity
                || (stack.is(Items.SPYGLASS) && player.isScoping() && player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get()))
                || (stack.is(ModItems.ARTILLERY_INDICATOR.get()))
        ) {
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

    // 枪械交互时禁止挥舞手臂
    @SubscribeEvent
    public static void stopSwing(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getItemInHand(event.getHand()).getItem() instanceof GunItem) {
            event.setSwingHand(false);
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
                && vehicle.banHand(player)
        ) {
            if (switchVehicleWeaponCooldown <= 0) {
                int index = vehicle.getSeatIndex(player);
                PacketDistributor.sendToServer(new SwitchVehicleWeaponMessage(index, -scroll, true));
                switchVehicleWeaponCooldown = 3;
            }
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

        if (player.isUsingItem() && player.getUseItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
            artilleryIndicatorCustomZoom = Mth.clamp(artilleryIndicatorCustomZoom + 0.4 * scroll, -2, 6);
            event.setCanceled(true);
        }

        Entity looking = TraceTool.findLookingEntity(player, 6);
        if (looking instanceof MortarEntity && player.isShiftKeyDown()) {
            PacketDistributor.sendToServer(new AdjustMortarAngleMessage(scroll));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        if (notInGame()) return;

        var mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        int key = event.getKey();
        if (key < 0) return;

        if (key == ModKeyMappings.DISMOUNT.getKey().getValue()) {
            handleDismountPress(player);
        }

        if (player.isSpectator()) return;

        var stack = player.getMainHandItem();

        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (player.hasEffect(ModMobEffects.SHOCK)) return;

            if (key == Minecraft.getInstance().options.keyJump.getKey().getValue()) {
                handleDoubleJump(player);
                handleParachute();
            }

            if (key == ModKeyMappings.CONFIG.getKey().getValue() && ModKeyMappings.CONFIG.getKeyModifier().isActive(KeyConflictContext.IN_GAME)) {
                handleConfigScreen(player);
            }

            if (key == ModKeyMappings.RELOAD.getKey().getValue()) {
                burstFireAmount = 0;
                isEditing = false;
                seekingTime = 0;
                lockOn = false;
                lockingEntity = null;
                seekingEntity = null;
                lockingPos = null;
                PacketDistributor.sendToServer(ReloadMessage.INSTANCE);
            }
            if (key == ModKeyMappings.FIRE_MODE.getKey().getValue() || key == ModKeyMappings.CHANGE_FIRE_MODE_BACKWARD.getKey().getValue()) {
                PacketDistributor.sendToServer(new FireModeMessage(false));
                burstFireAmount = 0;
            }
            if (key == ModKeyMappings.CHANGE_FIRE_MODE_FORWARD.getKey().getValue()) {
                PacketDistributor.sendToServer(new FireModeMessage(true));
                burstFireAmount = 0;
            }
            if (key == ModKeyMappings.INTERACT.getKey().getValue()) {
                if (stack.getItem() instanceof GunItem) {
                    KeyMapping.click(mc.options.keyUse.getKey());
                } else if (stack.is(ModItems.MONITOR.get())) {
                    PacketDistributor.sendToServer(InteractMessage.INSTANCE);
                }
            }

            // 玩家手持枪械时，处理卸弹/切换弹种
            if (stack.getItem() instanceof GunItem) {
                var data = GunData.from(stack);
                if (key == ModKeyMappings.UNLOAD.getKey().getValue()) {
                    if (data.useBackpackAmmo() || data.ammo.get() + data.virtualAmmo.get() <= 0) return;
                    PacketDistributor.sendToServer(UnloadMessage.INSTANCE);
                    burstFireAmount = 0;
                }
                if (data.compute().getAmmoConsumers().size() > 1) {
                    if (key == ModKeyMappings.CHANGE_AMMO_FORWARD.getKey().getValue()) {
                        PacketDistributor.sendToServer(new EditMessage(5, false, false));
                        burstFireAmount = 0;
                    } else if (key == ModKeyMappings.CHANGE_AMMO_BACKWARD.getKey().getValue()) {
                        PacketDistributor.sendToServer(new EditMessage(5, true, false));
                        burstFireAmount = 0;
                    }
                }
            }

            // 玩家位于载具上时，处理切换弹种
            if (player.getVehicle() instanceof VehicleEntity vehicle) {
                var data = vehicle.getGunData(player);
                if (data != null && data.getDefault().getAmmoConsumers().size() > 1) {
                    if (key == ModKeyMappings.CHANGE_AMMO_FORWARD.getKey().getValue()) {
                        PacketDistributor.sendToServer(new EditMessage(5, false, true));
                        burstFireAmount = 0;
                    }
                    if (key == ModKeyMappings.CHANGE_AMMO_BACKWARD.getKey().getValue() ||
                            key == ModKeyMappings.FIRE_MODE.getKey().getValue()) {
                        PacketDistributor.sendToServer(new EditMessage(5, true, true));
                        burstFireAmount = 0;
                    }
                }
            }

            if (key == ModKeyMappings.EDIT_MODE.getKey().getValue()) {
                if (stack.getItem() instanceof ItemScreenProvider provider) {
                    var screen = provider.getItemScreen(stack, player, InteractionHand.MAIN_HAND);
                    if (screen != null) {
                        Minecraft.getInstance().setScreen(screen);
                        if (screen instanceof WeaponEditScreen) {
                            ClientEventHandler.onOpenEditScreen();
                        }
                        return;
                    }
                }
                ItemStack offHand = player.getOffhandItem();
                if (offHand.getItem() instanceof ItemScreenProvider provider) {
                    var screen = provider.getItemScreen(offHand, player, InteractionHand.OFF_HAND);
                    if (screen != null) {
                        Minecraft.getInstance().setScreen(screen);
                        return;
                    }
                }
            }

            if (key == ModKeyMappings.BREATH.getKey().getValue() && !exhaustion && zoom) {
                breath = true;
            }
            if (key == ModKeyMappings.SENSITIVITY_INCREASE.getKey().getValue()) {
                PacketDistributor.sendToServer(new SensitivityMessage(true));
            }
            if (key == ModKeyMappings.SENSITIVITY_REDUCE.getKey().getValue()) {
                PacketDistributor.sendToServer(new SensitivityMessage(false));
            }

            if (stack.getItem() instanceof GunItem
                    || stack.is(ModItems.MONITOR.get())
                    || (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getFirstPassenger() == player)
                    || (stack.is(Items.SPYGLASS) && player.isScoping() && player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get()))
                    || (stack.is(ModItems.ARTILLERY_INDICATOR.get()))
            ) {
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

            if (key == ModKeyMappings.MARK.getKey().getValue()) {
                if (stack.is(ModItems.ARTILLERY_INDICATOR.get())) {
                    PacketDistributor.sendToServer(SetFiringParametersMessage.INSTANCE);
                }
                if (stack.is(ModItems.MONITOR.get()) && player.getOffhandItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
                    droneLeftClick(stack, player);
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

        if (player.getVehicle() instanceof VehicleEntity pVehicle && pVehicle.banHand(player)) {
            if (player.getVehicle() instanceof WeaponVehicleEntity iVehicle && iVehicle.hasWeapon(pVehicle.getSeatIndex(player))) {
                ClientEventHandler.holdFireVehicle = true;
            }
            return;
        }

        if (stack.is(ModItems.ARTILLERY_INDICATOR.get())) {
            ClientEventHandler.holdingFireKey = true;
        }

        if (stack.is(Items.SPYGLASS) && player.isScoping() && player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get())) {
            PacketDistributor.sendToServer(SetFiringParametersMessage.INSTANCE);
        }

        if (stack.is(ModItems.MONITOR.get())) {
            if (player.getOffhandItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
                ClientEventHandler.holdingFireKey = true;
            } else {
                droneLeftClick(stack, player);
            }
        }

        if (stack.is(ModItems.LUNGE_MINE.get())) {
            ClientEventHandler.usingLunge = true;
        }

        if (stack.getItem() instanceof GunItem gunItem
                && clientTimer.getProgress() == 0
                && !notInGame()
        ) {
            var data = GunData.from(stack);
            var resource = GunResource.compute(stack);

            // TODO 整合特殊处理
            if (!(stack.is(ModItems.BOCEK.get()) || stack.is(ModItems.AURELIA_SCEPTRE.get()))) {
                if (!data.meleeOnly()) {
                    // 普通枪（？）
                    if (stack.is(ModItems.QL_1031.get()) && data.selectedFireModeInfo().name.equals("Hold") && gunItem.canShoot(data, player)) {
                        player.playSound(ModSounds.QL_1031_CHARGE.get(), 1, 1);
                        shouldPlayDischargeSound = true;
                    }

                    var triggerSound = resource.triggerSound;
                    if (triggerSound != null && !data.meleeOnly()) {
                        player.playSound(triggerSound, 1, 1);
                    }
                }
            } else {
                // 波塞克、海月权杖特殊处理
                bowPower = 0;
                holdingFireKey = true;
                player.setSprinting(false);
                if (data.hasEnoughAmmoToShoot(player)) {
                    return;
                }
            }

            if (!data.useBackpackAmmo() && !data.meleeOnly() && !data.hasEnoughAmmoToShoot(player) && data.reload.time() == 0) {
                if (ReloadConfig.LEFT_CLICK_RELOAD.get()) {
                    PacketDistributor.sendToServer(ReloadMessage.INSTANCE);
                    burstFireAmount = 0;
                    seekingTime = 0;
                    lockOn = false;
                    lockingEntity = null;
                    seekingEntity = null;
                    lockingPos = null;
                }
            } else {
                PacketDistributor.sendToServer(new FireKeyMessage(0, bowPower, zoom));
                if ((!data.reloading()
                        && !data.charging()
                        && !data.bolt.needed.get())
                        && drawTime < 0.01
                ) {
                    var fireMode = data.selectedFireModeInfo().mode;

                    if (fireMode == FireMode.BURST) {
                        if (ClientEventHandler.burstFireAmount == 0) {
                            noSprintTicks = 8;
                            player.setSprinting(false);
                            ClientEventHandler.burstFireAmount = data.compute().burstAmount;
                        }
                    } else if (fireMode == FireMode.SEMI) {
                        if (ClientEventHandler.burstFireAmount == 0) {
                            noSprintTicks = 3;
                            player.setSprinting(false);
                            ClientEventHandler.burstFireAmount = 1;
                        }
                    }

                    ClientEventHandler.holdingFireKey = true;
                    player.setSprinting(false);
                }
            }
        }
    }

    public static void handleWeaponFireRelease() {
        PacketDistributor.sendToServer(new FireKeyMessage(1, bowPower, zoom));
        bowPull = false;
        holdingFireKey = false;
        holdFireVehicle = false;
        isEditing = false;
        customRpm = 0;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.isSpectator()) return;

        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModItems.BOCEK.get())) {
            PacketDistributor.sendToServer(ReloadMessage.INSTANCE);
        }
    }

    public static void handleWeaponZoomPress(Player player, ItemStack stack) {
        PacketDistributor.sendToServer(new ZoomMessage(0));

        isEditing = false;

        if (player.getVehicle() instanceof VehicleEntity pVehicle && player.getVehicle() instanceof WeaponVehicleEntity iVehicle && iVehicle.hasWeapon(pVehicle.getSeatIndex(player)) && pVehicle.banHand(player)) {
            ClientEventHandler.zoomVehicle = true;
            return;
        }

        if (!(stack.getItem() instanceof GunItem)) return;

        if (!GunResource.compute(stack).canZoom) return;

        var data = GunData.from(stack);
        ClientEventHandler.zoom = true;

        int level = data.perk.getLevel(ModPerks.INTELLIGENT_CHIP);
        if (level > 0) {
            if (ClientEventHandler.lockedEntity == null) {
                if (data.perk.has(ModPerks.PHASE_PENETRATING_BULLET.get()) || data.perk.has(ModPerks.BEAST_BULLET.get())) {
                    ClientEventHandler.lockedEntity = SeekTool.seekEntityThroughWall(player, 32 + 8 * (level - 1), 20);
                } else {
                    ClientEventHandler.lockedEntity = SeekTool.seekLivingEntity(player, 32 + 8 * (level - 1), 20);
                }
            }
        }
    }

    public static void handleWeaponZoomRelease() {
        PacketDistributor.sendToServer(new ZoomMessage(1));
        ClientEventHandler.zoom = false;
        ClientEventHandler.zoomVehicle = false;
        ClientEventHandler.lockedEntity = null;
        breath = false;
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
            PacketDistributor.sendToServer(DoubleJumpMessage.INSTANCE);
            canDoubleJump = false;
        }
    }

    private static void handleParachute() {
        PacketDistributor.sendToServer(ParachuteMessage.INSTANCE);
    }

    private static void handleConfigScreen(Player player) {
        if (ModList.get().isLoaded(CompatHolder.CLOTH_CONFIG)) {
            CompatHolder.hasMod(CompatHolder.CLOTH_CONFIG, () -> Minecraft.getInstance().setScreen(ClothConfigHelper.getConfigScreen(null)));
        } else {
            player.displayClientMessage(Component.translatable("tips.superbwarfare.no_cloth_config").withStyle(ChatFormatting.RED), true);
        }
    }

    private static void handleDismountPress(Player player) {
        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            if ((!vehicle.onGround() || vehicle.getDeltaMovement().length() >= 0.1) && ClientEventHandler.dismountCountdown <= 0) {
                if (vehicle.allowEjection(vehicle.getSeatIndex(player))) {
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.mount.onboard", ModKeyMappings.DISMOUNT.getTranslatedKeyMessage()), true);
                } else {
                    player.displayClientMessage(Component.translatable("mount.onboard", ModKeyMappings.DISMOUNT.getTranslatedKeyMessage()), true);
                }

                ClientEventHandler.dismountCountdown = 20;
                return;
            }
            PacketDistributor.sendToServer(new PlayerStopRidingMessage(false));
        }
    }

    public static void droneLeftClick(ItemStack stack, Player player) {
        var tag = NBTTool.getTag(stack);
        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            DroneEntity drone = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));
            if (drone != null) {
                boolean lookAtEntity = false;

                Entity lookingEntity = SeekTool.seekLivingEntity(drone, 512, 2 / droneFovLerp);

                BlockHitResult result = player.level().clip(new ClipContext(drone.getEyePosition(), drone.getEyePosition().add(drone.getLookAngle().scale(512)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, drone));
                Vec3 pos = result.getLocation();

                if (lookingEntity != null && !player.isShiftKeyDown()) {
                    lookAtEntity = true;
                }

                if (lookAtEntity) {
                    pos = lookingEntity.position();
                }

                PacketDistributor.sendToServer(new DroneFireMessage(pos.toVector3f()));
            }
        }
    }
}