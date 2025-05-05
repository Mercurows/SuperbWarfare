package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class PlayerSoundInstance extends AbstractTickableSoundInstance {
    private final LocalPlayer player;

    public PlayerSoundInstance(LocalPlayer pPlayer) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = pPlayer;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.1F;
    }

    public void tick() {
        if (!this.player.isRemoved() && (this.player.getVehicle() instanceof Tom6Entity tom6Entity)) {
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            float $$0 = (float)tom6Entity.getDeltaMovement().lengthSqr();
            if ((double)$$0 >= 1.0E-7) {
                this.volume = Mth.clamp($$0 / 4.0F, 0.0F, 1.0F);
            } else {
                this.volume = 0.0F;
            }
        } else {
            this.stop();
        }
    }
}
