package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.init.ModRarities
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.init.ModTags.commonItemTag
import com.atsuishio.superbwarfare.item.misc.PerkItem
import com.atsuishio.superbwarfare.perk.Perk
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.ItemTagsProvider
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.concurrent.CompletableFuture

private typealias PerkRegistry = DeferredHolder<Perk, out Perk>

class ModItemTagProvider(
    packOutput: PackOutput,
    providerCompletableFuture: CompletableFuture<HolderLookup.Provider>,
    tagLookupCompletableFuture: CompletableFuture<TagLookup<Block>>,
    existingFileHelper: ExistingFileHelper
) : ItemTagsProvider(packOutput, providerCompletableFuture, tagLookupCompletableFuture, Mod.MODID, existingFileHelper) {
    override fun addTags(pProvider: HolderLookup.Provider) {
        this.tag(Tags.Items.DUSTS).addTags(
            commonItemTag("dusts/coal_coke"),
            commonItemTag("dusts/coal"),
            commonItemTag("dusts/iron"),
            commonItemTag("dusts/tungsten"),
            commonItemTag("dusts/scheelite"),
            commonItemTag("dusts/sulfur")
        )
        this.tag(commonItemTag("dusts/coal_coke")).add(ModItems.COAL_POWDER.get())
        this.tag(commonItemTag("dusts/coal")).add(ModItems.COAL_POWDER.get())
        this.tag(commonItemTag("dusts/iron")).add(ModItems.IRON_POWDER.get())
        this.tag(commonItemTag("dusts/tungsten")).add(ModItems.TUNGSTEN_POWDER.get())
        this.tag(commonItemTag("dusts/scheelite")).add(ModItems.TUNGSTEN_POWDER.get())
        this.tag(commonItemTag("dusts/sulfur")).add(ModItems.SULFUR.get())

        this.tag(Tags.Items.GEMS).addTags(
            commonItemTag("gems/niter")
        )
        this.tag(commonItemTag("gems/niter")).add(ModItems.NITER.get())

        this.tag(Tags.Items.INGOTS).addTags(
            commonItemTag("ingots/lead"),
            commonItemTag("ingots/steel"),
            commonItemTag("ingots/tungsten"),
            commonItemTag("ingots/silver"),
            commonItemTag("ingots/scheelite"),
            commonItemTag("ingots/uranium")
        )
        this.tag(commonItemTag("ingots/lead")).add(ModItems.LEAD_INGOT.get())
        this.tag(commonItemTag("ingots/steel")).add(ModItems.STEEL_INGOT.get())
        this.tag(commonItemTag("ingots/tungsten")).add(ModItems.TUNGSTEN_INGOT.get())
        // 这个tag仅用于其他mod配方兼容，自己家配方不用这个
        this.tag(commonItemTag("ingots/scheelite")).add(ModItems.TUNGSTEN_INGOT.get())
        this.tag(commonItemTag("ingots/silver")).add(ModItems.SILVER_INGOT.get())
        this.tag(commonItemTag("ingots/uranium")).add(ModItems.URANIUM_INGOT.get())

        this.tag(ModTags.Items.INGOTS_CEMENTED_CARBIDE).add(ModItems.CEMENTED_CARBIDE_INGOT.get())

        this.tag(Tags.Items.STORAGE_BLOCKS).addTags(
            commonItemTag("storage_blocks/lead"),
            commonItemTag("storage_blocks/steel"),
            commonItemTag("storage_blocks/tungsten"),
            commonItemTag("storage_blocks/silver"),
            commonItemTag("storage_blocks/uranium"),
            commonItemTag("storage_blocks/sulfur"),
            commonItemTag("storage_blocks/niter"),
            commonItemTag("storage_blocks/raw_lead"),
            commonItemTag("storage_blocks/raw_tungsten"),
            commonItemTag("storage_blocks/raw_silver"),
            commonItemTag("storage_blocks/raw_scheelite"),
            commonItemTag("storage_blocks/raw_uranium"),
        )
        this.tag(commonItemTag("storage_blocks/lead")).add(ModItems.LEAD_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/steel")).add(ModItems.STEEL_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/tungsten")).add(ModItems.TUNGSTEN_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/scheelite")).add(ModItems.TUNGSTEN_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/silver")).add(ModItems.SILVER_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/uranium")).add(ModItems.URANIUM_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/sulfur")).add(ModItems.SULFUR_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/niter")).add(ModItems.NITER_BLOCK.get())

        this.tag(commonItemTag("storage_blocks/raw_lead")).add(ModItems.RAW_GALENA_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/raw_tungsten")).add(ModItems.RAW_SCHEELITE_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/raw_scheelite")).add(ModItems.RAW_SCHEELITE_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/raw_silver")).add(ModItems.RAW_SILVER_BLOCK.get())
        this.tag(commonItemTag("storage_blocks/raw_uranium")).add(ModItems.RAW_URANIUM_BLOCK.get())

        this.tag(ModTags.Items.STORAGE_BLOCK_CEMENTED_CARBIDE).add(ModItems.CEMENTED_CARBIDE_BLOCK.get())

        this.tag(Tags.Items.ORES)
            .addTags(
                commonItemTag("ores/lead"),
                commonItemTag("ores/tungsten"),
                commonItemTag("ores/scheelite"),
                commonItemTag("ores/silver"),
                commonItemTag("ores/uranium"),
                commonItemTag("ores/sulfur"),
                commonItemTag("ores/niter")
            )
        this.tag(commonItemTag("ores/lead")).add(ModItems.GALENA_ORE.get(), ModItems.DEEPSLATE_GALENA_ORE.get())
        this.tag(commonItemTag("ores/tungsten"))
            .add(ModItems.SCHEELITE_ORE.get(), ModItems.DEEPSLATE_SCHEELITE_ORE.get())
        this.tag(commonItemTag("ores/scheelite"))
            .add(ModItems.SCHEELITE_ORE.get(), ModItems.DEEPSLATE_SCHEELITE_ORE.get())
        this.tag(commonItemTag("ores/silver")).add(ModItems.SILVER_ORE.get(), ModItems.DEEPSLATE_SILVER_ORE.get())
        this.tag(commonItemTag("ores/uranium")).add(ModItems.URANIUM_ORE.get(), ModItems.DEEPSLATE_URANIUM_ORE.get())
        this.tag(commonItemTag("ores/sulfur")).add(ModItems.SULFUR_ORE.get(), ModItems.DEEPSLATE_SULFUR_ORE.get())
        this.tag(commonItemTag("ores/niter")).add(ModItems.NITER_ORE.get(), ModItems.DEEPSLATE_NITER_ORE.get())

        this.tag(Tags.Items.RAW_MATERIALS).addTags(
            commonItemTag("raw_materials/lead"),
            commonItemTag("raw_materials/tungsten"),
            commonItemTag("raw_materials/scheelite"),
            commonItemTag("raw_materials/silver"),
            commonItemTag("raw_materials/uranium"),
        )
        this.tag(commonItemTag("raw_materials/lead")).add(ModItems.GALENA.get())
        this.tag(commonItemTag("raw_materials/tungsten")).add(ModItems.SCHEELITE.get())
        this.tag(commonItemTag("raw_materials/scheelite")).add(ModItems.SCHEELITE.get())
        this.tag(commonItemTag("raw_materials/silver")).add(ModItems.RAW_SILVER.get())
        this.tag(commonItemTag("raw_materials/uranium")).add(ModItems.RAW_URANIUM.get())

        this.tag(Tags.Items.ORE_RATES_SINGULAR).add(
            ModItems.GALENA_ORE.get(), ModItems.DEEPSLATE_GALENA_ORE.get(),
            ModItems.SCHEELITE_ORE.get(), ModItems.DEEPSLATE_SCHEELITE_ORE.get(),
            ModItems.SILVER_ORE.get(), ModItems.DEEPSLATE_SILVER_ORE.get(),
            ModItems.URANIUM_ORE.get(), ModItems.DEEPSLATE_URANIUM_ORE.get(),
            ModItems.SULFUR_ORE.get(), ModItems.DEEPSLATE_SULFUR_ORE.get(),
            ModItems.NITER_ORE.get(), ModItems.DEEPSLATE_NITER_ORE.get()
        )

        this.tag(Tags.Items.ORES_IN_GROUND_STONE).add(
            ModItems.GALENA_ORE.get(),
            ModItems.SCHEELITE_ORE.get(),
            ModItems.SILVER_ORE.get(),
            ModItems.URANIUM_ORE.get(),
            ModItems.SULFUR_ORE.get(),
            ModItems.NITER_ORE.get()
        )
        this.tag(Tags.Items.ORES_IN_GROUND_DEEPSLATE).add(
            ModItems.DEEPSLATE_GALENA_ORE.get(),
            ModItems.DEEPSLATE_SCHEELITE_ORE.get(),
            ModItems.DEEPSLATE_SILVER_ORE.get(),
            ModItems.DEEPSLATE_URANIUM_ORE.get(),
            ModItems.DEEPSLATE_SULFUR_ORE.get(),
            ModItems.DEEPSLATE_NITER_ORE.get()
        )

        this.tag(commonItemTag("plates"))
            .addTags(commonItemTag("plates/copper"), commonItemTag("plates/steel"), commonItemTag("plates/plastic"))
        this.tag(commonItemTag("plates/copper")).add(ModItems.COPPER_PLATE.get())
        this.tag(commonItemTag("plates/steel")).add(ModItems.STEEL_PLATE.get())
        this.tag(commonItemTag("plates/plastic")).add(ModItems.ENGINEERING_PLASTIC.get())

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
            ModItems.M_1897.get(),
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
        this.tag(ModTags.Items.SHOTGUN)
            .add(ModItems.HOMEMADE_SHOTGUN.get(), ModItems.M_870.get(), ModItems.M_1897.get(), ModItems.AA_12.get())
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
            ModItems.M_1897_BLUEPRINT.get()
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
            ModItems.SENTINEL_BLUEPRINT.get(),
            ModItems.HUNTING_RIFLE_BLUEPRINT.get()
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
            ModItems.CROWBAR.get(),
            ModItems.CEMENTED_CARBIDE_SWORD.get()
        ).addTag(ModTags.Items.HAMMER)
        this.tag(ItemTags.SWORD_ENCHANTABLE).add(
            ModItems.MILITARY_SHOVEL.get(),
            ModItems.KNIFE.get(),
            ModItems.T_BATON.get(),
            ModItems.ELECTRIC_BATON.get(),
            ModItems.STEEL_PIPE.get(),
            ModItems.CROWBAR.get(),
            ModItems.CEMENTED_CARBIDE_SWORD.get()
        ).addTag(ModTags.Items.HAMMER)

        this.tag(ItemTags.AXES).add(ModItems.MILITARY_SHOVEL.get(), ModItems.CEMENTED_CARBIDE_AXE.get())
        this.tag(ItemTags.SHOVELS).add(ModItems.MILITARY_SHOVEL.get(), ModItems.CEMENTED_CARBIDE_SHOVEL.get())
        this.tag(ItemTags.HOES).add(ModItems.MILITARY_SHOVEL.get(), ModItems.CEMENTED_CARBIDE_HOE.get())
        this.tag(ItemTags.PICKAXES).add(ModItems.CEMENTED_CARBIDE_PICKAXE.get())

        this.tag(ItemTags.MINING_ENCHANTABLE).add(
            ModItems.MILITARY_SHOVEL.get(),
            ModItems.CEMENTED_CARBIDE_AXE.get(),
            ModItems.CEMENTED_CARBIDE_SHOVEL.get(),
            ModItems.CEMENTED_CARBIDE_PICKAXE.get()
        )
        this.tag(ItemTags.VANISHING_ENCHANTABLE).add(
            ModItems.MILITARY_SHOVEL.get(),
            ModItems.CEMENTED_CARBIDE_AXE.get(),
            ModItems.CEMENTED_CARBIDE_SHOVEL.get(),
            ModItems.CEMENTED_CARBIDE_PICKAXE.get(),
            ModItems.CEMENTED_CARBIDE_HOE.get(),
            ModItems.CEMENTED_CARBIDE_SWORD.get()
        )
        this.tag(ItemTags.DURABILITY_ENCHANTABLE).add(
            ModItems.MILITARY_SHOVEL.get(),
            ModItems.CEMENTED_CARBIDE_AXE.get(),
            ModItems.CEMENTED_CARBIDE_SHOVEL.get(),
            ModItems.CEMENTED_CARBIDE_PICKAXE.get(),
            ModItems.CEMENTED_CARBIDE_HOE.get(),
            ModItems.CEMENTED_CARBIDE_SWORD.get()
        )

        this.addPerkTags()
        this.addAttachmentTags()
    }

    private fun addPerkTags() {
        ModItems.PERKS.entries.forEach {
            val item = it.get()
            if (item is PerkItem) {
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

        this.tag(ModTags.Items.RESEARCHABLE_AMMO_PERK_COMMON).add(
            perkItem(ModPerks.AQUA_BULLET),
            perkItem(ModPerks.POISONOUS_BULLET),
            perkItem(ModPerks.INCENDIARY_BULLET),
            perkItem(ModPerks.JHP_BULLET),
            perkItem(ModPerks.LONGER_WIRE),
            perkItem(ModPerks.RIOT_BULLET),
            perkItem(ModPerks.SILVER_BULLET)
        )
        this.tag(ModTags.Items.RESEARCHABLE_AMMO_PERK_RARE).add(
            perkItem(ModPerks.BLADE_BULLET),
            perkItem(ModPerks.CUPID_ARROW),
            perkItem(ModPerks.HE_BULLET),
            perkItem(ModPerks.MICRO_MISSILE),
            perkItem(ModPerks.AP_BULLET)
        )
        this.tag(ModTags.Items.RESEARCHABLE_AMMO_PERK_EPIC).add(
            perkItem(ModPerks.PHASE_PENETRATING_BULLET),
            perkItem(ModPerks.PHOSPHORUS_FLAME_BULLET)
        )

        this.tag(ModTags.Items.RESEARCHABLE_FUNCTIONAL_PERK_COMMON).add(
            perkItem(ModPerks.CAST_NO_SHADOWS),
            perkItem(ModPerks.EAGER_EDGE),
            perkItem(ModPerks.FIELD_DOCTOR),
            perkItem(ModPerks.TRIPLE_TAP),
            perkItem(ModPerks.POWERFUL_ATTRACTION),
            perkItem(ModPerks.QUICKDRAW),
            perkItem(ModPerks.SNAPSHOT_SIGHTS)
        )
        this.tag(ModTags.Items.RESEARCHABLE_FUNCTIONAL_PERK_RARE).add(
            perkItem(ModPerks.POWERFUL_COOLER),
            perkItem(ModPerks.SUBSISTENCE),
            perkItem(ModPerks.FOURTH_TIMES_CHARM),
            perkItem(ModPerks.HEAL_CLIP),
            perkItem(ModPerks.REGENERATION)
        )
        this.tag(ModTags.Items.RESEARCHABLE_FUNCTIONAL_PERK_EPIC).add(
            perkItem(ModPerks.BACKPACK_LINKED_MAGAZINE),
            perkItem(ModPerks.ADRENALINE_RUSH),
            perkItem(ModPerks.TURBO_CHARGER)
        )
        this.tag(ModTags.Items.RESEARCHABLE_FUNCTIONAL_PERK_LEGENDARY).add(
            perkItem(ModPerks.INTELLIGENT_CHIP)
        )

        this.tag(ModTags.Items.RESEARCHABLE_DAMAGE_PERK_COMMON).add(
            perkItem(ModPerks.BRAIN_STORM),
            perkItem(ModPerks.DESPERADO),
            perkItem(ModPerks.FIREFLY),
            perkItem(ModPerks.GUTSHOT_STRAIGHT),
            perkItem(ModPerks.HEAD_SEEKER),
            perkItem(ModPerks.HIGH_IMPACT_RESERVES),
            perkItem(ModPerks.MAGNIFICENT_HOWL),
            perkItem(ModPerks.VOLT_OVERLOAD)
        )
        this.tag(ModTags.Items.RESEARCHABLE_DAMAGE_PERK_RARE).add(
            perkItem(ModPerks.KILLING_TALLY),
            perkItem(ModPerks.KILL_CLIP),
            perkItem(ModPerks.MONSTER_HUNTER),
            perkItem(ModPerks.ONE_TWO_PUNCH),
            perkItem(ModPerks.STEADY_RESOLVE),
            perkItem(ModPerks.VORPAL_WEAPON)
        )
        this.tag(ModTags.Items.RESEARCHABLE_DAMAGE_PERK_EPIC).add(
            perkItem(ModPerks.FAIR_MEANS),
            perkItem(ModPerks.TARGET_LOCK),
            perkItem(ModPerks.BATTLE_OF_WITS)
        )
        this.tag(ModTags.Items.RESEARCHABLE_DAMAGE_PERK_LEGENDARY).add(
            perkItem(ModPerks.SOUL_REAVER)
        )
    }

    private fun perkItem(perk: PerkRegistry): Item {
        return ModItems.PERK_ITEMS[perk]!!.get()
    }

    private fun addAttachmentTags() {
        this.tag(ModTags.Items.ATTACHMENT).addTags(
            ModTags.Items.ATTACHMENT_SCOPE,
            ModTags.Items.ATTACHMENT_MAGAZINE,
            ModTags.Items.ATTACHMENT_BARREL,
            ModTags.Items.ATTACHMENT_STOCK,
            ModTags.Items.ATTACHMENT_GRIP
        )

        this.addAttachmentTypeTags(
            ModTags.Items.ATTACHMENT_STOCK,
            ModTags.Items.ATTACHMENT_STOCK_COMMON,
            ModTags.Items.ATTACHMENT_STOCK_RARE,
            ModTags.Items.ATTACHMENT_STOCK_EPIC,
            ModTags.Items.ATTACHMENT_STOCK_LEGENDARY,
            ModTags.Items.ATTACHMENT_STOCK_SUPERB,
            ModTags.Items.ATTACHMENT_STOCK_VIRTUAL
        )
        this.addAttachmentItems(
            ModTags.Items.ATTACHMENT_STOCK_COMMON,
            ModTags.Items.ATTACHMENT_STOCK_RARE,
            ModTags.Items.ATTACHMENT_STOCK_EPIC,
            ModTags.Items.ATTACHMENT_STOCK_LEGENDARY,
            ModTags.Items.ATTACHMENT_STOCK_SUPERB,
            ModTags.Items.ATTACHMENT_STOCK_VIRTUAL,
            listOf(
                ModItems.OEM_STOCK_LIGHT,
                ModItems.OEM_STOCK_HEAVY,
                ModItems.STOCK_UBR,
                ModItems.STOCK_PRS,
                ModItems.STOCK_AR_TACTICAL,
                ModItems.STOCK_ZENIT_PT_1,
                ModItems.STOCK_STR,
                ModItems.STOCK_ELITE
            )
        )

        this.addAttachmentTypeTags(
            ModTags.Items.ATTACHMENT_MAGAZINE,
            ModTags.Items.ATTACHMENT_MAGAZINE_COMMON,
            ModTags.Items.ATTACHMENT_MAGAZINE_RARE,
            ModTags.Items.ATTACHMENT_MAGAZINE_EPIC,
            ModTags.Items.ATTACHMENT_MAGAZINE_LEGENDARY,
            ModTags.Items.ATTACHMENT_MAGAZINE_SUPERB,
            ModTags.Items.ATTACHMENT_MAGAZINE_VIRTUAL
        )
        this.addAttachmentItems(
            ModTags.Items.ATTACHMENT_MAGAZINE_COMMON,
            ModTags.Items.ATTACHMENT_MAGAZINE_RARE,
            ModTags.Items.ATTACHMENT_MAGAZINE_EPIC,
            ModTags.Items.ATTACHMENT_MAGAZINE_LEGENDARY,
            ModTags.Items.ATTACHMENT_MAGAZINE_SUPERB,
            ModTags.Items.ATTACHMENT_MAGAZINE_VIRTUAL,
            listOf(
                ModItems.MAGAZINE_EXTEND,
                ModItems.MAGAZINE_EXTEND_PRO
            )
        )

        this.addAttachmentTypeTags(
            ModTags.Items.ATTACHMENT_BARREL,
            ModTags.Items.ATTACHMENT_BARREL_COMMON,
            ModTags.Items.ATTACHMENT_BARREL_RARE,
            ModTags.Items.ATTACHMENT_BARREL_EPIC,
            ModTags.Items.ATTACHMENT_BARREL_LEGENDARY,
            ModTags.Items.ATTACHMENT_BARREL_SUPERB,
            ModTags.Items.ATTACHMENT_BARREL_VIRTUAL
        )
        this.addAttachmentItems(
            ModTags.Items.ATTACHMENT_BARREL_COMMON,
            ModTags.Items.ATTACHMENT_BARREL_RARE,
            ModTags.Items.ATTACHMENT_BARREL_EPIC,
            ModTags.Items.ATTACHMENT_BARREL_LEGENDARY,
            ModTags.Items.ATTACHMENT_BARREL_SUPERB,
            ModTags.Items.ATTACHMENT_BARREL_VIRTUAL,
            listOf(
                ModItems.MEOWLENCER,
                ModItems.HISSILENCER,
                ModItems.SILAOWUNCER,
                ModItems.RU_SILENCER,
                ModItems.MUZZLE_BRAKE_RU,
                ModItems.MUZZLE_BRAKE_AR,
                ModItems.MUZZLE_ZENIT_DTK_1,
                ModItems.MUZZLE_COMPENSATOR_R_17,
                ModItems.SILENCER_SUPERB_12,
                ModItems.SILENCER_50_CAL,
                ModItems.AR_SILENCER,
                ModItems.HANDGUN_SILENCER,
                ModItems.OSPREY_SILENCER,
                ModItems.LOUDSPEAKER,
                ModItems.OSPREY_SILENCER_TRACHELIUM,
                ModItems.SILENCER_QINGLAN,
                ModItems.TRIANGULAR_MUZZLE,
                ModItems.LINEAR_MUZZLE,
                ModItems.CROSS_MUZZLE,
                ModItems.STAR_MUZZLE,
                ModItems.LOVE_MUZZLE
            )
        )

        this.addAttachmentTypeTags(
            ModTags.Items.ATTACHMENT_SCOPE,
            ModTags.Items.ATTACHMENT_SCOPE_COMMON,
            ModTags.Items.ATTACHMENT_SCOPE_RARE,
            ModTags.Items.ATTACHMENT_SCOPE_EPIC,
            ModTags.Items.ATTACHMENT_SCOPE_LEGENDARY,
            ModTags.Items.ATTACHMENT_SCOPE_SUPERB,
            ModTags.Items.ATTACHMENT_SCOPE_VIRTUAL
        )
        this.addAttachmentItems(
            ModTags.Items.ATTACHMENT_SCOPE_COMMON,
            ModTags.Items.ATTACHMENT_SCOPE_RARE,
            ModTags.Items.ATTACHMENT_SCOPE_EPIC,
            ModTags.Items.ATTACHMENT_SCOPE_LEGENDARY,
            ModTags.Items.ATTACHMENT_SCOPE_SUPERB,
            ModTags.Items.ATTACHMENT_SCOPE_VIRTUAL,
            listOf(
                ModItems.SCOPE_COYOTE,
                ModItems.SCOPE_AIMPOINT_T2,
                ModItems.SCOPE_HANDGUN_RED_DOT,
                ModItems.SCOPE_EOTECH_551,
                ModItems.SCOPE_OKP_7,
                ModItems.SCOPE_PKAS,
                ModItems.SCOPE_BRUISER,
                ModItems.SCOPE_ACOG,
                ModItems.SCOPE_1P_78,
                ModItems.SCOPE_PU,
                ModItems.SCOPE_PK_A,
                ModItems.SCOPE_RANGER,
                ModItems.SCOPE_QMK171,
                ModItems.SCOPE_HAMR,
                ModItems.SCOPE_ELCAN_4X,
                ModItems.SCOPE_LPVO,
                ModItems.SCOPE_SNIPER
            )
        )

        this.addAttachmentTypeTags(
            ModTags.Items.ATTACHMENT_GRIP,
            ModTags.Items.ATTACHMENT_GRIP_COMMON,
            ModTags.Items.ATTACHMENT_GRIP_RARE,
            ModTags.Items.ATTACHMENT_GRIP_EPIC,
            ModTags.Items.ATTACHMENT_GRIP_LEGENDARY,
            ModTags.Items.ATTACHMENT_GRIP_SUPERB,
            ModTags.Items.ATTACHMENT_GRIP_VIRTUAL
        )
        this.addAttachmentItems(
            ModTags.Items.ATTACHMENT_GRIP_COMMON,
            ModTags.Items.ATTACHMENT_GRIP_RARE,
            ModTags.Items.ATTACHMENT_GRIP_EPIC,
            ModTags.Items.ATTACHMENT_GRIP_LEGENDARY,
            ModTags.Items.ATTACHMENT_GRIP_SUPERB,
            ModTags.Items.ATTACHMENT_GRIP_VIRTUAL,
            listOf(
                ModItems.VERTICAL_GRIP,
                ModItems.GRIP_RECTANGLE_VERTICAL,
                ModItems.TRIANGLE_GRIP,
                ModItems.GRIP_CQR_GEN2
            )
        )

        this.tag(ModTags.Items.ATTACHMENT_RESEARCHABLE_COMMON).addTags(
            ModTags.Items.ATTACHMENT_BARREL_COMMON,
            ModTags.Items.ATTACHMENT_GRIP_COMMON,
            ModTags.Items.ATTACHMENT_SCOPE_COMMON,
            ModTags.Items.ATTACHMENT_STOCK_COMMON,
            ModTags.Items.ATTACHMENT_MAGAZINE_COMMON
        )
        this.tag(ModTags.Items.ATTACHMENT_RESEARCHABLE_RARE).addTags(
            ModTags.Items.ATTACHMENT_BARREL_RARE,
            ModTags.Items.ATTACHMENT_GRIP_RARE,
            ModTags.Items.ATTACHMENT_SCOPE_RARE,
            ModTags.Items.ATTACHMENT_STOCK_RARE,
            ModTags.Items.ATTACHMENT_MAGAZINE_RARE
        )
        this.tag(ModTags.Items.ATTACHMENT_RESEARCHABLE_EPIC).addTags(
            ModTags.Items.ATTACHMENT_BARREL_EPIC,
            ModTags.Items.ATTACHMENT_GRIP_EPIC,
            ModTags.Items.ATTACHMENT_SCOPE_EPIC,
            ModTags.Items.ATTACHMENT_STOCK_EPIC,
            ModTags.Items.ATTACHMENT_MAGAZINE_EPIC
        )
        this.tag(ModTags.Items.ATTACHMENT_RESEARCHABLE_LEGENDARY).addTags(
            ModTags.Items.ATTACHMENT_BARREL_LEGENDARY,
            ModTags.Items.ATTACHMENT_GRIP_LEGENDARY,
            ModTags.Items.ATTACHMENT_SCOPE_LEGENDARY,
            ModTags.Items.ATTACHMENT_STOCK_LEGENDARY,
            ModTags.Items.ATTACHMENT_MAGAZINE_LEGENDARY
        )
        this.tag(ModTags.Items.ATTACHMENT_RESEARCHABLE_SUPERB).addTags(
            ModTags.Items.ATTACHMENT_BARREL_SUPERB,
            ModTags.Items.ATTACHMENT_GRIP_SUPERB,
            ModTags.Items.ATTACHMENT_SCOPE_SUPERB,
            ModTags.Items.ATTACHMENT_STOCK_SUPERB,
            ModTags.Items.ATTACHMENT_MAGAZINE_SUPERB
        )
        this.tag(ModTags.Items.ATTACHMENT_RESEARCHABLE_VIRTUAL).addTags(
            ModTags.Items.ATTACHMENT_BARREL_VIRTUAL,
            ModTags.Items.ATTACHMENT_GRIP_VIRTUAL,
            ModTags.Items.ATTACHMENT_SCOPE_VIRTUAL,
            ModTags.Items.ATTACHMENT_STOCK_VIRTUAL,
            ModTags.Items.ATTACHMENT_MAGAZINE_VIRTUAL
        )
    }

    private fun addAttachmentTypeTags(
        typeTag: TagKey<Item>,
        commonTag: TagKey<Item>,
        rareTag: TagKey<Item>,
        epicTag: TagKey<Item>,
        legendaryTag: TagKey<Item>,
        superbTag: TagKey<Item>,
        virtualTag: TagKey<Item>
    ) {
        this.tag(typeTag).addTags(commonTag, rareTag, epicTag, legendaryTag, superbTag, virtualTag)
        this.tag(commonTag)
        this.tag(rareTag)
        this.tag(epicTag)
        this.tag(legendaryTag)
        this.tag(superbTag)
        this.tag(virtualTag)
    }

    private fun addAttachmentItems(
        commonTag: TagKey<Item>,
        rareTag: TagKey<Item>,
        epicTag: TagKey<Item>,
        legendaryTag: TagKey<Item>,
        superbTag: TagKey<Item>,
        virtualTag: TagKey<Item>,
        attachments: List<DeferredHolder<Item, out Item>>
    ) {
        attachments.forEach {
            val item = it.get()
            val rarityTag = when (item.defaultInstance.getRarity()) {
                Rarity.COMMON -> commonTag
                Rarity.RARE -> rareTag
                Rarity.EPIC -> epicTag
                ModRarities.LEGENDARY -> legendaryTag
                ModRarities.SUPERB -> superbTag
                ModRarities.VIRTUAL -> virtualTag
                else -> error("Unsupported attachment rarity: ${item.defaultInstance.getRarity()}")
            }
            this.tag(rarityTag).add(item)
        }
    }
}
