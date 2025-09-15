package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.common.container.SmallContainerBlockItem;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
public class ModVillagers {

    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, Mod.MODID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, Mod.MODID);

    public static final RegistryObject<PoiType> ARMORY_POI = POI_TYPES.register("armory",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.REFORGING_TABLE.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final RegistryObject<VillagerProfession> ARMORY = VILLAGER_PROFESSIONS.register("armory",
            () -> new VillagerProfession("armory", holder -> holder.get() == ARMORY_POI.get(), holder -> holder.get() == ARMORY_POI.get(),
                    ImmutableSet.of(), ImmutableSet.of(), null));

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }

    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if (event.getType() == ModVillagers.ARMORY.get()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

            // 等级 1 交易
            trades.get(1).add(new BasicItemListing(new ItemStack(ModItems.TASER_BLUEPRINT.get()),
                    new ItemStack(Items.EMERALD, 2), 16, 5, 0.05f));

            trades.get(1).add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                    new ItemStack(ModItems.HANDGUN_AMMO.get(), 20), 16, 1, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                    new ItemStack(ModItems.RIFLE_AMMO.get(), 15), 16, 1, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                    new ItemStack(ModItems.SNIPER_AMMO.get(), 8), 16, 1, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                    new ItemStack(ModItems.SHOTGUN_AMMO.get(), 8), 16, 1, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                    new ItemStack(ModItems.HEAVY_AMMO.get(), 6), 32, 1, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                    new ItemStack(ModItems.SMALL_SHELL.get(), 4), 32, 1, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                    new ItemStack(ModItems.BLU_43_MINE.get(), 4), 32, 1, 0.05f));

            trades.get(1).add(new BasicItemListing(new ItemStack(ModItems.HANDGUN_AMMO.get(), 40),
                    new ItemStack(Items.EMERALD, 1), 32, 2, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(ModItems.RIFLE_AMMO.get(), 30),
                    new ItemStack(Items.EMERALD, 1), 32, 2, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(ModItems.SNIPER_AMMO.get(), 16),
                    new ItemStack(Items.EMERALD, 1), 32, 2, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(ModItems.SHOTGUN_AMMO.get(), 16),
                    new ItemStack(Items.EMERALD, 1), 32, 2, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(ModItems.HEAVY_AMMO.get(), 12),
                    new ItemStack(Items.EMERALD, 1), 64, 2, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(ModItems.SMALL_SHELL.get(), 8),
                    new ItemStack(Items.EMERALD, 1), 64, 2, 0.05f));
            trades.get(1).add(new BasicItemListing(new ItemStack(ModItems.BLU_43_MINE.get(), 8),
                    new ItemStack(Items.EMERALD, 1), 64, 2, 0.05f));

            // 等级 2 交易
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 10),
                    new ItemStack(ModItems.STEEL_MATERIALS.action().get()), 12, 5, 0.05f));
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 8),
                    new ItemStack(ModItems.STEEL_MATERIALS.barrel().get()), 12, 5, 0.05f));
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 6),
                    new ItemStack(ModItems.STEEL_MATERIALS.trigger().get()), 12, 5, 0.05f));
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 8),
                    new ItemStack(ModItems.STEEL_MATERIALS.spring().get()), 12, 5, 0.05f));

            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                    new ItemStack(ModItems.MARLIN_BLUEPRINT.get()), 8, 25, 0.05f));
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                    new ItemStack(ModItems.GLOCK_17_BLUEPRINT.get()), 8, 15, 0.05f));
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                    new ItemStack(ModItems.M_1911_BLUEPRINT.get()), 8, 15, 0.05f));
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                    new ItemStack(ModItems.MP_443_BLUEPRINT.get()), 8, 15, 0.05f));
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                    new ItemStack(ModItems.TASER_BLUEPRINT.get()), 8, 15, 0.05f));

            // 等级 3 交易
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 3),
                    new ItemStack(ModItems.HANDGUN_AMMO_BOX.get(), 2), 8, 5, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 2),
                    new ItemStack(ModItems.RIFLE_AMMO_BOX.get(), 1), 8, 5, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 3),
                    new ItemStack(ModItems.SNIPER_AMMO_BOX.get(), 1), 8, 5, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 3),
                    new ItemStack(ModItems.SHOTGUN_AMMO_BOX.get(), 1), 8, 5, 0.05f));

            trades.get(3).add(new BasicItemListing(new ItemStack(ModItems.HANDGUN_AMMO_BOX.get(), 4),
                    new ItemStack(Items.EMERALD, 3), 16, 5, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(ModItems.RIFLE_AMMO_BOX.get(), 1),
                    new ItemStack(Items.EMERALD, 1), 16, 5, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(ModItems.SNIPER_AMMO_BOX.get(), 2),
                    new ItemStack(Items.EMERALD, 3), 16, 5, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(ModItems.SHOTGUN_AMMO_BOX.get(), 2),
                    new ItemStack(Items.EMERALD, 3), 16, 5, 0.05f));

            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                    new ItemStack(ModItems.CEMENTED_CARBIDE_MATERIALS.barrel().get()), 12, 10, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 20),
                    new ItemStack(ModItems.CEMENTED_CARBIDE_MATERIALS.action().get()), 10, 10, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                    new ItemStack(ModItems.CEMENTED_CARBIDE_MATERIALS.spring().get()), 10, 10, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 12),
                    new ItemStack(ModItems.CEMENTED_CARBIDE_MATERIALS.trigger().get()), 10, 10, 0.05f));

            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.M_4_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.M_79_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.AK_47_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.GLOCK_18_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.SKS_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.M_870_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.K_98_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.MOSIN_NAGANT_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.RPG_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.HK_416_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.QBZ_95_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.AK_12_BLUEPRINT.get()), 10, 25, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.HUNTING_RIFLE_BLUEPRINT.get()), 10, 25, 0.05f));

            // 等级 4 交易
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 2),
                    new ItemStack(ModItems.GRENADE_40MM.get(), 1), 16, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 2),
                    new ItemStack(ModItems.HAND_GRENADE.get(), 1), 16, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 2),
                    new ItemStack(ModItems.RGO_GRENADE.get(), 1), 16, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 3),
                    new ItemStack(ModItems.MORTAR_SHELL.get(), 1), 16, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 4),
                    new ItemStack(ModItems.CLAYMORE_MINE.get(), 1), 16, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 4),
                    new ItemStack(ModItems.C4_BOMB.get(), 1), 16, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 4),
                    new ItemStack(ModItems.RPG_ROCKET_TBG.get(), 1), 16, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 4),
                    new ItemStack(ModItems.TM_62.get(), 1), 16, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 3),
                    new ItemStack(ModItems.SMALL_ROCKET.get(), 1), 16, 5, 0.05f));

            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.GRENADE_40MM.get(), 1),
                    new ItemStack(Items.EMERALD, 1), 32, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.HAND_GRENADE.get(), 1),
                    new ItemStack(Items.EMERALD, 1), 32, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.RGO_GRENADE.get(), 1),
                    new ItemStack(Items.EMERALD, 1), 32, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.MORTAR_SHELL.get(), 3),
                    new ItemStack(Items.EMERALD, 2), 32, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.CLAYMORE_MINE.get(), 1),
                    new ItemStack(Items.EMERALD, 2), 32, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.C4_BOMB.get(), 1),
                    new ItemStack(Items.EMERALD, 2), 32, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.RPG_ROCKET_TBG.get(), 1),
                    new ItemStack(Items.EMERALD, 2), 32, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.TM_62.get(), 1),
                    new ItemStack(Items.EMERALD, 2), 32, 5, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.SMALL_ROCKET.get(), 3),
                    new ItemStack(Items.EMERALD, 2), 32, 5, 0.05f));

            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 64),
                    new ItemStack(ModItems.RPK_BLUEPRINT.get()), 10, 30, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 64),
                    new ItemStack(ModItems.VECTOR_BLUEPRINT.get()), 10, 30, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 64),
                    new ItemStack(ModItems.MK_14_BLUEPRINT.get()), 10, 30, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 64),
                    new ItemStack(ModItems.M_60_BLUEPRINT.get()), 10, 30, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 64),
                    new ItemStack(ModItems.SVD_BLUEPRINT.get()), 10, 30, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 64),
                    new ItemStack(ModItems.M_98B_BLUEPRINT.get()), 10, 30, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 64),
                    new ItemStack(ModItems.AWM_BLUEPRINT.get()), 10, 30, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 64),
                    new ItemStack(ModItems.DEVOTION_BLUEPRINT.get()), 10, 30, 0.05f));

            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 8),
                    new ItemStack(ModItems.HE_5_INCHES.get(), 1), 8, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 8),
                    new ItemStack(ModItems.AP_5_INCHES.get(), 1), 8, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 8),
                    new ItemStack(ModItems.CM_5_INCHES.get(), 1), 8, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 8),
                    new ItemStack(ModItems.MEDIUM_ROCKET_HE.get(), 1), 8, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 8),
                    new ItemStack(ModItems.MEDIUM_ROCKET_AP.get(), 1), 8, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 8),
                    new ItemStack(ModItems.MEDIUM_ROCKET_CM.get(), 1), 8, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 12),
                    new ItemStack(ModItems.JAVELIN_MISSILE.get(), 1), 8, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 12),
                    new ItemStack(ModItems.WIRE_GUIDE_MISSILE.get(), 1), 8, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                    new ItemStack(ModItems.AGM.get(), 1), 8, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                    new ItemStack(ModItems.MEDIUM_AERIAL_BOMB.get(), 1), 8, 10, 0.05f));

            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.HE_5_INCHES.get(), 1),
                    new ItemStack(Items.EMERALD, 4), 32, 4, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.AP_5_INCHES.get(), 1),
                    new ItemStack(Items.EMERALD, 4), 32, 4, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.CM_5_INCHES.get(), 1),
                    new ItemStack(Items.EMERALD, 4), 32, 4, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.MEDIUM_ROCKET_HE.get(), 1),
                    new ItemStack(Items.EMERALD, 4), 32, 4, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.MEDIUM_ROCKET_AP.get(), 1),
                    new ItemStack(Items.EMERALD, 4), 32, 4, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.MEDIUM_ROCKET_CM.get(), 1),
                    new ItemStack(Items.EMERALD, 4), 32, 4, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.JAVELIN_MISSILE.get(), 1),
                    new ItemStack(Items.EMERALD, 6), 32, 4, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.WIRE_GUIDE_MISSILE.get(), 1),
                    new ItemStack(Items.EMERALD, 6), 32, 4, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.AGM.get(), 1),
                    new ItemStack(Items.EMERALD, 8), 32, 4, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.MEDIUM_AERIAL_BOMB.get(), 1),
                    new ItemStack(Items.EMERALD, 8), 32, 4, 0.05f));

            // 等级 5 交易
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 22),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.POISONOUS_BULLET).get(), 1), 4, 10, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 24),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.SUBSISTENCE).get(), 1), 4, 10, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 25),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.KILL_CLIP).get(), 1), 4, 10, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 26),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.GUTSHOT_STRAIGHT).get(), 1), 4, 10, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 22),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.HEAD_SEEKER).get(), 1), 4, 10, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 34),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.SILVER_BULLET).get(), 1), 4, 15, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 30),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.FIELD_DOCTOR).get(), 1), 4, 15, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 34),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.HEAL_CLIP).get(), 1), 4, 15, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 30),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.KILLING_TALLY).get(), 1), 4, 15, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 34),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.FOURTH_TIMES_CHARM).get(), 1), 4, 15, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 48),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.MONSTER_HUNTER).get(), 1), 4, 25, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 40),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.VORPAL_WEAPON).get(), 1), 4, 25, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 42),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.MAGNIFICENT_HOWL).get(), 1), 4, 25, 0.05f));
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 64),
                    new ItemStack(ModItems.PERK_ITEMS.get(ModPerks.FAIR_MEANS).get(), 1), 4, 25, 0.05f));
        }
    }

    @SubscribeEvent
    public static void addWandererTrade(WandererTradesEvent event) {
        List<VillagerTrades.ItemListing> rareTrades = event.getRareTrades();

        rareTrades.add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                SmallContainerBlockItem.createInstance(Mod.loc("containers/blueprints")), 10, 0, 0.05f));
        rareTrades.add(new BasicItemListing(new ItemStack(Items.EMERALD, 10),
                SmallContainerBlockItem.createInstance(Mod.loc("containers/common")), 10, 0, 0.05f));
    }
}
