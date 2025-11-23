package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Locale;
import java.util.function.Supplier;

public enum Ammo {
    HANDGUN(ChatFormatting.GREEN, ModItems.HANDGUN_AMMO::get),
    RIFLE(ChatFormatting.AQUA, ModItems.RIFLE_AMMO::get),
    SHOTGUN(ChatFormatting.RED, ModItems.SHOTGUN_AMMO::get),
    SNIPER(ChatFormatting.GOLD, ModItems.SNIPER_AMMO::get),
    HEAVY(ChatFormatting.LIGHT_PURPLE, ModItems.HEAVY_AMMO::get);

    /**
     * 翻译字段名称，如 item.superbwarfare.ammo.rifle
     */
    public final String translationKey;
    /**
     * 大驼峰格式命名的序列化字段名称，如 RifleAmmo
     */
    public final String serializationName;
    /**
     * 下划线格式命名的小写名称，如 rifle
     */
    public final String name;

    /**
     * 大驼峰格式命名的显示名称，如 Rifle Ammo
     */
    public final String displayName;

    /**
     * 该类型弹药默认的Item
     */
    public final Supplier<Item> defaultItemSupplier;

    public final ChatFormatting color;
    public DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> dataComponent;

    Ammo(ChatFormatting color, Supplier<Item> defaultItemSupplier) {
        this.color = color;
        this.defaultItemSupplier = defaultItemSupplier;

        var name = name().toLowerCase(Locale.ROOT);
        this.name = name;
        this.translationKey = "item.superbwarfare.ammo." + name;

        var builder = new StringBuilder();
        var useUpperCase = true;

        for (char c : name.toCharArray()) {
            if (c == '_') {
                useUpperCase = true;
            } else if (useUpperCase) {
                builder.append(String.valueOf(c).toUpperCase(Locale.ROOT));
                useUpperCase = false;
            } else {
                builder.append(c);
            }
        }

        this.displayName = builder + " Ammo";
        this.serializationName = builder + "Ammo";
    }

    public ItemStack getItemStack() {
        return getItemStack(1);
    }

    public ItemStack getItemStack(int count) {
        return new ItemStack(defaultItemSupplier.get(), count);
    }

    public static Ammo getType(String name) {
        for (Ammo type : values()) {
            if (type.serializationName.equals(name)) {
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
        return tag.getInt(this.serializationName);
    }

    public void set(CompoundTag tag, int count) {
        if (count < 0) count = 0;
        tag.putInt(this.serializationName, count);
    }

    public void add(CompoundTag tag, int count) {
        set(tag, safeAdd(get(tag), count));
    }

    // PlayerVariables
    public int get(PlayerVariable variable) {
        return variable.ammo.getOrDefault(this, 0);
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
        if (entity.level().isClientSide) return;
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
        return this.serializationName;
    }
}
