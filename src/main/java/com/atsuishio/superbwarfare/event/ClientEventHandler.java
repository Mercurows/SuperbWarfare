package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.animation.AnimationCurves;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.data.gun.*;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.send.*;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.resource.gun.GunResource;
import com.atsuishio.superbwarfare.tools.*;
import com.atsuishio.superbwarfare.world.TDMSavedData;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEventHandler {

    public static double zoomTime = 0;
    public static double zoomPos = 0;
    public static double zoomPosZ = 0;
    public static double swayTime = 0;
    public static double swayX = 0;
    public static double swayY = 0;
    public static double moveTime = 0;
    public static double sprintTime = 0;
    public static double movePosX = 0;
    public static double movePosY = 0;
    public static double moveRotZ = 0;

    public static double sprintBasicRotX = 0;
    public static double sprintBasicRotY = 0;
    public static double sprintBasicRotZ = 0;
    public static double sprintPosX = 0;
    public static double sprintPosY = 0;

    public static double sprintBasicPosX = 0;
    public static double sprintBasicPosY = 0;
    public static double sprintBasicPosZ = 0;

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
    public static double fireRotY = 0f;
    public static double fireRotZ = 0f;

    public static double customAnimSpeed = 1f;

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
    public static double fireCooldown = 0;
    public static double lookDistance = 0;
    public static double cameraLocation = 0.6;

    public static double drawTime = 1;

    public static int shellIndex = 0;
    public static double[] shellIndexTime = {0, 0, 0, 0, 0, 0};
    public static double[] randomShell = {0, 0, 0};

    public static double customZoom = 0;

    public static double artilleryIndicatorZoom = 1;
    public static double artilleryIndicatorCustomZoom = 0;
    public static MillisTimer clientTimer = new MillisTimer();
    public static MillisTimer clientTimerVehicle = new MillisTimer();

    // 正在按住开火键
    public static boolean holdingFireKey = false;
    public static boolean bowPull = false;

    public static boolean zoom = false;
    public static boolean breath = false;

    public static boolean tacticalSprint = false;
    public static float stamina = 0;
    public static double switchTime = 0;
    public static double moveFadeTime = 0;
    public static double sprintFadeTime = 0;

    public static boolean exhaustion = false;
    public static boolean holdFireVehicle = false;

    public static boolean zoomVehicle = false;
    public static int burstFireAmount = 0;

    public static int customRpm = 0;

    public static double chamberRot = 0;
    public static double actionMove = 0;

    // 按住开火键的持续tick
    public static int holdingFireKeyTicks = 0;
    public static float holdingFireKeyTicks0 = 0;
    public static boolean shouldPlayDischargeSound = true;
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

    // 智慧芯片锁定的实体
    public static Entity lockedEntity;

    public static int dismountCountdown = 0;
    public static int aimVillagerCountdown = 0;

    public static CameraType lastCameraType;

    public static float cameraPitch;
    public static float cameraYaw;
    public static float cameraRoll;

    // 禁止冲刺♿时长tick
    public static float noSprintTicks = 0;

    public static boolean canDoubleJump = false;

    public static int holdArtilleryIndicator;
    public static int holdToEjection;
    public static boolean isEditing = false;

    // 锁定类武器用
    public static Entity naerestEntity;
    public static Entity seekingEntity;
    public static Entity lockingEntity;
    public static Vec3 seekingPos;
    public static Vec3 lockingPos;
    public static int seekingTime;
    public static int guideType;
    public static boolean lockOn;

    public static UUID lastOperatingGunUUID = null;

    protected static short keysCache = 0;

    public static TDMSavedData tdmSavedData = new TDMSavedData();

    @SubscribeEvent
    public static void handleWeaponTurn(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        var fromY = player.yRotO;
        var toY = player.getYRot();

        if (fromY > 135 && toY < -135) {
            toY += 360;
        }
        if (fromY < -135 && toY > 135) {
            fromY += 360;
        }

        float yRotOffset = Mth.lerp(event.getPartialTick(), 0, toY - fromY);
//        float yRot = Mth.wrapDegrees(player.getYRot() + yRotOffset);

        turnRot[0] = Mth.clamp(0.05 * yRotOffset, -5, 5) * (1 - 0.75 * zoomTime);
        turnRot[1] = Mth.clamp(0.05 * yRotOffset, -10, 10) * (1 - 0.75 * zoomTime);
        turnRot[2] = Mth.clamp(0.1 * yRotOffset, -10, 10) * (1 - zoomTime);
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

    @SubscribeEvent
    public static void handleClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        final var tag = NBTTool.getTag(stack);

        // 射击延迟
        if (stack.getItem() instanceof GunItem gunItem) {
            var data = GunData.from(stack);

            UUID uuid = null;
            try {
                uuid = data.gunDataTag.getUUID("UUID");
            } catch (Exception ignored) {
            }

            if (notInGame()) {
                burstFireAmount = 0;
            }

            // 切枪时记得重置状态
            if (uuid == null || !uuid.equals(lastOperatingGunUUID)) {
                resetGunStatus();
            }
            lastOperatingGunUUID = uuid;

            if ((holdingFireKey || (zoom && stack.is(ModItems.MINIGUN.get()))) && gunItem.canShoot(data, player)) {
                holdingFireKeyTicks = Math.min(holdingFireKeyTicks + 1, data.get(GunProp.SHOOT_DELAY) + 1);

                // 加特林特有的旋转音效
                if (stack.is(ModItems.MINIGUN.get())) {
                    float rpm = (float) data.get(GunProp.RPM) / 3600;
                    player.playSound(ModSounds.MINIGUN_ROT.get(), 1, 0.7f + rpm);
                }

                // QL特有的樱花特效
                if (stack.is(ModItems.QL_1031.get()) && (player.tickCount & 5) == 0) {
                    double random = (Math.random() - 0.5) * 2;
                    player.level().addParticle(ParticleTypes.CHERRY_LEAVES, player.getX() + random, player.getEyeY() + 0.5 * random, player.getZ() + random, 0, 0, 0);
                }
            }
        } else {
            lastOperatingGunUUID = null;
            resetGunStatus();
        }

        if (notInGame() && !ClickHandler.switchZoom) {
            zoom = false;
        }

        isProne(player);

        var options = Minecraft.getInstance().options;
        short keys = 0;

        // 正在游戏内控制载具或无人机
        if (!notInGame() && (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getFirstPassenger() == player)
                || (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked"))
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
            PacketDistributor.sendToServer(new VehicleMovementMessage(keys));
            keysCache = keys;
        }

        if (player.onGround() && canDoubleJump) {
            canDoubleJump = false;
        }

        if ((stack.is(ModItems.ARTILLERY_INDICATOR.get()) || (stack.is(ModItems.MONITOR.get()) && player.getOffhandItem().is(ModItems.ARTILLERY_INDICATOR.get()))) && holdingFireKey) {
            holdArtilleryIndicator = Mth.clamp(holdArtilleryIndicator + 1, 0, 20);
            if (holdArtilleryIndicator >= 19) {
                PacketDistributor.sendToServer(ArtilleryIndicatorFireMessage.INSTANCE);
            }
        } else {
            holdArtilleryIndicator = 0;
        }

        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.allowEjection() && ModKeyMappings.DISMOUNT.isDown()) {
            holdToEjection = Mth.clamp(holdToEjection + 1, 0, 10);
            if (holdToEjection >= 10) {
                PacketDistributor.sendToServer(new PlayerStopRidingMessage(true));
            }
        } else {
            holdToEjection = 0;
        }

        isProne(player);
        handleVariableDecrease();
        aimAtVillager(player);
        staminaSystem();
        handlePlayerSprint();
        handleLungeAttack(player, stack);
        handleGunMelee(player, stack);
        weaponZooming(stack);
        lockWeaponSeeking(player, stack);
    }

    public static void lockWeaponSeeking(Player player, ItemStack stack) {
        if (stack.getItem() instanceof GunItem) {
            var data = GunData.from(stack);
            //锁定所需时间
            int lockTime = data.get(GunProp.SEEK_TIME);
            //搜寻角度
            float fovAdjust = (float) Minecraft.getInstance().options.fov().get() / 80;
            float seekAngle = data.get(GunProp.SEEK_ANGLE).floatValue() * fovAdjust;
            double range = data.get(GunProp.SEEK_RANGE);

            if (zoomTime > 0.7) {
                naerestEntity = SeekTool.seekLivingEntity(player, range, seekAngle);
                if (data.get(GunProp.SEEK_TYPE) == SeekType.HOLD_FIRE) {
                    if (naerestEntity == null || player.isShiftKeyDown()) {
                        // 锁定方块
                        BlockHitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getViewVector(1).scale(512)),
                                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
                        seekingPos = result.getLocation();

                        if (seekingTime > lockTime + 2 && !lockOn) {
                            lockOn = true;
                        }

                        //锁定失败
                        if (lockingPos != null && (VectorTool.calculateAngle(player.getLookAngle(), player.getEyePosition().vectorTo(lockingPos)) > seekAngle || !noClip(player, lockingPos))) {
                            seekingTime = 0;
                            lockOn = false;
                            lockingPos = null;
                        }

                        if (holdingFireKey) {
                            if (seekingPos.distanceToSqr(player.getEyePosition()) < range * range) {
                                seekingTime++;
                                if (seekingTime == 1) {
                                    lockingPos = seekingPos;
                                }
                            } else {
                                seekingTime = 0;
                                lockingPos = null;
                            }
                            guideType = 1;
                        } else {
                            if (lockOn) {
                                if (lockingPos != null) {
                                    PacketDistributor.sendToServer(new ShootMessage(gunSpread, zoom, Optional.empty(), Optional.of(lockingPos.toVector3f())));
                                }
                                lockOn = false;
                            }
                            seekingTime = 0;
                            lockingPos = null;
                        }

                    } else {
                        // 锁定实体
                        if (seekingTime > lockTime + 2 && !lockOn) {
                            lockingEntity = seekingEntity;
                            lockOn = true;
                        }

                        //锁定失败
                        if (seekingEntity != null && (VectorTool.calculateAngle(player.getLookAngle(), player.getEyePosition().vectorTo(VectorTool.lerpGetEntityBoundingBoxCenter(seekingEntity, 1))) > seekAngle
                                || !SeekTool.NOT_IN_SMOKE.test(seekingEntity)
                                || !noClip(player, seekingEntity))) {
                            seekingTime = 0;
                            lockingEntity = null;
                            seekingEntity = null;
                            lockOn = false;
                        }

                        if (holdingFireKey) {
                            if (seekingEntity == null) {
                                seekingEntity = naerestEntity;
                            }
                            if (naerestEntity != null && lockingPos == null) {
                                seekingTime++;
                                if ((!seekingEntity.getPassengers().isEmpty() || seekingEntity instanceof VehicleEntity) && player.tickCount % 3 == 0 && !lockOn) {
                                    PacketDistributor.sendToServer(new SeekingWeaponWarningMessage(false, seekingEntity.getUUID()));
                                }
                                guideType = 0;
                            }
                        } else {
                            if (lockOn) {
                                if (lockingEntity != null) {
                                    PacketDistributor.sendToServer(new ShootMessage(gunSpread, zoom, Optional.of(lockingEntity.getUUID()), Optional.of(lockingEntity.getEyePosition().toVector3f())));
                                }
                                lockOn = false;
                            }
                            seekingTime = 0;
                            lockingEntity = null;
                            seekingEntity = null;
                        }
                    }
                } else if (data.get(GunProp.SEEK_TYPE) == SeekType.HOLD_ZOOM) {
                    // 瞄准锁定只能锁实体
                    if (seekingTime > lockTime + 2 && !lockOn) {
                        lockingEntity = seekingEntity;
                        lockOn = true;
                    }

                    // 锁定失败
                    if (seekingEntity != null && (VectorTool.calculateAngle(player.getLookAngle(), player.getEyePosition().vectorTo(VectorTool.lerpGetEntityBoundingBoxCenter(seekingEntity, 1))) > seekAngle
                            || !SeekTool.NOT_IN_SMOKE.test(seekingEntity)
                            || !noClip(player, seekingEntity))) {
                        seekingTime = 0;
                        lockingEntity = null;
                        seekingEntity = null;
                        lockOn = false;
                    }

                    if (zoomTime > 0.7) {
                        if (seekingEntity == null) {
                            seekingEntity = naerestEntity;
                        }
                        if (naerestEntity != null && data.hasEnoughAmmoToShoot(player)) {
                            seekingTime++;
                            if ((!seekingEntity.getPassengers().isEmpty() || seekingEntity instanceof VehicleEntity) && player.tickCount % 3 == 0 && !lockOn) {
                                PacketDistributor.sendToServer(new SeekingWeaponWarningMessage(false, seekingEntity.getUUID()));
                            }
                        }
                    } else {
                        seekingTime = 0;
                        lockingEntity = null;
                        seekingEntity = null;
                    }

                    if (lockOn && holdingFireKey && lockingEntity != null) {
                        PacketDistributor.sendToServer(new ShootMessage(gunSpread, zoom, Optional.of(lockingEntity.getUUID()), Optional.of(lockingEntity.getEyePosition().toVector3f())));
                        holdingFireKey = false;
                    }
                }
            } else {
                seekingTime = 0;
                lockOn = false;
                lockingEntity = null;
                seekingEntity = null;
                lockingPos = null;
            }

            if (lockingEntity != null && !lockingEntity.isAlive()) {
                seekingTime = 0;
                lockOn = false;
                lockingEntity = null;
                seekingEntity = null;
                lockingPos = null;
            }

            if (seekingTime == 2) {
                playLockingSound(data, player);
            }

            if (seekingTime > lockTime) {
                playLockedSound(data, player);
                if (guideType == 0 && lockingEntity != null && (!lockingEntity.getPassengers().isEmpty() || lockingEntity instanceof VehicleEntity) && player.tickCount % 2 == 0) {
                    PacketDistributor.sendToServer(new SeekingWeaponWarningMessage(true, lockingEntity.getUUID()));
                }
            }
        }
    }

    public static void playLockingSound(GunData data, Player player) {
        var soundInfo = data.get(GunProp.SOUND_INFO);
        var sound = soundInfo.locking;
        if (sound != null) {
            player.playSound(sound, 4f, 1);
        }
    }

    public static void playLockedSound(GunData data, Player player) {
        var soundInfo = data.get(GunProp.SOUND_INFO);
        var sound = soundInfo.locked;
        if (sound != null) {
            player.playSound(sound, 4f, 1);
        }
    }

    public static boolean noClip(Entity entity, Entity e) {
        return entity.level()
                .clip(new ClipContext(entity.getEyePosition(), e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity))
                .getType() != HitResult.Type.BLOCK;
    }

    public static boolean noClip(Entity entity, Vec3 pos) {
        return entity.level()
                .clip(new ClipContext(entity.getEyePosition(), pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity))
                .getType() != HitResult.Type.BLOCK;
    }

    public static void weaponZooming(ItemStack stack) {
        if (stack.getItem() instanceof GunItem) {
            PacketDistributor.sendToServer(new WeaponZoomingMessage(zoomTime >= 0.7));
        }
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
            sprintCost = (float) (0.5 + 0.02 * data.get(GunProp.WEIGHT));
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

        if ((ModKeyMappings.BREATH.isDown() && zoom) || tacticalSprint) {
            switchTime = Math.min(switchTime + 0.65, 5);
        } else if (switchTime > 0 && stamina == 0) {
            switchTime = Math.max(switchTime - 0.15, 0);
        }

        if (zoom) {
            tacticalSprint = false;
        }

        if (tacticalSprint && (player.onGround() || player.jumping)) {
            PacketDistributor.sendToServer(new TacticalSprintMessage(true));
        } else {
            PacketDistributor.sendToServer(new TacticalSprintMessage(false));
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
            noSprintTicks = 3;
        }

        if (noSprintTicks > 0) {
            noSprintTicks--;
        }

        if (ClientEventHandler.zoom || ClientEventHandler.holdingFireKey) {
            player.setSprinting(false);
        }
    }

    private static void handleVariableDecrease() {
        if (holdingFireKeyTicks > 0 && !holdingFireKey) {
            holdingFireKeyTicks--;
            if (holdingFireKeyTicks == 0) {
                holdingFireKeyTicks0 = 0;
            }
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
        if (player.getPose() == Pose.SWIMMING && !player.isSwimming()) return true;
        Vec3 forward = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
        return player.isCrouching() && level.getBlockState(BlockPos.containing(player.getX() + 0.7 * forward.x, player.getY() + 0.5, player.getZ() + 0.7 * forward.z)).canOcclude()
                && !level.getBlockState(BlockPos.containing(player.getX() + 0.7 * forward.x, player.getY() + 1.5, player.getZ() + 0.7 * forward.z)).canOcclude();
    }

    public static void handleGunMelee(Player player, ItemStack stack) {
        if (stack.getItem() instanceof GunItem gunItem) {
            var data = GunData.from(stack);
            if (gunItem.hasMeleeAttack(data) && gunMelee == 0 && drawTime < 0.01
                    && (ModKeyMappings.MELEE.isDown() || (data.meleeOnly() && holdingFireKey))
                    && !(player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player))
                    && !holdFireVehicle
                    && !notInGame()
                    && !isEditing
                    && !(data.reload.normal() || data.reload.empty())
                    && !data.reloading()
                    && !data.charging() && !player.getCooldowns().isOnCooldown(stack.getItem())
            ) {
                gunMelee = data.get(GunProp.MELEE_DURATION);
                fireCooldown = gunMelee + 4;
            }
            if (gunMelee == data.get(GunProp.MELEE_DURATION) - data.get(GunProp.MELEE_DAMAGE_TIME)) {
                doGunMeleeAttack(player);
            }
        }

        if (gunMelee > 0) {
            gunMelee--;
        }
    }

    public static void doGunMeleeAttack(Player player) {
        player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1);
        Entity lookingEntity = TraceTool.findMeleeEntity(player, player.entityInteractionRange());
        if (lookingEntity != null) {
            PacketDistributor.sendToServer(new MeleeAttackMessage(lookingEntity.getUUID()));
        }
    }

    public static void handleLungeAttack(Player player, ItemStack stack) {
        if (stack.is(ModItems.LUNGE_MINE.get()) && lungeAttack == 0 && lungeDraw == 0 && holdingFireKey) {
            lungeAttack = 18;
            holdingFireKey = false;
            player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1);
        }

        if (stack.is(ModItems.LUNGE_MINE.get()) && ((lungeAttack >= 9 && lungeAttack <= 10.5) || lungeSprint > 0)) {
            Entity lookingEntity = TraceTool.findLookingEntity(player, player.entityInteractionRange() + 1.5);

            BlockHitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(player.entityInteractionRange() + 0.5)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

            Vec3 looking = Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(player.entityInteractionRange() + 0.5)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos());
            BlockState blockState = player.level().getBlockState(BlockPos.containing(looking.x(), looking.y(), looking.z()));

            if (lookingEntity != null) {
                PacketDistributor.sendToServer(new LungeMineAttackMessage(0, lookingEntity.getUUID(), result.getLocation()));
                lungeSprint = 0;
                lungeAttack = 0;
                lungeDraw = 15;
            } else if ((blockState.canOcclude() || blockState.getBlock() instanceof DoorBlock || blockState.getBlock() instanceof CrossCollisionBlock || blockState.getBlock() instanceof BellBlock) && lungeSprint == 0) {
                PacketDistributor.sendToServer(new LungeMineAttackMessage(1, player.getUUID(), result.getLocation()));
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
    public static void handleWeaponFire(RenderFrameEvent.Pre event) {
        ClientLevel level = Minecraft.getInstance().level;
        Player player = Minecraft.getInstance().player;
        if (player == null || level == null) return;

        if (notInGame()) {
            holdingFireKey = false;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            clientTimer.stop();
            fireSpread = 0;
            gunSpread = 0;
            return;
        }

        var data = GunData.from(stack);
        var resource = GunResource.compute(stack);
        var mode = data.selectedFireModeInfo().mode;

        var partialHoldingFireKeyTicks = Mth.lerp(getDelta(), holdingFireKeyTicks0, holdingFireKeyTicks);
        holdingFireKeyTicks0 = holdingFireKeyTicks;

        if (partialHoldingFireKeyTicks > holdingFireKeyTicks && partialHoldingFireKeyTicks > data.get(GunProp.SHOOT_DELAY) * 0.25 && shouldPlayDischargeSound) {
            var dischargeSound = resource.dischargeSound;
            if (dischargeSound != null) {
                player.playSound(dischargeSound, partialHoldingFireKeyTicks * 0.03f, 0.6f + partialHoldingFireKeyTicks * 0.02f);
            }

            shouldPlayDischargeSound = false;
            burstFireAmount = 0;
        }

        if (!gunItem.canShoot(data, player) && !data.meleeOnly()) {
            holdingFireKey = false;
        }

        // 精准度
        float times = (float) Math.min(getDelta(), 0.8);

        double basicDev = data.get(GunProp.SPREAD);
        double walk = isMoving() ? 0.3 * basicDev : 0;
        double sprint = player.isSprinting() ? 0.25 * basicDev : 0;
        double crouching = player.isCrouching() ? -0.15 * basicDev : 0;
        double prone = isProne(player) ? -0.3 * basicDev : 0;
        double jump = player.onGround() ? 0 * basicDev : 0.35 * basicDev;
        double ride = player.onGround() ? -0.25 * basicDev : 0;

        double zoomSpread = 1 - (1 - data.get(GunProp.ZOOM_SPREAD_RATE)) * zoomTime;

        double spread = data.isShotgun() || stack.is(ModItems.MINIGUN.get()) ? 1.2 * zoomSpread * (basicDev + 0.2 * (walk + sprint + crouching + prone + jump + ride) + fireSpread) : zoomSpread * (0.7 * basicDev + walk + sprint + crouching + prone + jump + ride + 0.8 * fireSpread);

        gunSpread = Mth.lerp(0.14 * times, gunSpread, spread);

        // 开火部分
        double weight = data.get(GunProp.WEIGHT);
        double speed = 5 / (weight + 4);

        if (noSprintTicks == 0 && player.isSprinting() && !zoom && !holdingFireKey) {
            fireCooldown = Mth.clamp(fireCooldown + 3 * times, 0, 24);
        } else {
            fireCooldown = Mth.clamp(fireCooldown - 6 * speed * times, 0, 40);
        }

        int rpm = Mth.clamp(data.get(GunProp.RPM) + customRpm, 1, 114514);
        double rps = (double) rpm / 60;

        // cooldown in ms
        int cooldown = (int) Math.round(1000 / rps);

        //左轮类
        if (clientTimer.getProgress() == 0 && stack.is(ModItems.TRACHELIUM.get()) && holdingFireKey) {
            revolverPreTime = Mth.clamp(revolverPreTime + 0.3 * times, 0, 1);
            revolverWheelPreTime = Mth.clamp(revolverWheelPreTime + 0.32 * times, 0, revolverPreTime > 0.7 ? 1 : 0.55);
        } else {
            revolverPreTime = Mth.clamp(revolverPreTime - 1.2 * times, 0, 1);
        }

        if (((holdingFireKey || burstFireAmount > 0) && holdingFireKeyTicks >= data.get(GunProp.SHOOT_DELAY))
                && !(player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player))
                && !holdFireVehicle
                && gunItem.canShoot(data, player)
                && !gunItem.useSpecialFireProcedure(data)
                && fireCooldown == 0
                && sprintBasicRotX * sprintBasicRotY * sprintBasicRotZ < 0.0001
                && drawTime < 0.01
                && !notInGame()
                && !isEditing
        ) {
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

        if (GunData.from(stack).reload.normal() || GunData.from(stack).reload.empty()) {
            customRpm = 0;
        }

        data.save();
    }

    private static float getDelta() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
    }

    public static void shootClient(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;
        var data = GunData.from(stack);
        if (!gunItem.canShoot(data, player) || gunItem.useSpecialFireProcedure(data)) return;

        var mode = data.selectedFireModeInfo().mode;
        if (mode != FireMode.AUTO) {
            holdingFireKey = false;
        }

        if (data.get(GunProp.CLEAR_HOLD_PROGRESS_AFTER_SHOOT)) {
            holdingFireKeyTicks = 0;
        }

        if (mode == FireMode.BURST && burstFireAmount == 1) {
            fireCooldown = data.get(GunProp.BURST_COOLDOWN);
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
        if (data.get(GunProp.BOLT_ACTION_TIME) > 0 && data.hasEnoughAmmoToShoot(player)) {
            data.bolt.needed.set(true);
        }

        revolverPreTime = 0;
        revolverWheelPreTime = 0;

        playGunClientSounds(player);
        handleClientShoot();
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

        PacketDistributor.sendToServer(new ShootMessage(gunSpread, zoom, lockedEntity != null ? Optional.of(lockedEntity.getUUID()) : Optional.empty(), Optional.empty()));
        fireRecoilTime = 10;

        // 真实后座（
        if (data.get(GunProp.RECOIL) != 0) {
            player.setDeltaMovement(player.getDeltaMovement().add(player.getViewVector(1).scale(-data.get(GunProp.RECOIL))));
        }

        var gunRecoilY = data.get(GunProp.RECOIL_Y) * 10;

        recoilY = (float) (2 * Math.random() - 1) * gunRecoilY;

        if (shellIndex < 5) {
            shellIndex++;
        }

        noSprintTicks = 7;

        shellIndexTime[shellIndex] = 0.001;

        randomShell[0] = (1 + 0.2 * (Math.random() - 0.5));
        randomShell[1] = (0.2 + (Math.random() - 0.5));
        randomShell[2] = (0.7 + (Math.random() - 0.5));
    }

    public static void handleShakeClient(double time, double radius, double amplitude, double x, double y, double z) {
        Player player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) return;

        double distance = player.position().distanceTo(new Vec3(x, y, z));

        int time2 = (int) (distance / 17);

        if (time2 == 0) {
            float shakeStrength = (float) DisplayConfig.EXPLOSION_SCREEN_SHAKE.get() / 100.0f;
            if (shakeStrength <= 0.0f) return;

            shakeTime = time;
            shakeRadius = radius;
            shakeAmplitude = amplitude * Mth.DEG_TO_RAD * shakeStrength;
            shakePos[0] = x * shakeStrength;
            shakePos[1] = y * shakeStrength;
            shakePos[2] = z * shakeStrength;
            shakeType = 2 * (Math.random() - 0.5);
        } else {
            Mod.queueClientWork((int) (distance / 17), () -> {
                float shakeStrength = (float) DisplayConfig.EXPLOSION_SCREEN_SHAKE.get() / 100.0f;
                if (shakeStrength <= 0.0f) return;
                shakeTime = time;
                shakeRadius = radius;
                shakeAmplitude = amplitude * Mth.DEG_TO_RAD * shakeStrength;
                shakePos[0] = x * shakeStrength;
                shakePos[1] = y * shakeStrength;
                shakePos[2] = z * shakeStrength;
                shakeType = 2 * (Math.random() - 0.5);
            });
        }
    }

    public static void playGunClientSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        if (stack.getItem() == ModItems.SENTINEL.get()) {
            var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            var charged = cap != null && cap.getEnergyStored() > 0;

            if (charged) {
                player.playSound(ModSounds.SENTINEL_CHARGE_FIRE_1P.get(), 2f, (float) ((2 * Math.random() - 1) * 0.05f + 1.0f));
                return;
            }
        }

        if (stack.getItem() == ModItems.SECONDARY_CATACLYSM.get()) {
            var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            var hasEnoughEnergy = cap != null && cap.getEnergyStored() > 3000;

            boolean isChargedFire = zoom && hasEnoughEnergy;

            if (isChargedFire) {
                player.playSound(ModSounds.SECONDARY_CATACLYSM_FIRE_1P_CHARGE.get(), 2f, (float) ((2 * Math.random() - 1) * 0.05f + 1.0f));
                return;
            }
        }

        var data = GunData.from(stack);

        var perk = data.perk.get(Perk.Type.AMMO);
        SoundInfo soundInfo = data.get(GunProp.SOUND_INFO);

        float pitch = data.heat.get() <= 75 ? 1 : (float) (1 - 0.02 * Math.abs(75 - data.heat.get()));

        if (perk == ModPerks.BEAST_BULLET.get()) {
            player.playSound(ModSounds.HENG.get(), 1f, (float) ((2 * Math.random() - 1) * 0.1f + pitch));
        }

        boolean isSilent = GunData.from(stack).attachment.get(AttachmentType.BARREL) == 2;
        var fire1p = isSilent ? soundInfo.fire1PSilent : soundInfo.fire1P;

        if (fire1p != null) {
            player.playSound(fire1p, 4f, (float) ((2 * Math.random() - 1) * 0.05f + pitch));
        }

        double shooterHeight = player.getEyePosition().distanceTo((Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos())));

        Mod.queueClientWork((int) (1 + 1.5 * shooterHeight), () -> {
            if (GunResource.compute(stack).ejectShell) {
                if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                    var ammoType = data.selectedAmmoConsumer().getPlayerAmmoType();
                    switch (ammoType) {
                        case SHOTGUN ->
                                player.playSound(ModSounds.SHELL_CASING_SHOTGUN.get(), (float) Math.max(0.75 - 0.12 * shooterHeight, 0), (float) ((2 * Math.random() - 1) * 0.05f + 1.0f));
                        case SNIPER, HEAVY ->
                                player.playSound(ModSounds.SHELL_CASING_50CAL.get(), (float) Math.max(1 - 0.15 * shooterHeight, 0), (float) ((2 * Math.random() - 1) * 0.05f + 1.0f));
                        default ->
                                player.playSound(ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), (float) ((2 * Math.random() - 1) * 0.05f + 1.0f));
                    }
                } else {
                    player.playSound(ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), (float) ((2 * Math.random() - 1) * 0.05f + 1.0f));
                }
            }
        });
    }

    @SubscribeEvent
    public static void handleVehicleFire(RenderFrameEvent.Pre event) {
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

            if (holdFireVehicle) {
                if (!clientTimerVehicle.started()) {
                    clientTimerVehicle.start();
                    // 首发瞬间发射
                    clientTimerVehicle.setProgress((cooldown + 1));
                }

                if (clientTimerVehicle.getProgress() >= cooldown) {
                    var newProgress = clientTimerVehicle.getProgress();

                    // 低帧率下的开火次数补偿
                    do {
                        PacketDistributor.sendToServer(VehicleFireMessage.INSTANCE);
                        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || zoomVehicle) {
                            playVehicleClientSounds(player, pVehicle);
                        }

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

    public static void playVehicleClientSounds(Player player, VehicleEntity vehicle) {
        var gunData = vehicle.getGunData(vehicle.getSeatIndex(player));
        if (gunData != null) {
            var soundInfo = gunData.get(GunProp.SOUND_INFO);
            float pitch = 1;
            // TODO 正确获取热量
//            float pitch = getWeaponHeat(living) <= 60 ? 1 : (float) (1 - 0.011 * java.lang.Math.abs(60 - getWeaponHeat(living)));

            var sound = soundInfo.fire1P;
            if (sound != null) {
                player.playSound(sound, 1f, pitch);
            }
        }
    }

    @SubscribeEvent
    public static void handleWeaponBreathSway(RenderFrameEvent.Pre event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;
        var data = GunData.from(stack);

        if (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.isDriver(player) && iArmedVehicle.getVehicleEntity().hidePassenger(player))
            return;

        float pose;
        float times = 2 * (float) Math.min(getDelta(), 0.8);

        if (player.isCrouching() && player.getBbHeight() >= 1 && !isProne(player)) {
            pose = 0.85f;
        } else if (isProne(player)) {
            pose = (data.attachment.get(AttachmentType.GRIP) == 3 || gunItem.hasBipod(data)) ? 0 : 0.25f;
        } else {
            pose = 1;
        }

        int stockType = data.attachment.get(AttachmentType.STOCK);

        double sway = switch (stockType) {
            case 1 -> 1;
            case 2 -> 0.55;
            default -> 0.8;
        };

        float customWeight = Mth.clamp(data.get(GunProp.WEIGHT).floatValue(), 1, 30);

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
        final var tag = NBTTool.getTag(stack);

        if (level != null
                && (stack.is(ModItems.MONITOR.get())
                && tag.getBoolean("Using")
                && tag.getBoolean("Linked"))
        ) {
            handleDroneCamera(event, living, tag);
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
        cameraRoll *= 0.99f;

        if (player != null && player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player)) {
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

    private static void handleDroneCamera(ViewportEvent.ComputeCameraAngles event, LivingEntity entity, final CompoundTag tag) {
        DroneEntity drone = EntityFindUtil.findDrone(entity.level(), tag.getString("LinkedDrone"));

        if (drone != null) {
            cameraRoll = drone.getRoll((float) event.getPartialTick()) * (1 - (drone.getPitch((float) event.getPartialTick()) / 90));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        var mainHand = Minecraft.getInstance().options.mainHand().get();
        InteractionHand leftHand = mainHand == HumanoidArm.RIGHT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        InteractionHand rightHand = mainHand == HumanoidArm.RIGHT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

        ItemStack rightHandItem = player.getItemInHand(rightHand);
        final var tag = NBTTool.getTag(rightHandItem);

        if (event.getHand() == leftHand) {
            if (rightHandItem.getItem() instanceof GunItem) {
                event.setCanceled(true);
            }
            if (rightHandItem.is(ModItems.LUNGE_MINE.get())) {
                event.setCanceled(true);
            }
            if (player.isUsingItem() && player.getUseItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
                event.setCanceled(true);
            }
        }

        if (event.getHand() == rightHand) {
            if (rightHandItem.getItem() instanceof GunItem && drawTime > 0.15) {
                event.setCanceled(true);
            }
            if (player.isUsingItem() && player.getUseItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
                event.setCanceled(true);
            }
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using")
                && tag.getBoolean("Linked")
                && EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone")) != null) {
            event.setCanceled(true);
        }

        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player)) {
            event.setCanceled(true);
        }
    }

    private static void handleWeaponSway(LivingEntity entity) {
        ItemStack stack = entity.getMainHandItem();

        if (stack.getItem() instanceof GunItem gunItem && entity instanceof Player player) {
            var data = GunData.from(stack);
            float times = 2 * (float) Math.min(getDelta(), 0.8);
            double pose;

            if (player.isShiftKeyDown() && player.getBbHeight() >= 1 && isProne(player)) {
                pose = 0.85;
            } else if (isProne(player)) {
                pose = (data.attachment.get(AttachmentType.GRIP) == 3 || gunItem.hasBipod(data)) ? 0 : 0.25f;
            } else {
                pose = 1;
            }

            swayTime += 0.05 * times;

            swayX = pose * -0.008 * Math.sin(swayTime) * (1 - 0.95 * zoomTime);
            swayY = pose * 0.125 * Math.sin(swayTime - 1.585) * (1 - 0.95 * zoomTime) - 3 * moveRotZ;
        }
    }

    private static void handleWeaponMove(LivingEntity entity) {
        ItemStack stack = entity.getMainHandItem();
        if (stack.getItem() instanceof GunItem && entity instanceof Player player) {
            float times = 3.7f * (float) Math.min(getDelta(), 0.8);
            double moveSpeed = entity.getDeltaMovement().horizontalDistance();
            double animSpeed;

            var data = GunData.from(stack);

            if (entity.onGround()) {
                if (entity.isSprinting()) {
                    animSpeed = 1.8;
                } else {
                    animSpeed = 2.0;
                }
            } else {
                animSpeed = 0.005;
            }

            float customWeight = Mth.clamp(data.get(GunProp.WEIGHT).floatValue(), 1, 50);

            if (!isEditing) {
                if (!entity.isSprinting() && Minecraft.getInstance().options.keyUp.isDown() && firePosTimer == 0) {
                    moveRotZ = Mth.lerp(0.2f * times, moveRotZ, 0.14) * (1 - zoomTime);
                } else {
                    moveRotZ = Mth.lerp(0.2f * times, moveRotZ, 0) * (1 - zoomTime);
                }
                if (entity.isSprinting() && !data.reloading() && firePosTimer == 0 && !ModKeyMappings.FIRE.isDown() && noSprintTicks == 0 && zoomTime < 0.1) {
                    sprintBasicRotX = Mth.clamp(Mth.lerp(0.3f * times / (customWeight + 4), sprintBasicRotX, 1), 0, 1);
                    sprintBasicRotY = Mth.clamp(Mth.lerp(0.18f * times / (customWeight + 4), sprintBasicRotY, 1), 0, 1);
                    sprintBasicRotZ = Mth.clamp(Mth.lerp(0.3f * times / (customWeight + 4), sprintBasicRotZ, 1), 0, 1);

                    sprintBasicPosX = Mth.clamp(Mth.lerp(0.8f * times / (customWeight + 4), sprintBasicPosX, 1), 0, 1);
                    sprintBasicPosY = Mth.clamp(Mth.lerp(0.25f * times / (customWeight + 4), sprintBasicPosY, 1), 0, 1);
                    sprintBasicPosZ = Mth.clamp(Mth.lerp(0.8f * times / (customWeight + 4), sprintBasicPosZ, 1), 0, 1);
                } else {
                    sprintBasicRotX = Mth.clamp(Mth.lerp(1.4f * times / customWeight, sprintBasicRotX, 0), 0, 1);
                    sprintBasicRotY = Mth.clamp(Mth.lerp(0.96f * times / customWeight, sprintBasicRotY, 0), 0, 1);
                    sprintBasicRotZ = Mth.clamp(Mth.lerp(1.4f * times / customWeight, sprintBasicRotZ, 0), 0, 1);

                    sprintBasicPosX = Mth.clamp(Mth.lerp(0.8f * times / customWeight, sprintBasicPosX, 0), 0, 1);
                    sprintBasicPosY = Mth.clamp(Mth.lerp(0.8f * times / customWeight, sprintBasicPosY, 0), 0, 1);
                    sprintBasicPosZ = Mth.clamp(Mth.lerp(0.8f * times / customWeight, sprintBasicPosZ, 0), 0, 1);
                }
            }

            if (isMoving()) {
                moveTime += 0.15 * animSpeed * times * moveSpeed * (firePosTimer != 0 ? 0.4 : 1);
                sprintTime += 0.15 * animSpeed * times * moveSpeed * (player.isSprinting() ? sprintBasicPosX : 1) * (firePosTimer != 0 ? 0.4 : 1);
                moveFadeTime = Mth.lerp(0.13 * times, moveFadeTime, 1);
            } else {
                moveFadeTime = Mth.lerp(0.1 * times, moveFadeTime, 0);
            }

            if (entity.isSprinting() && !data.reloading() && firePosTimer == 0 && !ModKeyMappings.FIRE.isDown() && noSprintTicks == 0) {
                if (entity.onGround()) {
                    sprintFadeTime = Mth.lerp(0.08 * times, sprintFadeTime, 1);
                } else {
                    sprintFadeTime = Mth.lerp(0.15 * times, sprintFadeTime, 0);
                }

                sprintPosX = 2 * Math.sin(1 * Math.PI * sprintTime) * (1 - 0.95 * zoomTime) * sprintFadeTime;
                sprintPosY = 1 * Math.sin(2 * Math.PI * sprintTime) * (1 - 0.95 * zoomTime) * sprintFadeTime;
            } else {

                sprintPosX = Mth.lerp(0.1 * times, sprintPosX, 0);
                sprintPosY = Mth.lerp(0.1 * times, sprintPosY, 0);

                sprintFadeTime = Mth.lerp(0.1 * times, sprintFadeTime, 0);
            }

            movePosX = 0.2 * Math.sin(1 * Math.PI * moveTime) * (1 - 0.95 * zoomTime) * moveFadeTime;
            movePosY = -0.135 * Math.sin(2 * Math.PI * (moveTime - 0.25)) * (1 - 0.95 * zoomTime) * moveFadeTime;

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

    public static void gunRootMove(AnimationProcessor<?> animationProcessor, float customX, float customY, float customZ, boolean useCustomAnim) {
        GeoBone root = animationProcessor.getBone("root");
        float walkPosX = (float) movePosX;
        float walkPosY = (float) (swayY + movePosY);
        float walkPosZ = 0;
        float walkRotX = (float) swayX;
        float walkRotY = (float) (0.2f * movePosX);
        float walkRotZ = (float) (0.2f * movePosX);

        int i = useCustomAnim ? 0 : 1;

        float basicSprintPosX = (float) (sprintBasicPosX * (1.5 + customX)) * i;
        float basicSprintPosY = (float) (sprintBasicPosY * (-2.35 + customY - 8 * AnimationCurves.PARABOLA.apply(sprintBasicPosY))) * i;
        float basicSprintPosZ = (float) (sprintBasicPosZ * (-0.55 + customZ)) * i;

        float basicSprintRotX = (float) (sprintBasicRotX * 39 * Mth.DEG_TO_RAD) * i;
        float basicSprintRotY = (float) (sprintBasicRotY * 35.6 * Mth.DEG_TO_RAD) * i;
        float basicSprintRotZ = (float) (sprintBasicRotZ * 34.7 * Mth.DEG_TO_RAD) * i;

        float gunPosX = (float) (walkPosX + basicSprintPosX + sprintPosX * i + 20 * drawTime + 9.3f * movePosHorizon) * (float) (1 - 1 * zoomTime);
        float gunPosY = (float) (walkPosY + basicSprintPosY + sprintPosY * i - 40 * drawTime - 2f * velocityY) * (float) (1 - 1 * zoomTime);
        float gunPosZ = (walkPosZ + basicSprintPosZ) * (float) (1 - 1 * zoomTime);
        float gunRotX = (float) ((walkRotX + basicSprintRotX - Mth.DEG_TO_RAD * 60 * drawTime - 0.15f * velocityY) * (1 - 1 * zoomTime) + Mth.DEG_TO_RAD * turnRot[0]);
        float gunRotY = (float) ((walkRotY + basicSprintRotY + (0.2f * sprintBasicPosX * i) + Mth.DEG_TO_RAD * 300 * drawTime) * (1 - 1 * zoomTime) + Mth.DEG_TO_RAD * turnRot[1]);
        float gunRotZ = (float) ((walkRotZ + basicSprintRotZ + moveRotZ + Mth.DEG_TO_RAD * 90 * drawTime + 2.7f * movePosHorizon) * (1 - 1 * zoomTime) + Mth.DEG_TO_RAD * turnRot[2]);

        root.setPosX(gunPosX);
        root.setPosY(gunPosY);
        root.setPosZ(gunPosZ);
        root.setRotX(gunRotX);
        root.setRotY(gunRotY);
        root.setRotZ(gunRotZ);
    }

    private static void handleWeaponZoom(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        var stack = player.getMainHandItem();
        var data = GunData.from(stack);
        float times = 5 * getDelta();

        double weight = data.get(GunProp.WEIGHT);
        double speed = 7 / (weight + 2);

        if (zoom
                && !(player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player))
                && !notInGame()
                && drawTime < 0.01
                && !isEditing
                && !(data.reloading() && !data.get(GunProp.ZOOM_RELOAD))) {
            if (Minecraft.getInstance().player != null) {
                noSprintTicks = 5;
            }
            if (fireCooldown <= 10) {
                zoomTime = Mth.clamp(zoomTime + 0.03 * speed * times, 0, 1);
            }
        } else {
            zoomTime = Mth.clamp(zoomTime - 0.04 * speed * times, 0, 1);
        }
        zoomPos = AnimationCurves.EASE_IN_OUT_QUINT.apply(zoomTime);
        zoomPosZ = AnimationCurves.PARABOLA.apply(zoomTime);
    }

    private static void handleWeaponFire(ViewportEvent.ComputeCameraAngles event, LivingEntity entity) {
        float times = (float) (2f * customAnimSpeed * Math.min(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), 0.48f));
        ItemStack stack = entity.getMainHandItem();

        var data = GunData.from(stack);
        double amplitude = 25000 * data.get(GunProp.RECOIL_Y) * data.get(GunProp.RECOIL_X);

        if (fireRecoilTime > 0) {
            firePosTimer = 0.001;
            fireRotTimer = 0.001;
            fireRecoilTime -= 7 * times;
            fireSpread += 0.1 * times;
            firePosZ += (0.8 * firePosZ + 0.4) * (4 * Math.random() + 0.85) * times;
            recoilTime = 0.01;
        }

        fireSpread = Mth.clamp(fireSpread - 0.1 * (Math.pow(fireSpread, 2) * times), 0, 2);
        firePosZ = Mth.clamp(firePosZ - 1.2 * (Math.pow(firePosZ, 2) * times), 0, 2.5);
        firePosZ *= 0.98f;

        if (0 < firePosTimer) {
            firePosTimer += 0.2 * (2.05 - firePosTimer) * times;
        }
        if (0 < fireRotTimer) {
            fireRotTimer += 0.1 * (3.1 - fireRotTimer) * times;
        }

        if (firePosTimer >= 2) {
            firePosTimer = 0;
        }
        if (fireRotTimer >= 3) {
            fireRotTimer = 0;
        }

        firePos = MathTool.decayingOscillation(2.5f, 2, 0.5f, (float) firePosTimer);
        fireRot = MathTool.decayingOscillation(0.2f, 3, 0.5f, (float) fireRotTimer) * Mth.sin((float) fireRotTimer);

        if (fireRot < 0) {
            fireRot *= 0.5;
        }

        fireRotZ = MathTool.decayingOscillation((float) (1f * recoilHorizon), 3, 0.5f, (float) fireRotTimer);
        fireRotY = MathTool.decayingOscillation((float) (0.1f * recoilHorizon), 3, 0.5f, (float) fireRotTimer);

        if (entity instanceof Player player && player.isSpectator()) return;

        float yaw = event.getYaw();
        float pitch = event.getPitch();

        if (0 < fireRotTimer) {
            float shake = (float) (MathTool.decayingOscillation(0.5f, 2f, 0.75f, (float) fireRotTimer) * (1 + amplitude) * (float) (DisplayConfig.WEAPON_SCREEN_SHAKE.get() / 100.0));
            if (recoilY > 0) {
                event.setYaw(yaw - 0.5f * shake);
                event.setPitch(pitch + shake);
                cameraRot[2] = shake;
            } else if (recoilY <= 0) {
                event.setYaw(yaw + 0.5f * shake);
                event.setPitch(pitch - shake);
                cameraRot[2] = shake;
            }
        }
    }

    public static void handleShootAnimation(GeoBone bone, float x, float y, float z, float rotX, float rotY, float rotZ, float zoomMultiply, float customSpeed) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;

        customAnimSpeed = customSpeed;

        var data = GunData.from(stack);
        int barrelType = data.attachment.get(AttachmentType.BARREL);
        int gripType = data.attachment.get(AttachmentType.GRIP);
        int scopeType = data.attachment.get(AttachmentType.SCOPE);

        float recoil = switch (barrelType) {
            case 1 -> 0.75f;
            case 2 -> 0.95f;
            default -> 1;
        };

        float gripRecoilX = switch (gripType) {
            case 1 -> 0.85f;
            case 2 -> 0.95f;
            default -> 1;
        };

        float gripRecoilY = switch (gripType) {
            case 1 -> 0.95f;
            case 2 -> 0.85f;
            default -> 1;
        };

        float zoomRecoil = switch (scopeType) {
            case 2 -> 1.25f - (float) (zoomTime * 0.8f);
            case 3 -> 1.25f - (float) zoomTime;
            default -> 1.25f;
        };


        float pose = 1;
        if (player.isShiftKeyDown() && player.getBbHeight() >= 1 && !isProne(player)) {
            pose = 0.85f;
        } else if (isProne(player)) {
            if (data.attachment.get(AttachmentType.GRIP) == 3 || gunItem.hasBipod(data)) {
                pose = 0.5f;
            } else {
                pose = 0.75f;
            }
        }

        zoomMultiply = Mth.clamp(zoomMultiply, 0, 1);

        float zoom = (float) (1 - zoomMultiply * zoomTime) * pose;

        if (bone != null) {
            bone.setPosX(zoom * x * (float) (ClientEventHandler.recoilHorizon * (0.12f * firePos)));
            bone.setPosY(zoom * y * (float) (0.05f * firePos));
            bone.setPosZ(zoom * z * (float) (firePos + 0.3f * firePosZ) * (float) (1 - 0.25 * zoomTime));
            bone.setRotX(zoom * rotX * (float) (fireRot + 0.03f * firePosZ) * gripRecoilX * recoil * (float) (1 - 0.75 * zoomTime) * zoomRecoil);
            bone.setRotY(2 * zoom * rotY * (float) fireRotY * gripRecoilY * recoil * (float) (1 - 0.3 * zoomTime) * zoomRecoil);
            bone.setRotZ(zoom * rotZ * (float) fireRotZ * gripRecoilY * recoil * (float) (1 - 0.25 * zoomTime) * zoomRecoil);
        }
    }

    private static void handleWeaponShell() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        float times = (float) Math.min(getDelta(), 0.8);

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
        var data = GunData.from(stack);

        float times = (float) Math.min(getDelta(), 1.6);
        int barrelType = data.attachment.get(AttachmentType.BARREL);
        int gripType = data.attachment.get(AttachmentType.GRIP);

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

        double customWeight = data.get(GunProp.WEIGHT);

        double rpm = 1;

        if (stack.is(ModItems.MINIGUN.get())) {
            rpm = (double) data.get(GunProp.RPM) / 1800;
        }

        float gunRecoilX = data.get(GunProp.RECOIL_X).floatValue() * 60;

        recoilHorizon = Mth.lerp(0.2 * times, recoilHorizon, 0) + recoilY;
        recoilY = 0;

        // 计算后坐力
        float pose = 1;
        if (player.isShiftKeyDown() && player.getBbHeight() >= 1 && !isProne(player)) {
            pose = 0.7f;
        } else if (isProne(player)) {
            if (data.attachment.get(AttachmentType.GRIP) == 3 || gunItem.hasBipod(data)) {
                pose = 0.1f;
            } else {
                pose = 0.5f;
            }
        }

        // 水平后座
        float newYaw = player.getYRot() - (float) (0.6 * recoilHorizon * pose * times * (0.5 + fireSpread) * recoil * (4 / (customWeight + 4)) * gripRecoilX * rpm);
        player.setYRot(newYaw);
        player.yRotO = player.getYRot();

        double sinRes = 0;

        // 竖直后座
        if (0 < recoilTime && recoilTime < 0.5) {
            float newPitch = (float) (player.getXRot() - 0.02f * gunRecoilX * times * recoil * (4 / (customWeight + 4)) * gripRecoilY * rpm);
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
            float newPitch = player.getXRot() - (float) (1.5 * pose * gunRecoilX * (sinRes + Mth.clamp(0.5 - recoilTime, 0, 0.5)) * times * (0.5 + fireSpread) * recoil * (4 / (customWeight + 4)) * gripRecoilY * rpm);
            player.setXRot(newPitch);
            player.xRotO = player.getXRot();
        }

        if (recoilTime >= 2.5) recoilTime = 0;
    }

    private static void handleShockCamera(ViewportEvent.ComputeCameraAngles event, LivingEntity entity) {
        if (entity.hasEffect(ModMobEffects.SHOCK) && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
            float shakeStrength = (float) DisplayConfig.SHOCK_SCREEN_SHAKE.get() / 100.0f;
            if (shakeStrength <= 0.0f) return;
            event.setYaw(Minecraft.getInstance().gameRenderer.getMainCamera().getYRot() +
                    (float) Mth.nextDouble(RandomSource.create(), -3, 3) * shakeStrength);
            event.setPitch(Minecraft.getInstance().gameRenderer.getMainCamera().getXRot() +
                    (float) Mth.nextDouble(RandomSource.create(), -3, 3) * shakeStrength);
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
        float times = (float) Math.min(getDelta(), 0.8);
        LocalPlayer player = Minecraft.getInstance().player;

        if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
            cameraLocation = Mth.clamp(cameraLocation - 0.05 * getDelta(), -0.6, 0.6);
        }

        if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
            cameraLocation = Mth.clamp(cameraLocation + 0.05 * getDelta(), -0.6, 0.6);
        }

        if (player == null) return;

        double range;
        Entity lookingEntity = SeekTool.seekEntity(player, 520, 5);

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
        float times = 4 * (float) Math.min(getDelta(), 0.8);

        var data = GunData.from(stack);

        if (holdingFireKey && data.hasEnoughAmmoToShoot(entity) && !bowPull && stack.is(ModItems.BOCEK.get())) {
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
        float times = (float) Math.min(getDelta(), 1.6);
        Player player = mc.player;
        if (player == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        final var tag = NBTTool.getTag(stack);

        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player) && player.getVehicle() instanceof WeaponVehicleEntity iVehicle && zoomVehicle) {
            event.setFOV(event.getFOV() / iVehicle.zoomFov());
            fov = event.getFOV();
            return;
        }

        double factor;

        if (player.isUsingItem() && player.getUseItem().is(ModItems.ARTILLERY_INDICATOR.get()) && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            factor = 4 + artilleryIndicatorCustomZoom;
        } else {
            factor = 1;
        }

        artilleryIndicatorZoom = Mth.lerp(0.3 * times, artilleryIndicatorZoom, factor);

        event.setFOV(event.getFOV() / artilleryIndicatorZoom);

        if (stack.getItem() instanceof GunItem) {
            if (!event.usedConfiguredFov()) {
                lastX = player.getXRot();
                lastY = player.getYRot();
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
            if (zoom && !notInGame() && drawTime < 0.01 && !isEditing) {
                if (player.isShiftKeyDown()) {
                    lockedEntity = null;
                } else {
                    int intelligentChipLevel = GunData.from(stack).perk.getLevel(ModPerks.INTELLIGENT_CHIP);
                    double seekRange = 32 + 8 * (intelligentChipLevel - 1);

                    if (intelligentChipLevel > 0) {
                        if (ClientEventHandler.lockedEntity == null || !lockedEntity.isAlive()) {
                            if (GunData.from(stack).perk.has(ModPerks.PHASE_PENETRATING_BULLET.get()) || GunData.from(stack).perk.has(ModPerks.BEAST_BULLET.get())) {
                                ClientEventHandler.lockedEntity = SeekTool.seekEntityThroughWall(player, seekRange, 16 / customZoom);
                            } else {
                                ClientEventHandler.lockedEntity = SeekTool.seekLivingEntity(player, seekRange, 16 / customZoom);
                            }
                        }
                        if (lockedEntity != null && lockedEntity.isAlive()) {
                            Vec3 targetVec = new Vec3(Mth.lerp(event.getPartialTick(), lockedEntity.xo, lockedEntity.getX()), Mth.lerp(event.getPartialTick(), lockedEntity.yo + lockedEntity.getEyeHeight(), lockedEntity.getEyeY()), Mth.lerp(event.getPartialTick(), lockedEntity.zo, lockedEntity.getZ()));
                            Vec3 playerVec = new Vec3(Mth.lerp(event.getPartialTick(), player.xo - 0.1 * player.getViewVector(1).x, player.getX() - 0.1 * player.getViewVector(1).x),
                                    Mth.lerp(event.getPartialTick(), player.yo + player.getEyeHeight() - 0.1 * player.getViewVector(1).y, player.getEyeY() - 0.1 * player.getViewVector(1).y),
                                    Mth.lerp(event.getPartialTick(), player.zo - 0.1 * player.getViewVector(1).z, player.getZ() - 0.1 * player.getViewVector(1).z));

                            var hasGravity = data.perk.getLevel(ModPerks.MICRO_MISSILE) <= 0;
                            double velocity;

                            if (stack.is(ModItems.BOCEK.get())) {
                                velocity = zoomTime * 24;
                            } else {
                                velocity = data.get(GunProp.VELOCITY);
                            }

                            Vec3 toVec = RangeTool.calculateFiringSolution(playerVec, targetVec, lockedEntity.getDeltaMovement(), velocity, hasGravity ? 0.03 : 0);
                            look(player, toVec);

                            if (player.distanceTo(lockedEntity) > seekRange) {
                                lockedEntity = null;
                            }
                        }
                    }
                }
            } else {
                lockedEntity = null;
            }

            lastX = player.getXRot();
            lastY = player.getYRot();
        }

        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            droneFovLerp = Mth.lerp(0.1 * getDelta(), droneFovLerp, droneFov);
            event.setFOV(event.getFOV() / droneFovLerp);
            fov = event.getFOV();
        }
    }

    private static float lastX;
    private static float lastY;

    public static void look(Player player, Vec3 pTarget) {
        double d0 = pTarget.x;
        double d1 = pTarget.y;
        double d2 = pTarget.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        float fromX = lastX;
        float fromY = Mth.wrapDegrees(lastY);
        float toX = (float) Mth.wrapDegrees(-(Mth.atan2(d1, d3) * 57.2957763671875));
        float toY = (float) Mth.wrapDegrees((Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F);

        var diffY = Mth.wrapDegrees(toY - fromY);
        var finalY = Mth.wrapDegrees(fromY + diffY * 0.2F);

        player.setXRot(Mth.wrapDegrees(Mth.lerp(0.2F, fromX, toX)));
        player.setYRot(Mth.wrapDegrees(finalY));
    }

    @SubscribeEvent
    public static void setPlayerInvisible(RenderPlayerEvent.Pre event) {
        var otherPlayer = event.getEntity();
        if (otherPlayer.getVehicle() instanceof VehicleEntity vehicle && vehicle.hidePassenger(otherPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void handleRenderCrossHair(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR)) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;

        if (player.isUsingItem() && player.getUseItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
            event.setCanceled(true);
        }

        ItemStack stack = player.getMainHandItem();
        final var tag = NBTTool.getTag(stack);

        if (stack.getItem() instanceof GunItem) {
            event.setCanceled(true);
        }

        if (player.getVehicle() instanceof VehicleEntity pVehicle && player.getVehicle() instanceof WeaponVehicleEntity iVehicle && iVehicle.hasWeapon(pVehicle.getSeatIndex(player))) {
            event.setCanceled(true);
        }

        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            event.setCanceled(true);
        }
    }

    /**
     * 载具banHand时，禁用快捷栏渲染
     */
    @SubscribeEvent
    public static void handleAvoidRenderingHotbar(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player)) {
            event.setCanceled(true);
        }
    }

    public static void resetGunStatus() {
        drawTime = 1;
        for (int i = 0; i < 5; i++) {
            shellIndexTime[i] = 0;
        }
        clientTimer.stop();
        zoom = false;
//        holdingFireKey = false;
        holdingFireKeyTicks = 0;
        holdingFireKeyTicks0 = 0;
        ClickHandler.switchZoom = false;
        lungeDraw = 30;
        lungeSprint = 0;
        lungeAttack = 0;
        burstFireAmount = 0;
        bowPullTimer = 0;
        bowPower = 0;
        noSprintTicks = 10;
        seekingTime = 0;
        lockOn = false;
        lockingEntity = null;
        seekingEntity = null;
        lockingPos = null;
        isEditing = false;
        zoomTime = 0;
    }

    private static void handleWeaponDraw(LivingEntity entity) {
        float times = getDelta();
        ItemStack stack = entity.getMainHandItem();
        var data = GunData.from(stack);
        double weight = data.get(GunProp.WEIGHT);
        double speed = 20 / (weight + 5);
        drawTime = Math.max(drawTime - Math.max(0.2 * speed * times * drawTime, 0.0008), 0);
    }

    public static void handleShells(float x, float y, GeoBone... shells) {
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
                List<Entity> entities = SeekTool.seekLivingEntities(villager, 16, 120);
                for (var e : entities) {
                    if (e == player) {
                        PacketDistributor.sendToServer(new AimVillagerMessage(villager.getId()));
                        aimVillagerCountdown = 80;
                        break;
                    }
                }
            }
        }
    }

    /**
     * 能否开启改枪GUI，只有在当前没有待发射的子弹，且物品为武器，主手持有的情况下才能开启
     *
     * @param stack 待改装武器
     * @param hand  持有武器的手
     * @return 能否成功打开GUI
     */
    public static boolean canOpenEditScreen(ItemStack stack, InteractionHand hand) {
        return burstFireAmount == 0 && stack.getItem() instanceof GunItem && hand == InteractionHand.MAIN_HAND;
    }

    public static void onOpenEditScreen() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        isEditing = true;
        holdingFireKey = false;
        player.playSound(ModSounds.EDIT_MODE.get(), 1, 1);
    }

    public static void onCloseEditScreen() {
        isEditing = false;
    }

    public static void editModelShake() {
        movePosY = -0.8;
        fireRotTimer = 0.4;
    }
}
