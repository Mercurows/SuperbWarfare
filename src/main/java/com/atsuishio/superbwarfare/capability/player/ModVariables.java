package com.atsuishio.superbwarfare.capability.player;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.network.message.SavedDataSyncMessage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;


public class ModVariables {

    // 这玩意有用吗？

    public static class WorldVariables extends SavedData {
        public static final String DATA_NAME = ModUtils.MODID + "_world_variables";

        public static WorldVariables load(CompoundTag tag, HolderLookup.Provider provider) {
            WorldVariables data = new WorldVariables();
            data.read(tag);
            return data;
        }

        public void read(CompoundTag nbt) {
        }

        @Override
        public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
            return tag;
        }

        public void syncData(LevelAccessor world) {
            this.setDirty();
            if (world instanceof Level level && !level.isClientSide()) {
                PacketDistributor.sendToAllPlayers(new SavedDataSyncMessage(1, this, null));
            }
        }

        public static WorldVariables clientSide = new WorldVariables();

        public static WorldVariables get(LevelAccessor world) {
            if (world instanceof ServerLevel level)
                return level.getDataStorage().computeIfAbsent(new Factory<>(
                        WorldVariables::new, WorldVariables::load, null
                ), DATA_NAME);
            return clientSide;
        }
    }

}
