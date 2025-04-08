package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.init.ModCapabilities;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

public enum AmmoType {
    HANDGUN("item.superbwarfare.ammo.handgun", "HandgunAmmo"),
    RIFLE("item.superbwarfare.ammo.rifle", "RifleAmmo"),
    SHOTGUN("item.superbwarfare.ammo.shotgun", "ShotgunAmmo"),
    SNIPER("item.superbwarfare.ammo.sniper", "SniperAmmo"),
    HEAVY("item.superbwarfare.ammo.heavy", "HeavyAmmo");
    public final String translatableKey;
    public final String name;
    public DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> dataComponent;

    AmmoType(String translatableKey, String name) {
        this.translatableKey = translatableKey;
        this.name = name;
    }

    public static AmmoType getType(String name) {
        for (AmmoType type : values()) {
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
        return switch (this) {
            case HANDGUN -> variable.handgunAmmo;
            case RIFLE -> variable.rifleAmmo;
            case SHOTGUN -> variable.shotgunAmmo;
            case SNIPER -> variable.sniperAmmo;
            case HEAVY -> variable.heavyAmmo;
        };
    }

    public void set(PlayerVariable variable, int count) {
        if (count < 0) count = 0;

        switch (this) {
            case HANDGUN -> variable.handgunAmmo = count;
            case RIFLE -> variable.rifleAmmo = count;
            case SHOTGUN -> variable.shotgunAmmo = count;
            case SNIPER -> variable.sniperAmmo = count;
            case HEAVY -> variable.heavyAmmo = count;
        }
    }

    public void add(PlayerVariable variable, int count) {
        set(variable, safeAdd(get(variable), count));
    }


    // Entity
    public int get(Entity entity) {
        var cap = entity.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) return 0;

        return get(cap);
    }

    public void set(Entity entity, int count) {
        var cap = entity.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) return;

        set(cap, count);
        cap.syncPlayerVariables(entity);
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
