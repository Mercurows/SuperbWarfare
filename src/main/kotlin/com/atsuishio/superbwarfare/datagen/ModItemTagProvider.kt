package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.init.ModTags.commonItemTag
import com.atsuishio.superbwarfare.item.misc.PerkItem
import com.atsuishio.superbwarfare.perk.Perk
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.ItemTagsProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class ModItemTagProvider(
    packOutput: PackOutput,
    providerCompletableFuture: CompletableFuture<HolderLookup.Provider>,
    tagLookupCompletableFuture: CompletableFuture<TagLookup<Block>>,
    existingFileHelper: ExistingFileHelper
) : ItemTagsProvider(packOutput, providerCompletableFuture, tagLookupCompletableFuture, Mod.MODID, existingFileHelper) {
    override fun addTags(pProvider: HolderLookup.Provider) {
        this.tag(Tags.Items.DUSTS).addTags(commonItemTag("dusts/coal_coke"), commonItemTag("dusts/tungsten"))
        this.tag(commonItemTag("dusts/coal_coke")).add(ModItems.COAL_POWDER.get())
        this.tag(commonItemTag("dusts/iron")).add(ModItems.IRON_POWDER.get())
        this.tag(commonItemTag("dusts/tungsten")).add(ModItems.TUNGSTEN_POWDER.get())

        this.tag(Tags.Items.INGOTS).addTags(
            commonItemTag("ingots/lead"),
            commonItemTag("ingots/steel"),
            commonItemTag("ingots/tungsten"),
            commonItemTag("ingots/silver")
        )
        this.tag(commonItemTag("ingots/lead")).add(ModItems.LEAD_INGOT.get())
        this.tag(commonItemTag("ingots/steel")).add(ModItems.STEEL_INGOT.get())
        this.tag(commonItemTag("ingots/tungsten")).add(ModItems.TUNGSTEN_INGOT.get())
        this.tag(commonItemTag("ingots/silver")).add(ModItems.SILVER_INGOT.get())

        this.tag(ModTags.Items.INGOTS_STEEL).addTag(commonItemTag("ingots/steel"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("dreamaticvoyage", "fukamizu_bread_ingot"))
        this.tag(ModTags.Items.INGOTS_CEMENTED_CARBIDE).add(ModItems.CEMENTED_CARBIDE_INGOT.get())
            .addOptional(ResourceLocation.fromNamespaceAndPath("dreamaticvoyage", "hqss_bread_ingot"))
        this.tag(ModTags.Items.INGOTS_LEAD).addTag(commonItemTag("ingots/lead"))

        this.tag(Tags.Items.STORAGE_BLOCKS).addTags(
            commonItemTag("storage_blocks/lead"),
            commonItemTag("storage_blocks/steel"),
            commonItemTag("storage_blocks/tungsten"),
            commonItemTag("storage_blocks/silver"),
            commonItemTag("storage_blocks/raw_lead"),
            commonItemTag("storage_blocks/raw_tungsten"),
            commonItemTag("storage_blocks/raw_silver"),
            commonItemTag("storage_blocks/raw_scheelite")
        )
        this.tag(commonItemTag("storage_blocks/lead")).add(ModItems.LEAD_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/steel")).add(ModItems.STEEL_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/tungsten")).add(ModItems.TUNGSTEN_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/silver")).add(ModItems.SILVER_BLOCK.get())

        this.tag(commonItemTag("storage_blocks/raw_lead")).add(ModItems.RAW_GALENA_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/raw_tungsten")).add(ModItems.RAW_SCHEELITE_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/raw_silver")).add(ModItems.RAW_SILVER_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/raw_scheelite")).add(ModItems.RAW_SCHEELITE_BLOCK.get())

        this.tag(ModTags.Items.STORAGE_BLOCK_STEEL).addTag(commonItemTag("storage_blocks/steel"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("dreamaticvoyage", "fukamizu_bread_bricks"))
        this.tag(ModTags.Items.STORAGE_BLOCK_CEMENTED_CARBIDE).add(ModItems.CEMENTED_CARBIDE_BLOCK.get())
            .addOptional(ResourceLocation.fromNamespaceAndPath("dreamaticvoyage", "hqss_bread_bricks"))

        this.tag(Tags.Items.ORES)
            .addTags(commonItemTag("ores/lead"), commonItemTag("ores/tungsten"), commonItemTag("ores/silver"))
        this.tag(commonItemTag("ores/lead")).add(ModItems.GALENA_ORE.get(), ModItems.DEEPSLATE_GALENA_ORE.get())
        this.tag(commonItemTag("ores/tungsten"))
            .add(ModItems.SCHEELITE_ORE.get(), ModItems.DEEPSLATE_SCHEELITE_ORE.get())
        this.tag(commonItemTag("ores/silver")).add(ModItems.SILVER_ORE.get(), ModItems.DEEPSLATE_SILVER_ORE.get())

        this.tag(Tags.Items.RAW_MATERIALS).addTags(
            commonItemTag("raw_materials/lead"),
            commonItemTag("raw_materials/tungsten"),
            commonItemTag("raw_materials/silver")
        )
        this.tag(commonItemTag("raw_materials/lead")).add(ModItems.GALENA.get())
        this.tag(commonItemTag("raw_materials/tungsten")).add(ModItems.SCHEELITE.get())
        this.tag(commonItemTag("raw_materials/silver")).add(ModItems.RAW_SILVER.get())

        // 这个tag仅用于其他mod配方兼容，自己家配方不用这个
        this.tag(commonItemTag("ingots/scheelite")).add(ModItems.TUNGSTEN_INGOT.get())
        this.tag(commonItemTag("ores/scheelite"))
            .add(ModItems.SCHEELITE_ORE.get(), ModItems.DEEPSLATE_SCHEELITE_ORE.get())
        this.tag(commonItemTag("raw_materials/scheelite")).add(ModItems.SCHEELITE.get())
        this.tag(commonItemTag("dusts/scheelite")).add(ModItems.TUNGSTEN_POWDER.get())
        this.tag(commonItemTag("storage_blocks/scheelite")).add(ModItems.TUNGSTEN_BLOCK.get())

        this.tag(Tags.Items.ORE_RATES_SINGULAR).add(
            ModItems.GALENA_ORE.get(), ModItems.DEEPSLATE_GALENA_ORE.get(),
            ModItems.SCHEELITE_ORE.get(), ModItems.DEEPSLATE_SCHEELITE_ORE.get(),
            ModItems.SILVER_ORE.get(), ModItems.DEEPSLATE_SILVER_ORE.get()
        )

        this.tag(Tags.Items.ORES_IN_GROUND_STONE)
            .add(ModItems.GALENA_ORE.get(), ModItems.SCHEELITE_ORE.get(), ModItems.SILVER_ORE.get())
        this.tag(Tags.Items.ORES_IN_GROUND_DEEPSLATE).add(
            ModItems.DEEPSLATE_GALENA_ORE.get(),
            ModItems.DEEPSLATE_SCHEELITE_ORE.get(),
            ModItems.DEEPSLATE_SILVER_ORE.get()
        )

        this.tag(commonItemTag("plates")).addTags(commonItemTag("plates/copper"))
        this.tag(commonItemTag("plates/copper")).add(ModItems.COPPER_PLATE.get())

        this.tag(commonItemTag("tools/crowbar")).add(ModItems.CROWBAR.get())

        this.tag(ModTags.Items.HAMMER).add(
            ModItems.HAMMER.get(),
            ModItems.GOLDEN_HAMMER.get(),
            ModItems.STEEL_HAMMER.get(),
            ModItems.DIAMOND_HAMMER.get(),
            ModItems.CEMENTED_CARBIDE_HAMMER.get(),
            ModItems.NETHERITE_HAMMER.get()
        )
        this.tag(ModTags.Items.TOOLS_HAMMER).addTag(ModTags.Items.HAMMER)

        this.tag(Tags.Items.ARMORS).add(
            ModItems.RU_HELMET_6B47.get(),
            ModItems.US_HELMET_PASGT.get(),
            ModItems.GE_HELMET_M_35.get(),
            ModItems.RU_CHEST_6B43.get(),
            ModItems.US_CHEST_IOTV.get()
        )

        this.tag(ItemTags.CHEST_ARMOR).add(
            ModItems.RU_CHEST_6B43.get(),
            ModItems.US_CHEST_IOTV.get()
        )
        this.tag(ItemTags.CHEST_ARMOR_ENCHANTABLE).add(
            ModItems.RU_CHEST_6B43.get(),
            ModItems.US_CHEST_IOTV.get()
        )

        this.tag(ItemTags.HEAD_ARMOR).add(
            ModItems.RU_HELMET_6B47.get(),
            ModItems.US_HELMET_PASGT.get(),
            ModItems.GE_HELMET_M_35.get()
        )
        this.tag(ItemTags.HEAD_ARMOR_ENCHANTABLE).add(
            ModItems.RU_HELMET_6B47.get(),
            ModItems.US_HELMET_PASGT.get(),
            ModItems.GE_HELMET_M_35.get()
        )

        this.tag(ModTags.Items.RESEARCH_FUEL).add(Items.GUNPOWDER, Items.GLOWSTONE_DUST, Items.REDSTONE, Items.SUGAR)

        // 专门给其他模组添加动画用的枪械武器分类 tag
        this.tag(ModTags.Items.ANIMATED_PISTOL).add(
            ModItems.TASER.get(),
            ModItems.GLOCK_17.get(),
            ModItems.GLOCK_18.get(),
            ModItems.MP_443.get(),
            ModItems.M_1911.get(),
            ModItems.TRACHELIUM.get(),
            ModItems.REPAIR_TOOL.get()
        )
        this.tag(ModTags.Items.ANIMATED_SNIPER).add(
            ModItems.MOSIN_NAGANT.get(),
            ModItems.SVD.get(),
            ModItems.AWM.get(),
            ModItems.NTW_20.get()
        )
        this.tag(ModTags.Items.ANIMATED_RIFLE).add(
            ModItems.AK_47.get(),
            ModItems.AK_12.get(),
            ModItems.SKS.get(),
            ModItems.M_4.get(),
            ModItems.HK_416.get(),
            ModItems.QBZ_95.get(),
            ModItems.QBZ_191.get(),
            ModItems.INSIDIOUS.get(),
            ModItems.MK_14.get(),
            ModItems.MARLIN.get(),
            ModItems.K_98.get(),
            ModItems.M_98B.get(),
            ModItems.SENTINEL.get(),
            ModItems.HUNTING_RIFLE.get(),
            ModItems.QL_1031.get()
        )
        this.tag(ModTags.Items.ANIMATED_SHOTGUN).add(
            ModItems.HOMEMADE_SHOTGUN.get(),
            ModItems.M_870.get(),
            ModItems.AA_12.get(),
            ModItems.M_79.get(),
            ModItems.SECONDARY_CATACLYSM.get()
        )
        this.tag(ModTags.Items.ANIMATED_SMG).add(
            ModItems.MP_5.get(),
            ModItems.VECTOR.get()
        )
        this.tag(ModTags.Items.ANIMATED_RPG).add(
            ModItems.RPG.get(),
            ModItems.JAVELIN.get(),
            ModItems.IGLA_9K38.get()
        )
        this.tag(ModTags.Items.ANIMATED_MG).add(
            ModItems.DEVOTION.get(),
            ModItems.RPK.get(),
            ModItems.M_60.get(),
            ModItems.M_2_HB.get()
        )
        this.tag(ModTags.Items.ANIMATED_MINIGUN).add(
            ModItems.MINIGUN.get()
        )

        ModItems.GUNS.getEntries().forEach {
            this.tag(ModTags.Items.GUN).add(it.get())
        }

        this.tag(ModTags.Items.SMG).add(ModItems.VECTOR.get(), ModItems.MP_5.get())
        this.tag(ModTags.Items.RIFLE).add(
            ModItems.M_4.get(),
            ModItems.HK_416.get(),
            ModItems.SKS.get(),
            ModItems.MK_14.get(),
            ModItems.MARLIN.get(),
            ModItems.AK_47.get(),
            ModItems.AK_12.get(),
            ModItems.QBZ_95.get(),
            ModItems.QBZ_191.get()
        )
        this.tag(ModTags.Items.SNIPER_RIFLE).add(
            ModItems.HUNTING_RIFLE.get(),
            ModItems.SENTINEL.get(),
            ModItems.NTW_20.get(),
            ModItems.SVD.get(),
            ModItems.M_98B.get(),
            ModItems.K_98.get(),
            ModItems.MOSIN_NAGANT.get(),
            ModItems.AWM.get(),
            ModItems.QL_1031.get()
        )
        this.tag(ModTags.Items.SHOTGUN).add(ModItems.HOMEMADE_SHOTGUN.get(), ModItems.M_870.get(), ModItems.AA_12.get())
        this.tag(ModTags.Items.MACHINE_GUN).add(ModItems.MINIGUN.get(), ModItems.M_2_HB.get())
        this.tag(ModTags.Items.LAUNCHER).add(
            ModItems.RPG.get(), ModItems.JAVELIN.get(), ModItems.IGLA_9K38.get(),
            ModItems.M_79.get(), ModItems.SECONDARY_CATACLYSM.get(), ModItems.SUPER_STAR_SHOOTER.get()
        )

        this.tag(ModTags.Items.MILITARY_ARMOR).add(ModItems.RU_CHEST_6B43.get(), ModItems.US_CHEST_IOTV.get())

        this.tag(ModTags.Items.BLUEPRINT).addTags(
            ModTags.Items.COMMON_BLUEPRINT, ModTags.Items.RARE_BLUEPRINT, ModTags.Items.EPIC_BLUEPRINT,
            ModTags.Items.LEGENDARY_BLUEPRINT, ModTags.Items.CANNON_BLUEPRINT
        )

        this.tag(ModTags.Items.COMMON_BLUEPRINT).add(
            ModItems.GLOCK_17_BLUEPRINT.get(), ModItems.MP_443_BLUEPRINT.get(), ModItems.MARLIN_BLUEPRINT.get(),
            ModItems.TASER_BLUEPRINT.get(), ModItems.M_1911_BLUEPRINT.get()
        )

        this.tag(ModTags.Items.RARE_BLUEPRINT).add(
            ModItems.GLOCK_18_BLUEPRINT.get(),
            ModItems.M_79_BLUEPRINT.get(),
            ModItems.M_4_BLUEPRINT.get(),
            ModItems.SKS_BLUEPRINT.get(),
            ModItems.M_870_BLUEPRINT.get(),
            ModItems.AK_47_BLUEPRINT.get(),
            ModItems.K_98_BLUEPRINT.get(),
            ModItems.MOSIN_NAGANT_BLUEPRINT.get(),
            ModItems.M_2_HB_BLUEPRINT.get(),
            ModItems.HK_416_BLUEPRINT.get(),
            ModItems.AK_12_BLUEPRINT.get(),
            ModItems.QBZ_95_BLUEPRINT.get(),
            ModItems.RPG_BLUEPRINT.get(),
            ModItems.HUNTING_RIFLE_BLUEPRINT.get()
        )

        this.tag(ModTags.Items.EPIC_BLUEPRINT).add(
            ModItems.BOCEK_BLUEPRINT.get(),
            ModItems.RPK_BLUEPRINT.get(),
            ModItems.VECTOR_BLUEPRINT.get(),
            ModItems.MK_14_BLUEPRINT.get(),
            ModItems.M_60_BLUEPRINT.get(),
            ModItems.SVD_BLUEPRINT.get(),
            ModItems.M_98B_BLUEPRINT.get(),
            ModItems.DEVOTION_BLUEPRINT.get(),
            ModItems.INSIDIOUS_BLUEPRINT.get(),
            ModItems.QBZ_191_BLUEPRINT.get(),
            ModItems.AWM_BLUEPRINT.get(),
            ModItems.IGLA_BLUEPRINT.get(),
            ModItems.SENTINEL_BLUEPRINT.get()
        )

        this.tag(ModTags.Items.LEGENDARY_BLUEPRINT).add(
            ModItems.AA_12_BLUEPRINT.get(),
            ModItems.NTW_20_BLUEPRINT.get(),
            ModItems.MINIGUN_BLUEPRINT.get(),
            ModItems.JAVELIN_BLUEPRINT.get(),
            ModItems.MK_42_BLUEPRINT.get(),
            ModItems.MLE_1934_BLUEPRINT.get(),
            ModItems.ANNIHILATOR_BLUEPRINT.get(),
            ModItems.HPJ_11_BLUEPRINT.get(),
            ModItems.BL_132_BLUEPRINT.get()
        )

        this.tag(ModTags.Items.SUPERB_BLUEPRINT).add(ModItems.SUPER_STAR_SHOOTER_BLUEPRINT.get())

        this.tag(ModTags.Items.VIRTUAL_BLUEPRINT).add(
            ModItems.TRACHELIUM_BLUEPRINT.get(),
            ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get(),
            ModItems.QL_1031_BLUEPRINT.get()
        )

        this.tag(ModTags.Items.CANNON_BLUEPRINT).add(
            ModItems.MK_42_BLUEPRINT.get(), ModItems.MLE_1934_BLUEPRINT.get(), ModItems.ANNIHILATOR_BLUEPRINT.get(),
            ModItems.HPJ_11_BLUEPRINT.get(), ModItems.BL_132_BLUEPRINT.get()
        )

        this.tag(ModTags.Items.ENLARGED_COMMON_BLUEPRINT)
            .addTags(ModTags.Items.COMMON_BLUEPRINT, ModTags.Items.RARE_BLUEPRINT)
        this.tag(ModTags.Items.ENLARGED_RARE_BLUEPRINT)
            .addTags(ModTags.Items.RARE_BLUEPRINT, ModTags.Items.EPIC_BLUEPRINT)
        this.tag(ModTags.Items.ENLARGED_EPIC_BLUEPRINT)
            .addTags(ModTags.Items.EPIC_BLUEPRINT, ModTags.Items.LEGENDARY_BLUEPRINT)
        this.tag(ModTags.Items.ENLARGED_LEGENDARY_BLUEPRINT)
            .addTags(ModTags.Items.LEGENDARY_BLUEPRINT, ModTags.Items.SUPERB_BLUEPRINT)

        this.tag(ItemTags.SWORDS).add(
            ModItems.MILITARY_SHOVEL.get(),
            ModItems.KNIFE.get(),
            ModItems.T_BATON.get(),
            ModItems.ELECTRIC_BATON.get(),
            ModItems.STEEL_PIPE.get(),
            ModItems.CROWBAR.get()
        ).addTag(ModTags.Items.HAMMER)
        this.tag(ItemTags.SWORD_ENCHANTABLE).add(
            ModItems.MILITARY_SHOVEL.get(),
            ModItems.KNIFE.get(),
            ModItems.T_BATON.get(),
            ModItems.ELECTRIC_BATON.get(),
            ModItems.STEEL_PIPE.get(),
            ModItems.CROWBAR.get()
        ).addTag(ModTags.Items.HAMMER)

        this.tag(ItemTags.AXES).add(ModItems.MILITARY_SHOVEL.get())
        this.tag(ItemTags.SHOVELS).add(ModItems.MILITARY_SHOVEL.get())
        this.tag(ItemTags.HOES).add(ModItems.MILITARY_SHOVEL.get())
        this.tag(ItemTags.MINING_ENCHANTABLE).add(ModItems.MILITARY_SHOVEL.get())
        this.tag(ItemTags.VANISHING_ENCHANTABLE).add(ModItems.MILITARY_SHOVEL.get())
        this.tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.MILITARY_SHOVEL.get())

        ModItems.PERKS.entries.forEach {
            val item = it.get()
            if (item is PerkItem<*>) {
                when (item.perk.type) {
                    Perk.Type.AMMO -> {
                        this.tag(ModTags.Items.AMMO_PERK).add(item)
                        if (item.perk != ModPerks.BEAST_BULLET.get()) {
                            this.tag(ModTags.Items.RESEARCHABLE_AMMO_PERK).add(item)
                        }
                    }

                    Perk.Type.FUNCTIONAL -> {
                        this.tag(ModTags.Items.FUNCTIONAL_PERK).add(item)
                        this.tag(ModTags.Items.RESEARCHABLE_FUNCTIONAL_PERK).add(item)
                    }

                    Perk.Type.DAMAGE -> {
                        this.tag(ModTags.Items.DAMAGE_PERK).add(item)
                        this.tag(ModTags.Items.RESEARCHABLE_DAMAGE_PERK).add(item)
                    }
                }
            }
        }
    }
}
