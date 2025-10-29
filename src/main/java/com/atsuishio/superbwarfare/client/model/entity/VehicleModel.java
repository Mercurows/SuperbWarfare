package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.resource.vehicle.DefaultVehicleResource;
import com.atsuishio.superbwarfare.resource.vehicle.VehicleResource;
import com.atsuishio.superbwarfare.tools.ResourceOnceLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

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

        Player player = Minecraft.getInstance().player;
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

    @Override
    public void setCustomAnimations(T vehicle, long instanceId, AnimationState<T> animationState) {
        float partialTick = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();

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

        // turret.*
        var turret = getAnimationProcessor().getBone("turret");

        if (turret != null) {
            turret.setHidden(hideFor1stPassengerWhileZooming);
            turret.setRotY(turretYRot * Mth.DEG_TO_RAD);
        }

        // flare.*
        var flare = getAnimationProcessor().getBone("flare");

        if (flare != null) {
            flare.setRotZ((float) (0.5 * (Math.random() - 0.5)));
        }

        // barrel.*
        var barrel = getAnimationProcessor().getBone("barrel");

        if (barrel != null) {
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

            barrel.setRotX(-turretXRot * Mth.DEG_TO_RAD - r * pitch * Mth.DEG_TO_RAD - r2 * roll * Mth.DEG_TO_RAD);
        }

        // track(Mov|Rot)\d+
        this.getAnimationProcessor().getRegisteredBones().forEach(bone -> {
            var name = bone.getName();
            if (hasTrack() && name.length() > 9 && name.startsWith("track")) {
                var isL = name.charAt(8) == 'L';

                if (name.startsWith("trackRot")) {
                    int i = Integer.parseInt(name.substring(9));
                    if (isL) {
                        float t = wrap(vehicle.getLeftTrack() + 2 * i);
                        float tO = wrap(vehicle.leftTrackO + 2 * i);
                        bone.setRotX(-Mth.lerp(partialTick, getBoneRotX(tO), getBoneRotX(t)) * Mth.DEG_TO_RAD);
                    } else {
                        float tO2 = wrap(vehicle.rightTrackO + 2 * i);
                        float t2 = wrap(vehicle.getRightTrack() + 2 * i);
                        bone.setRotX(-Mth.lerp(partialTick, getBoneRotX(tO2), getBoneRotX(t2)) * Mth.DEG_TO_RAD);
                    }
                } else if (name.startsWith("trackMov")) {
                    int i = Integer.parseInt(name.substring(9));
                    if (isL) {
                        float tO = wrap(vehicle.leftTrackO + 2 * i);
                        float t = wrap(vehicle.getLeftTrack() + 2 * i);
                        bone.setPosY(Mth.lerp(partialTick, getBoneMoveY(tO), getBoneMoveY(t)));
                        bone.setPosZ(Mth.lerp(partialTick, getBoneMoveZ(tO), getBoneMoveZ(t)));
                    } else {
                        float tO2 = wrap(vehicle.rightTrackO + 2 * i);
                        float t2 = wrap(vehicle.getRightTrack() + 2 * i);
                        bone.setPosY(Mth.lerp(partialTick, getBoneMoveY(tO2), getBoneMoveY(t2)));
                        bone.setPosZ(Mth.lerp(partialTick, getBoneMoveZ(tO2), getBoneMoveZ(t2)));
                    }
                }
                if (vehicle.getFirstPassenger() instanceof Player player) {
                    player.displayClientMessage(Component.literal(vehicle.getLeftTrack() + " " + vehicle.getRightTrack()), true);
                }
            }

            // wheel[LR].*
            if (hasTrackWheel() && name.length() >= 6 && name.startsWith("wheel")) {
                char LR = name.charAt(5);
                if (LR == 'L') {
                    bone.setRotX(1.5f * leftWheelRot);
                } else if (LR == 'R') {
                    bone.setRotX(1.5f * rightWheelRot);
                }
            }
        });


    }

    public boolean hasTrack() {
        return false;
    }

    public boolean hasTrackWheel() {
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
