package com.atsuishio.superbwarfare.capability.laser;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;


public class LaserCapability {


    public LaserHandler laserHandler;

    public void init(LaserHandler handler) {
        this.laserHandler = handler;
    }

    public void start() {
        this.laserHandler.start();
    }

    public void tick() {
    }

    public void stop() {
        if (this.laserHandler != null) {
            this.laserHandler.stop();
        }
    }

    public void end() {
        if (this.laserHandler != null) {
            this.laserHandler.end();
        }
    }

    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (this.laserHandler != null) {
            tag.put("Laser", this.laserHandler.writeNBT());
        }
        return tag;
    }

    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag compoundTag) {
        if (compoundTag.contains("Laser") && this.laserHandler != null) {
            this.laserHandler.readNBT(compoundTag.getCompound("Laser"));
        }
    }

}
