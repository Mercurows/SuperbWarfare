package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.init.ModAttachments;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

public enum Ammo {
    HANDGUN("item.superbwarfare.ammo.handgun", "HandgunAmmo"),
    RIFLE("item.superbwarfare.ammo.rifle", "RifleAmmo"),
    SHOTGUN("item.superbwarfare.ammo.shotgun", "ShotgunAmmo"),
    SNIPER("item.superbwarfare.ammo.sniper", "SniperAmmo"),
    HEAVY("item.superbwarfare.ammo.heavy", "HeavyAmmo");
    public final String translatableKey;
    public final String name;
    public DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> dataComponent;

    Ammo(String translatableKey, String name) {
        this.translatableKey = translatableKey;
        this.name = name;
    }

    public static Ammo getType(String name) {
        for (Ammo type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }

    // ItemStack
    public int get(ItemStack stack) {
        var count = stack.get(this.dataComponent);
        return count == null ? 0 : count;
    }

    public void set(ItemStack stack, int count) {
        stack.set(this.dataComponent, count);
    }

    public void add(ItemStack stack, int count) {
        set(stack, safeAdd(get(stack), count));
    }

    // NBTTag
    public int get(CompoundTag tag) {
        return tag.getInt(this.name);
    }

    public void set(CompoundTag tag, int count) {
        if (count < 0) count = 0;
        tag.putInt(this.name, count);
    }

    public void add(CompoundTag tag, int count) {
        set(tag, safeAdd(get(tag), count));
    }

    // PlayerVariables
    public int get(PlayerVariable variable) {
        return variable.ammo.get(this);
    }

    public void set(PlayerVariable variable, int count) {
        if (count < 0) count = 0;

        variable.ammo.put(this, count);
    }

    public void add(PlayerVariable variable, int count) {
        set(variable, safeAdd(get(variable), count));
    }


    // Entity
    public int get(Entity entity) {
        return get(entity.getData(ModAttachments.PLAYER_VARIABLE));
    }

    public void set(Entity entity, int count) {
        var cap = entity.getData(ModAttachments.PLAYER_VARIABLE).watch();

        set(cap, count);
        entity.setData(ModAttachments.PLAYER_VARIABLE, cap);
        cap.sync(entity);
    }

    public void add(Entity entity, int count) {
        set(entity, safeAdd(get(entity), count));
    }


    private int safeAdd(int a, int b) {
        var newCount = (long) a + (long) b;

        if (newCount > Integer.MAX_VALUE) {
            newCount = Integer.MAX_VALUE;
        } else if (newCount < 0) {
            newCount = 0;
        }

        return (int) newCount;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
