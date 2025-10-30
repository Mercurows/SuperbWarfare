package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.resource.vehicle.DefaultVehicleResource;
import com.atsuishio.superbwarfare.resource.vehicle.VehicleResource;
import com.atsuishio.superbwarfare.tools.ResourceOnceLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import oshi.util.tuples.Pair;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.atsuishio.superbwarfare.entity.vehicle.PrismTankEntity.CANNON_RECOIL_FORCE;
import static com.atsuishio.superbwarfare.entity.vehicle.PrismTankEntity.CANNON_RECOIL_ROTATE;
import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.YAW_WHILE_SHOOT;

public class VehicleModel<T extends VehicleEntity & GeoAnimatable> extends GeoModel<T> {

    protected float pitch;
    protected float yaw;
    protected float roll;
    protected float leftWheelRot;
    protected float rightWheelRot;
    protected float turretYRot;
    protected float turretXRot;
    protected float turretYaw;
    protected float recoilShake;
    protected boolean hideFor1stPassengerWhileZooming;

    private final ResourceOnceLogger LOGGER = new ResourceOnceLogger();

    @Override
    public ResourceLocation getAnimationResource(T vehicle) {
        return getDefault(vehicle).getModel().animation;
    }

    @Override
    public ResourceLocation getModelResource(T vehicle) {
        int lodLevel = getLODLevel(vehicle);
        var lodModel = getDefault(vehicle).getModel().getLODModel(lodLevel);
        if (lodModel == null) {
            LOGGER.log(vehicle, logger -> logger.error("failed to load model for {}!", vehicle));
            return Mod.loc("geo/" + VehicleResource.getRegistryId(vehicle.getType()) + ".geo.json");
        }
        return lodModel;
    }

    @Override
    public ResourceLocation getTextureResource(T vehicle) {
        int lodLevel = getLODLevel(vehicle);
        var lodTexture = getDefault(vehicle).getModel().getLODTexture(lodLevel);
        if (lodTexture == null) {
            LOGGER.log(vehicle, logger -> logger.error("failed to load texture for {}!", vehicle));
            return Mod.loc("textures/entity/" + VehicleResource.getRegistryId(vehicle.getType()) + ".png");
        }
        return lodTexture;
    }

    public int getLODLevel(T vehicle) {
        var defaultData = getDefault(vehicle);
        var model = defaultData.getModel();
        if (defaultData.lodDistance == null || defaultData.lodDistance.list.isEmpty() || !model.hasLOD()) return 0;

        var player = Minecraft.getInstance().player;
        if (player == null || player.isScoping()) return 0;

        var distance = player.position().distanceTo(vehicle.position());
        for (int i = 0; i < defaultData.lodDistance.list.size(); i++) {
            if (distance <= defaultData.lodDistance.list.get(i)) {
                return i;
            }
        }

        return Integer.MAX_VALUE;
    }

    private static <T extends VehicleEntity & GeoAnimatable> DefaultVehicleResource getDefault(T vehicle) {
        return VehicleResource.getDefault(vehicle);
    }

    @FunctionalInterface
    public interface TransformContext<T extends VehicleEntity & GeoAnimatable> {
        void transform(CoreGeoBone bone, T vehicle, AnimationState<T> animationState);
    }

    public static final Pattern TRACK_PATTERN = Pattern.compile("^track(?<type>Mov|Rot)(?<direction>[LR])(?<id>\\d+)$");
    public static final Pattern WHEEL_PATTERN = Pattern.compile("^wheel(?<direction>[LR]).*$");

    protected boolean init = false;
    // TODO 在重载资源包时清空缓存
    protected final List<Pair<String, TransformContext<T>>> TRANSFORMS = new ArrayList<>();

