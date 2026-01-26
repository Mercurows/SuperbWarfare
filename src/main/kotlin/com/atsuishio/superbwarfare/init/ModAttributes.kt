package com.atsuishio.superbwarfare.init

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.RangedAttribute
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import java.util.function.Supplier

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
object ModAttributes {
    val ATTRIBUTES: DeferredRegister<Attribute> =
        DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, com.atsuishio.superbwarfare.Mod.MODID)

    @JvmField
    val BULLET_RESISTANCE: DeferredHolder<Attribute, out Attribute> = ATTRIBUTES.register(
        "bullet_resistance",
        Supplier {
            (RangedAttribute(
                "attribute." + com.atsuishio.superbwarfare.Mod.MODID + ".bullet_resistance",
                0.0,
                0.0,
                1.0
            )).setSyncable(true)
        })

    @SubscribeEvent
    fun register(event: FMLConstructModEvent) {
        event.enqueueWork { ATTRIBUTES.register(MOD_BUS) }
    }

    @SubscribeEvent
    fun addAttributes(event: EntityAttributeModificationEvent) {
        event.types.forEach { e -> event.add(e, BULLET_RESISTANCE) }
    }
}