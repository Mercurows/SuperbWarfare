package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.item.SmallContainerBlockItem;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

@Mod.EventBusSubscriber(modid = ModUtils.MODID)
public class ModVillagers {

    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, ModUtils.MODID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, ModUtils.MODID);

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

            // 等级 2 交易
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 10),
                    new ItemStack(ModItems.STEEL_ACTION.get()), 12, 5, 0.05f));
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 8),
                    new ItemStack(ModItems.STEEL_BARREL.get()), 12, 5, 0.05f));
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 6),
                    new ItemStack(ModItems.STEEL_TRIGGER.get()), 12, 5, 0.05f));
            trades.get(2).add(new BasicItemListing(new ItemStack(Items.EMERALD, 8),
                    new ItemStack(ModItems.STEEL_SPRING.get()), 12, 5, 0.05f));

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
                    new ItemStack(ModItems.CEMENTED_CARBIDE_BARREL.get()), 12, 10, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 20),
                    new ItemStack(ModItems.CEMENTED_CARBIDE_ACTION.get()), 10, 10, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                    new ItemStack(ModItems.CEMENTED_CARBIDE_SPRING.get()), 10, 10, 0.05f));
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 12),
                    new ItemStack(ModItems.CEMENTED_CARBIDE_TRIGGER.get()), 10, 10, 0.05f));

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
            trades.get(3).add(new BasicItemListing(new ItemStack(Items.EMERALD, 40),
                    new ItemStack(ModItems.MOSIN_NAGANT_BLUEPRINT.get()), 10, 25, 0.05f));

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
                    new ItemStack(ModItems.ROCKET.get(), 1), 16, 5, 0.05f));

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
            trades.get(4).add(new BasicItemListing(new ItemStack(ModItems.ROCKET.get(), 1),
                    new ItemStack(Items.EMERALD, 2), 32, 5, 0.05f));

            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 22),
                    new ItemStack(getItemHolder("poisonous_bullet"), 1), 4, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 24),
                    new ItemStack(getItemHolder("subsistence"), 1), 4, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 25),
                    new ItemStack(getItemHolder("kill_clip"), 1), 4, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 26),
                    new ItemStack(getItemHolder("gutshot_straight"), 1), 4, 10, 0.05f));
            trades.get(4).add(new BasicItemListing(new ItemStack(Items.EMERALD, 22),
                    new ItemStack(getItemHolder("head_seeker"), 1), 4, 10, 0.05f));

            // 等级 5 交易
            trades.get(5).add(new BasicItemListing(new ItemStack(Items.EMERALD, 64), new ItemStack(Items.EMERALD, 50),
                    new ItemStack(getItemHolder("beast_bullet"), 1), 1, 15, 0.05f));
        }
    }

    private static Holder<Item> getItemHolder(String name) {
        return ForgeRegistries.ITEMS.getHolder(new ResourceLocation(ModUtils.MODID, name)).orElse(new Holder.Direct<>(ItemStack.EMPTY.getItem()));
    }

    @SubscribeEvent
    public static void addWandererTrade(WandererTradesEvent event) {
        List<VillagerTrades.ItemListing> rareTrades = event.getRareTrades();

        rareTrades.add(new BasicItemListing(new ItemStack(Items.EMERALD, 16),
                SmallContainerBlockItem.createInstance(ModUtils.loc("containers/blueprints")), 10, 0, 0.05f));
    }
}
