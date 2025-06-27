package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import com.atsuishio.superbwarfare.item.*;
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
                            energy.receiveEnergy(Integer.MAX_VALUE, false);
                            output.accept(stack);
                        });
                    }))
                    .build());

    public static final RegistryObject<CreativeModeTab> PERK_TAB = TABS.register("perk",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.superbwarfare.perk"))
                    .icon(() -> new ItemStack(ModItems.AP_BULLET.get()))
                    .withTabsBefore(GUN_TAB.getKey())
                    .displayItems((param, output) -> ModItems.PERKS.getEntries().forEach(registryObject -> output.accept(registryObject.get())))
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
                    .displayItems((param, output) -> ModItems.BLOCKS.getEntries().forEach(registryObject -> {
                        if (registryObject.get() == ModItems.CONTAINER.get()) {
                            RegisterContainersEvent.CONTAINERS.forEach(output::accept);
                        } else if (registryObject.get() == ModItems.SMALL_CONTAINER.get()) {
                            output.accept(registryObject.get());
                            SmallContainerBlockItem.SMALL_CONTAINER_LOOT_TABLES.stream().map(Supplier::get).forEach(output::accept);
                        } else {
                            output.accept(registryObject.get());
                        }
                    }))
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
