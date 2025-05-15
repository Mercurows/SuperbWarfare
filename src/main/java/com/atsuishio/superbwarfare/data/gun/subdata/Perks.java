package com.atsuishio.superbwarfare.data.gun.subdata;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.PerkItem;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;

public final class Perks {

    private final CompoundTag perks;

    public Perks(GunData gun) {
        this.perks = gun.perk();
    }

    public boolean has(Perk perk) {
        if (!has(perk.type)) return false;
        return getTag(perk).getString("Name").equals(perk.name);
    }

    public boolean has(Perk.Type type) {
        return perks.contains(type.getName()) && !perks.getCompound(type.getName()).getString("Name").isEmpty();
    }

    public void set(PerkInstance instance) {
        set(instance.perk(), instance.level());
    }

    public CompoundTag getTag(DeferredHolder<Perk, Perk> registry) {
        return getTag(registry.get().type);
    }

    public CompoundTag getTag(Perk perk) {
        return getTag(perk.type);
    }

    public CompoundTag getTag(Perk.Type type) {
        CompoundTag typeTag;
        if (!perks.contains(type.getName())) {
            typeTag = new CompoundTag();
            perks.put(type.getName(), typeTag);
        }
        return perks.getCompound(type.getName());
    }

    public void set(Perk perk, short level) {
        getTag(perk).putString("Name", perk.name);
        getTag(perk).putShort("Level", level);
    }

    public short getLevel(PerkItem<?> item) {
        return getLevel(item.getPerk());
    }

    public short getLevel(DeferredHolder<Perk, ? extends Perk> perk) {
        return getLevel(perk.get());
    }

    public short getLevel(Perk perk) {
        var name = perk.name;
        var tag = getTag(perk);
        if (!tag.getString("Name").equals(name)) return 0;
        return getLevel(perk.type);
    }

    public short getLevel(Perk.Type type) {
        return getTag(type).getShort("Level");
    }

    public @Nullable Perk get(DeferredHolder<Perk, Perk> registry) {
        return get(registry.get());
    }

    public @Nullable Perk get(Perk perk) {
        return get(perk.type);
    }

    public @Nullable Perk get(Perk.Type type) {
        var perksRegistry = new ArrayList<DeferredHolder<Perk, ? extends Perk>>();
        perksRegistry.addAll(ModPerks.AMMO_PERKS.getEntries());
        perksRegistry.addAll(ModPerks.FUNC_PERKS.getEntries());
        perksRegistry.addAll(ModPerks.DAMAGE_PERKS.getEntries());

        for (var registry : perksRegistry) {
            var name = getTag(type).getString("Name");
            if (registry.get().name.equals(name)) {
                return registry.get();
            }
        }
        return null;
    }

    public @Nullable PerkInstance getInstance(Perk perk) {
        return getInstance(perk.type);
    }

    public @Nullable PerkInstance getInstance(Perk.Type type) {
        var perk = get(type);
        if (perk == null) return null;

        return new PerkInstance(perk, getLevel(type));
    }

    public void reduceCooldown(DeferredHolder<Perk, Perk> registry, String name) {
        reduceCooldown(registry.get(), name);
    }

    public void reduceCooldown(Perk perk, String name) {
        reduceCooldown(perk.type, name);
    }

    public void reduceCooldown(Perk.Type type, String name) {
        var tag = getTag(type);
        var value = tag.getInt(name);
        value--;

        if (value <= 0) {
            tag.remove(name);
        } else {
            tag.putInt(name, value);
        }
    }

    public void remove(Perk perk) {
        remove(perk.type);
    }

    public void remove(Perk.Type type) {
        perks.remove(type.getName());
    }

}