    @Nullable
    public TransformContext<T> collectTransform(String boneName) {
        // 瞄准时隐藏车体
        if (boneName.equals("root") && hideFor1stPassengerWhileZooming()) {
            return (bone, vehicle, state) -> bone.setHidden(hideFor1stPassengerWhileZooming);
        }

        //射击时带来的车体摇晃视觉效果
        switch (boneName) {
            case "base" -> {
                return (bone, vehicle, state) -> {
                    float a = vehicle.getEntityData().get(YAW_WHILE_SHOOT);
                    float r = (Mth.abs(a) - 90f) / 90f;

                    float r2;

                    if (Mth.abs(a) <= 90f) {
                        r2 = a / 90f;
                    } else {
                        if (a < 0) {
                            r2 = -(180f + a) / 90f;
                        } else {
                            r2 = (180f - a) / 90f;
                        }
                    }

                    bone.setPosX(r2 * recoilShake * 0.5f * vehicle.getEntityData().get(CANNON_RECOIL_FORCE));
                    bone.setPosZ(r * recoilShake * 1f * vehicle.getEntityData().get(CANNON_RECOIL_FORCE));
                    bone.setRotX(r * recoilShake * Mth.DEG_TO_RAD * 0.5f * vehicle.getEntityData().get(CANNON_RECOIL_ROTATE));
                    bone.setRotZ(r2 * recoilShake * Mth.DEG_TO_RAD * 1f * vehicle.getEntityData().get(CANNON_RECOIL_ROTATE));
                };
            }

            // turret
            case "turret" -> {
                return (bone, vehicle, state) -> {
                    bone.setHidden(hideFor1stPassengerWhileZooming);
                    bone.setRotY(turretYRot * Mth.DEG_TO_RAD);

                    var turretLaser = getAnimationProcessor().getBone("turretLaser");

                    if (turretLaser != null) {
                        turretLaser.setRotY(bone.getRotY());
                    }
                };
            }

            // barrel
            case "barrel" -> {
                return (bone, vehicle, state) -> {
                    float a = turretYaw;
                    float r = (Mth.abs(a) - 90f) / 90f;

                    float r2;

                    if (Mth.abs(a) <= 90f) {
                        r2 = a / 90f;
                    } else {
                        if (a < 0) {
                            r2 = -(180f + a) / 90f;
                        } else {
                            r2 = (180f - a) / 90f;
                        }
                    }

                    bone.setRotX(-turretXRot * Mth.DEG_TO_RAD - r * pitch * Mth.DEG_TO_RAD - r2 * roll * Mth.DEG_TO_RAD);

                    var barrelLaser = getAnimationProcessor().getBone("barrelLaser");
                    if (barrelLaser != null) {
                        barrelLaser.setRotX(bone.getRotX());
                    }
                };
            }

            // turret上的成员武器站Yaw
            case "passengerWeaponStationYaw" -> {
                return (bone, vehicle, state) -> bone.setRotY(Mth.lerp(state.getPartialTick(), vehicle.gunYRotO, vehicle.getGunYRot()) * Mth.DEG_TO_RAD - turretYRot * Mth.DEG_TO_RAD);
            }

            // turret上的成员武器站Pitch
            case "passengerWeaponStationPitch" -> {
                return (bone, vehicle, state) -> {
                    float a = vehicle.getTurretYaw(state.getPartialTick());
                    float r = (Mth.abs(a) - 90f) / 90f;

                    float r2;

                    if (Mth.abs(a) <= 90f) {
                        r2 = a / 90f;
                    } else {
                        if (a < 0) {
                            r2 = -(180f + a) / 90f;
                        } else {
                            r2 = (180f - a) / 90f;
                        }
                    }

                    bone.setRotX(Mth.clamp(
                            -Mth.lerp(state.getPartialTick(), vehicle.gunXRotO, vehicle.getGunXRot()) * Mth.DEG_TO_RAD
                                    - r * pitch * Mth.DEG_TO_RAD
                                    - r2 * roll * Mth.DEG_TO_RAD,
                            -10 * Mth.DEG_TO_RAD, 60 * Mth.DEG_TO_RAD)
                    );
                };
            }

            // flare
            case "flare" -> {
                return (bone, vehicle, state) -> bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
            }
        }

        // track(Mov|Rot)[RL]\d+
        var trackMatcher = TRACK_PATTERN.matcher(boneName);
        if (trackMatcher.matches()) {
            var isRot = trackMatcher.group("type").equals("Rot");
            var isL = trackMatcher.group("direction").equals("L");
            var index = Integer.parseInt(trackMatcher.group("id"));

            if (isRot) {
                if (isL) {
                    return (bone, vehicle, state) -> {
                        float t = wrap(vehicle.getLeftTrack() + 2 * index);
                        float tO = wrap(vehicle.leftTrackO + 2 * index);
                        bone.setRotX(-Mth.lerp(state.getPartialTick(), getBoneRotX(tO), getBoneRotX(t)) * Mth.DEG_TO_RAD);
                    };
                } else {
                    return (bone, vehicle, state) -> {
                        float tO2 = wrap(vehicle.rightTrackO + 2 * index);
                        float t2 = wrap(vehicle.getRightTrack() + 2 * index);
                        bone.setRotX(-Mth.lerp(state.getPartialTick(), getBoneRotX(tO2), getBoneRotX(t2)) * Mth.DEG_TO_RAD);
                    };
                }
            } else {
                if (isL) {
                    return (bone, vehicle, state) -> {
                        float tO = wrap(vehicle.leftTrackO + 2 * index);
                        float t = wrap(vehicle.getLeftTrack() + 2 * index);
                        bone.setPosY(Mth.lerp(state.getPartialTick(), getBoneMoveY(tO), getBoneMoveY(t)));
                        bone.setPosZ(Mth.lerp(state.getPartialTick(), getBoneMoveZ(tO), getBoneMoveZ(t)));
                    };
                } else {
                    return (bone, vehicle, state) -> {
                        float tO2 = wrap(vehicle.rightTrackO + 2 * index);
                        float t2 = wrap(vehicle.getRightTrack() + 2 * index);
                        bone.setPosY(Mth.lerp(state.getPartialTick(), getBoneMoveY(tO2), getBoneMoveY(t2)));
                        bone.setPosZ(Mth.lerp(state.getPartialTick(), getBoneMoveZ(tO2), getBoneMoveZ(t2)));
                    };
                }
            }
        }

        var wheelMatcher = WHEEL_PATTERN.matcher(boneName);
        if (wheelMatcher.matches()) {
            var isL = wheelMatcher.group("direction").equals("L");

            if (boneName.endsWith("Turn")) {
                return (bone, vehicle, state) -> {
                    bone.setRotX(1.5f * (isL ? leftWheelRot : rightWheelRot));
                    bone.setRotY(Mth.lerp(state.getPartialTick(), vehicle.rudderRotO, vehicle.getRudderRot()));
                };
            } else {
                return (bone, vehicle, state) -> bone.setRotX(1.5f * (isL ? leftWheelRot : rightWheelRot));
            }
        }

        return null;
    }

