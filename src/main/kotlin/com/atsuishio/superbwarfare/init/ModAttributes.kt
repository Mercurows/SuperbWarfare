package com.atsuishio.superbwarfare.init

import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.RangedAttribute
import net.minecraftforge.event.entity.EntityAttributeModificationEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import java.util.function.Supplier

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
object ModAttributes {
    val ATTRIBUTES: DeferredRegister<Attribute> = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, com.atsuishio.superbwarfare.Mod.MODID)

    @JvmField
    val BULLET_RESISTANCE: RegistryObject<Attribute> = ATTRIBUTES.register(
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
        event.types.forEach { event.add(it, BULLET_RESISTANCE.get()) }
    }
}