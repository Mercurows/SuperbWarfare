package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
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

        private final String weaponName;

        public VehicleFireSound(VehicleEntity vehicle, String weaponName) {
            super(vehicle.getShootSoundInstance(weaponName), Minecraft.getInstance(), vehicle);
            this.weaponName = weaponName;
        }

        @Override
        protected boolean canPlay(VehicleEntity vehicle) {
            return vehicle.isWeaponFiring(weaponName);
        }

        @Override
        protected float getPitch(VehicleEntity vehicle) {
            return vehicle.weaponShootingPitch(weaponName);
        }

        @Override
        protected float getVolume(VehicleEntity vehicle) {
            return vehicle.weaponShootingVolume(weaponName);
        }
    }
}
