package com.atsuishio.superbwarfare.tiers

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.crafting.Ingredient
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.*

object ModArmorMaterial {
    val MATERIALS: DeferredRegister<ArmorMaterial> =
        DeferredRegister.create(BuiltInRegistries.ARMOR_MATERIAL, Mod.MODID)

    @JvmField
    val CEMENTED_CARBIDE: DeferredHolder<ArmorMaterial, ArmorMaterial> = MATERIALS.register("cemented_carbide") { ->
        ArmorMaterial(
            EnumMap<ArmorItem.Type, Int>(ArmorItem.Type::class.java).apply {
                put(ArmorItem.Type.BOOTS, 3)
                put(ArmorItem.Type.LEGGINGS, 6)
                put(ArmorItem.Type.CHESTPLATE, 8)
                put(ArmorItem.Type.HELMET, 3)
            },
            10,
            SoundEvents.ARMOR_EQUIP_IRON,
            { Ingredient.of(ModItems.CEMENTED_CARBIDE_INGOT.value()) },
            listOf(ArmorMaterial.Layer(loc("cemented_carbide"))),
            4f,
            0.05f,
        )
    }

    @JvmField
    val STEEL: DeferredHolder<ArmorMaterial, ArmorMaterial> = MATERIALS.register("steel") { ->
        ArmorMaterial(
            EnumMap<ArmorItem.Type, Int>(ArmorItem.Type::class.java).apply {
                put(ArmorItem.Type.BOOTS, 2)
                put(ArmorItem.Type.LEGGINGS, 5)
                put(ArmorItem.Type.CHESTPLATE, 7)
                put(ArmorItem.Type.HELMET, 2)
            },
            9,
            SoundEvents.ARMOR_EQUIP_IRON,
            { Ingredient.of(ModItems.STEEL_INGOT.value()) },
            listOf(ArmorMaterial.Layer(loc("steel"))),
            1f,
            0f,
        )
    }
}
