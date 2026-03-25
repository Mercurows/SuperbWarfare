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
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.world.item.alchemy.Potions
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.RegistryObject

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
@Suppress("unused")
object ModTabs {
    @JvmField
    val TABS: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, com.atsuishio.superbwarfare.Mod.MODID)

    @JvmField
    val GUN_TAB: RegistryObject<CreativeModeTab> = TABS.register("guns") {
        CreativeModeTab.builder()
            .title(Component.translatable("item_group.superbwarfare.guns"))
            .icon { ItemStack(ModItems.TASER.get()) }
            .displayItems { param: CreativeModeTab.ItemDisplayParameters, output: CreativeModeTab.Output ->
                ModItems.GUNS.getEntries().forEach {
                    if (it === ModItems.VEHICLE_GUN) return@forEach
                    output.accept(it.get())

                    val stack = ItemStack(it.get())
                    stack.getCapability(ForgeCapabilities.ENERGY)
                        .ifPresent { storage ->
                            if (storage.maxEnergyStored > 0) {
                                storage.receiveEnergy(Int.MAX_VALUE, false)
                                output.accept(stack)
                            }
                        }
                }
            }
            .build()
    }

    @JvmField
    val PERK_TAB: RegistryObject<CreativeModeTab> = TABS.register("perk") {
        CreativeModeTab.builder()
            .title(Component.translatable("item_group.superbwarfare.perk"))
            .icon { ItemStack(ModItems.AP_BULLET!!.get()) }
            .withTabsBefore(GUN_TAB.getKey())
            .displayItems { param: CreativeModeTab.ItemDisplayParameters, output: CreativeModeTab.Output ->
                output.accept(ModItems.REFORGING_TABLE.get())
                ModItems.PERKS.getEntries().forEach { output.accept(it.get()) }
            }
            .build()
    }

    @JvmField
    val AMMO_TAB: RegistryObject<CreativeModeTab> = TABS.register("ammo") {
        CreativeModeTab.builder()
            .title(Component.translatable("item_group.superbwarfare.ammo"))
            .icon { ItemStack(ModItems.SHOTGUN_AMMO_BOX.get()) }
            .withTabsBefore(PERK_TAB.getKey())
            .displayItems { param: CreativeModeTab.ItemDisplayParameters, output: CreativeModeTab.Output ->
                ModItems.AMMO.getEntries().forEach {
                    if (it.get() !== ModItems.POTION_MORTAR_SHELL.get()) {
                        output.accept(it.get())

                        if (it.get() === ModItems.C4_BOMB.get()) {
                            output.accept(C4BombItem.makeInstance())
                        }
                    }
                }
                param.holders().lookup(Registries.POTION)
                    .ifPresent { generatePotionEffectTypes(output, it, ModItems.POTION_MORTAR_SHELL.get()) }
            }
            .build()
    }

    @JvmField
    val ITEM_TAB: RegistryObject<CreativeModeTab> = TABS.register("item") {
        CreativeModeTab.builder()
            .title(Component.translatable("item_group.superbwarfare.item"))
            .icon { ItemStack(ModItems.TARGET_DEPLOYER.get()) }
            .withTabsBefore(AMMO_TAB.getKey())
            .displayItems { param: CreativeModeTab.ItemDisplayParameters, output: CreativeModeTab.Output ->
                ModItems.ITEMS.getEntries().forEach {
                    val item = it.get()
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
    }

    @JvmField
    val BLOCK_TAB: RegistryObject<CreativeModeTab> = TABS.register("block") {
        CreativeModeTab.builder()
            .title(Component.translatable("item_group.superbwarfare.block"))
            .icon { ItemStack(ModItems.SANDBAG.get()) }
            .withTabsBefore(ITEM_TAB.getKey())
            .displayItems { param: CreativeModeTab.ItemDisplayParameters, output: CreativeModeTab.Output ->
                ModItems.BLOCKS.getEntries().forEach { output.accept(it.get()) }
            }
            .build()
    }

    @JvmField
    val VEHICLE_TAB: RegistryObject<CreativeModeTab> = TABS.register("vehicle") {
        CreativeModeTab.builder()
            .title(Component.translatable("item_group.superbwarfare.vehicle"))
            .icon { ItemStack(ModItems.CONTAINER.get()) }
            .withTabsBefore(BLOCK_TAB.getKey())
            .displayItems { param: CreativeModeTab.ItemDisplayParameters, output: CreativeModeTab.Output ->
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
    }

    @SubscribeEvent
    fun buildTabContentsVanilla(tabData: BuildCreativeModeTabContentsEvent) {
        if (tabData.tabKey === CreativeModeTabs.SPAWN_EGGS) {
            tabData.accept(ModItems.SENPAI_SPAWN_EGG.get())
            tabData.accept(ModItems.STEEL_COIL_SPAWN_EGG.get())
        }
    }

    private fun generatePotionEffectTypes(
        output: CreativeModeTab.Output,
        potions: HolderLookup<Potion>,
        potionItem: Item
    ) {
        potions.listElements().filter { !it.`is`(Potions.EMPTY_ID) }
            .map { PotionUtils.setPotion(ItemStack(potionItem), it.value()) }
            .forEach { output.accept(it) }
    }
}