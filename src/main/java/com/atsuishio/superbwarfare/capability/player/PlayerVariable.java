package com.atsuishio.superbwarfare.capability.player;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.network.message.receive.PlayerVariablesSyncMessage;
import com.atsuishio.superbwarfare.tools.AmmoType;
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

    public Map<AmmoType, Integer> ammo = new HashMap<>();
    public boolean tacticalSprint = false;
    public boolean edit = false;

    public void sync(Entity entity) {
        if (!entity.hasData(ModAttachments.PLAYER_VARIABLE)) return;

        var newVariable = entity.getData(ModAttachments.PLAYER_VARIABLE);
        if (old != null && old.equals(newVariable)) return;

        if (entity instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new PlayerVariablesSyncMessage(entity.getId(), newVariable.writeToNBT()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getId(), player.getData(ModAttachments.PLAYER_VARIABLE).writeToNBT()));
    }

    public PlayerVariable watch() {
        this.old = this.copy();
        return this;
    }

    public CompoundTag writeToNBT() {
        CompoundTag nbt = new CompoundTag();

        for (var type : AmmoType.values()) {
            type.set(nbt, type.get(this));
        }

        nbt.putBoolean("TacticalSprint", tacticalSprint);
        nbt.putBoolean("EditMode", edit);

        return nbt;
    }

    public PlayerVariable readFromNBT(CompoundTag tag) {
        for (var type : AmmoType.values()) {
            type.set(this, type.get(tag));
        }

        tacticalSprint = tag.getBoolean("TacticalSprint");
        edit = tag.getBoolean("EditMode");

        return this;
    }

    public PlayerVariable copy() {
        var clone = new PlayerVariable();

        for (var type : AmmoType.values()) {
            type.set(clone, type.get(this));
        }

        clone.edit = this.edit;
        clone.tacticalSprint = this.tacticalSprint;

        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlayerVariable other)) return false;

        for (var type : AmmoType.values()) {
            if (type.get(this) != type.get(other)) return false;
        }

        return tacticalSprint == other.tacticalSprint
                && edit == other.edit;
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
