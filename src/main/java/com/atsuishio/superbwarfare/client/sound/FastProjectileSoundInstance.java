package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class FastProjectileSoundInstance extends AbstractTickableSoundInstance {

    private final Minecraft client;
    private final FastThrowableProjectile entity;
    private double lastDistance;
    private int fade = 0;
    private boolean die = false;

    public FastProjectileSoundInstance(SoundEvent sound, Minecraft client, FastThrowableProjectile entity) {
        super(sound, SoundSource.AMBIENT, entity.getCommandSenderWorld().getRandom());
        this.client = client;
        this.entity = entity;
        this.looping = true;
        this.delay = 0;
    }

    protected abstract boolean canPlay(FastThrowableProjectile entity);

    protected abstract float getPitch(FastThrowableProjectile entity);

    protected abstract float getVolume(FastThrowableProjectile entity);

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

        if (player.getVehicle() != this.entity) {
            double distance = this.entity.position().subtract(player.position()).length();
            this.pitch += (float) (0.16 * java.lang.Math.atan(lastDistance - distance));

            this.lastDistance = distance;
        } else {
            this.lastDistance = 0;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class FlySound extends FastProjectileSoundInstance {

        public FlySound(FastThrowableProjectile entity) {
            super(entity.getSound(), Minecraft.getInstance(), entity);
        }

        @Override
        protected boolean canPlay(FastThrowableProjectile entity) {
            return entity.isFastMoving();
        }

        @Override
        protected float getPitch(FastThrowableProjectile entity) {
            return 1;
        }

        @Override
        protected float getVolume(FastThrowableProjectile entity) {
            return (float) Math.min(entity.getVolume() * 0.1 * entity.getDeltaMovement().length(), 1.5);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class NearFlySound extends FastProjectileSoundInstance {

        public NearFlySound(FastThrowableProjectile entity) {
            super(entity.getCloseSound(), Minecraft.getInstance(), entity);
        }

        @Override
        protected boolean canPlay(FastThrowableProjectile entity) {
            return entity.isFastMoving();
        }

        @Override
        protected float getPitch(FastThrowableProjectile entity) {
            return 1;
        }

        @Override
        protected float getVolume(FastThrowableProjectile entity) {
            return (float) Math.min(entity.getVolume() * 0.1 * entity.getDeltaMovement().length(), 1.5);
        }
    }
}
