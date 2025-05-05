package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

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
        this.volume = this.getVolume(this.mobileVehicle) * (float)fade / 3;

        this.x = this.mobileVehicle.getX();
        this.y = this.mobileVehicle.getY();
        this.z = this.mobileVehicle.getZ();

        this.pitch = this.getPitch(this.mobileVehicle);

        if (player.getVehicle() != this.mobileVehicle) {
            double distance = this.mobileVehicle.position().subtract(player.position()).length();
            this.pitch += (float) (0.36 * Math.atan(lastDistance - distance));

            this.lastDistance = distance;
        } else {
            this.lastDistance = 0;
        }
    }

    public static class EngineSound extends VehicleSoundInstance {
        public EngineSound(Minecraft client, MobileVehicleEntity mobileVehicle) {
            super(mobileVehicle.getEngineSound(), client, mobileVehicle);
        }

        @Override
        protected boolean canPlay(MobileVehicleEntity mobileVehicle) {
            return true;
        }

        @Override
        protected float getPitch(MobileVehicleEntity mobileVehicle) {
            return 1;
        }

        @Override
        protected float getVolume(MobileVehicleEntity mobileVehicle) {
            return 1;
        }
    }
}
