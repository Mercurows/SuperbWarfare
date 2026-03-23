package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class HornSoundInstance extends AbstractTickableSoundInstance {

    private final Minecraft client;
    private final VehicleEntity entity;
    private double lastDistance;
    private int fade = 0;
    private boolean die = false;

    public HornSoundInstance(SoundEvent sound, Minecraft client, VehicleEntity entity) {
        super(sound, SoundSource.PLAYERS, entity.getCommandSenderWorld().getRandom());
        this.client = client;
        this.entity = entity;
        this.looping = true;
        this.delay = 0;
    }

    protected abstract boolean canPlay(VehicleEntity entity);

    protected abstract float getPitch(VehicleEntity entity);

    protected abstract float getVolume(VehicleEntity entity);

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

    @OnlyIn(Dist.CLIENT)
    public static class VehicleHornSound extends HornSoundInstance {

        public VehicleHornSound(VehicleEntity entity) {
            super(entity.getHornSound(), Minecraft.getInstance(), entity);
        }

        @Override
        protected boolean canPlay(VehicleEntity entity) {
            return entity.hornWorking();
        }

        @Override
        protected float getPitch(VehicleEntity entity) {
            return 1;
        }

        @Override
        protected float getVolume(VehicleEntity entity) {
            return entity.getHornVolume();
        }
    }
}
