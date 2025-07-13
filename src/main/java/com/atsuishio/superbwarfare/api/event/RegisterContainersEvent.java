package com.atsuishio.superbwarfare.api.event;

import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Register Entities as a container
 */
@ApiStatus.AvailableSince("0.8.0")
public class RegisterContainersEvent extends Event implements IModBusEvent {

    public static final List<ItemStack> CONTAINERS = new ArrayList<>();

    public <T extends Entity> void add(RegistryObject<EntityType<T>> type) {
        add(type.get());
    }

    public <T extends Entity> void add(EntityType<T> type) {
        ItemStack stack = ContainerBlockItem.createInstance(type);
        CONTAINERS.add(stack);
    }

    public void add(Entity entity) {
        ItemStack stack = ContainerBlockItem.createInstance(entity);
        CONTAINERS.add(stack);
    }
}
