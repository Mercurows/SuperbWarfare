package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import com.atsuishio.superbwarfare.item.ArmorPlate;
import com.atsuishio.superbwarfare.item.BatteryItem;
import com.atsuishio.superbwarfare.item.C4BombItem;
import com.atsuishio.superbwarfare.item.ElectricBaton;
import com.atsuishio.superbwarfare.item.common.container.LuckyContainerBlockItem;
import com.atsuishio.superbwarfare.item.common.container.SmallContainerBlockItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
@SuppressWarnings("unused")
public class ModTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mod.MODID);

    public static final RegistryObject<CreativeModeTab> GUN_TAB = TABS.register("guns",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.superbwarfare.guns"))
                    .icon(() -> new ItemStack(ModItems.TASER.get()))
                    .displayItems((param, output) -> ModItems.GUNS.getEntries().forEach(registryObject -> {
                        output.accept(registryObject.get());

                        var stack = new ItemStack(registryObject.get());
                        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
                            if (energy.getMaxEnergyStored() > 0) {
                                energy.receiveEnergy(Integer.MAX_VALUE, false);
                                output.accept(stack);
                            }
                        });
                    }))
                    .build());

    public static final RegistryObject<CreativeModeTab> PERK_TAB = TABS.register("perk",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.superbwarfare.perk"))
                    .icon(() -> new ItemStack(ModItems.AP_BULLET.get()))
                    .withTabsBefore(GUN_TAB.getKey())
                    .displayItems((param, output) -> {
                        output.accept(ModItems.REFORGING_TABLE.get());
                        ModItems.PERKS.getEntries().forEach(registryObject -> output.accept(registryObject.get()));
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> AMMO_TAB = TABS.register("ammo",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.superbwarfare.ammo"))
                    .icon(() -> new ItemStack(ModItems.SHOTGUN_AMMO_BOX.get()))
                    .withTabsBefore(PERK_TAB.getKey())
                    .displayItems((param, output) -> {
                        ModItems.AMMO.getEntries().forEach(registryObject -> {
                            if (registryObject.get() != ModItems.POTION_MORTAR_SHELL.get()) {
                                output.accept(registryObject.get());

                                if (registryObject.get() == ModItems.C4_BOMB.get()) {
                                    output.accept(C4BombItem.makeInstance());
                                }
                            }
                        });

                        param.holders().lookup(Registries.POTION)
                                .ifPresent(potion -> generatePotionEffectTypes(output, potion, ModItems.POTION_MORTAR_SHELL.get()));
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> ITEM_TAB = TABS.register("item",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.superbwarfare.item"))
                    .icon(() -> new ItemStack(ModItems.TARGET_DEPLOYER.get()))
                    .withTabsBefore(AMMO_TAB.getKey())
                    .displayItems((param, output) -> ModItems.ITEMS.getEntries().forEach(registryObject -> {
                        output.accept(registryObject.get());
                        if (registryObject.get() == ModItems.ARMOR_PLATE.get()) {
                            output.accept(ArmorPlate.getInfiniteInstance());
                        }
                        if (registryObject.get() instanceof BatteryItem batteryItem) {
                            output.accept(batteryItem.makeFullEnergyStack());
                        }
                        if (registryObject.get() == ModItems.ELECTRIC_BATON.get()) {
                            output.accept(ElectricBaton.makeFullEnergyStack());
                        }
                    }))
                    .build());

    public static final RegistryObject<CreativeModeTab> BLOCK_TAB = TABS.register("block",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.superbwarfare.block"))
                    .icon(() -> new ItemStack(ModItems.SANDBAG.get()))
                    .withTabsBefore(ITEM_TAB.getKey())
                    .displayItems((param, output) -> ModItems.BLOCKS.getEntries().forEach(registryObject -> output.accept(registryObject.get())))
                    .build());

    public static final RegistryObject<CreativeModeTab> VEHICLE_TAB = TABS.register("vehicle",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.superbwarfare.vehicle"))
                    .icon(() -> new ItemStack(ModItems.CONTAINER.get()))
                    .withTabsBefore(BLOCK_TAB.getKey())
                    .displayItems((param, output) -> {
                        output.accept(ModItems.CROWBAR.get());
                        output.accept(ModItems.VEHICLE_ASSEMBLING_TABLE.get());

                        RegisterContainersEvent.CONTAINERS.forEach(output::accept);

                        output.accept(ModItems.LUCKY_CONTAINER.get());
                        LuckyContainerBlockItem.LUCKY_CONTAINERS.stream().map(Supplier::get).forEach(output::accept);

                        output.accept(ModItems.SMALL_CONTAINER.get());
                        SmallContainerBlockItem.SMALL_CONTAINERS.stream().map(Supplier::get).forEach(output::accept);
                    })
                    .build());

    @SubscribeEvent
    public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
        if (tabData.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            tabData.accept(ModItems.SENPAI_SPAWN_EGG.get());
        }
    }

    private static void generatePotionEffectTypes(CreativeModeTab.Output output, HolderLookup<Potion> potions, Item potionItem) {
        potions.listElements().filter(potion -> !potion.is(Potions.EMPTY_ID))
                .map(potion -> PotionUtils.setPotion(new ItemStack(potionItem), potion.value()))
                .forEach(output::accept);
    }
}
