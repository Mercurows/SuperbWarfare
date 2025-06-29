package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.capability.PlayerVariable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public enum Ammo {
    HANDGUN(ChatFormatting.GREEN),
    RIFLE(ChatFormatting.AQUA),
    SHOTGUN(ChatFormatting.RED),
    SNIPER(ChatFormatting.GOLD),
    HEAVY(ChatFormatting.LIGHT_PURPLE);

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

    public final ChatFormatting color;

    Ammo(ChatFormatting color) {
        this.color = color;

        var name = name().toLowerCase(Locale.ENGLISH);
        this.name = name;
        this.translationKey = "item.superbwarfare.ammo." + name;

        var builder = new StringBuilder();
        var useUpperCase = true;

        for (char c : name.toCharArray()) {
            if (c == '_') {
                useUpperCase = true;
            } else if (useUpperCase) {
                builder.append(String.valueOf(c).toUpperCase(Locale.ENGLISH));
                useUpperCase = false;
            } else {
                builder.append(c);
            }
        }

        this.displayName = builder + " Ammo";
        this.serializationName = builder + "Ammo";
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
        return get(stack.getOrCreateTag());
    }

    public void set(ItemStack stack, int count) {
        set(stack.getOrCreateTag(), count);
    }

    public void add(ItemStack stack, int count) {
        add(stack.getOrCreateTag(), count);
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

    public int get(Player player) {
        return player.getCapability(ModCapabilities.PLAYER_VARIABLE)
                .map(this::get)
                .orElse(0);
    }

    public void set(Player player, int count) {
        PlayerVariable.modify(player, c -> set(c, Math.max(0, count)));
    }

    public void add(Player player, int count) {
        set(player, safeAdd(get(player), count));
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
