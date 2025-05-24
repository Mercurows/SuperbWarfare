package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

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
    }

    public static class A10FireSound extends VehicleSoundInstance {

        public A10FireSound(MobileVehicleEntity mobileVehicle) {
            super(ModSounds.A_10_FIRE.get(), Minecraft.getInstance(), mobileVehicle);
        }

        @Override
        protected boolean canPlay(MobileVehicleEntity mobileVehicle) {
            if (mobileVehicle instanceof A10Entity a10Entity) {
                return a10Entity.isFiring();
            }
            return false;
        }

        @Override
        protected float getPitch(MobileVehicleEntity mobileVehicle) {
            if (mobileVehicle instanceof A10Entity a10Entity) {
                return a10Entity.shootingPitch();
            }
            return 1;
        }

        @Override
        protected float getVolume(MobileVehicleEntity mobileVehicle) {
            if (mobileVehicle instanceof A10Entity a10Entity) {
                return a10Entity.shootingVolume();
            }
            return 0;
        }
    }

    public static class HPJ11CloseFireSound extends VehicleSoundInstance {

        public HPJ11CloseFireSound(MobileVehicleEntity mobileVehicle) {
            super(ModSounds.HPJ_11_FIRE_3P.get(), Minecraft.getInstance(), mobileVehicle);
        }

        @Override
        protected boolean canPlay(MobileVehicleEntity mobileVehicle) {
            if (mobileVehicle instanceof Hpj11Entity hpj11Entity) {
                return hpj11Entity.isFiring();
            }
            return false;
        }

        @Override
        protected float getPitch(MobileVehicleEntity mobileVehicle) {
            if (mobileVehicle instanceof Hpj11Entity hpj11Entity) {
                return hpj11Entity.shootingPitch();
            }
            return 1;
        }

        @Override
        protected float getVolume(MobileVehicleEntity mobileVehicle) {
            if (mobileVehicle instanceof Hpj11Entity hpj11Entity) {
                return hpj11Entity.shootingVolume();
            }
            return 0;
        }
    }
}
