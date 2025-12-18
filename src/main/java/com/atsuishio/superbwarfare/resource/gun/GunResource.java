package com.atsuishio.superbwarfare.resource.gun;

import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.DefaultDataSupplier;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GunResource implements DefaultDataSupplier<DefaultGunResource> {

    public static final LoadingCache<ItemStack, GunResource> RESOURCE_CACHE = CacheBuilder.newBuilder()
            .weakKeys()
            .weakValues()
            .build(new CacheLoader<>() {
                public @NotNull GunResource load(@NotNull ItemStack stack) {
                    return new GunResource(stack);
                }
            });

    public final ItemStack stack;
    public final GunItem item;
    public final String id;

    private GunResource(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            throw new IllegalArgumentException("stack is not GunItem!");
        }

        this.item = gunItem;
        this.stack = stack;
        this.id = getRegistryId(stack.getItem());
    }

    public static DefaultGunResource compute(ItemStack stack) {
        return from(stack).compute();
    }

    private DefaultGunResource cache = null;

    public DefaultGunResource compute() {
        if (cache != null) return cache;

        var defaultResource = getDefault().copy();
        // TODO 正确实现属性计算

        cache = defaultResource;

        return defaultResource;
    }

    public void update() {
        this.cache = null;
    }

    public static DefaultGunResource getDefault(String id) {
        return CustomData.GUN_RESOURCE.getOrElseGet(id, DefaultGunResource::new);
    }

    @Override
    public DefaultGunResource getDefault() {
        return CustomData.GUN_RESOURCE.getOrElseGet(id, DefaultGunResource::new);
    }

    public static DefaultGunResource getDefault(ItemStack stack) {
        return getDefault(stack.getItem());
    }

    public static DefaultGunResource getDefault(Item item) {
        return getDefault(getRegistryId(item));
    }

    public static GunResource create(Item item) {
        return from(new ItemStack(item));
    }

    public static GunResource from(ItemStack stack) {
        return RESOURCE_CACHE.getUnchecked(stack);
    }

    public static String getRegistryId(Item item) {
        var id = item.getDescriptionId();
        id = id.substring(id.indexOf(".") + 1).replace('.', ':');
        return id;
    }
}