    @Override
    public void setCustomAnimations(T vehicle, long instanceId, AnimationState<T> animationState) {
        if (!init) {
            getAnimationProcessor().getRegisteredBones().forEach(bone -> {
                var name = bone.getName();
                try {
                    var transform = collectTransform(name);
                    if (transform != null) {
                        TRANSFORMS.add(new Pair<>(name, transform));
                    }
                } catch (Exception exception) {
                    Mod.LOGGER.error("failed to collect transform for vehicle {} bone {}:", vehicle, name, exception);
                }
            });
            init = true;
        }

        float partialTick = animationState.getPartialTick();

        pitch = vehicle.getPitch(partialTick);
        yaw = vehicle.getYaw(partialTick);
        roll = vehicle.getRoll(partialTick);

        leftWheelRot = Mth.lerp(partialTick, vehicle.leftWheelRotO, vehicle.getLeftWheelRot());
        rightWheelRot = Mth.lerp(partialTick, vehicle.rightWheelRotO, vehicle.getRightWheelRot());

        turretYRot = Mth.lerp(partialTick, vehicle.turretYRotO, vehicle.getTurretYRot());
        turretXRot = Mth.lerp(partialTick, vehicle.turretXRotO, vehicle.getTurretXRot());

        turretYaw = vehicle.getTurretYaw(partialTick);

        recoilShake = Mth.lerp(partialTick, (float) vehicle.recoilShakeO, (float) vehicle.getRecoilShake());

        hideFor1stPassengerWhileZooming = ClientEventHandler.zoomVehicle && vehicle.getFirstPassenger() == Minecraft.getInstance().player;


        TRANSFORMS.forEach(pair -> {
            var name = pair.getA();
            var bone = getAnimationProcessor().getBone(name);

            // TODO 这里怎么可能为空？
            if (bone != null) {
                pair.getB().transform(bone, vehicle, animationState);
            }
        });

    }

    public boolean hideFor1stPassengerWhileZooming() {
        return false;
    }

    public float getBoneRotX(float t) {
        return t;
    }

    public float getBoneMoveY(float t) {
        return t;
    }

    public float getBoneMoveZ(float t) {
        return t;
    }

    protected float wrap(float value, int range) {
        return ((value % range) + range) % range;
    }

    protected float wrap(float value) {
        return wrap(value, getDefaultWrapRange());
    }

    public int getDefaultWrapRange() {
        return 100;
    }
}
