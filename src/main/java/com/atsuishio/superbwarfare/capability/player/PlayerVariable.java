package com.atsuishio.superbwarfare.capability.player;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.network.message.receive.PlayerVariablesSyncMessage;
import com.atsuishio.superbwarfare.tools.Ammo;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;


@EventBusSubscriber(modid = Mod.MODID)
public class PlayerVariable implements INBTSerializable<CompoundTag> {
    private PlayerVariable old = null;

    public Map<Ammo, Integer> ammo = new HashMap<>();
    public boolean tacticalSprint = false;

    public void sync(Entity entity) {
        if (!entity.hasData(ModAttachments.PLAYER_VARIABLE)) return;

        var newVariable = entity.getData(ModAttachments.PLAYER_VARIABLE);
        if (old != null && old.equals(newVariable)) return;

        if (entity instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new PlayerVariablesSyncMessage(entity.getId(), compareAndUpdate()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getId(), player.getData(ModAttachments.PLAYER_VARIABLE).compareAndUpdate()));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getId(), player.getData(ModAttachments.PLAYER_VARIABLE).compareAndUpdate()));
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getId(), player.getData(ModAttachments.PLAYER_VARIABLE).forceUpdate()));
    }

    public PlayerVariable watch() {
        this.old = this.copy();
        return this;
    }

    public Map<Byte, Integer> forceUpdate() {
        var map = new HashMap<Byte, Integer>();

        for (var type : Ammo.values()) {
            map.put((byte) type.ordinal(), type.get(this));
        }

        map.put((byte) -1, this.tacticalSprint ? 1 : 0);

        return map;
    }

    public Map<Byte, Integer> compareAndUpdate() {
        var map = new HashMap<Byte, Integer>();
        var old = this.old == null ? new PlayerVariable() : this.old;

        for (var type : Ammo.values()) {
            var oldCount = old.ammo.getOrDefault(type, 0);
            var newCount = type.get(this);

            if (oldCount != newCount) {
                map.put((byte) type.ordinal(), newCount);
            }
        }

        if (old.tacticalSprint != this.tacticalSprint) {
            map.put((byte) -1, this.tacticalSprint ? 1 : 0);
        }

        return map;
    }


    public CompoundTag writeToNBT() {
        CompoundTag nbt = new CompoundTag();

        for (var type : Ammo.values()) {
            type.set(nbt, type.get(this));
        }

        nbt.putBoolean("TacticalSprint", tacticalSprint);

        return nbt;
    }

    public void readFromNBT(CompoundTag tag) {
        for (var type : Ammo.values()) {
            type.set(this, type.get(tag));
        }

        tacticalSprint = tag.getBoolean("TacticalSprint");

    }

    public PlayerVariable copy() {
        var clone = new PlayerVariable();

        for (var type : Ammo.values()) {
            type.set(clone, type.get(this));
        }

        clone.tacticalSprint = this.tacticalSprint;

        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlayerVariable other)) return false;

        for (var type : Ammo.values()) {
            if (type.get(this) != type.get(other)) return false;
        }

        return tacticalSprint == other.tacticalSprint;
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().revive();
        var original = event.getOriginal().getData(ModAttachments.PLAYER_VARIABLE);
        if (event.getEntity().level().isClientSide()) return;

        event.getEntity().setData(ModAttachments.PLAYER_VARIABLE, original.copy());
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        return writeToNBT();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        readFromNBT(nbt);
    }
}
