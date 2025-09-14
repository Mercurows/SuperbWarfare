package com.atsuishio.superbwarfare.world;

import com.google.common.collect.Sets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Set;

public class TDMSavedData extends SavedData {

    public static final String FILE_ID = "superbwarfare_tdm";

    private final Set<String> entities = Sets.newHashSet();

    public TDMSavedData() {
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

    public boolean addEntity(String entity) {
        return this.entities.add(entity);
    }

    public boolean removeEntity(String entity) {
        return this.entities.remove(entity);
    }
}
