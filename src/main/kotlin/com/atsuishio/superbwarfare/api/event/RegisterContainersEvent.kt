package com.atsuishio.superbwarfare.api.event

import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.fml.event.IModBusEvent
import net.minecraftforge.registries.RegistryObject
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

    fun <T : Entity> add(type: RegistryObject<EntityType<T>>) {
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
