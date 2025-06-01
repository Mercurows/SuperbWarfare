package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.data.gun.FireMode;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.send.*;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.*;
import com.atsuishio.superbwarfare.tools.animation.AnimationCurves;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    public static double zoomTime = 0;
    public static double zoomPos = 0;
    public static double zoomPosZ = 0;
    public static double swayTime = 0;
    public static double swayX = 0;
    public static double swayY = 0;
    public static double moveXTime = 0;
    public static double moveYTime = 0;
    public static double movePosX = 0;
    public static double movePosY = 0;
    public static double moveRotZ = 0;
    public static double movePosHorizon = 0;
    public static double velocityY = 0;

    public static double[] turnRot = {0, 0, 0};
    public static double[] cameraRot = {0, 0, 0};

    public static double fireRecoilTime = 0;
    public static double firePosTimer = 0;
    public static double fireRotTimer = 0;
    public static double firePos = 0;
    public static double firePosZ = 0;
    public static double fireRot = 0;

    public static double recoilTime = 0;

    public static double recoilHorizon = 0;
    public static double recoilY = 0;

    public static double droneFov = 1;
    public static double droneFovLerp = 1;
    public static double fov = 0;
    public static double bowPullTimer = 0;
    public static double bowPower = 0;
    public static double bowPullPos = 0;
    public static double gunSpread = 0;
    public static double fireSpread = 0;
    public static double cantFireTime = 0;
    public static double lookDistance = 0;
    public static double cameraLocation = 0.6;

    public static double drawTime = 1;

    public static int shellIndex = 0;
    public static double[] shellIndexTime = {0, 0, 0, 0, 0, 0};
    public static double[] randomShell = {0, 0, 0};

    public static double customZoom = 0;
    public static MillisTimer clientTimer = new MillisTimer();
    public static MillisTimer clientTimerVehicle = new MillisTimer();

    public static boolean holdFire = false;
    public static boolean bowPull = false;

    public static boolean zoom = false;
    public static boolean breath = false;
    public static boolean tacticalSprint = false;
    public static float stamina = 0;
    public static double switchTime = 0;
    public static double moveFadeTime = 0;

    public static boolean exhaustion = false;
    public static boolean holdFireVehicle = false;

    public static boolean zoomVehicle = false;
    public static int burstFireAmount = 0;

    public static int customRpm = 0;

    public static double chamberRot = 0;
    public static double actionMove = 0;

    public static int shootDelay = 0;

    public static double revolverPreTime = 0;
    public static double revolverWheelPreTime = 0;

    public static double shakeTime = 0;
    public static double shakeRadius = 0;
    public static double shakeAmplitude = 0;
    public static double[] shakePos = {0, 0, 0};
    public static double shakeType = 0;
    public static int lungeAttack;
    public static int lungeDraw;
    public static int gunMelee;
    public static int lungeSprint;
    public static Entity entity;

    public static int dismountCountdown = 0;
    public static int aimVillagerCountdown = 0;

    public static CameraType lastCameraType;

    public static float cameraPitch;
    public static float cameraYaw;
    public static float cameraRoll;
    public static float cantSprint = 0;

    public static boolean canDoubleJump = false;


    @SubscribeEvent
    public static void handleWeaponTurn(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float xRotOffset = Mth.lerp(event.getPartialTick(), player.xBobO, player.xBob);
        float yRotOffset = Mth.lerp(event.getPartialTick(), player.yBobO, player.yBob);
        float xRot = player.getViewXRot(event.getPartialTick()) - xRotOffset;
        float yRot = player.getViewYRot(event.getPartialTick()) - yRotOffset;
        turnRot[0] = Mth.clamp(0.05 * xRot, -5, 5) * (1 - 0.75 * zoomTime);
        turnRot[1] = Mth.clamp(0.05 * yRot, -10, 10) * (1 - 0.75 * zoomTime);
        turnRot[2] = Mth.clamp(0.1 * yRot, -10, 10) * (1 - zoomTime);
    }

    private static boolean notInGame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return true;
        if (mc.getOverlay() != null) return true;
        if (mc.screen != null) return true;
        if (!mc.mouseHandler.isMouseGrabbed()) return true;
        return !mc.isWindowActive();
    }

    public static boolean isFreeCam(Player player) {
        return player.getVehicle() instanceof VehicleEntity vehicle && vehicle.allowFreeCam() && ModKeyMappings.FREE_CAMERA.isDown();
    }

    private static boolean isMoving() {
        Player player = Minecraft.getInstance().player;
        return Minecraft.getInstance().options.keyLeft.isDown()
                || Minecraft.getInstance().options.keyRight.isDown()
                || Minecraft.getInstance().options.keyUp.isDown()
                || Minecraft.getInstance().options.keyDown.isDown()
                || (player != null && player.isSprinting());
    }

    static short keysCache = 0;

    @SubscribeEvent
    public static void handleClientTick(TickEvent.ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        ItemStack stack = player.getMainHandItem();

        // 射击延迟
        if (stack.is(ModTags.Items.GUN)) {
            var data = GunData.from(stack);

            if (holdFire || (zoom && stack.is(ModItems.MINIGUN.get()))) {
                shootDelay = Math.min(shootDelay + 2, data.shootDelay() + 1);

                // 加特林特有的旋转音效
                if (stack.is(ModItems.MINIGUN.get())) {
                    float rpm = (float) data.rpm() / 3600;
                    player.playSound(ModSounds.MINIGUN_ROT.get(), 1, 0.7f + rpm);
                }

            }
        }

        if (notInGame() && !ClickHandler.switchZoom) {
            zoom = false;
        }

        var options = Minecraft.getInstance().options;
        short keys = 0;

        // 正在游戏内控制载具或无人机
        if (!notInGame() && (player.getVehicle() instanceof MobileVehicleEntity mobileVehicle && mobileVehicle.getFirstPassenger() == player)
                || (stack.is(ModItems.MONITOR.get()) && ItemNBTTool.getBoolean(stack, "Using", false) && ItemNBTTool.getBoolean(stack, "Linked", false))
        ) {
            if (options.keyLeft.isDown()) {
                keys |= 0b000000001;
            }
            if (options.keyRight.isDown()) {
                keys |= 0b000000010;
            }
            if (options.keyUp.isDown()) {
                keys |= 0b000000100;
            }
            if (options.keyDown.isDown()) {
                keys |= 0b000001000;
            }
            if (options.keyJump.isDown()) {
                keys |= 0b000010000;
            }
            if (options.keyShift.isDown()) {
                keys |= 0b000100000;
            }
            if (ModKeyMappings.RELEASE_DECOY.isDown()) {
                keys |= 0b001000000;
            }
            if (holdFireVehicle) {
                keys |= 0b010000000;
            }
            if (options.keySprint.isDown()) {
                keys |= 0b100000000;
            }
        }

        if (keys != keysCache) {
            Mod.PACKET_HANDLER.sendToServer(new VehicleMovementMessage(keys));
            keysCache = keys;
        }

        if (player.onGround() && canDoubleJump) {
            canDoubleJump = false;
        }

        isProne(player);
        beamShoot(player, stack);
        handleVariableDecrease();
        aimAtVillager(player);
        CrossHairOverlay.handleRenderDamageIndicator();
        staminaSystem();
        handlePlayerSprint();
        handleLungeAttack(player, stack);
        handleGunMelee(player, stack);
    }

    // 耐力
    public static void staminaSystem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;

        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        tacticalSprint = MiscConfig.ALLOW_TACTICAL_SPRINT.get()
                && !exhaustion
                && !zoom
                && isMoving()
                && player.isSprinting()
                && player.getVehicle() == null
                && !player.getAbilities().flying;

        ItemStack stack = player.getMainHandItem();

        float sprintCost;

        if (stack.getItem() instanceof GunItem) {
            var data = GunData.from(stack);
            sprintCost = (float) (0.5 + 0.02 * data.weight());
        } else {
            sprintCost = 0.5f;
        }

        if (breath) {
            stamina += 0.5f;
        } else if (tacticalSprint) {
            stamina += sprintCost;
        } else if (stamina > 0) {
            stamina = Math.max(stamina - 0.5f, 0);
        }

        if (stamina >= 100) {
            exhaustion = true;
            breath = false;
            tacticalSprint = false;
        }

        if (exhaustion && stamina <= 0) {
            exhaustion = false;
        }

        if ((ModKeyMappings.BREATH.isDown() && zoom) || (tacticalSprint)) {
            switchTime = Math.min(switchTime + 0.65, 5);
        } else if (switchTime > 0 && stamina == 0) {
            switchTime = Math.max(switchTime - 0.15, 0);
        }

        if (zoom) {
            tacticalSprint = false;
        }

        if (tacticalSprint && (player.onGround() || player.jumping)) {
            Mod.PACKET_HANDLER.sendToServer(new TacticalSprintMessage(true));
        } else {
            Mod.PACKET_HANDLER.sendToServer(new TacticalSprintMessage(false));
        }
    }

    /**
     * 禁止玩家奔跑
     */
    private static void handlePlayerSprint() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (player.isShiftKeyDown()
                || player.isPassenger()
                || player.isInWater()
                || ClientEventHandler.zoom) {
            cantSprint = 3;
        }

        if (cantSprint > 0) {
            cantSprint--;
        }

        if (ClientEventHandler.zoom || ClientEventHandler.holdFire) {
            player.setSprinting(false);
        }
    }

    private static void handleVariableDecrease() {
        if (shootDelay > 0) {
            shootDelay = Math.max(shootDelay - 1, 0);
        }

        if (dismountCountdown > 0) {
            dismountCountdown--;
        }

        if (aimVillagerCountdown > 0) {
            aimVillagerCountdown--;
        }
    }

    public static boolean isProne(Player player) {
        Level level = player.level();
        if (player.getBbHeight() <= 1) return true;

        return player.isCrouching() && level.getBlockState(BlockPos.containing(player.getX() + 0.7 * player.getLookAngle().x, player.getY() + 0.5, player.getZ() + 0.7 * player.getLookAngle().z)).canOcclude()
                && !level.getBlockState(BlockPos.containing(player.getX() + 0.7 * player.getLookAngle().x, player.getY() + 1.5, player.getZ() + 0.7 * player.getLookAngle().z)).canOcclude();
    }

    public static void handleGunMelee(Player player, ItemStack stack) {
        if (stack.getItem() instanceof GunItem gunItem) {
            var data = GunData.from(stack);
            if (gunItem.hasMeleeAttack(stack) && gunMelee == 0 && drawTime < 0.01
                    && ModKeyMappings.MELEE.isDown()
                    && !(player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))
                    && !holdFireVehicle
                    && !notInGame()
                    && !ClickHandler.isEditing
                    && !(GunData.from(stack).reload.normal() || GunData.from(stack).reload.empty())
                    && !data.reloading()
                    && !data.charging() && !player.getCooldowns().isOnCooldown(stack.getItem())
            ) {
                gunMelee = data.meleeDuration();
                cantFireTime = gunMelee + 4;
            }
            if (gunMelee == data.meleeDuration() - data.meleeDamageTime()) {
                player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1);
                Entity lookingEntity = TraceTool.findMeleeEntity(player, player.getEntityReach());
                if (lookingEntity != null) {
                    Mod.PACKET_HANDLER.sendToServer(new MeleeAttackMessage(lookingEntity.getUUID()));
                }
            }
        }

        if (gunMelee > 0) {
            gunMelee--;
        }
    }

    public static void handleLungeAttack(Player player, ItemStack stack) {
        if (stack.is(ModItems.LUNGE_MINE.get()) && lungeAttack == 0 && lungeDraw == 0 && holdFire) {
            lungeAttack = 18;
            holdFire = false;
            player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1);
        }

        if (stack.is(ModItems.LUNGE_MINE.get()) && ((lungeAttack >= 9 && lungeAttack <= 10.5) || lungeSprint > 0)) {
            Entity lookingEntity = TraceTool.findLookingEntity(player, player.getEntityReach() + 1.5);

            BlockHitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(player.getBlockReach() + 0.5)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

            Vec3 looking = Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(player.getBlockReach() + 0.5)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos());
            BlockState blockState = player.level().getBlockState(BlockPos.containing(looking.x(), looking.y(), looking.z()));

            if (lookingEntity != null) {
                Mod.PACKET_HANDLER.sendToServer(new LungeMineAttackMessage(0, lookingEntity.getUUID(), result));
                lungeSprint = 0;
                lungeAttack = 0;
                lungeDraw = 15;
            } else if ((blockState.canOcclude() || blockState.getBlock() instanceof DoorBlock || blockState.getBlock() instanceof CrossCollisionBlock || blockState.getBlock() instanceof BellBlock) && lungeSprint == 0) {
                Mod.PACKET_HANDLER.sendToServer(new LungeMineAttackMessage(1, player.getUUID(), result));
                lungeSprint = 0;
                lungeAttack = 0;
                lungeDraw = 15;
            }
        }

        if (lungeSprint > 0) {
            lungeSprint--;
        }

        if (lungeAttack > 0) {
            lungeAttack--;
        }

        if (lungeDraw > 0) {
            lungeDraw--;
        }
    }

    @SubscribeEvent
    public static void handleWeaponFire(TickEvent.RenderTickEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        Player player = Minecraft.getInstance().player;

        if (player == null) return;
        if (level == null) return;

        if (notInGame()) {
            holdFire = false;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            clientTimer.stop();
            fireSpread = 0;
            gunSpread = 0;
            return;
        }
        var data = GunData.from(stack);

        var perk = data.perk.get(Perk.Type.AMMO);
        var mode = data.fireMode.get();

        // 精准度
        float times = (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);

        double basicDev = data.spread();
        double walk = isMoving() ? 0.3 * basicDev : 0;
        double sprint = player.isSprinting() ? 0.25 * basicDev : 0;
        double crouching = player.isCrouching() ? -0.15 * basicDev : 0;
        double prone = isProne(player) ? -0.3 * basicDev : 0;
        double jump = player.onGround() ? 0 * basicDev : 0.35 * basicDev;
        double ride = player.onGround() ? -0.25 * basicDev : 0;

        double zoomSpread;

        if (stack.is(ModTags.Items.SNIPER_RIFLE) || stack.is(ModTags.Items.HEAVY_WEAPON)) {
            zoomSpread = 1 - (0.995 * zoomTime);
        } else if (stack.is(ModTags.Items.SHOTGUN)) {
            if (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug) {
                zoomSpread = 1 - (0.85 * zoomTime);
            } else {
                zoomSpread = 1 - (0.25 * zoomTime);
            }
        } else if (stack.is(ModItems.MINIGUN.get())) {
            zoomSpread = 1 - (0.25 * zoomTime);
        } else {
            zoomSpread = 1 - (0.9 * zoomTime);
        }

        double spread = stack.is(ModTags.Items.SHOTGUN) || stack.is(ModItems.MINIGUN.get()) ? 1.2 * zoomSpread * (basicDev + 0.2 * (walk + sprint + crouching + prone + jump + ride) + fireSpread) : zoomSpread * (0.7 * basicDev + walk + sprint + crouching + prone + jump + ride + 0.8 * fireSpread);

        gunSpread = Mth.lerp(0.14 * times, gunSpread, spread);

        // 开火部分
        double weight = data.weight();
        double speed = 1 - (0.04 * weight);

        if (cantSprint == 0 && player.isSprinting() && !zoom && !holdFire) {
            cantFireTime = Mth.clamp(cantFireTime + 3 * times, 0, 24);
        } else {
            cantFireTime = Mth.clamp(cantFireTime - 6 * speed * times, 0, 40);
        }

        int rpm = data.rpm() + customRpm;
        if (rpm == 0) {
            rpm = 600;
        }

        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                rpm = instance.perk().getModifiedRPM(rpm, data, instance);
            }
        }

        double rps = (double) rpm / 60;

        // cooldown in ms
        int cooldown = (int) Math.round(1000 / rps);

        //左轮类
        if (clientTimer.getProgress() == 0 && stack.is(ModItems.TRACHELIUM.get()) && holdFire) {
            revolverPreTime = Mth.clamp(revolverPreTime + 0.3 * times, 0, 1);
            revolverWheelPreTime = Mth.clamp(revolverWheelPreTime + 0.32 * times, 0, revolverPreTime > 0.7 ? 1 : 0.55);
        } else {
            revolverPreTime = Mth.clamp(revolverPreTime - 1.2 * times, 0, 1);
        }

        if (((holdFire || burstFireAmount > 0) && shootDelay >= data.shootDelay())
                && !(player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))
                && !holdFireVehicle
                && gunItem.canShoot(data)
                && !data.overHeat.get()
                && (stack.is(ModTags.Items.NORMAL_GUN)
                && cantFireTime == 0
                && drawTime < 0.01
                && !notInGame()
                && !ClickHandler.isEditing
                && (!(data.reload.normal() || data.reload.empty())
                && !data.reloading()
                && !data.charging()
                && data.hasEnoughAmmoToShoot(player)
                && !player.getCooldowns().isOnCooldown(stack.getItem())
                && !GunData.from(stack).bolt.needed.get())
        )) {
            if (mode == FireMode.SEMI) {
                if (clientTimer.getProgress() == 0) {
                    clientTimer.start();
                    shootClient(player);
                }
            } else {
                if (!clientTimer.started()) {
                    clientTimer.start();
                    // 首发瞬间发射
                    clientTimer.setProgress(cooldown + 1);
                }

                if (clientTimer.getProgress() >= cooldown) {
                    var newProgress = clientTimer.getProgress();

                    // 低帧率下的开火次数补偿
                    do {
                        shootClient(player);
                        newProgress -= cooldown;
                    } while (newProgress - cooldown > 0);

                    clientTimer.setProgress(newProgress);
                }
            }

            if (notInGame()) {
                clientTimer.stop();
            }

        } else {
            if (mode != FireMode.SEMI && clientTimer.getProgress() >= cooldown) {
                clientTimer.stop();
            }
            fireSpread = 0;
        }

        gunPartMove(times);

        if (mode == FireMode.SEMI && clientTimer.getProgress() >= cooldown) {
            clientTimer.stop();
        }

        if (stack.getItem() == ModItems.DEVOTION.get() && (GunData.from(stack).reload.normal() || GunData.from(stack).reload.empty())) {
            customRpm = 0;
        }
    }

    public static void beamShoot(Player player, ItemStack stack) {
        if (stack.is(ModItems.BEAM_TEST.get()) && player.getUseItem() == stack) {
            Entity lookingEntity = TraceTool.laserfindLookingEntity(player, 512);

            if (player.isCrouching()) {
                Entity seekingEntity = SeekTool.seekLivingEntity(player, player.level(), 64, 32);
                if (seekingEntity != null && seekingEntity.isAlive()) {
                    player.lookAt(EntityAnchorArgument.Anchor.EYES, seekingEntity.getEyePosition());
                }
            }

            if (lookingEntity == null) {
                return;
            }

            boolean canAttack = lookingEntity != player && !(lookingEntity instanceof Player player_ && (player_.isCreative() || player_.isSpectator()))
                    && (!player.isAlliedTo(lookingEntity) || lookingEntity.getTeam() == null || lookingEntity.getTeam().getName().equals("TDM"));

            if (canAttack) {
                Mod.PACKET_HANDLER.sendToServer(new LaserShootMessage(1, lookingEntity.getUUID(), TraceTool.laserHeadshot));
            }
        }
    }

    public static void shootClient(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;
        var data = GunData.from(stack);
        if (!data.hasEnoughAmmoToShoot(player)) return;
        if (!gunItem.canShoot(data)) return;

        if (stack.is(ModTags.Items.NORMAL_GUN)) {
            var mode = data.fireMode.get();
            if (mode != FireMode.AUTO) {
                holdFire = false;
            }

            if (mode == FireMode.BURST) {
                if (data.ammo.get() == 1) {
                    burstFireAmount = 1;
                }
                if (burstFireAmount == 1) {
                    cantFireTime = 30;
                }
            }

            if (burstFireAmount > 0) {
                burstFireAmount--;
            }

            for (Perk.Type type : Perk.Type.values()) {
                var instance = data.perk.getInstance(type);
                if (instance != null) {
                    customRpm = instance.perk().getModifiedCustomRPM(customRpm, data, instance);
                }
            }

            if (stack.is(ModItems.DEVOTION.get())) {
                customRpm = Math.min(customRpm + 15, 500);
            }

            if (stack.getItem() == ModItems.SENTINEL.get()) {
                chamberRot = 1;
            }

            if (stack.getItem() == ModItems.NTW_20.get()) {
                actionMove = 1;
            }

            // 判断是否为栓动武器（BoltActionTime > 0），并在开火后给一个需要上膛的状态
            if (data.defaultActionTime() > 0 && data.ammo.get() > 1) {
                data.bolt.needed.set(true);
            }

            revolverPreTime = 0;
            revolverWheelPreTime = 0;

            playGunClientSounds(player);
            handleClientShoot();
        }
    }

    public static void gunPartMove(float times) {
        chamberRot = Mth.lerp(0.07 * times, chamberRot, 0);
        actionMove = Mth.lerp(0.125 * times, actionMove, 0);
    }

    public static void handleClientShoot() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        Mod.PACKET_HANDLER.sendToServer(new ShootMessage(gunSpread, zoom));
        fireRecoilTime = 10;

        var gunRecoilY = data.recoilY() * 10;

        recoilY = (float) (2 * Math.random() - 1) * gunRecoilY;

        if (shellIndex < 5) {
            shellIndex++;
        }

        cantSprint = 10;

        shellIndexTime[shellIndex] = 0.001;

        randomShell[0] = (1 + 0.2 * (Math.random() - 0.5));
        randomShell[1] = (0.2 + (Math.random() - 0.5));
        randomShell[2] = (0.7 + (Math.random() - 0.5));
    }

    public static void handleShakeClient(double time, double radius, double amplitude, double x, double y, double z, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            Player player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) return;

            float shakeStrength = (float) DisplayConfig.EXPLOSION_SCREEN_SHAKE.get() / 100.0f;
            if (shakeStrength <= 0.0f) return;

            shakeTime = time;
            shakeRadius = radius;
            shakeAmplitude = amplitude * Mth.DEG_TO_RAD * shakeStrength;
            shakePos[0] = x * shakeStrength;
            shakePos[1] = y * shakeStrength;
            shakePos[2] = z * shakeStrength;
            shakeType = 2 * (Math.random() - 0.5);
        }
    }

    public static void playGunClientSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            return;
        }

        String origin = stack.getItem().getDescriptionId();
        String name = origin.substring(origin.lastIndexOf(".") + 1);

        if (stack.getItem() == ModItems.SENTINEL.get()) {
            AtomicBoolean charged = new AtomicBoolean(false);

            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                    e -> charged.set(e.getEnergyStored() > 0)
            );

            if (charged.get()) {
                player.playSound(ModSounds.SENTINEL_CHARGE_FIRE_1P.get(), 2f, (float) ((2 * org.joml.Math.random() - 1) * 0.05f + 1.0f));
                return;
            }
        }

        if (stack.getItem() == ModItems.SECONDARY_CATACLYSM.get()) {
            var hasEnoughEnergy = stack.getCapability(ForgeCapabilities.ENERGY)
                    .map(storage -> storage.getEnergyStored() >= 3000)
                    .orElse(false);

            boolean isChargedFire = zoom && hasEnoughEnergy;

            if (isChargedFire) {
                player.playSound(ModSounds.SECONDARY_CATACLYSM_FIRE_1P_CHARGE.get(), 2f, (float) ((2 * org.joml.Math.random() - 1) * 0.05f + 1.0f));
                return;
            }
        }


        var data = GunData.from(stack);
        var perk = data.perk.get(Perk.Type.AMMO);
        float pitch = data.heat.get() <= 75 ? 1 : (float) (1 - 0.02 * Math.abs(75 - data.heat.get()));

        if (perk == ModPerks.BEAST_BULLET.get()) {
            player.playSound(ModSounds.HENG.get(), 1f, (float) ((2 * org.joml.Math.random() - 1) * 0.1f + pitch));
        }

        int barrelType = GunData.from(stack).attachment.get(AttachmentType.BARREL);

        SoundEvent sound1p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + (barrelType == 2 ? "_fire_1p_s" : "_fire_1p")));

        if (sound1p != null) {
            player.playSound(sound1p, 4f, (float) ((2 * org.joml.Math.random() - 1) * 0.05f + pitch));
        }

        double shooterHeight = player.getEyePosition().distanceTo((Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos())));

        Mod.queueClientWork((int) (1 + 1.5 * shooterHeight), () -> {
            if (gunItem.canEjectShell(stack)) {
                var ammoType = data.ammoTypeInfo().playerAmmoType();
                if (ammoType != null) {
                    switch (ammoType) {
                        case SHOTGUN ->
                                player.playSound(ModSounds.SHELL_CASING_SHOTGUN.get(), (float) Math.max(0.75 - 0.12 * shooterHeight, 0), (float) ((2 * Math.random() - 1) * 0.05f + 1.0f));
                        case SNIPER, HEAVY ->
                                player.playSound(ModSounds.SHELL_CASING_50CAL.get(), (float) Math.max(1 - 0.15 * shooterHeight, 0), (float) ((2 * org.joml.Math.random() - 1) * 0.05f + 1.0f));
                        default ->
                                player.playSound(ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), (float) ((2 * org.joml.Math.random() - 1) * 0.05f + 1.0f));
                    }
                } else {
                    player.playSound(ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), (float) ((2 * org.joml.Math.random() - 1) * 0.05f + 1.0f));
                }
            }
        });
    }

    @SubscribeEvent
    public static void handleVehicleFire(TickEvent.RenderTickEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (level == null) return;

        if (notInGame()) {
            clientTimerVehicle.stop();
            holdFireVehicle = false;
        }

        if (player.getVehicle() instanceof VehicleEntity pVehicle && player.getVehicle() instanceof WeaponVehicleEntity iVehicle && iVehicle.hasWeapon(pVehicle.getSeatIndex(player)) && iVehicle.canShoot(player)) {
            int rpm = iVehicle.mainGunRpm(player);
            if (rpm == 0) {
                rpm = 240;
            }

            double rps = (double) rpm / 60;
            int cooldown = (int) Math.round(1000 / rps);

            if ((holdFireVehicle)) {
                if (!clientTimerVehicle.started()) {
                    clientTimerVehicle.start();
                    // 首发瞬间发射
                    clientTimerVehicle.setProgress((cooldown + 1));
                }

                if (clientTimerVehicle.getProgress() >= cooldown) {
                    var newProgress = clientTimerVehicle.getProgress();

                    // 低帧率下的开火次数补偿
                    do {
                        Mod.PACKET_HANDLER.sendToServer(new VehicleFireMessage(pVehicle.getSeatIndex(player)));
                        playVehicleClientSounds(player, iVehicle, pVehicle.getSeatIndex(player));

                        newProgress -= cooldown;
                    } while (newProgress - cooldown > 0);

                    clientTimerVehicle.setProgress(newProgress);
                }
            } else if (clientTimerVehicle.getProgress() >= cooldown) {
                clientTimerVehicle.stop();
            }
        } else {
            clientTimerVehicle.stop();
        }
    }

    public static void playVehicleClientSounds(Player player, WeaponVehicleEntity iVehicle, int type) {
        var weapons = iVehicle.getAvailableWeapons(type);
        var weapon = weapons.get(iVehicle.getWeaponIndex(type));

        float pitch = iVehicle.getWeaponHeat(player) <= 60 ? 1 : (float) (1 - 0.011 * Math.abs(60 - iVehicle.getWeaponHeat(player)));

        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || (zoomVehicle && !(iVehicle instanceof Ah6Entity))) {
            if (weapon.sound1p != null) {
                player.playSound(weapon.sound1p, 1f, pitch);
            }
        } else {
            if (weapon.sound3p != null) {
                player.playSound(weapon.sound3p, 3f, pitch);
            }
        }
    }

    @SubscribeEvent
    public static void handleWeaponBreathSway(TickEvent.RenderTickEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;
        if (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.isDriver(player) && iArmedVehicle.hidePassenger(player))
            return;

        float pose;
        float times = 2 * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);

        if (player.isCrouching() && player.getBbHeight() >= 1 && !isProne(player)) {
            pose = 0.85f;
        } else if (isProne(player)) {
            pose = (GunData.from(stack).attachment.get(AttachmentType.GRIP) == 3 || gunItem.hasBipod(stack)) ? 0 : 0.25f;
        } else {
            pose = 1;
        }

        int stockType = GunData.from(stack).attachment.get(AttachmentType.STOCK);

        double sway = switch (stockType) {
            case 1 -> 1;
            case 2 -> 0.55;
            default -> 0.8;
        };

        var data = GunData.from(stack);
        double customWeight = data.customWeight();

        if (!breath && zoom) {
            float newPitch = (float) (player.getXRot() - 0.01f * Mth.sin((float) (0.03 * player.tickCount)) * pose * Mth.nextDouble(RandomSource.create(), 0.1, 1) * times * sway * (1 - 0.03 * customWeight));
            player.setXRot(newPitch);
            player.xRotO = player.getXRot();

            float newYaw = (float) (player.getYRot() - 0.005f * Mth.cos((float) (0.025 * (player.tickCount + 2 * Math.PI))) * pose * Mth.nextDouble(RandomSource.create(), 0.05, 1.25) * times * sway * (1 - 0.03 * customWeight));
            player.setYRot(newYaw);
            player.yRotO = player.getYRot();
        }
    }

    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        ClientLevel level = Minecraft.getInstance().level;
        Entity entity = event.getCamera().getEntity();

        if (!(entity instanceof LivingEntity living)) return;
        ItemStack stack = living.getMainHandItem();

        if (level != null &&
                (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked"))) {
            handleDroneCamera(event, living);
        }

        LocalPlayer player = Minecraft.getInstance().player;

        float yaw = event.getYaw();
        float pitch = event.getPitch();
        float roll = event.getRoll();

        shakeTime = Mth.lerp(0.05 * event.getPartialTick(), shakeTime, 0);

        if (player != null && shakeTime > 0) {
            float shakeRadiusAmplitude = (float) Mth.clamp(1 - player.position().distanceTo(new Vec3(shakePos[0], shakePos[1], shakePos[2])) / shakeRadius, 0, 1);

            boolean onVehicle = player.getVehicle() != null;

            if (shakeType > 0) {
                event.setYaw((float) (yaw + (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType * (onVehicle ? 0.1 : 1))));
                event.setPitch((float) (pitch - (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType * (onVehicle ? 0.1 : 1))));
                event.setRoll((float) (roll - (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * (onVehicle ? 0.1 : 1))));
            } else {
                event.setYaw((float) (yaw - (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType * (onVehicle ? 0.1 : 1))));
                event.setPitch((float) (pitch + (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType * (onVehicle ? 0.1 : 1))));
                event.setRoll((float) (roll + (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * (onVehicle ? 0.1 : 1))));
            }
        }

        cameraPitch = event.getPitch();
        cameraYaw = event.getYaw();
        cameraRoll = event.getRoll();

        if (player != null && player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player)) {
            return;
        }

        if (level != null && stack.getItem() instanceof GunItem) {
            handleWeaponSway(living);
            handleWeaponMove(living);
            handleWeaponZoom(living);
            handleWeaponFire(event, living);
            handleWeaponShell();
            handleGunRecoil();
            handleBowPullAnimation(living, stack);
            handleWeaponDraw(living);
            handlePlayerCamera(event);
        }

        handleShockCamera(event, living);
    }

    private static void handleDroneCamera(ViewportEvent.ComputeCameraAngles event, LivingEntity entity) {
        ItemStack stack = entity.getMainHandItem();

        DroneEntity drone = EntityFindUtil.findDrone(entity.level(), stack.getOrCreateTag().getString("LinkedDrone"));

        if (drone != null) {
            event.setRoll(drone.getRoll((float) event.getPartialTick()) * (1 - (drone.getPitch((float) event.getPartialTick()) / 90)));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        InteractionHand leftHand = Minecraft.getInstance().options.mainHand().get() == HumanoidArm.RIGHT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        InteractionHand rightHand = Minecraft.getInstance().options.mainHand().get() == HumanoidArm.RIGHT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

        ItemStack rightHandItem = player.getItemInHand(rightHand);

        if (event.getHand() == leftHand) {
            if (rightHandItem.getItem() instanceof GunItem) {
                event.setCanceled(true);
            }
        }

        if (event.getHand() == rightHand) {
            if (rightHandItem.getItem() instanceof GunItem && drawTime > 0.15) {
                event.setCanceled(true);
            }
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
            if (EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone")) != null) {
                event.setCanceled(true);
            }
        }

        if (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player)) {
            event.setCanceled(true);
        }
    }

    private static void handleWeaponSway(LivingEntity entity) {
        ItemStack stack = entity.getMainHandItem();
        if (stack.getItem() instanceof GunItem gunItem && entity instanceof Player player) {
            float times = 2 * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
            double pose;

            if (player.isShiftKeyDown() && player.getBbHeight() >= 1 && isProne(player)) {
                pose = 0.85;
            } else if (isProne(player)) {
                pose = (GunData.from(stack).attachment.get(AttachmentType.GRIP) == 3 || gunItem.hasBipod(stack)) ? 0 : 0.25f;
            } else {
                pose = 1;
            }

            swayTime += 0.05 * times;

            swayX = pose * -0.008 * Math.sin(swayTime) * (1 - 0.95 * zoomTime);
            swayY = pose * 0.125 * Math.sin(swayTime - 1.585) * (1 - 0.95 * zoomTime) - 3 * moveRotZ;
        }
    }

    private static void handleWeaponMove(LivingEntity entity) {
        if (entity.getMainHandItem().is(ModTags.Items.GUN)) {
            float times = 3.7f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
            double moveSpeed = (float) Mth.clamp(entity.getDeltaMovement().horizontalDistanceSqr(), 0, 0.02);
            double onGround;

            if (entity.onGround()) {
                if (entity.isSprinting()) {
                    onGround = 1.35;
                } else {
                    onGround = 2.0;
                }
            } else {
                onGround = 0.001;
            }

            if (!ClickHandler.isEditing) {
                if (Minecraft.getInstance().options.keyUp.isDown() && firePosTimer == 0) {
                    moveRotZ = Mth.lerp(0.2f * times, moveRotZ, 0.14) * (1 - zoomTime);
                } else {
                    moveRotZ = Mth.lerp(0.2f * times, moveRotZ, 0) * (1 - zoomTime);
                }
            }

            if (isMoving() && !entity.isSprinting() && firePosTimer == 0) {
                moveYTime += 1.2 * onGround * times * moveSpeed;
                moveXTime += 1.2 * onGround * times * moveSpeed;
                moveFadeTime = Mth.lerp(0.13 * times, moveFadeTime, 1);
            } else {
                moveFadeTime = Mth.lerp(0.1 * times, moveFadeTime, 0);
            }

            movePosX = 0.2 * Math.sin(1 * Math.PI * moveXTime) * (1 - 0.95 * zoomTime) * moveFadeTime;
            movePosY = -0.135 * Math.sin(2 * Math.PI * (moveYTime - 0.25)) * (1 - 0.95 * zoomTime) * moveFadeTime;

            boolean left = Minecraft.getInstance().options.keyLeft.isDown();
            boolean right = Minecraft.getInstance().options.keyRight.isDown();
            double pos = 0;

            if (left) {
                pos = -0.04;
            }

            if (right) {
                pos = 0.04;
            }

            if (left && right) {
                pos = 0;
            }

            movePosHorizon = Mth.lerp(0.1f * times, movePosHorizon, pos * (1 - 1 * zoomTime));

            double velocity = entity.getDeltaMovement().y() + 0.078;

            velocityY = Mth.clamp(Mth.lerp(0.23f * times, velocityY, velocity) * (1 - 0.8 * zoomTime), -0.8, 0.8);
        }
    }

    public static void gunRootMove(AnimationProcessor<?> animationProcessor) {
        CoreGeoBone root = animationProcessor.getBone("root");
        root.setPosX((float) (movePosX + 20 * drawTime + 9.3f * movePosHorizon));
        root.setPosY((float) (swayY + movePosY - 40 * drawTime - 2f * velocityY));
        root.setRotX((float) (swayX - Mth.DEG_TO_RAD * 60 * drawTime + Mth.DEG_TO_RAD * turnRot[0] - 0.15f * velocityY));
        root.setRotY((float) (0.2f * movePosX + Mth.DEG_TO_RAD * 300 * drawTime + Mth.DEG_TO_RAD * turnRot[1]));
        root.setRotZ((float) (0.2f * movePosX + moveRotZ + Mth.DEG_TO_RAD * 90 * drawTime + 2.7f * movePosHorizon + Mth.DEG_TO_RAD * turnRot[2]));
    }

    private static void handleWeaponZoom(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();
        var data = GunData.from(stack);
        float times = 5 * Minecraft.getInstance().getDeltaFrameTime();

        double weight = data.weight();
        double speed = 1.5 - (0.07 * weight);

        if (zoom
                && !(player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))
                && !notInGame()
                && drawTime < 0.01
                && !ClickHandler.isEditing) {
            if (Minecraft.getInstance().player != null) {
                cantSprint = 5;
            }
            if (cantFireTime <= 10) {
                zoomTime = Mth.clamp(zoomTime + 0.03 * speed * times, 0, 1);
            }
        } else {
            zoomTime = Mth.clamp(zoomTime - 0.04 * speed * times, 0, 1);
        }
        zoomPos = AnimationCurves.EASE_IN_OUT_QUINT.apply(zoomTime);
        zoomPosZ = AnimationCurves.PARABOLA.apply(zoomTime);
    }

    private static void handleWeaponFire(ViewportEvent.ComputeCameraAngles event, LivingEntity entity) {
        float times = 2f * Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.48f);
        float yaw = event.getYaw();
        float pitch = event.getPitch();
        float roll = event.getRoll();
        ItemStack stack = entity.getMainHandItem();
        var data = GunData.from(stack);
        double amplitude = 15000 * data.recoilY() * data.recoilX();

        if (fireRecoilTime > 0) {
            firePosTimer = 0.001;
            fireRotTimer = 0.001;
            fireRecoilTime -= 7 * times;
            fireSpread += 0.1 * times;
            firePosZ += (0.8 * firePosZ + 0.25) * (4 * Math.random() + 0.85) * times;
            recoilTime = 0.01;
        }

        fireSpread = Mth.clamp(fireSpread - 0.1 * (Math.pow(fireSpread, 2) * times), 0, 2);
        firePosZ = Mth.clamp(firePosZ - 1.2 * (Math.pow(firePosZ, 2) * times), 0, 1.5);

        firePosZ *= 0.96f;
        firePos *= 0.96f;
        fireRot *= 0.96f;

        if (0 < firePosTimer) {
            firePosTimer += 0.35 * (1.1 - firePosTimer) * times;
        }
        if (0 < firePosTimer && firePosTimer < 0.454) {
            firePos = (-18.34) * Math.pow(firePosTimer, 2) + 8.58 * firePosTimer;
        }
        if (0.454 <= firePosTimer && firePosTimer < 1) {
            firePos = 4.34 * Math.pow(firePosTimer, 2) - 6.5 * firePosTimer + 2.167;
        }
        if (0 < fireRotTimer && fireRotTimer < 1.732) {
            fireRotTimer += 0.18 * (1.9 - fireRotTimer) * times;
        }

        double rpm = 1;

        if (stack.is(ModItems.MINIGUN.get())) {
            rpm = (double) data.rpm() / 1800;
        }

        float[] shake = {0, 0};
        shake[0] = (float) (1.3 * amplitude * (1 / 6.3 * (fireRotTimer - 0.5)) * Math.sin(6.3 * (fireRotTimer - 0.5)) * (3 - Math.pow(fireRotTimer, 2))
                + 1 * Mth.clamp(0.3 - fireRotTimer, 0, 1) * (2 * Math.random() - 1)) * (float) (DisplayConfig.WEAPON_SCREEN_SHAKE.get() / 100.0);
        shake[1] = (float) (4.2 * amplitude * (1 / 6.3 * (fireRotTimer - 0.5)) * Math.sin(6.3 * (fireRotTimer - 0.5)) * (3 - Math.pow(fireRotTimer, 2))
                + 3 * Mth.clamp(0.5 - fireRotTimer, 0, 0.5) * (2 * Math.random() - 1)) * (float) (DisplayConfig.WEAPON_SCREEN_SHAKE.get() / 100.0);


        if (firePosTimer >= 1) {
            firePosTimer = 0;
        }
        if (fireRotTimer >= 1.732) {
            fireRotTimer = 0;
            fireRot = 0;
        }

        if (entity instanceof Player player && player.isSpectator()) return;

        if (0 < fireRotTimer && fireRotTimer < 1.732) {
            fireRot = 1 / 6.3 * (fireRotTimer - 0.5) * Math.sin(6.3 * (fireRotTimer - 0.5)) * (3 - Math.pow(fireRotTimer, 2));
            if (recoilY > 0) {
                event.setYaw((float) (yaw - shake[0] * rpm));
                event.setPitch((float) (pitch + shake[0] * rpm));
                event.setRoll((float) (roll + shake[1] * rpm));
            } else if (recoilY <= 0) {
                event.setYaw((float) (yaw + shake[0] * rpm));
                event.setPitch((float) (pitch - shake[0] * rpm));
                event.setRoll((float) (roll - shake[1] * rpm));
            }
        }
    }

    private static void handleWeaponShell() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        float times = (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);

        if (shellIndex >= 5) {
            shellIndex = 0;
            shellIndexTime[0] = 0.001;
        }

        for (int i = 0; i < 5; i++) {
            if (shellIndexTime[i] > 0) {
                shellIndexTime[i] = Math.min(shellIndexTime[i] + 8 * times, 50);
            }
            if (shellIndexTime[i] == 50) {
                shellIndexTime[i] = 0;
            }
        }
    }

    private static void handleGunRecoil() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;

        float times = (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 1.6);
        int barrelType = GunData.from(stack).attachment.get(AttachmentType.BARREL);
        int gripType = GunData.from(stack).attachment.get(AttachmentType.GRIP);

        double recoil = switch (barrelType) {
            case 1 -> 1.5;
            case 2 -> 2.2;
            default -> 2.4;
        };

        double gripRecoilX = switch (gripType) {
            case 1 -> 1.25;
            case 2 -> 0.25;
            default -> 1.5;
        };

        double gripRecoilY = switch (gripType) {
            case 1 -> 0.7;
            case 2 -> 1.75;
            default -> 2.0;
        };

        if (!gunItem.isCustomizable(stack)) {
            recoil = 1.6;
            gripRecoilX = 0.75;
            gripRecoilY = 1.25;
        }

        var data = GunData.from(stack);
        double customWeight = data.customWeight();

        double rpm = 1;

        if (stack.is(ModItems.MINIGUN.get())) {
            rpm = (double) data.rpm() / 1800;
        }

        float gunRecoilX = (float) data.recoilX() * 60;

        recoilHorizon = Mth.lerp(0.2 * times, recoilHorizon, 0) + recoilY;
        recoilY = 0;

        // 计算后坐力
        float pose = 1;
        if (player.isShiftKeyDown() && player.getBbHeight() >= 1 && !isProne(player)) {
            pose = 0.7f;
        } else if (isProne(player)) {
            if (GunData.from(stack).attachment.get(AttachmentType.GRIP) == 3 || gunItem.hasBipod(stack)) {
                pose = 0.1f;
            } else {
                pose = 0.5f;
            }
        }

        // 水平后座
        float newYaw = player.getYRot() - (float) (0.6 * recoilHorizon * pose * times * (0.5 + fireSpread) * recoil * (1 - 0.06 * customWeight) * gripRecoilX * rpm);
        player.setYRot(newYaw);
        player.yRotO = player.getYRot();

        double sinRes = 0;

        // 竖直后座
        if (0 < recoilTime && recoilTime < 0.5) {
            float newPitch = (float) (player.getXRot() - 0.02f * gunRecoilX * times * recoil * (1 - 0.06 * customWeight) * gripRecoilY * rpm);
            player.setXRot(newPitch);
            player.xRotO = player.getXRot();
        }

        if (0 < recoilTime && recoilTime < 2) {
            recoilTime = recoilTime + 0.3 * times;
            sinRes = Math.sin(Math.PI * recoilTime);
        }

        if (2 <= recoilTime && recoilTime < 2.5) {
            recoilTime = recoilTime + 0.17 * times;
            sinRes = 0.4 * Math.sin(2 * Math.PI * recoilTime);
        }

        if (0 < recoilTime && recoilTime < 2.5) {
            float newPitch = player.getXRot() - (float) (1.5 * pose * gunRecoilX * (sinRes + Mth.clamp(0.5 - recoilTime, 0, 0.5)) * times * (0.5 + fireSpread) * recoil * (1 - 0.06 * customWeight) * gripRecoilY * rpm);
            player.setXRot(newPitch);
            player.xRotO = player.getXRot();
        }

        if (recoilTime >= 2.5) recoilTime = 0;
    }

    private static void handleShockCamera(ViewportEvent.ComputeCameraAngles event, LivingEntity entity) {
        if (entity instanceof Player player && player.isSpectator()) return;

        if (entity.hasEffect(ModMobEffects.SHOCK.get()) && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
            float shakeStrength = (float) DisplayConfig.SHOCK_SCREEN_SHAKE.get() / 100.0f;
            if (shakeStrength <= 0.0f) return;

            event.setYaw(Minecraft.getInstance().gameRenderer.getMainCamera().getYRot() +
                    (float) Mth.nextDouble(RandomSource.create(), -3, 3) * shakeStrength);
            event.setPitch(Minecraft.getInstance().gameRenderer.getMainCamera().getXRot() +
                    (float) Mth.nextDouble(RandomSource.create(), -3, 3) * shakeStrength);
            event.setRoll((float) Mth.nextDouble(RandomSource.create(), 8, 12) * shakeStrength);
        }
    }

    public static void handleReloadShake(double boneRotX, double boneRotY, double boneRotZ) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) return;

        float shakeStrength = (float) DisplayConfig.WEAPON_SCREEN_SHAKE.get() / 100.0f;
        if (shakeStrength <= 0.0f) return;

        cameraRot[0] = -boneRotX * shakeStrength;
        cameraRot[1] = -boneRotY * shakeStrength;
        cameraRot[2] = -boneRotZ * shakeStrength;
    }

    private static void handlePlayerCamera(ViewportEvent.ComputeCameraAngles event) {
        double yaw = event.getYaw();
        double pitch = event.getPitch();
        double roll = event.getRoll();
        float times = (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        LocalPlayer player = Minecraft.getInstance().player;

        if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
            cameraLocation = Mth.clamp(cameraLocation - 0.05 * Minecraft.getInstance().getDeltaFrameTime(), -0.6, 0.6);
        }

        if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
            cameraLocation = Mth.clamp(cameraLocation + 0.05 * Minecraft.getInstance().getDeltaFrameTime(), -0.6, 0.6);
        }

        if (player == null) return;

        double range;
        Entity lookingEntity = SeekTool.seekEntity(player, player.level(), 520, 5);

        if (lookingEntity != null) {
            range = Math.max(player.distanceTo(lookingEntity), 0.01);
        } else {
            range = Math.max(player.position().distanceTo((Vec3.atLowerCornerOf(player.level().clip(
                    new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(520)),
                            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos()))), 0.01);
        }

        lookDistance = Mth.lerp(0.2f * times, lookDistance, range);

        double angle = 0;

        if (lookDistance != 0 && cameraLocation != 0) {
            angle = Math.atan(Mth.abs((float) cameraLocation) / (lookDistance + 2.9)) * Mth.RAD_TO_DEG;
        }

        event.setPitch((float) (pitch + cameraRot[0] + (DisplayConfig.CAMERA_ROTATE.get() ? 0.2 : 0) * turnRot[0] + 3 * velocityY));
        if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
            event.setYaw((float) (yaw + cameraRot[1] + (DisplayConfig.CAMERA_ROTATE.get() ? 0.8 : 0) * turnRot[1] - (cameraLocation > 0 ? 1 : -1) * angle * zoomPos));
        } else {
            event.setYaw((float) (yaw + cameraRot[1] + (DisplayConfig.CAMERA_ROTATE.get() ? 0.8 : 0) * turnRot[1]));
        }

        event.setRoll((float) (roll + cameraRot[2] + (DisplayConfig.CAMERA_ROTATE.get() ? 0.35 : 0) * turnRot[2]));
    }

    private static void handleBowPullAnimation(LivingEntity entity, ItemStack stack) {
        float times = 4 * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);

        var data = GunData.from(stack);

        if (holdFire && data.ammo.get() > 0 && !bowPull && stack.is(ModItems.BOCEK.get())) {
            entity.playSound(ModSounds.BOCEK_PULL_1P.get(), 1, 1);
            bowPull = true;
        }

        if (bowPull) {
            bowPullTimer = Math.min(bowPullTimer + 0.024 * times, 1.4);
            bowPower = Math.min(bowPower + 0.018 * times, 1);
        } else {
            bowPullTimer = Math.max(bowPullTimer - 0.021 * times, 0);
            bowPower = Math.max(bowPower - 0.04 * times, 0);
        }
        bowPullPos = 0.5 * Math.cos(Math.PI * Math.pow(Math.pow(Mth.clamp(bowPullTimer, 0, 1), 2) - 1, 2)) + 0.5;
    }

    @SubscribeEvent
    public static void onFovUpdate(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        float times = (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 1.6);
        Player player = mc.player;
        if (player == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();

        if (player.getVehicle() instanceof WeaponVehicleEntity iVehicle && zoomVehicle && iVehicle.banHand(player)) {
            event.setFOV(event.getFOV() / iVehicle.zoomFov());
            fov = event.getFOV();
            return;
        }

        if (stack.getItem() instanceof GunItem) {
            if (!event.usedConfiguredFov()) {
                return;
            }

            double p;
            if (stack.is(ModItems.BOCEK.get())) {
                p = bowPullPos * zoomTime;
            } else {
                p = zoomPos;
            }

            var data = GunData.from(stack);

            customZoom = Mth.lerp(0.6 * times, customZoom, data.zoom() + (breath ? 0.75 : 0));

            if (mc.options.getCameraType().isFirstPerson()) {
                event.setFOV(event.getFOV() / (1.0 + p * (customZoom - 1)));
            } else if (mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK)
                event.setFOV(event.getFOV() / (1.0 + p * 0.01));
            fov = event.getFOV();

            // 智慧芯片
            if (zoom
                    && !notInGame()
                    && drawTime < 0.01
                    && !ClickHandler.isEditing) {
                if (!player.isShiftKeyDown()) {
                    int intelligentChipLevel = GunData.from(stack).perk.getLevel(ModPerks.INTELLIGENT_CHIP);

                    if (intelligentChipLevel > 0) {
                        if (ClientEventHandler.entity == null || !entity.isAlive()) {
                            ClientEventHandler.entity = SeekTool.seekLivingEntity(player, player.level(), 32 + 8 * (intelligentChipLevel - 1), 16 / customZoom);
                        }
                        if (entity != null && entity.isAlive()) {
                            Vec3 toVec = getVec3(event, player);
                            look(player, toVec);
                        }
                    }
                } else {
                    entity = null;
                }

            }
            return;
        }

        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
            droneFovLerp = Mth.lerp(0.1 * Minecraft.getInstance().getDeltaFrameTime(), droneFovLerp, droneFov);
            event.setFOV(event.getFOV() / droneFovLerp);
            fov = event.getFOV();
        }
    }

    private static Vec3 getVec3(ViewportEvent.ComputeFov event, Player player) {
        Vec3 targetVec = new Vec3(Mth.lerp(event.getPartialTick(), entity.xo, entity.getX()), Mth.lerp(event.getPartialTick(), entity.yo + entity.getEyeHeight(), entity.getEyeY()), Mth.lerp(event.getPartialTick(), entity.zo, entity.getZ()));
        Vec3 playerVec = new Vec3(Mth.lerp(event.getPartialTick(), player.xo, player.getX()), Mth.lerp(event.getPartialTick(), player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(event.getPartialTick(), player.zo, player.getZ()));
        return playerVec.vectorTo(targetVec);
    }

    public static void look(Player player, Vec3 pTarget) {
        double d0 = pTarget.x;
        double d1 = pTarget.y;
        double d2 = pTarget.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        player.setXRot(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * 57.2957763671875))));
        player.setYRot(Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F));
        player.setYHeadRot(player.getYRot());
        player.xRotO = player.getXRot();
        player.yRotO = player.getYRot();
    }

    @SubscribeEvent
    public static void setPlayerInvisible(RenderPlayerEvent.Pre event) {
        var otherPlayer = event.getEntity();

        if (otherPlayer.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.hidePassenger(otherPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void handleRenderCrossHair(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() instanceof GunItem) {
            event.setCanceled(true);
        }

        if (player.getVehicle() instanceof VehicleEntity pVehicle && player.getVehicle() instanceof WeaponVehicleEntity iVehicle && iVehicle.hasWeapon(pVehicle.getSeatIndex(player))) {
            event.setCanceled(true);
        }

        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
            event.setCanceled(true);
        }
    }

    /**
     * 载具banHand时，禁用快捷栏渲染
     */
    @SubscribeEvent
    public static void handleAvoidRenderingHotbar(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        if (player.getVehicle() instanceof ArmedVehicleEntity vehicle && vehicle.banHand(player)) {
            event.setCanceled(true);
        }
    }

    public static void handleDrawMessage(Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            drawTime = 1;
            for (int i = 0; i < 5; i++) {
                shellIndexTime[i] = 0;
            }
            zoom = false;
            holdFire = false;
            ClickHandler.switchZoom = false;
            lungeDraw = 30;
            lungeSprint = 0;
            lungeAttack = 0;
            burstFireAmount = 0;
            bowPullTimer = 0;
            bowPower = 0;
            cantSprint = 10;
            ClickHandler.isEditing = false;
        }
    }

    private static void handleWeaponDraw(LivingEntity entity) {
        float times = Minecraft.getInstance().getDeltaFrameTime();
        ItemStack stack = entity.getMainHandItem();
        var data = GunData.from(stack);
        double weight = data.weight();
        double speed = 3.2 - (0.13 * weight);
        drawTime = Math.max(drawTime - Math.max(0.2 * speed * times * drawTime, 0.0008), 0);
    }

    public static void handleShells(float x, float y, CoreGeoBone... shells) {
        for (int i = 0; i < shells.length; i++) {
            if (i >= 5) break;

            shells[i].setPosX((float) (-x * shellIndexTime[i] * ((150 - shellIndexTime[i]) / 150)));
            shells[i].setPosY((float) (y * randomShell[0] * shellIndexTime[i] - 0.025 * Math.pow(shellIndexTime[i], 2)));
            shells[i].setRotX((float) (randomShell[1] * shellIndexTime[i]));
            shells[i].setRotY((float) (randomShell[2] * shellIndexTime[i]));
        }
    }

    public static void aimAtVillager(Player player) {
        if (aimVillagerCountdown > 0) return;

        if (zoom) {
            Entity entity = TraceTool.findLookingEntity(player, 10);
            if (entity instanceof AbstractVillager villager) {
                List<Entity> entities = SeekTool.seekLivingEntities(villager, villager.level(), 16, 120);
                for (var e : entities) {
                    if (e == player) {
                        Mod.PACKET_HANDLER.sendToServer(new AimVillagerMessage(villager.getId()));
                        aimVillagerCountdown = 80;
                    }
                }
            }
        }
    }
}
