package com.atsuishio.superbwarfare.api.event;

import com.atsuishio.superbwarfare.item.ContainerBlockItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Register Entities as a container
 */
@ApiStatus.AvailableSince("0.8.0")
public class RegisterContainersEvent extends Event implements IModBusEvent {
    public static final List<ItemStack> containers = new ArrayList<>();

    public <T extends Entity> void add(DeferredHolder<EntityType<?>, EntityType<T>> type) {
        add(type.get());
    }

    public <T extends Entity> void add(EntityType<T> type) {
        containers.add(ContainerBlockItem.createInstance(type));
    }

    public void add(Entity entity) {
        containers.add(ContainerBlockItem.createInstance(entity));
    }
}
