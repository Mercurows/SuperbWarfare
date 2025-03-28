package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(BuiltInRegistries.ARMOR_MATERIAL, Mod.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CEMENTED_CARBIDE = MATERIALS.register("cemented_carbide", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), p -> {
                p.put(ArmorItem.Type.BOOTS, 3);
                p.put(ArmorItem.Type.LEGGINGS, 6);
                p.put(ArmorItem.Type.CHESTPLATE, 8);
                p.put(ArmorItem.Type.HELMET, 3);
            }),
            10,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ModItems.CEMENTED_CARBIDE_INGOT.value()),
            List.of(),
            4F, 0.05F)
    );

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STEEL = MATERIALS.register("steel", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), p -> {
                p.put(ArmorItem.Type.BOOTS, 2);
                p.put(ArmorItem.Type.LEGGINGS, 5);
                p.put(ArmorItem.Type.CHESTPLATE, 7);
                p.put(ArmorItem.Type.HELMET, 2);
            }),
            9,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ModItems.STEEL_INGOT.value()),
            List.of(),
            1F, 0)
    );
}
