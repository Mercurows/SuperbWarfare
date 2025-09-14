package com.atsuishio.superbwarfare.world;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.network.message.receive.TDMSyncMessage;
import com.google.common.collect.Sets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.Set;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber
public class TDMSavedData extends SavedData {

    public static final String FILE_ID = "superbwarfare_tdm";

    private final Set<String> entities = Sets.newHashSet();

    public TDMSavedData() {
    }

    public TDMSavedData(Collection<String> entities) {
        this.entities.addAll(entities);
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        pCompoundTag.put("Entities", this.saveEntities());
        return pCompoundTag;
    }

    public static TDMSavedData load(CompoundTag pCompoundTag) {
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

    public void sync() {
        this.setDirty();
        Mod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new TDMSyncMessage(this));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level)) return;

        var data = level.getDataStorage().get(TDMSavedData::load, FILE_ID);
        if (data == null) return;
        Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new TDMSyncMessage(data));
    }
}
