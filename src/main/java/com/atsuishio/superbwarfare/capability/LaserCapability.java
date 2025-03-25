package com.atsuishio.superbwarfare.capability;

import com.atsuishio.superbwarfare.ModUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class LaserCapability {

    public static ResourceLocation ID = ModUtils.loc("laser_capability");

    public interface ILaserCapability extends INBTSerializable<CompoundTag> {

        void init(LaserHandler handler);

        void start();

        void tick();

        void stop();

        void end();

    }

    public static class LaserCapabilityImpl implements ILaserCapability {

        public LaserHandler laserHandler;

        @Override
        public void init(LaserHandler handler) {
            this.laserHandler = handler;
        }

        @Override
        public void start() {
            this.laserHandler.start();
        }

        @Override
        public void tick() {
        }

        @Override
        public void stop() {
            if (this.laserHandler != null) {
                this.laserHandler.stop();
            }
        }

        @Override
        public void end() {
            if (this.laserHandler != null) {
                this.laserHandler.end();
            }
        }

        @Override
        public @UnknownNullability CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
            CompoundTag tag = new CompoundTag();
            if (this.laserHandler != null) {
                tag.put("Laser", this.laserHandler.writeNBT());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag compoundTag) {
            if (compoundTag.contains("Laser") && this.laserHandler != null) {
                this.laserHandler.readNBT(compoundTag.getCompound("Laser"));
            }
        }
    }

    public static class LaserCapabilityProvider implements ICapabilityProvider<Player, Void, ILaserCapability>, INBTSerializable<CompoundTag> {

        private final LaserCapabilityImpl instance = new LaserCapabilityImpl();

        @Override
        public @Nullable ILaserCapability getCapability(@NotNull Player object, Void context) {
            return object.getCapability(ModCapabilities.LASER_CAPABILITY, context);
        }

        @Override
        public @UnknownNullability CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
            return instance.serializeNBT(provider);
        }

        @Override
        public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
            instance.deserializeNBT(provider, nbt);
        }
    }
}
