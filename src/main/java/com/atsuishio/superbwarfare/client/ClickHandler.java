package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.config.client.ReloadConfig;
import com.atsuishio.superbwarfare.entity.MortarEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.send.*;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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

    @SubscribeEvent
    public static void onButtonPressed(InputEvent.MouseButton.Pre event) {
        if (notInGame()) return;
        if (event.getAction() != InputConstants.PRESS) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.isSpectator()) return;

        ItemStack stack = player.getMainHandItem();
        final var tag = NBTTool.getTag(stack);

        int button = event.getButton();

        if (stack.is(ModTags.Items.GUN) || stack.is(ModItems.MONITOR.get()) || stack.is(ModItems.LUNGE_MINE.get()) || player.hasEffect(ModMobEffects.SHOCK)
                || (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                event.setCanceled(true);
            }
        }

        if (player.hasEffect(ModMobEffects.SHOCK)) return;

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (stack.is(ModTags.Items.GUN)
                    || (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.isDriver(player) && stack.get(DataComponents.FOOD) != null)) {
                event.setCanceled(true);
            }
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            if (player.hasEffect(ModMobEffects.SHOCK)) {
                event.setCanceled(true);
            }
        }

        if (stack.is(ModTags.Items.GUN)
                || stack.is(ModItems.MONITOR.get())
                || stack.is(ModItems.LUNGE_MINE.get())
                || (player.getVehicle() instanceof ArmedVehicleEntity)
                || (stack.is(Items.SPYGLASS) && player.isScoping() && player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get()))) {
            if (button == ModKeyMappings.FIRE.getKey().getValue()) {
                handleWeaponFirePress(player, stack);
            }

            if (button == ModKeyMappings.HOLD_ZOOM.getKey().getValue()) {
                handleWeaponZoomPress(player, tag);
                switchZoom = false;
                return;
            }

            if (button == ModKeyMappings.SWITCH_ZOOM.getKey().getValue()) {
                handleWeaponZoomPress(player, tag);
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

        // 未按下shift时，为有武器的载具切换武器
        if (!Screen.hasShiftDown()
                && player.getVehicle() instanceof VehicleEntity vehicle
                && vehicle instanceof WeaponVehicleEntity weaponVehicle
                && weaponVehicle.hasWeapon(vehicle.getSeatIndex(player))
        ) {
            int index = vehicle.getSeatIndex(player);
            PacketDistributor.sendToServer(new SwitchVehicleWeaponMessage(index, -scroll, true));
            event.setCanceled(true);
        }

        var tag = NBTTool.getTag(stack);

        if (stack.is(ModTags.Items.GUN) && ClientEventHandler.zoom) {
            if (GunsTool.getGunBooleanTag(tag, "CanSwitchScope")) {
                PacketDistributor.sendToServer(new SwitchScopeMessage(scroll));
            } else if (tag.getBoolean("CanAdjustZoomFov") || stack.is(ModItems.MINIGUN.get())) {
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
        final var tag = NBTTool.getTag(stack);

        int key = event.getKey();
        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (player.hasEffect(ModMobEffects.SHOCK)) return;

            if (key == Minecraft.getInstance().options.keyJump.getKey().getValue()) {
                handleDoubleJump(player);
            }

            // TODO do we need cloth config?
//            if (key == ModKeyMappings.CONFIG.getKey().getValue() && ModKeyMappings.CONFIG.getKeyModifier().isActive(KeyConflictContext.IN_GAME)) {
//                handleConfigScreen(player);
//            }

            if (key == ModKeyMappings.RELOAD.getKey().getValue()) {
                ClientEventHandler.burstFireSize = 0;
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
            if (key == ModKeyMappings.EDIT_MODE.getKey().getValue() && ClientEventHandler.burstFireSize == 0) {
                ClientEventHandler.holdFire = false;
                PacketDistributor.sendToServer(new EditModeMessage(0));
            }

            var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
            if (cap != null && cap.edit) {
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

            if (stack.is(ModTags.Items.GUN)
                    || stack.is(ModItems.MONITOR.get())
                    || (player.getVehicle() instanceof ArmedVehicleEntity iVehicle && iVehicle.isDriver(player))
                    || (stack.is(Items.SPYGLASS) && player.isScoping() && player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get()))) {
                if (key == ModKeyMappings.FIRE.getKey().getValue()) {
                    handleWeaponFirePress(player, stack);
                }

                if (key == ModKeyMappings.HOLD_ZOOM.getKey().getValue()) {
                    handleWeaponZoomPress(player, tag);
                    switchZoom = false;
                    return;
                }

                if (key == ModKeyMappings.SWITCH_ZOOM.getKey().getValue()) {
                    handleWeaponZoomPress(player, tag);
                    switchZoom = !switchZoom;
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

        var tag = NBTTool.getTag(stack);
        if (stack.getItem() instanceof GunItem gunItem && !(player.getVehicle() != null
                && player.getVehicle() instanceof CannonEntity) && clientTimer.getProgress() == 0 && cantFireTime == 0
                && (!(tag.getBoolean("is_normal_reloading") || tag.getBoolean("is_empty_reloading"))
                && !GunsTool.getGunBooleanTag(tag, "Reloading")
                && !GunsTool.getGunBooleanTag(tag, "Charging")
                && !GunsTool.getGunBooleanTag(tag, "NeedBoltAction"))
                && cantFireTime == 0
                && drawTime < 0.01
                && !notInGame()
        ) {
            if ((!(tag.getBoolean("is_normal_reloading") || tag.getBoolean("is_empty_reloading"))
                    && !GunsTool.getGunBooleanTag(tag, "Reloading")
                    && !GunsTool.getGunBooleanTag(tag, "Charging")
                    && !GunsTool.getGunBooleanTag(tag, "NeedBoltAction"))
                    && cantFireTime == 0
                    && drawTime < 0.01
                    && !notInGame()) {
                player.playSound(ModSounds.TRIGGER_CLICK.get(), 1, 1);
            }

            if (!gunItem.useBackpackAmmo(stack) && GunsTool.getGunIntTag(tag, "Ammo") <= 0 && GunsTool.getGunIntTag(tag, "ReloadTime") == 0) {
                if (ReloadConfig.LEFT_CLICK_RELOAD.get()) {
                    PacketDistributor.sendToServer(new ReloadMessage(0));
                    ClientEventHandler.burstFireSize = 0;
                }
            } else {
                PacketDistributor.sendToServer(new FireMessage(0));
                if (GunsTool.getGunIntTag(tag, "FireMode") == 1) {
                    if (ClientEventHandler.burstFireSize == 0) {
                        ClientEventHandler.burstFireSize = GunsTool.getGunIntTag(tag, "BurstSize");
                    }
                } else {
                    if (!stack.is(ModItems.BOCEK.get())) {
                        ClientEventHandler.holdFire = true;
                    }
                }
            }
        }
    }

    public static void handleWeaponFireRelease() {
        PacketDistributor.sendToServer(new FireMessage(1));
        ClientEventHandler.holdFire = false;
        ClientEventHandler.holdFireVehicle = false;
        ClientEventHandler.customRpm = 0;
    }

    public static void handleWeaponZoomPress(Player player, final CompoundTag tag) {
        PacketDistributor.sendToServer(new ZoomMessage(0));

        if (player.getVehicle() instanceof VehicleEntity pVehicle && player.getVehicle() instanceof WeaponVehicleEntity iVehicle && iVehicle.hasWeapon(pVehicle.getSeatIndex(player))) {
            ClientEventHandler.zoomVehicle = true;
            return;
        }

        ClientEventHandler.zoom = true;
        int level = PerkHelper.getItemPerkLevel(ModPerks.INTELLIGENT_CHIP.get(), tag);
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

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap != null && cap.playerDoubleJump) {
            player.setDeltaMovement(new Vec3(player.getLookAngle().x, 0.8, player.getLookAngle().z));
            level.playLocalSound(x, y, z, ModSounds.DOUBLE_JUMP.get(), SoundSource.BLOCKS, 1, 1, false);

            PacketDistributor.sendToServer(new DoubleJumpMessage(false));
        }
    }

    // TODO do we need cloth config?
//    private static void handleConfigScreen(Player player) {
//        if (ModList.get().isLoaded(CompatHolder.CLOTH_CONFIG)) {
//            CompatHolder.hasMod(CompatHolder.CLOTH_CONFIG, () -> Minecraft.getInstance().setScreen(ClothConfigHelper.getConfigScreen(null)));
//        } else {
//            player.displayClientMessage(Component.translatable("tips.superbwarfare.no_cloth_config").withStyle(ChatFormatting.RED), true);
//        }
//    }

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