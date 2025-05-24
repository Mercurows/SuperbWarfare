package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public abstract class VehicleSoundInstance extends AbstractTickableSoundInstance {

    private final Minecraft client;
    private final MobileVehicleEntity mobileVehicle;
    private double lastDistance;
    private int fade = 0;
    private boolean die = false;

    public VehicleSoundInstance(SoundEvent sound, Minecraft client, MobileVehicleEntity mobileVehicle) {
        super(sound, SoundSource.AMBIENT, mobileVehicle.getCommandSenderWorld().getRandom());
        this.client = client;
        this.mobileVehicle = mobileVehicle;
        this.looping = true;
        this.delay = 0;
    }

    protected abstract boolean canPlay(MobileVehicleEntity mobileVehicle);

    protected abstract float getPitch(MobileVehicleEntity mobileVehicle);

    protected abstract float getVolume(MobileVehicleEntity mobileVehicle);

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

        if (player.getVehicle() != this.mobileVehicle) {
            double distance = this.mobileVehicle.position().subtract(player.position()).length();
            this.pitch += (float) (0.16 * java.lang.Math.atan(lastDistance - distance));

            this.lastDistance = distance;
        } else {
            this.lastDistance = 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.is(ModItems.MONITOR.get()) && NBTTool.getTag(stack).getBoolean("Using")) {
            DroneEntity drone = EntityFindUtil.findDrone(player.level(), NBTTool.getTag(stack).getString("LinkedDrone"));
            if (this.mobileVehicle == drone) {
                pitch = 1;
            }
        }
    }

    public static class EngineSound extends VehicleSoundInstance {

        public EngineSound(MobileVehicleEntity mobileVehicle) {
            super(mobileVehicle.getEngineSound(), Minecraft.getInstance(), mobileVehicle);
        }

        @Override
        protected boolean canPlay(MobileVehicleEntity mobileVehicle) {
            return mobileVehicle.engineRunning();
        }

        @Override
        protected float getPitch(MobileVehicleEntity mobileVehicle) {
            return 1;
        }

        @Override
        protected float getVolume(MobileVehicleEntity mobileVehicle) {
            return mobileVehicle.getEngineSoundVolume();
        }
    }

    public static class TrackSound extends VehicleSoundInstance {

        public TrackSound(MobileVehicleEntity mobileVehicle) {
            super(ModSounds.TRACK_MOVE.get(), Minecraft.getInstance(), mobileVehicle);
        }

        @Override
        protected boolean canPlay(MobileVehicleEntity mobileVehicle) {
            return mobileVehicle.engineRunning() && mobileVehicle.onGround();
        }

        @Override
        protected float getPitch(MobileVehicleEntity mobileVehicle) {
            return 1;
        }

        @Override
        protected float getVolume(MobileVehicleEntity mobileVehicle) {
            return (float) Mth.lerp(Mth.clamp(mobileVehicle.getDeltaMovement().length(), 0F, 0.3F), 0.0F, 0.3F) * (mobileVehicle.onGround() ? 1 : 0.5f);
        }
    }

    public static class SwimSound extends VehicleSoundInstance {

        public SwimSound(MobileVehicleEntity mobileVehicle) {
            super(ModSounds.VEHICLE_SWIM.get(), Minecraft.getInstance(), mobileVehicle);
        }

        @Override
        protected boolean canPlay(MobileVehicleEntity mobileVehicle) {
            return mobileVehicle.engineRunning() && mobileVehicle.isInWater();
        }

        @Override
        protected float getPitch(MobileVehicleEntity mobileVehicle) {
            return 1;
        }

        @Override
        protected float getVolume(MobileVehicleEntity mobileVehicle) {
            return (float) Mth.lerp(Mth.clamp(mobileVehicle.getDeltaMovement().horizontalDistance() * (mobileVehicle.isInWater() ? 1.2 : 0), 0F, 0.6F), 0.0F, 0.6F);
        }
    }
}
