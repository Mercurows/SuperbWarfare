package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public abstract class VehicleFireSoundInstance extends AbstractTickableSoundInstance {

    private final Minecraft client;
    private final Entity entity;
    private double lastDistance;
    private int fade = 0;
    private boolean die = false;

    public VehicleFireSoundInstance(SoundEvent sound, Minecraft client, Entity entity) {
        super(sound, SoundSource.AMBIENT, entity.getCommandSenderWorld().getRandom());
        this.client = client;
        this.entity = entity;
        this.looping = true;
        this.delay = 0;
    }

    protected abstract boolean canPlay(Entity entity);

    protected abstract float getPitch(Entity entity);

    protected abstract float getVolume(Entity entity);

    @Override
    public void tick() {
        var player = this.client.player;
        if (entity.isRemoved() || player == null) {
            this.stop();
            return;
        } else if (!this.canPlay(entity)) {
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

        this.volume = this.getVolume(this.entity) * fade;

        this.x = this.entity.getX();
        this.y = this.entity.getY();
        this.z = this.entity.getZ();

        this.pitch = this.getPitch(this.entity);
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        if (player.getVehicle() != this.entity) {
            double distance = this.entity.position().subtract(cameraPos).length();
            this.pitch += (float) (0.1 * Math.atan(lastDistance - distance));

            this.lastDistance = distance;
        } else {
            this.lastDistance = 0;
        }
    }

    public static class VehicleFireSound extends VehicleSoundInstance {
        private final int layer;

        public VehicleFireSound(VehicleEntity vehicle, int layer) {
            super(vehicle.getShootSoundInstance(layer), Minecraft.getInstance(), vehicle);
            this.layer = layer;
        }

        public static void play(VehicleEntity vehicle) {
            for (int layer = VehicleEntity.SHOOT_SOUND_INTERNAL;
                 layer <= VehicleEntity.SHOOT_SOUND_VERY_FAR;
                 layer++) {
                if (vehicle.hasShootSoundInstance(layer)) {
                    Minecraft.getInstance().getSoundManager().play(new VehicleFireSound(vehicle, layer));
                }
            }
        }

        @Override
        protected boolean canPlay(VehicleEntity vehicle) {
            return vehicle.isFiring();
        }

        @Override
        protected float getPitch(VehicleEntity vehicle) {
            return vehicle.shootingPitch();
        }

        @Override
        protected float getVolume(VehicleEntity vehicle) {
            var player = Minecraft.getInstance().player;
            boolean inside = player != null
                    && player.getVehicle() == vehicle
                    && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON
                    || ClientEventHandler.zoomVehicle);
            boolean active = layer == VehicleEntity.SHOOT_SOUND_INTERNAL ? inside : !inside;

            return active ? vehicle.shootingVolume() : 0;
        }
    }
}
