package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public abstract class VehicleSoundInstance extends AbstractTickableSoundInstance {

    private final Minecraft client;
    private final VehicleEntity mobileVehicle;
    private double lastDistance;
    private int fade = 0;
    private boolean die = false;

    public VehicleSoundInstance(SoundEvent sound, Minecraft client, VehicleEntity mobileVehicle) {
        super(sound, SoundSource.AMBIENT, mobileVehicle.getCommandSenderWorld().getRandom());
        this.client = client;
        this.mobileVehicle = mobileVehicle;
        this.looping = true;
        this.delay = 0;
    }

    protected abstract boolean canPlay(VehicleEntity mobileVehicle);

    protected abstract float getPitch(VehicleEntity mobileVehicle);

    protected abstract float getVolume(VehicleEntity mobileVehicle);

    @Override
    public void tick() {
        var player = this.client.player;
        if (mobileVehicle.isRemoved() || player == null) {
            this.stop();
            return;
        } else if (!this.canPlay(mobileVehicle)) {
            this.die = true;
        }

        if (this.die) {
            if (this.fade > 0) this.fade--;
            else if (this.fade == 0) {
                this.stop();
                return;
            }
        } else if (this.fade < 3) {
            this.fade++;
        }

        this.volume = this.getVolume(this.mobileVehicle) * fade;

        this.x = this.mobileVehicle.getX();
        this.y = this.mobileVehicle.getY();
        this.z = this.mobileVehicle.getZ();

        this.pitch = this.getPitch(this.mobileVehicle);
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        if (player.getVehicle() != this.mobileVehicle) {
            double distance = this.mobileVehicle.position().subtract(cameraPos).length();
            this.pitch += (float) (0.16 * java.lang.Math.atan(lastDistance - distance));

            this.lastDistance = distance;
        } else {
            this.lastDistance = 0;
        }

        if (this.mobileVehicle instanceof DroneEntity drone && Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(drone.position()) < 0.0625) {
            pitch = 1;
        }
    }

    public static class EngineSound extends VehicleSoundInstance {

        public EngineSound(VehicleEntity mobileVehicle) {
            super(mobileVehicle.getEngineSound(), Minecraft.getInstance(), mobileVehicle);
        }

        @Override
        protected boolean canPlay(VehicleEntity mobileVehicle) {
            return mobileVehicle.engineRunning();
        }

        @Override
        protected float getPitch(VehicleEntity mobileVehicle) {
            if (mobileVehicle.getVehicleType() == VehicleType.HELICOPTER) return 1;
            float power = mobileVehicle.getPower();
            float pitch;
            if (power < 0.5) {
                pitch = 0.6f + power * 0.4f;
            } else if (power <= 1) {
                pitch = 0.8f + ((power - 0.5f) * 0.4f);
            } else {
                pitch = Math.min(power, 1.5f);
            }
            return pitch;
        }

        @Override
        protected float getVolume(VehicleEntity mobileVehicle) {
            return mobileVehicle.getEngineSoundVolume();
        }
    }

    public static class TrackSound extends VehicleSoundInstance {

        public TrackSound(VehicleEntity mobileVehicle) {
            super(ModSounds.TRACK_VEHICLE_STEP.get(), Minecraft.getInstance(), mobileVehicle);
        }

        @Override
        protected boolean canPlay(VehicleEntity mobileVehicle) {
            return mobileVehicle.engineRunning();
        }

        @Override
        protected float getPitch(VehicleEntity mobileVehicle) {
            return 1;
        }

        @Override
        protected float getVolume(VehicleEntity mobileVehicle) {
            return (float) Mth.lerp(Mth.clamp(mobileVehicle.getDeltaMovement().horizontalDistance(), 0F, 0.3F), 0F, 0.3F) * 1.4f;
        }
    }

    public static class SwimSound extends VehicleSoundInstance {

        public SwimSound(VehicleEntity mobileVehicle) {
            super(ModSounds.VEHICLE_SWIM.get(), Minecraft.getInstance(), mobileVehicle);
        }

        @Override
        protected boolean canPlay(VehicleEntity mobileVehicle) {
            return mobileVehicle.engineRunning() && mobileVehicle.isInWater();
        }

        @Override
        protected float getPitch(VehicleEntity mobileVehicle) {
            return 1;
        }

        @Override
        protected float getVolume(VehicleEntity mobileVehicle) {
            return (float) Mth.lerp(Mth.clamp(mobileVehicle.getDeltaMovement().horizontalDistance() * (mobileVehicle.isInWater() ? 1.2 : 0), 0F, 0.6F), 0, 0.6F);
        }
    }

    public static class StukaSound extends VehicleSoundInstance {

        public StukaSound(VehicleEntity mobileVehicle) {
            super(ModSounds.STUKA.get(), Minecraft.getInstance(), mobileVehicle);
        }

        @Override
        protected boolean canPlay(VehicleEntity mobileVehicle) {
            return mobileVehicle.engineRunning() && mobileVehicle.stuka();
        }

        @Override
        protected float getPitch(VehicleEntity mobileVehicle) {
            return 1;
        }

        @Override
        protected float getVolume(VehicleEntity mobileVehicle) {
            float angle = Math.max((float) VectorTool.calculateAngle(mobileVehicle.getLookAngle(), new Vec3(0, 1, 0)) - 95, 0) / 85;

            return (float) Mth.clamp((-mobileVehicle.getDeltaMovement().y - 0.4) * angle * 0.2, 0F, 5F);
        }
    }

    public static class HeliCrashSound extends VehicleSoundInstance {

        public HeliCrashSound(VehicleEntity mobileVehicle) {
            super(ModSounds.HELI_CRASH.get(), Minecraft.getInstance(), mobileVehicle);
        }

        @Override
        protected boolean canPlay(VehicleEntity mobileVehicle) {
            return mobileVehicle.engineRunning() && mobileVehicle.heliCrash();
        }

        @Override
        protected float getPitch(VehicleEntity mobileVehicle) {
            return 0.85f + (float) Mth.clamp((mobileVehicle.getDeltaMovement().y + 0.3) * -0.5f, 0F, 0.5F);
        }

        @Override
        protected float getVolume(VehicleEntity mobileVehicle) {
            return (float) Mth.clamp((mobileVehicle.getDeltaMovement().y + 0.3) * -0.25f, 0F, 5F);
        }
    }

    public static class SkipSound extends VehicleSoundInstance {

        public SkipSound(VehicleEntity mobileVehicle) {
            super(mobileVehicle.getEngineInfo() instanceof EngineInfo.Track ? ModSounds.TRACK_VEHICLE_SKIP.get() : ModSounds.WHEEL_VEHICLE_SKIP.get(), Minecraft.getInstance(), mobileVehicle);
        }

        @Override
        protected boolean canPlay(VehicleEntity mobileVehicle) {
            return mobileVehicle.engineRunning() && mobileVehicle.vehicleSkip();
        }

        @Override
        protected float getPitch(VehicleEntity mobileVehicle) {
            return 1;
        }

        @Override
        protected float getVolume(VehicleEntity mobileVehicle) {
            return (float) Mth.clamp(mobileVehicle.getDeltaMovement().length(), 0F, 1F);
        }
    }
}
