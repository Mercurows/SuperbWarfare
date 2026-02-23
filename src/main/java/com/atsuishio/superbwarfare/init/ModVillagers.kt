package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.container.SmallContainerBlockItem
import com.google.common.collect.ImmutableSet
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.ai.village.poi.PoiType
import net.minecraft.world.entity.npc.VillagerProfession
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.BasicItemListing
import net.neoforged.neoforge.event.village.VillagerTradesEvent
import net.neoforged.neoforge.event.village.WandererTradesEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

@EventBusSubscriber(modid = Mod.MODID)
object ModVillagers {
    val POI_TYPES: DeferredRegister<PoiType> =
        DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, Mod.MODID)
    val VILLAGER_PROFESSIONS: DeferredRegister<VillagerProfession> =
        DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, Mod.MODID)

    val ARMORY_POI: DeferredHolder<PoiType, PoiType> = POI_TYPES.register(
        "armory",
        Supplier {
            PoiType(ImmutableSet.copyOf(ModBlocks.REFORGING_TABLE.get().getStateDefinition().getPossibleStates()), 1, 1)
        })

    val ARMORY: DeferredHolder<VillagerProfession, VillagerProfession> =
        VILLAGER_PROFESSIONS.register(
            "armory",
            Supplier {
                VillagerProfession(
                    "armory",
                    { it.value() === ARMORY_POI.get() },
                    { it.value() === ARMORY_POI.get() },
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    null
                )
            })

    fun register(eventBus: IEventBus) {
        POI_TYPES.register(eventBus)
        VILLAGER_PROFESSIONS.register(eventBus)
    }

    @SubscribeEvent
    fun addCustomTrades(event: VillagerTradesEvent) {
        if (event.getType() === ARMORY.get()) {
            val trades = event.getTrades()

            // 等级 1 交易
            val list1 = listOf(
                BasicItemListing(
                    ItemStack(ModItems.TASER_BLUEPRINT.get()),
                    ItemStack(Items.EMERALD, 2), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 1),
                    ItemStack(ModItems.HANDGUN_AMMO.get(), 20), 16, 1, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 1),
                    ItemStack(ModItems.RIFLE_AMMO.get(), 15), 16, 1, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 1),
                    ItemStack(ModItems.SNIPER_AMMO.get(), 8), 16, 1, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 1),
                    ItemStack(ModItems.SHOTGUN_AMMO.get(), 8), 16, 1, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 1),
                    ItemStack(ModItems.HEAVY_AMMO.get(), 6), 32, 1, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 1),
                    ItemStack(ModItems.SMALL_SHELL_AP.get(), 4), 32, 1, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 1),
                    ItemStack(ModItems.SMALL_SHELL_HE.get(), 4), 32, 1, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 1),
                    ItemStack(ModItems.SMALL_SHELL_GS.get(), 6), 48, 1, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 1),
                    ItemStack(ModItems.SMALL_SHELL_AA.get(), 8), 64, 1, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 1),
                    ItemStack(ModItems.BLU_43_MINE.get(), 8), 32, 1, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.HANDGUN_AMMO.get(), 40),
                    ItemStack(Items.EMERALD, 1), 32, 2, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.RIFLE_AMMO.get(), 30),
                    ItemStack(Items.EMERALD, 1), 32, 2, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.SNIPER_AMMO.get(), 16),
                    ItemStack(Items.EMERALD, 1), 32, 2, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.SHOTGUN_AMMO.get(), 16),
                    ItemStack(Items.EMERALD, 1), 32, 2, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.HEAVY_AMMO.get(), 12),
                    ItemStack(Items.EMERALD, 1), 64, 2, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.SMALL_SHELL_AP.get(), 8),
                    ItemStack(Items.EMERALD, 1), 64, 2, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.SMALL_SHELL_HE.get(), 8),
                    ItemStack(Items.EMERALD, 1), 64, 2, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.SMALL_SHELL_GS.get(), 12),
                    ItemStack(Items.EMERALD, 1), 64, 2, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.SMALL_SHELL_AA.get(), 16),
                    ItemStack(Items.EMERALD, 1), 64, 2, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.BLU_43_MINE.get(), 16),
                    ItemStack(Items.EMERALD, 1), 64, 2, 0.05f
                ),
            )
            trades[1].addAll(list1)

            // 等级 2 交易
            val list2 = listOf(
                BasicItemListing(
                    ItemStack(Items.EMERALD, 10),
                    ItemStack(ModItems.STEEL_MATERIALS.action.get()), 12, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 8),
                    ItemStack(ModItems.STEEL_MATERIALS.barrel.get()), 12, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 6),
                    ItemStack(ModItems.STEEL_MATERIALS.trigger.get()), 12, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 8),
                    ItemStack(ModItems.STEEL_MATERIALS.spring.get()), 12, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 16),
                    ItemStack(ModItems.MARLIN_BLUEPRINT.get()), 8, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 16),
                    ItemStack(ModItems.GLOCK_17_BLUEPRINT.get()), 8, 15, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 16),
                    ItemStack(ModItems.M_1911_BLUEPRINT.get()), 8, 15, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 16),
                    ItemStack(ModItems.MP_443_BLUEPRINT.get()), 8, 15, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 16),
                    ItemStack(ModItems.TASER_BLUEPRINT.get()), 8, 15, 0.05f
                )
            )
            trades[2].addAll(list2)

            // 等级 3 交易
            val list3 = listOf(
                BasicItemListing(
                    ItemStack(Items.EMERALD, 3),
                    ItemStack(ModItems.HANDGUN_AMMO_BOX.get(), 2), 8, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 2),
                    ItemStack(ModItems.RIFLE_AMMO_BOX.get(), 1), 8, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 3),
                    ItemStack(ModItems.SNIPER_AMMO_BOX.get(), 1), 8, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 3),
                    ItemStack(ModItems.SHOTGUN_AMMO_BOX.get(), 1), 8, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.HANDGUN_AMMO_BOX.get(), 4),
                    ItemStack(Items.EMERALD, 3), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.RIFLE_AMMO_BOX.get(), 1),
                    ItemStack(Items.EMERALD, 1), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.SNIPER_AMMO_BOX.get(), 2),
                    ItemStack(Items.EMERALD, 3), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.SHOTGUN_AMMO_BOX.get(), 2),
                    ItemStack(Items.EMERALD, 3), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 16),
                    ItemStack(ModItems.CEMENTED_CARBIDE_MATERIALS.barrel.get()), 12, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 20),
                    ItemStack(ModItems.CEMENTED_CARBIDE_MATERIALS.action.get()), 10, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 16),
                    ItemStack(ModItems.CEMENTED_CARBIDE_MATERIALS.spring.get()), 10, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 12),
                    ItemStack(ModItems.CEMENTED_CARBIDE_MATERIALS.trigger.get()), 10, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.M_4_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.M_79_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.AK_47_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.GLOCK_18_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.SKS_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.M_870_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.K_98_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.MOSIN_NAGANT_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.RPG_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.HK_416_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.QBZ_95_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.AK_12_BLUEPRINT.get()), 10, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(ModItems.HUNTING_RIFLE_BLUEPRINT.get()), 10, 25, 0.05f
                )
            )
            trades[3].addAll(list3)

            // 等级 4 交易
            val list4 = listOf(
                BasicItemListing(
                    ItemStack(Items.EMERALD, 2),
                    ItemStack(ModItems.GRENADE_40MM.get(), 1), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 2),
                    ItemStack(ModItems.HAND_GRENADE.get(), 1), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 2),
                    ItemStack(ModItems.RGO_GRENADE.get(), 1), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 3),
                    ItemStack(ModItems.MORTAR_SHELL.get(), 1), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 4),
                    ItemStack(ModItems.CLAYMORE_MINE.get(), 1), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 4),
                    ItemStack(ModItems.C4_BOMB.get(), 1), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 4),
                    ItemStack(ModItems.RPG_ROCKET_TBG.get(), 1), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 4),
                    ItemStack(ModItems.TM_62.get(), 1), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 3),
                    ItemStack(ModItems.SMALL_ROCKET.get(), 1), 16, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.GRENADE_40MM.get(), 1),
                    ItemStack(Items.EMERALD, 1), 32, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.HAND_GRENADE.get(), 1),
                    ItemStack(Items.EMERALD, 1), 32, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.RGO_GRENADE.get(), 1),
                    ItemStack(Items.EMERALD, 1), 32, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.MORTAR_SHELL.get(), 3),
                    ItemStack(Items.EMERALD, 2), 32, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.CLAYMORE_MINE.get(), 1),
                    ItemStack(Items.EMERALD, 2), 32, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.C4_BOMB.get(), 1),
                    ItemStack(Items.EMERALD, 2), 32, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.RPG_ROCKET_TBG.get(), 1),
                    ItemStack(Items.EMERALD, 2), 32, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.TM_62.get(), 1),
                    ItemStack(Items.EMERALD, 2), 32, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.SMALL_ROCKET.get(), 3),
                    ItemStack(Items.EMERALD, 2), 32, 5, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 64),
                    ItemStack(ModItems.RPK_BLUEPRINT.get()), 10, 30, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 64),
                    ItemStack(ModItems.VECTOR_BLUEPRINT.get()), 10, 30, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 64),
                    ItemStack(ModItems.MK_14_BLUEPRINT.get()), 10, 30, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 64),
                    ItemStack(ModItems.M_60_BLUEPRINT.get()), 10, 30, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 64),
                    ItemStack(ModItems.SVD_BLUEPRINT.get()), 10, 30, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 64),
                    ItemStack(ModItems.M_98B_BLUEPRINT.get()), 10, 30, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 64),
                    ItemStack(ModItems.AWM_BLUEPRINT.get()), 10, 30, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 64),
                    ItemStack(ModItems.DEVOTION_BLUEPRINT.get()), 10, 30, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 8),
                    ItemStack(ModItems.LARGE_SHELL_HE.get(), 1), 8, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 8),
                    ItemStack(ModItems.LARGE_SHELL_AP.get(), 1), 8, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 8),
                    ItemStack(ModItems.LARGE_SHELL_CM.get(), 1), 8, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 8),
                    ItemStack(ModItems.MEDIUM_ROCKET_HE.get(), 1), 8, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 8),
                    ItemStack(ModItems.MEDIUM_ROCKET_AP.get(), 1), 8, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 8),
                    ItemStack(ModItems.MEDIUM_ROCKET_CM.get(), 1), 8, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 12),
                    ItemStack(ModItems.JAVELIN_MISSILE.get(), 1), 8, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 12),
                    ItemStack(ModItems.MEDIUM_ANTI_GROUND_MISSILE.get(), 1), 8, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 16),
                    ItemStack(ModItems.LARGE_ANTI_GROUND_MISSILE.get(), 1), 8, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 16),
                    ItemStack(ModItems.MEDIUM_AERIAL_BOMB.get(), 1), 8, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.LARGE_SHELL_HE.get(), 1),
                    ItemStack(Items.EMERALD, 4), 32, 4, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.LARGE_SHELL_AP.get(), 1),
                    ItemStack(Items.EMERALD, 4), 32, 4, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.LARGE_SHELL_CM.get(), 1),
                    ItemStack(Items.EMERALD, 4), 32, 4, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.MEDIUM_ROCKET_HE.get(), 1),
                    ItemStack(Items.EMERALD, 4), 32, 4, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.MEDIUM_ROCKET_AP.get(), 1),
                    ItemStack(Items.EMERALD, 4), 32, 4, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.MEDIUM_ROCKET_CM.get(), 1),
                    ItemStack(Items.EMERALD, 4), 32, 4, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.JAVELIN_MISSILE.get(), 1),
                    ItemStack(Items.EMERALD, 6), 32, 4, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.MEDIUM_ANTI_GROUND_MISSILE.get(), 1),
                    ItemStack(Items.EMERALD, 6), 32, 4, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.LARGE_ANTI_GROUND_MISSILE.get(), 1),
                    ItemStack(Items.EMERALD, 8), 32, 4, 0.05f
                ),
                BasicItemListing(
                    ItemStack(ModItems.MEDIUM_AERIAL_BOMB.get(), 1),
                    ItemStack(Items.EMERALD, 8), 32, 4, 0.05f
                )
            )
            trades[4].addAll(list4)

            // 等级 5 交易
            val list5 = listOf(
                BasicItemListing(
                    ItemStack(Items.EMERALD, 22),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.POISONOUS_BULLET]!!.get(), 1), 4, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 24),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.SUBSISTENCE]!!.get(), 1), 4, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 25),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.KILL_CLIP]!!.get(), 1), 4, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 26),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.GUTSHOT_STRAIGHT]!!.get(), 1), 4, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 22),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.HEAD_SEEKER]!!.get(), 1), 4, 10, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 34),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.SILVER_BULLET]!!.get(), 1), 4, 15, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 30),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.FIELD_DOCTOR]!!.get(), 1), 4, 15, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 34),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.HEAL_CLIP]!!.get(), 1), 4, 15, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 30),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.KILLING_TALLY]!!.get(), 1), 4, 15, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 34),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.FOURTH_TIMES_CHARM]!!.get(), 1), 4, 15, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 48),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.MONSTER_HUNTER]!!.get(), 1), 4, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 40),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.VORPAL_WEAPON]!!.get(), 1), 4, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 42),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.MAGNIFICENT_HOWL]!!.get(), 1), 4, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 64),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.FAIR_MEANS]!!.get(), 1), 4, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 40),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.HIGH_IMPACT_RESERVES]!!.get(), 1), 4, 25, 0.05f
                ),
                BasicItemListing(
                    ItemStack(Items.EMERALD, 48),
                    ItemStack(ModItems.PERK_ITEMS[ModPerks.ONE_TWO_PUNCH]!!.get(), 1), 4, 25, 0.05f
                )
            )
            trades[5].addAll(list5)
        }
    }

    @SubscribeEvent
    fun addWandererTrade(event: WandererTradesEvent) {
        val rareTrades = event.rareTrades

        rareTrades.add(
            BasicItemListing(
                ItemStack(Items.EMERALD, 16),
                SmallContainerBlockItem.createInstance(loc("containers/blueprints")), 10, 0, 0.05f
            )
        )
        rareTrades.add(
            BasicItemListing(
                ItemStack(Items.EMERALD, 10),
                SmallContainerBlockItem.createInstance(loc("containers/common")), 10, 0, 0.05f
            )
        )
    }
}
