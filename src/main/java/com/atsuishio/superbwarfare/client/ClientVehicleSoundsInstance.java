package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientVehicleSoundsInstance extends AbstractTickableSoundInstance {
    private final Tom6Entity tom6Entity;

    public ClientVehicleSoundsInstance(Tom6Entity pTom6Entity) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.tom6Entity = pTom6Entity;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.x = pTom6Entity.getX();
        this.y = pTom6Entity.getY();
        this.z = pTom6Entity.getZ();
    }

    public void tick() {
        if (this.tom6Entity.isRemoved()) {
            this.stop();
        } else {
            this.x = tom6Entity.getX();
            this.y = tom6Entity.getY();
            this.z = tom6Entity.getZ();
            float $$0 = (float) tom6Entity.getDeltaMovement().horizontalDistance();
            if ($$0 >= 0.01F) {
                this.pitch = Mth.clamp(this.pitch + 0.0025F, 0.0F, 1.0F);
                this.volume = Mth.lerp(Mth.clamp($$0, 0.0F, 0.5F), 0.0F, 0.7F);
            } else {
                this.pitch = 0.0F;
                this.volume = 0.0F;
            }

        }
    }
}

