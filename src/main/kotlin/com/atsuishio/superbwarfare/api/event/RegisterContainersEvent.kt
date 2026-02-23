package com.atsuishio.superbwarfare.api.event

import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.Event
import net.neoforged.fml.event.IModBusEvent
import net.neoforged.neoforge.registries.DeferredHolder
import org.jetbrains.annotations.ApiStatus

/**
 * Register Entities as a container
 */
@ApiStatus.AvailableSince("0.8.0")
class RegisterContainersEvent : Event(), IModBusEvent {
    companion object {
        @JvmField
        val CONTAINERS = arrayListOf<ItemStack>()
    }

    fun <T : Entity> add(type: DeferredHolder<EntityType<*>, EntityType<T>>) {
        add(type.get())
    }

    fun <T : Entity> add(type: EntityType<T>) {
        val stack = ContainerBlockItem.createInstance(type)
        CONTAINERS.add(stack)
    }

    fun add(entity: Entity) {
        val stack = ContainerBlockItem.createInstance(entity)
        CONTAINERS.add(stack)
    }
}
