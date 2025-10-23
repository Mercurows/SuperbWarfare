package com.atsuishio.superbwarfare.resource.gun;

import com.atsuishio.superbwarfare.data.DefaultDataSupplier;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class GunResource implements DefaultDataSupplier<DefaultGunResource> {

    public final ItemStack stack;
    public final GunItem item;
    public final String id;

    @NotNull
    public Supplier<DefaultGunResource> defaultResourceSupplier;

    private GunResource(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            throw new IllegalArgumentException("stack is not GunItem!");
        }

        this.item = gunItem;
        this.stack = stack;
        this.id = getRegistryId(stack.getItem());
    }

    @Override
    public DefaultGunResource getDefault() {
        return this.defaultResourceSupplier.get();
    }

    public static String getRegistryId(Item item) {
        var id = item.getDescriptionId();
        id = id.substring(id.indexOf(".") + 1).replace('.', ':');
        return id;
    }
}
