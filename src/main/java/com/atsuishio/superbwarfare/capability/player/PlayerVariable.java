package com.atsuishio.superbwarfare.capability.player;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.network.message.receive.PlayerVariablesSyncMessage;
import com.atsuishio.superbwarfare.tools.Ammo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@AutoRegisterCapability
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
public class PlayerVariable implements INBTSerializable<CompoundTag> {

    public static ResourceLocation ID = Mod.loc("player_variables");
    private PlayerVariable old = null;

    public Map<Ammo, Integer> ammo = new HashMap<>();
    public boolean tacticalSprint = false;

    public void sync(Entity entity) {
        if (!entity.getCapability(ModCapabilities.PLAYER_VARIABLE).isPresent()) return;

        var newVariable = getOrDefault(entity);
        if (old != null && old.equals(newVariable)) return;

        if (entity instanceof ServerPlayer player) {
            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new PlayerVariablesSyncMessage(entity.getId(), compareAndUpdate()));
        }
    }

    public static PlayerVariable getOrDefault(Entity entity) {
        return entity.getCapability(ModCapabilities.PLAYER_VARIABLE).orElse(new PlayerVariable());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new PlayerVariablesSyncMessage(player.getId(), getOrDefault(player).compareAndUpdate()));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new PlayerVariablesSyncMessage(player.getId(), getOrDefault(player).compareAndUpdate()));
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new PlayerVariablesSyncMessage(player.getId(), getOrDefault(player).forceUpdate()));
    }

    /**
     * 编辑并自动同步玩家变量
     */
    public static void modify(Entity entity, Consumer<PlayerVariable> consumer) {
        var cap = entity.getCapability(ModCapabilities.PLAYER_VARIABLE).orElse(new PlayerVariable());

        cap.watch();
        consumer.accept(cap);
        cap.sync(entity);
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
        if (event.getEntity().level().isClientSide()) return;
        var original = getOrDefault(event.getOriginal());
        var clone = event.getEntity().getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable());

        for (var type : Ammo.values()) {
            type.set(clone, type.get(original));
        }

        clone.tacticalSprint = original.tacticalSprint;

        if (event.getEntity().level().isClientSide()) return;

        var player = event.getEntity();
        player.getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable()).sync(player);
    }

    @Override
    public CompoundTag serializeNBT() {
        return writeToNBT();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void deserializeNBT(CompoundTag nbt) {
        readFromNBT(nbt);
    }
}
