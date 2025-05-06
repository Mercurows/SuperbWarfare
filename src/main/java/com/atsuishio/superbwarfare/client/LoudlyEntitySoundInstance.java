package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.entity.LoudlyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public abstract class LoudlyEntitySoundInstance extends AbstractTickableSoundInstance {
    private final Minecraft client;
    private final Entity entity;
    private double lastDistance;
    private int fade = 0;
    private boolean die = false;

    public LoudlyEntitySoundInstance(SoundEvent sound, Minecraft client, Entity entity) {
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

        if (player.getVehicle() != this.entity) {
            double distance = this.entity.position().subtract(player.position()).length();
            this.pitch += (float) (0.16 * java.lang.Math.atan(lastDistance - distance));

            this.lastDistance = distance;
        } else {
            this.lastDistance = 0;
        }
    }
    
    public static class EntitySound extends LoudlyEntitySoundInstance {
        public EntitySound(Entity entity) {
            super(entity instanceof LoudlyEntity loudlyEntity ? loudlyEntity.getSound() : null, Minecraft.getInstance(), entity);
        }

        @Override
        protected boolean canPlay(Entity entity) {
            return true;
        }

        @Override
        protected float getPitch(Entity entity) {
            return 1;
        }

        @Override
        protected float getVolume(Entity entity) {
            if (entity instanceof LoudlyEntity loudlyEntity) {
                return loudlyEntity.getVolume();
            }
            return 0;
        }
    }

    public static class EntitySoundClose extends LoudlyEntitySoundInstance {
        public EntitySoundClose(Entity entity) {
            super(entity instanceof LoudlyEntity loudlyEntity ? loudlyEntity.getCloseSound() : null, Minecraft.getInstance(), entity);
        }

        @Override
        protected boolean canPlay(Entity entity) {
            return true;
        }

        @Override
        protected float getPitch(Entity entity) {
            return 1;
        }

        @Override
        protected float getVolume(Entity entity) {
            if (entity instanceof LoudlyEntity loudlyEntity) {
                return loudlyEntity.getVolume() * 1.5f;
            }
            return 0;
        }
    }
}
