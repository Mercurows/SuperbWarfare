package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {

    public ModItemTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> providerCompletableFuture,
                              CompletableFuture<TagLookup<Block>> tagLookupCompletableFuture, @Nullable ExistingFileHelper existingFileHelper) {
        super(packOutput, providerCompletableFuture, tagLookupCompletableFuture, Mod.MODID, existingFileHelper);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addTags(HolderLookup.@NotNull Provider pProvider) {
        this.tag(Tags.Items.DUSTS).addTags(cTag("dusts/coal_coke"), cTag("dusts/tungsten"));
        this.tag(cTag("dusts/coal_coke")).add(ModItems.COAL_POWDER.get());
        this.tag(cTag("dusts/iron")).add(ModItems.IRON_POWDER.get());
        this.tag(cTag("dusts/tungsten")).add(ModItems.TUNGSTEN_POWDER.get());

        this.tag(Tags.Items.INGOTS).addTags(cTag("ingots/lead"), cTag("ingots/steel"), cTag("ingots/tungsten"), cTag("ingots/silver"));
        this.tag(cTag("ingots/lead")).add(ModItems.LEAD_INGOT.get());
        this.tag(cTag("ingots/steel")).add(ModItems.STEEL_INGOT.get());
        this.tag(cTag("ingots/tungsten")).add(ModItems.TUNGSTEN_INGOT.get());
        this.tag(cTag("ingots/silver")).add(ModItems.SILVER_INGOT.get());

        this.tag(ModTags.Items.INGOTS_STEEL).addTag(cTag("ingots/steel"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("dreamaticvoyage", "fukamizu_bread_ingot"));
        this.tag(ModTags.Items.INGOTS_CEMENTED_CARBIDE).add(ModItems.CEMENTED_CARBIDE_INGOT.get())
                .addOptional(ResourceLocation.fromNamespaceAndPath("dreamaticvoyage", "hqss_bread_ingot"));

        this.tag(Tags.Items.STORAGE_BLOCKS).addTags(cTag("storage_blocks/lead"), cTag("storage_blocks/steel"), cTag("storage_blocks/tungsten"), cTag("storage_blocks/silver"));
        this.tag(cTag("storage_blocks/lead")).add(ModItems.LEAD_BLOCK.get());
        this.tag(cTag("storage_blocks/steel")).add(ModItems.STEEL_BLOCK.get());
        this.tag(cTag("storage_blocks/tungsten")).add(ModItems.TUNGSTEN_BLOCK.get());
        this.tag(cTag("storage_blocks/silver")).add(ModItems.SILVER_BLOCK.get());

        this.tag(ModTags.Items.STORAGE_BLOCK_STEEL).addTag(cTag("storage_blocks/steel"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("dreamaticvoyage", "fukamizu_bread_bricks"));
        this.tag(ModTags.Items.STORAGE_BLOCK_CEMENTED_CARBIDE).add(ModItems.CEMENTED_CARBIDE_BLOCK.get())
                .addOptional(ResourceLocation.fromNamespaceAndPath("dreamaticvoyage", "hqss_bread_bricks"));

        this.tag(Tags.Items.ORES).addTags(cTag("ores/lead"), cTag("ores/tungsten"), cTag("ores/silver"));
        this.tag(cTag("ores/lead")).add(ModItems.GALENA_ORE.get(), ModItems.DEEPSLATE_GALENA_ORE.get());
        this.tag(cTag("ores/tungsten")).add(ModItems.SCHEELITE_ORE.get(), ModItems.DEEPSLATE_SCHEELITE_ORE.get());
        this.tag(cTag("ores/silver")).add(ModItems.SILVER_ORE.get(), ModItems.DEEPSLATE_SILVER_ORE.get());

        this.tag(Tags.Items.RAW_MATERIALS).addTags(cTag("raw_materials/lead"), cTag("raw_materials/tungsten"), cTag("raw_materials/silver"));
        this.tag(cTag("raw_materials/lead")).add(ModItems.GALENA.get());
        this.tag(cTag("raw_materials/tungsten")).add(ModItems.SCHEELITE.get());
        this.tag(cTag("raw_materials/silver")).add(ModItems.RAW_SILVER.get());

        this.tag(Tags.Items.ORE_RATES_SINGULAR).add(ModItems.GALENA_ORE.get(), ModItems.DEEPSLATE_GALENA_ORE.get(),
                ModItems.SCHEELITE_ORE.get(), ModItems.DEEPSLATE_SCHEELITE_ORE.get(),
                ModItems.SILVER_ORE.get(), ModItems.DEEPSLATE_SILVER_ORE.get());

        this.tag(Tags.Items.ORES_IN_GROUND_STONE).add(ModItems.GALENA_ORE.get(), ModItems.SCHEELITE_ORE.get(), ModItems.SILVER_ORE.get());
        this.tag(Tags.Items.ORES_IN_GROUND_DEEPSLATE).add(ModItems.DEEPSLATE_GALENA_ORE.get(), ModItems.DEEPSLATE_SCHEELITE_ORE.get(), ModItems.DEEPSLATE_SILVER_ORE.get());

        this.tag(cTag("plates")).addTags(cTag("plates/copper"));
        this.tag(cTag("plates/copper")).add(ModItems.COPPER_PLATE.get());

        // TODO 清理枪械Tag
        ModItems.GUNS.getEntries().forEach(registryObject -> this.tag(ModTags.Items.GUN).add(registryObject.get()));

        this.tag(ModTags.Items.SMG).add(ModItems.VECTOR.get());

        this.tag(ModTags.Items.RIFLE).add(ModItems.M_4.get(), ModItems.HK_416.get(), ModItems.SKS.get(),
                ModItems.MK_14.get(), ModItems.MARLIN.get(), ModItems.AK_47.get(), ModItems.AK_12.get(), ModItems.QBZ_95.get());

        this.tag(ModTags.Items.SNIPER_RIFLE).add(ModItems.HUNTING_RIFLE.get(), ModItems.SENTINEL.get(),
                ModItems.SVD.get(), ModItems.M_98B.get(), ModItems.K_98.get(), ModItems.MOSIN_NAGANT.get());

        this.tag(ModTags.Items.HEAVY_WEAPON).add(ModItems.NTW_20.get());

        this.tag(ModTags.Items.SHOTGUN).add(ModItems.HOMEMADE_SHOTGUN.get(), ModItems.M_870.get(), ModItems.AA_12.get());

        this.tag(ModTags.Items.NORMAL_GUN).add(ModItems.HOMEMADE_SHOTGUN.get(), ModItems.AK_47.get(), ModItems.AK_12.get(), ModItems.SVD.get(), ModItems.M_60.get(), ModItems.MK_14.get(), ModItems.VECTOR.get(),
                ModItems.SKS.get(), ModItems.RPK.get(), ModItems.HK_416.get(), ModItems.AA_12.get(), ModItems.M_4.get(), ModItems.DEVOTION.get(), ModItems.TRACHELIUM.get(), ModItems.M_79.get(),
                ModItems.HUNTING_RIFLE.get(), ModItems.NTW_20.get(), ModItems.M_98B.get(), ModItems.SENTINEL.get(), ModItems.M_870.get(), ModItems.MARLIN.get(), ModItems.GLOCK_17.get(), ModItems.RPG.get(),
                ModItems.GLOCK_18.get(), ModItems.M_1911.get(), ModItems.AURELIA_SCEPTRE.get(), ModItems.QBZ_95.get(), ModItems.K_98.get(), ModItems.MOSIN_NAGANT.get(), ModItems.MP_443.get(), ModItems.INSIDIOUS.get(), ModItems.SECONDARY_CATACLYSM.get(),
                ModItems.TASER.get(), ModItems.MINIGUN.get(), ModItems.MP_5.get());

        this.tag(ModTags.Items.LAUNCHER).add(ModItems.RPG.get(), ModItems.JAVELIN.get())
                .addTag(ModTags.Items.LAUNCHER_GRENADE);
        this.tag(ModTags.Items.LAUNCHER_GRENADE).add(ModItems.M_79.get(), ModItems.SECONDARY_CATACLYSM.get());

        this.tag(ModTags.Items.MILITARY_ARMOR).add(ModItems.RU_CHEST_6B43.get(), ModItems.US_CHEST_IOTV.get());

        this.tag(ModTags.Items.BLUEPRINT).addTags(ModTags.Items.COMMON_BLUEPRINT, ModTags.Items.RARE_BLUEPRINT, ModTags.Items.EPIC_BLUEPRINT,
                ModTags.Items.LEGENDARY_BLUEPRINT, ModTags.Items.CANNON_BLUEPRINT);

        this.tag(ModTags.Items.COMMON_BLUEPRINT).add(ModItems.GLOCK_17_BLUEPRINT.get(), ModItems.MP_443_BLUEPRINT.get(), ModItems.MARLIN_BLUEPRINT.get(),
                ModItems.TASER_BLUEPRINT.get(), ModItems.M_1911_BLUEPRINT.get());

        this.tag(ModTags.Items.RARE_BLUEPRINT).add(ModItems.GLOCK_18_BLUEPRINT.get(), ModItems.M_79_BLUEPRINT.get(), ModItems.M_4_BLUEPRINT.get(),
                ModItems.SKS_BLUEPRINT.get(), ModItems.M_870_BLUEPRINT.get(), ModItems.AK_47_BLUEPRINT.get(), ModItems.K_98_BLUEPRINT.get(),
                ModItems.MOSIN_NAGANT_BLUEPRINT.get(), ModItems.M_2_HB_BLUEPRINT.get(), ModItems.HK_416_BLUEPRINT.get(), ModItems.AK_12_BLUEPRINT.get()
                , ModItems.QBZ_95_BLUEPRINT.get(), ModItems.RPG_BLUEPRINT.get());

        this.tag(ModTags.Items.EPIC_BLUEPRINT).add(ModItems.TRACHELIUM_BLUEPRINT.get(), ModItems.HUNTING_RIFLE_BLUEPRINT.get(), ModItems.BOCEK_BLUEPRINT.get(),
                ModItems.RPK_BLUEPRINT.get(), ModItems.VECTOR_BLUEPRINT.get(), ModItems.MK_14_BLUEPRINT.get(), ModItems.M_60_BLUEPRINT.get(), ModItems.SVD_BLUEPRINT.get(),
                ModItems.M_98B_BLUEPRINT.get(), ModItems.DEVOTION_BLUEPRINT.get(), ModItems.INSIDIOUS_BLUEPRINT.get());

        this.tag(ModTags.Items.LEGENDARY_BLUEPRINT).add(ModItems.AA_12_BLUEPRINT.get(), ModItems.NTW_20_BLUEPRINT.get(), ModItems.MINIGUN_BLUEPRINT.get(),
                ModItems.SENTINEL_BLUEPRINT.get(), ModItems.JAVELIN_BLUEPRINT.get(), ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get(), ModItems.MK_42_BLUEPRINT.get(),
                ModItems.MLE_1934_BLUEPRINT.get(), ModItems.ANNIHILATOR_BLUEPRINT.get(), ModItems.HPJ_11_BLUEPRINT.get(), ModItems.AURELIA_SCEPTRE_BLUEPRINT.get());

        this.tag(ModTags.Items.CANNON_BLUEPRINT).add(ModItems.MK_42_BLUEPRINT.get(), ModItems.MLE_1934_BLUEPRINT.get(), ModItems.ANNIHILATOR_BLUEPRINT.get(),
                ModItems.HPJ_11_BLUEPRINT.get());
    }

    public static TagKey<Item> cTag(String name) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", name));
    }
}
