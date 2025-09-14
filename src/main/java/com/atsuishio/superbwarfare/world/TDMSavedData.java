package com.atsuishio.superbwarfare.world;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.network.message.receive.TDMSyncMessage;
import com.google.common.collect.Sets;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Set;

@EventBusSubscriber
public class TDMSavedData extends SavedData {

    public static final String FILE_ID = "superbwarfare_tdm";

    private final Set<String> entities = Sets.newHashSet();

    public TDMSavedData() {
    }

    public TDMSavedData(Collection<String> entities) {
        this.entities.addAll(entities);
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Entities", this.saveEntities());
        return tag;
    }


    public static TDMSavedData load(CompoundTag pCompoundTag, HolderLookup.Provider registries) {
        TDMSavedData tdmSavedData = new TDMSavedData();
        if (pCompoundTag.contains("Entities", Tag.TAG_LIST)) {
            tdmSavedData.loadEntities(pCompoundTag.getList("Entities", Tag.TAG_STRING));
        }
        return tdmSavedData;
    }

    private ListTag saveEntities() {
        ListTag listtag = new ListTag();

        for (String s : this.entities) {
            listtag.add(StringTag.valueOf(s));
        }

        return listtag;
    }

    private void loadEntities(ListTag pTagList) {
        for (int i = 0; i < pTagList.size(); ++i) {
            this.entities.add(pTagList.getString(i));
        }
    }

    public Set<String> getEntities() {
        return this.entities;
    }

    public boolean addEntity(String entity) {
        return this.entities.add(entity);
    }

    public boolean removeEntity(String entity) {
        return this.entities.remove(entity);
    }

    public boolean containsEntity(String entity) {
        return this.entities.contains(entity);
    }

    public void sync() {
        this.setDirty();
        PacketDistributor.sendToAllPlayers(new TDMSyncMessage(this));
    }

    public static boolean enabledTDM(Entity entity) {
        var level = entity.level();
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getDataStorage().computeIfAbsent(new SavedData.Factory<>(TDMSavedData::new, TDMSavedData::load, null), FILE_ID).containsEntity(entity.getStringUUID());
        } else {
            return ClientEventHandler.tdmSavedData.containsEntity(entity.getStringUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level)) return;

        var data = level.getDataStorage().get(new SavedData.Factory<>(TDMSavedData::new, TDMSavedData::load, null), FILE_ID);
        if (data == null) return;
        PacketDistributor.sendToPlayer(player, new TDMSyncMessage(data));
    }
}
