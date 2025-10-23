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
            .build(new CacheLoader<>() {
                public @NotNull GunResource load(@NotNull ItemStack stack) {
                    return new GunResource(stack);
                }
            });

    public final ItemStack stack;
    public final GunItem item;
    public final String id;

    @NotNull
    public DefaultGunResource defaultResource;

    private GunResource(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            throw new IllegalArgumentException("stack is not GunItem!");
        }

        this.item = gunItem;
        this.stack = stack;
        this.id = getRegistryId(stack.getItem());

        this.defaultResource = CustomData.GUN_RESOURCE.get(id);
    }

    @Override
    public DefaultGunResource getDefault() {
        return this.defaultResource;
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
