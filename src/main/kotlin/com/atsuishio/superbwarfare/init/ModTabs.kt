package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent
import com.atsuishio.superbwarfare.item.container.LuckyContainerBlockItem
import com.atsuishio.superbwarfare.item.container.SmallContainerBlockItem
import com.atsuishio.superbwarfare.item.material.BatteryItem
import com.atsuishio.superbwarfare.item.misc.ArmorPlateItem
import com.atsuishio.superbwarfare.item.projectile.C4BombItem
import com.atsuishio.superbwarfare.item.weapon.ElectricBatonItem
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTab.*
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionContents
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

@EventBusSubscriber
@Suppress("unused")
object ModTabs {

    @JvmField
    val TABS: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, com.atsuishio.superbwarfare.Mod.MODID)

    @JvmStatic
    val GUN_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> = TABS.register(
        "guns",
        Supplier {
            builder()
                .title(Component.translatable("item_group.superbwarfare.guns"))
                .icon { ItemStack(ModItems.TASER.get()) }
                .displayItems { param, output ->
                    ModItems.GUNS.getEntries().forEach { registryObject ->
                        if (registryObject === ModItems.VEHICLE_GUN) return@forEach

                        // 普通枪械
                        val stack = ItemStack(registryObject.get())
                        output.accept(stack)

                        // 充电后枪械
                        val newStack = stack.copy()
                        val cap = newStack.getCapability(Capabilities.EnergyStorage.ITEM)
                        if (cap != null && cap.maxEnergyStored > 0) {
                            cap.receiveEnergy(Int.MAX_VALUE, false)
                            output.accept(newStack)
                        }
                    }
                }
                .build()
        })

    @JvmStatic
    val PERK_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> = TABS.register(
        "perk",
        Supplier {
            builder()
                .title(Component.translatable("item_group.superbwarfare.perk"))
                .icon { ItemStack(ModItems.AP_BULLET!!.get()) }
                .withTabsBefore(GUN_TAB.getKey())
                .displayItems { param, output ->
                    output.accept(ModItems.REFORGING_TABLE.get())

                    ModItems.PERKS.getEntries().forEach { registryObject ->
                        output.accept(registryObject.get())
                    }
                }
                .build()
        })

    @JvmStatic
    val AMMO_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> = TABS.register(
        "ammo",
        Supplier {
            builder()
                .title(Component.translatable("item_group.superbwarfare.ammo"))
                .icon { ItemStack(ModItems.SHOTGUN_AMMO_BOX.get()) }
                .withTabsBefore(PERK_TAB.getKey())
                .displayItems { param, output ->
                    ModItems.AMMO.getEntries().forEach { registryObject ->
                        if (registryObject.get() !== ModItems.POTION_MORTAR_SHELL.get()) {
                            output.accept(registryObject.get())

                            if (registryObject.get() === ModItems.C4_BOMB.get()) {
                                output.accept(C4BombItem.makeInstance())
                            }
                        }
                    }

                    param.holders().lookup(Registries.POTION).ifPresent { potion ->
                        generatePotionEffectTypes(
                            output, potion, ModItems.POTION_MORTAR_SHELL.get(),
                            TabVisibility.PARENT_AND_SEARCH_TABS,
                            param.enabledFeatures()
                        )
                    }
                }
                .build()
        })

    @JvmStatic
    val ITEM_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> = TABS.register("item", Supplier {
        builder()
            .title(Component.translatable("item_group.superbwarfare.item"))
            .icon { ItemStack(ModItems.TARGET_DEPLOYER.get()) }
            .withTabsBefore(AMMO_TAB.getKey())
            .displayItems { param, output ->
                ModItems.ITEMS.getEntries().forEach { registryObject ->
                    val item = registryObject.get()
                    output.accept(item)

                    if (item === ModItems.ARMOR_PLATE.get()) {
                        output.accept(ArmorPlateItem.getInfiniteInstance())
                    }
                    if (item is BatteryItem) {
                        output.accept(item.makeFullEnergyStack())
                    }
                    if (item === ModItems.ELECTRIC_BATON.get()) {
                        output.accept(ElectricBatonItem.makeFullEnergyStack())
                    }
                }
            }
            .build()
    })

    @JvmStatic
    val BLOCK_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> = TABS.register(
        "block",
        Supplier {
            builder()
                .title(Component.translatable("item_group.superbwarfare.block"))
                .icon { ItemStack(ModItems.SANDBAG.get()) }
                .withTabsBefore(ITEM_TAB.getKey())
                .displayItems { param, output ->
                    ModItems.BLOCKS.getEntries().forEach { output.accept(it.get()) }
                }
                .build()
        })

    @JvmStatic
    val VEHICLE_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> = TABS.register(
        "vehicle",
        Supplier {
            builder()
                .title(Component.translatable("item_group.superbwarfare.vehicle"))
                .icon { ItemStack(ModItems.CONTAINER.get()) }
                .withTabsBefore(BLOCK_TAB.getKey())
                .displayItems { param, output ->
                    output.accept(ModItems.CROWBAR.get())
                    output.accept(ModItems.VEHICLE_ASSEMBLING_TABLE.get())

                    RegisterContainersEvent.CONTAINERS.forEach { output.accept(it) }

                    output.accept(ModItems.LUCKY_CONTAINER.get())
                    LuckyContainerBlockItem.LUCKY_CONTAINERS.stream()
                        .map { it() }
                        .forEach { output.accept(it) }

                    output.accept(ModItems.SMALL_CONTAINER.get())
                    SmallContainerBlockItem.SMALL_CONTAINERS.stream()
                        .map { it() }
                        .forEach { output.accept(it) }
                }
                .build()
        })


    @SubscribeEvent
    fun buildTabContentsVanilla(tabData: BuildCreativeModeTabContentsEvent) {
        if (tabData.tabKey === CreativeModeTabs.SPAWN_EGGS) {
            tabData.accept(ModItems.SENPAI_SPAWN_EGG.get())
            tabData.accept(ModItems.STEEL_COIL_SPAWN_EGG.get())
        }
    }

    private fun generatePotionEffectTypes(
        output: Output,
        potions: HolderLookup<Potion>,
        item: Item,
        visibility: TabVisibility,
        requiredFeatures: FeatureFlagSet
    ) {
        potions.listElements()
            .filter { potion -> potion.value().isEnabled(requiredFeatures) }
            .map { potion -> PotionContents.createItemStack(item, potion) }
            .forEach { itemStack -> output.accept(itemStack, visibility) }
    }
}