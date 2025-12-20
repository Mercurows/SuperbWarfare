package com.atsuishio.superbwarfare.tiers

import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.Util
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.crafting.Ingredient
import java.util.*
import java.util.function.Supplier

enum class ModArmorMaterial(
    val serializedName: String,
    private val durabilityMultiplier: Int,
    private val protectionFunctionForType: EnumMap<ArmorItem.Type?, Int?>,
    private val enchantmentValue: Int,
    private val sound: SoundEvent,
    private val toughness: Float,
    private val knockbackResistance: Float,
    private val repairIngredient: Supplier<Ingredient>
) : ArmorMaterial {
    CEMENTED_CARBIDE(
        "cemented_carbide",
        50,
        Util.make(
            EnumMap<ArmorItem.Type?, Int?>(ArmorItem.Type::class.java)
        ) { p: EnumMap<ArmorItem.Type?, Int?> ->
            p[ArmorItem.Type.BOOTS] = 3
            p[ArmorItem.Type.LEGGINGS] = 6
            p[ArmorItem.Type.CHESTPLATE] = 8
            p[ArmorItem.Type.HELMET] = 3
        },
        10,
        SoundEvents.ARMOR_EQUIP_IRON,
        4.toFloat(),
        0.05f,
        { Ingredient.of(ModItems.CEMENTED_CARBIDE_INGOT.get()) }),
    STEEL(
        "steel",
        35,
        Util.make(
            EnumMap<ArmorItem.Type?, Int?>(ArmorItem.Type::class.java)
        ) { p: EnumMap<ArmorItem.Type?, Int?> ->
            p[ArmorItem.Type.BOOTS] = 2
            p[ArmorItem.Type.LEGGINGS] = 5
            p[ArmorItem.Type.CHESTPLATE] = 7
            p[ArmorItem.Type.HELMET] = 2
        },
        9,
        SoundEvents.ARMOR_EQUIP_IRON,
        1.toFloat(),
        0.toFloat(),
        { Ingredient.of(ModItems.STEEL_INGOT.get()) });

    val ingredient by lazy { repairIngredient.get() }

    override fun getDurabilityForType(pType: ArmorItem.Type): Int {
        return HEALTH_FUNCTION_FOR_TYPE[pType]!! * this.durabilityMultiplier
    }

    override fun getDefenseForType(pType: ArmorItem.Type): Int {
        return this.protectionFunctionForType[pType]!!
    }

    override fun getEnchantmentValue(): Int {
        return this.enchantmentValue
    }

    override fun getEquipSound(): SoundEvent {
        return this.sound
    }

    override fun getRepairIngredient(): Ingredient {
        return this.ingredient
    }

    override fun getName(): String {
        return this.serializedName
    }

    override fun getToughness(): Float {
        return this.toughness
    }

    override fun getKnockbackResistance(): Float {
        return this.knockbackResistance
    }

    companion object {
        private val HEALTH_FUNCTION_FOR_TYPE =
            Util.make(
                EnumMap<ArmorItem.Type, Int>(ArmorItem.Type::class.java)
            ) { map: EnumMap<ArmorItem.Type, Int> ->
                map[ArmorItem.Type.BOOTS] = 13
                map[ArmorItem.Type.LEGGINGS] = 15
                map[ArmorItem.Type.CHESTPLATE] = 16
                map[ArmorItem.Type.HELMET] = 11
            }
    }
}
