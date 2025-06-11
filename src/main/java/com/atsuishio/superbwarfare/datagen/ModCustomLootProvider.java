package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ModCustomLootProvider implements LootTableSubProvider {

    public static ResourceKey<LootTable> containers(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, Mod.loc("containers/" + name));
    }

    public static ResourceKey<LootTable> chests(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, Mod.loc("chests/" + name));
    }

    public ModCustomLootProvider(HolderLookup.Provider provider) {
        super();
    }

    public static ResourceKey<LootTable> special(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, Mod.loc("special/" + name));
    }

    @Override
    public void generate(@NotNull BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(chests("ancient_cpu"), LootTable.lootTable().withPool(singleItem(ModItems.ANCIENT_CPU.get(), 1, 1, 1, 1)
                .when(() -> LootItemRandomChanceCondition.randomChance(0.4f).build()))
        );
        output.accept(chests("blue_print_common"),
                LootTable.lootTable().withPool(multiItems(1, 0,
                                new ItemEntry(ModItems.TASER_BLUEPRINT.get(), 50),
                                new ItemEntry(ModItems.GLOCK_17_BLUEPRINT.get(), 50),
                                new ItemEntry(ModItems.MP_443_BLUEPRINT.get(), 50),
                                new ItemEntry(ModItems.M_1911_BLUEPRINT.get(), 50),
                                new ItemEntry(ModItems.MARLIN_BLUEPRINT.get(), 50),

                                new ItemEntry(ModItems.GLOCK_18_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.M_79_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.M_4_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.SKS_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.K_98_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.MOSIN_NAGANT_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.AK_47_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.M_870_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.HK_416_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.AK_12_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.QBZ_95_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.RPG_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.M_2_HB_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.MP_5_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.HUNTING_RIFLE_BLUEPRINT.get(), 15),

                                new ItemEntry(ModItems.TRACHELIUM_BLUEPRINT.get(), 1),
                                new ItemEntry(ModItems.SENTINEL_BLUEPRINT.get(), 1),
                                new ItemEntry(ModItems.BOCEK_BLUEPRINT.get(), 1),
                                new ItemEntry(ModItems.RPK_BLUEPRINT.get(), 1),
                                new ItemEntry(ModItems.VECTOR_BLUEPRINT.get(), 1),
                                new ItemEntry(ModItems.MK_14_BLUEPRINT.get(), 1),
                                new ItemEntry(ModItems.M_60_BLUEPRINT.get(), 1),
                                new ItemEntry(ModItems.SVD_BLUEPRINT.get(), 1),
                                new ItemEntry(ModItems.M_98B_BLUEPRINT.get(), 1),
                                new ItemEntry(ModItems.DEVOTION_BLUEPRINT.get(), 1),
                                new ItemEntry(ModItems.INSIDIOUS_BLUEPRINT.get(), 1)
                        ))
                        .withPool(multiItems(2, 0,
                                new ItemEntry(ModItems.HANDGUN_AMMO_BOX.get(), 12)
                                        .setCountBetween(1, 2),
                                new ItemEntry(ModItems.RIFLE_AMMO_BOX.get(), 20)
                                        .setCountBetween(1, 2),
                                new ItemEntry(ModItems.SNIPER_AMMO_BOX.get(), 10)
                                        .setCountBetween(1, 2),
                                new ItemEntry(ModItems.SHOTGUN_AMMO_BOX.get(), 17)
                                        .setCountBetween(1, 2),
                                new ItemEntry(ModItems.GRENADE_40MM.get(), 6)
                                        .setCountBetween(1, 3),
                                new ItemEntry(ModItems.ROCKET.get(), 4)
                                        .setCountBetween(1, 2),
                                new ItemEntry(ModItems.MORTAR_SHELL.get(), 6)
                                        .setCountBetween(1, 4),
                                new ItemEntry(ModItems.CLAYMORE_MINE.get(), 3)
                                        .setCountBetween(1, 3),
                                new ItemEntry(ModItems.C4_BOMB.get(), 1)
                        )));
        output.accept(chests("blue_print_rare"),
                LootTable.lootTable().withPool(multiItems(1, 0,
                                new ItemEntry(ModItems.TASER_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.GLOCK_17_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.MP_443_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.M_1911_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.MARLIN_BLUEPRINT.get(), 10),

                                new ItemEntry(ModItems.GLOCK_18_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.M_79_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.M_4_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.SKS_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.K_98_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.MOSIN_NAGANT_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.AK_47_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.M_870_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.HK_416_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.AK_12_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.QBZ_95_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.RPG_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.M_2_HB_BLUEPRINT.get(), 30),
                                new ItemEntry(ModItems.HUNTING_RIFLE_BLUEPRINT.get(), 30),

                                new ItemEntry(ModItems.TRACHELIUM_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.SENTINEL_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.BOCEK_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.RPK_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.VECTOR_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.MK_14_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.M_60_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.SVD_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.M_98B_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.DEVOTION_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.INSIDIOUS_BLUEPRINT.get(), 10),

                                new ItemEntry(ModItems.AA_12_BLUEPRINT.get(), 3),
                                new ItemEntry(ModItems.NTW_20_BLUEPRINT.get(), 3),
                                new ItemEntry(ModItems.MINIGUN_BLUEPRINT.get(), 3),
                                new ItemEntry(ModItems.JAVELIN_BLUEPRINT.get(), 3),
                                new ItemEntry(ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get(), 3),
                                new ItemEntry(ModItems.AURELIA_SCEPTRE_BLUEPRINT.get(), 2),
                                new ItemEntry(ModItems.MK_42_BLUEPRINT.get(), 3),
                                new ItemEntry(ModItems.MLE_1934_BLUEPRINT.get(), 2),
                                new ItemEntry(ModItems.HPJ_11_BLUEPRINT.get(), 2),
                                new ItemEntry(ModItems.ANNIHILATOR_BLUEPRINT.get(), 1)
                        ))
                        .withPool(multiItems(2, 0,
                                new ItemEntry(ModItems.HANDGUN_AMMO_BOX.get(), 12)
                                        .setCountBetween(1, 3),
                                new ItemEntry(ModItems.RIFLE_AMMO_BOX.get(), 20)
                                        .setCountBetween(1, 3),
                                new ItemEntry(ModItems.SNIPER_AMMO_BOX.get(), 10)
                                        .setCountBetween(1, 3),
                                new ItemEntry(ModItems.SHOTGUN_AMMO_BOX.get(), 17)
                                        .setCountBetween(1, 3),
                                new ItemEntry(ModItems.GRENADE_40MM.get(), 6)
                                        .setCountBetween(2, 6),
                                new ItemEntry(ModItems.ROCKET.get(), 4)
                                        .setCountBetween(2, 4),
                                new ItemEntry(ModItems.MORTAR_SHELL.get(), 6)
                                        .setCountBetween(2, 8),
                                new ItemEntry(ModItems.CLAYMORE_MINE.get(), 3)
                                        .setCountBetween(2, 6),
                                new ItemEntry(ModItems.C4_BOMB.get(), 1)
                                        .setCountBetween(1, 2)
                        )));
        output.accept(chests("blue_print_epic"),
                LootTable.lootTable().withPool(multiItems(1, 0,
                                new ItemEntry(ModItems.TRACHELIUM_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.SENTINEL_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.BOCEK_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.RPK_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.VECTOR_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.MK_14_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.M_60_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.SVD_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.M_98B_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.DEVOTION_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.INSIDIOUS_BLUEPRINT.get(), 10),

                                new ItemEntry(ModItems.AA_12_BLUEPRINT.get(), 20),
                                new ItemEntry(ModItems.NTW_20_BLUEPRINT.get(), 20),
                                new ItemEntry(ModItems.MINIGUN_BLUEPRINT.get(), 20),
                                new ItemEntry(ModItems.JAVELIN_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get(), 15),
                                new ItemEntry(ModItems.AURELIA_SCEPTRE_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.MK_42_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.MLE_1934_BLUEPRINT.get(), 10),
                                new ItemEntry(ModItems.HPJ_11_BLUEPRINT.get(), 5),
                                new ItemEntry(ModItems.ANNIHILATOR_BLUEPRINT.get(), 5)
                        ))
                        .withPool(multiItems(2, 0,
                                new ItemEntry(ModItems.HANDGUN_AMMO_BOX.get(), 12)
                                        .setCountBetween(2, 4),
                                new ItemEntry(ModItems.RIFLE_AMMO_BOX.get(), 20)
                                        .setCountBetween(2, 4),
                                new ItemEntry(ModItems.SNIPER_AMMO_BOX.get(), 10)
                                        .setCountBetween(2, 4),
                                new ItemEntry(ModItems.SHOTGUN_AMMO_BOX.get(), 17)
                                        .setCountBetween(2, 4),
                                new ItemEntry(ModItems.HEAVY_AMMO.get(), 10)
                                        .setCountBetween(10, 24),
                                new ItemEntry(ModItems.GRENADE_40MM.get(), 6)
                                        .setCountBetween(4, 12),
                                new ItemEntry(ModItems.ROCKET.get(), 4)
                                        .setCountBetween(4, 8),
                                new ItemEntry(ModItems.MORTAR_SHELL.get(), 6)
                                        .setCountBetween(4, 8),
                                new ItemEntry(ModItems.CLAYMORE_MINE.get(), 3)
                                        .setCountBetween(4, 12),
                                new ItemEntry(ModItems.C4_BOMB.get(), 1)
                                        .setCountBetween(2, 4),
                                new ItemEntry(ModItems.JAVELIN_MISSILE.get(), 1)
                                        .setCountBetween(1, 2)
                        )));

        output.accept(containers("blueprints"),
                LootTable.lootTable().withPool(multiItems(1, 0,
                        new ItemEntry(ModItems.GLOCK_17_BLUEPRINT.get(), 60),
                        new ItemEntry(ModItems.MP_443_BLUEPRINT.get(), 60),
                        new ItemEntry(ModItems.TASER_BLUEPRINT.get(), 60),
                        new ItemEntry(ModItems.MARLIN_BLUEPRINT.get(), 60),
                        new ItemEntry(ModItems.M_1911_BLUEPRINT.get(), 60),

                        new ItemEntry(ModItems.GLOCK_18_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.M_79_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.M_4_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.SKS_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.M_870_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.AK_47_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.K_98_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.MOSIN_NAGANT_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.HK_416_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.AK_12_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.QBZ_95_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.RPG_BLUEPRINT.get(), 42),
                        new ItemEntry(ModItems.HUNTING_RIFLE_BLUEPRINT.get(), 42),

                        new ItemEntry(ModItems.TRACHELIUM_BLUEPRINT.get(), 15),
                        new ItemEntry(ModItems.SENTINEL_BLUEPRINT.get(), 15),
                        new ItemEntry(ModItems.BOCEK_BLUEPRINT.get(), 15),
                        new ItemEntry(ModItems.RPK_BLUEPRINT.get(), 15),
                        new ItemEntry(ModItems.VECTOR_BLUEPRINT.get(), 15),
                        new ItemEntry(ModItems.MK_14_BLUEPRINT.get(), 15),
                        new ItemEntry(ModItems.M_60_BLUEPRINT.get(), 15),
                        new ItemEntry(ModItems.SVD_BLUEPRINT.get(), 15),
                        new ItemEntry(ModItems.M_98B_BLUEPRINT.get(), 15),
                        new ItemEntry(ModItems.DEVOTION_BLUEPRINT.get(), 15),
                        new ItemEntry(ModItems.INSIDIOUS_BLUEPRINT.get(), 15),

                        new ItemEntry(ModItems.AA_12_BLUEPRINT.get(), 5),
                        new ItemEntry(ModItems.NTW_20_BLUEPRINT.get(), 5),
                        new ItemEntry(ModItems.MINIGUN_BLUEPRINT.get(), 5),
                        new ItemEntry(ModItems.JAVELIN_BLUEPRINT.get(), 5),
                        new ItemEntry(ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get(), 5),
                        new ItemEntry(ModItems.AURELIA_SCEPTRE_BLUEPRINT.get(), 5)
                )));
        output.accept(containers("common"),
                LootTable.lootTable().withPool(multiItems(1, 0,
                        new ItemEntry(ModItems.EPIC_MATERIAL_PACK.get(), 2),
                        new ItemEntry(ModItems.CEMENTED_CARBIDE_BLOCK.get(), 2),
                        new ItemEntry(Items.EXPERIENCE_BOTTLE, 2)
                                .setCount(4),
                        new ItemEntry(ModItems.RARE_MATERIAL_PACK.get(), 4)
                                .setCount(2),
                        new ItemEntry(ModItems.COMMON_MATERIAL_PACK.get(), 6)
                                .setCount(3),
                        new ItemEntry(ModItems.STEEL_BLOCK.get(), 14),
                        new ItemEntry(Items.GOLD_BLOCK, 20),
                        new ItemEntry(ModItems.HANDGUN_AMMO.get(), 6)
                                .setCount(64),
                        new ItemEntry(ModItems.RIFLE_AMMO.get(), 6)
                                .setCount(64),
                        new ItemEntry(ModItems.SHOTGUN_AMMO.get(), 6)
                                .setCount(32),
                        new ItemEntry(ModItems.SNIPER_AMMO.get(), 6)
                                .setCount(32),
                        new ItemEntry(ModItems.HEAVY_AMMO.get(), 6)
                                .setCount(16),
                        new ItemEntry(Items.COAL_BLOCK, 30)
                                .setCount(9))
                        .add(NestedLootTable.lootTableReference(special("common/flags")).setWeight(40))
                        .add(NestedLootTable.lootTableReference(special("common/blueprints")).setWeight(50))
                ));

        output.accept(special("common/flags"), LootTable.lootTable()
                .withPool(singleItem(Items.RED_BANNER, 1))
                .withPool(singleItem(Items.ORANGE_BANNER, 1))
                .withPool(singleItem(Items.YELLOW_BANNER, 1))
                .withPool(singleItem(Items.GREEN_BANNER, 1))
                .withPool(singleItem(Items.CYAN_BANNER, 1))
                .withPool(singleItem(Items.BLUE_BANNER, 1))
                .withPool(singleItem(Items.PURPLE_BANNER, 1))
                .withPool(singleItem(Items.PINK_BANNER, 1))
        );
        output.accept(special("common/blueprints"),
                LootTable.lootTable().withPool(multiItems(1, 0,
                        new ItemEntry(ModItems.GLOCK_17_BLUEPRINT.get(), 4),
                        new ItemEntry(ModItems.MP_443_BLUEPRINT.get(), 4),
                        new ItemEntry(ModItems.M_1911_BLUEPRINT.get(), 4),
                        new ItemEntry(ModItems.MARLIN_BLUEPRINT.get(), 4),
                        new ItemEntry(ModItems.TASER_BLUEPRINT.get(), 4),

                        new ItemEntry(ModItems.GLOCK_18_BLUEPRINT.get(), 2),
                        new ItemEntry(ModItems.AK_47_BLUEPRINT.get(), 2),
                        new ItemEntry(ModItems.QBZ_95_BLUEPRINT.get(), 2),
                        new ItemEntry(ModItems.SKS_BLUEPRINT.get(), 2),
                        new ItemEntry(ModItems.MOSIN_NAGANT_BLUEPRINT.get(), 2),
                        new ItemEntry(ModItems.M_870_BLUEPRINT.get(), 2),
                        new ItemEntry(ModItems.M_79_BLUEPRINT.get(), 2),

                        new ItemEntry(ModItems.BOCEK_BLUEPRINT.get(), 2),
                        new ItemEntry(ModItems.TRACHELIUM_BLUEPRINT.get(), 2),
                        new ItemEntry(ModItems.VECTOR_BLUEPRINT.get(), 2),
                        new ItemEntry(ModItems.DEVOTION_BLUEPRINT.get(), 2),
                        new ItemEntry(ModItems.M_98B_BLUEPRINT.get(), 2),

                        new ItemEntry(ModItems.AA_12_BLUEPRINT.get(), 1),
                        new ItemEntry(ModItems.NTW_20_BLUEPRINT.get(), 1),
                        new ItemEntry(ModItems.MINIGUN_BLUEPRINT.get(), 1),
                        new ItemEntry(ModItems.JAVELIN_BLUEPRINT.get(), 1),

                        new ItemEntry(ModItems.MK_42_BLUEPRINT.get(), 1),
                        new ItemEntry(ModItems.MLE_1934_BLUEPRINT.get(), 1)
                )));
    }

    public LootPool.Builder singleItem(ItemLike item, int weight) {
        return singleItem(item, 1, 0, weight, 0);
    }

    public LootPool.Builder singleItem(ItemLike item, float rolls, float bonus, int weight, int quality) {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(rolls)).setBonusRolls(ConstantValue.exactly(bonus))
                .add(LootItem.lootTableItem(item).setWeight(weight).setQuality(quality));
    }

    public final LootPool.Builder multiItems(float rolls, float bonus, ItemEntry... triplet) {
        var builder = LootPool.lootPool().setRolls(ConstantValue.exactly(rolls)).setBonusRolls(ConstantValue.exactly(bonus));
        for (var t : triplet) {
            var entry = LootItem.lootTableItem(t.item).setWeight(t.weight).setQuality(t.quality);
            for (var c : t.conditions) {
                entry.when(c);
            }
            for (var f : t.functions) {
                entry.apply(f);
            }
            builder.add(entry);
        }
        return builder;
    }

    public static class ItemEntry {

        public ItemLike item;
        public int weight;
        public int quality;
        public List<LootItemCondition.Builder> conditions = new ArrayList<>();
        public List<LootItemFunction.Builder> functions = new ArrayList<>();

        public ItemEntry(ItemLike item, int weight) {
            this(item, weight, 0);
        }

        public ItemEntry(ItemLike item, int weight, int quality) {
            this.item = item;
            this.weight = weight;
            this.quality = quality;
        }

        public ItemEntry condition(LootItemCondition.Builder condition) {
            this.conditions.add(condition);
            return this;
        }

        public ItemEntry function(LootItemFunction.Builder function) {
            this.functions.add(function);
            return this;
        }

        public ItemEntry setCountBetween(int min, int max) {
            return this.function(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
        }

        public ItemEntry setCount(int count) {
            return this.function(SetItemCountFunction.setCount(ConstantValue.exactly(count)));
        }
    }
}
