package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.ModUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModUtils.MODID);

//    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GUN_TAB = TABS.register("guns",
//            () -> CreativeModeTab.builder()
//                    .title(Component.translatable("item_group.superbwarfare.guns"))
//                    .icon(() -> new ItemStack(ModItems.TASER.get()))
//                    .displayItems(
//                            (param, output) -> {
//                                output.accept(TaserItem.getGunInstance());
//                                output.accept(Glock17Item.getGunInstance());
//                                output.accept(Glock18Item.getGunInstance());
//                                output.accept(M1911Item.getGunInstance());
//                                output.accept(Mp443Item.getGunInstance());
//                                output.accept(HomemadeShotgunItem.getGunInstance());
//                                output.accept(Trachelium.getGunInstance());
//                                output.accept(VectorItem.getGunInstance());
//                                output.accept(SksItem.getGunInstance());
//                                output.accept(AK47Item.getGunInstance());
//                                output.accept(AK12Item.getGunInstance());
//                                output.accept(M4Item.getGunInstance());
//                                output.accept(Hk416Item.getGunInstance());
//                                output.accept(Qbz95Item.getGunInstance());
//                                output.accept(Mk14Item.getGunInstance());
//                                output.accept(MarlinItem.getGunInstance());
//                                output.accept(K98Item.getGunInstance());
//                                output.accept(MosinNagantItem.getGunInstance());
//                                output.accept(SvdItem.getGunInstance());
//                                output.accept(HuntingRifleItem.getGunInstance());
//                                output.accept(M98bItem.getGunInstance());
//                                output.accept(SentinelItem.getGunInstance());
//                                output.accept(Ntw20Item.getGunInstance());
//                                output.accept(M870Item.getGunInstance());
//                                output.accept(Aa12Item.getGunInstance());
//                                output.accept(DevotionItem.getGunInstance());
//                                output.accept(RpkItem.getGunInstance());
//                                output.accept(M60Item.getGunInstance());
//                                output.accept(MinigunItem.getGunInstance());
//                                output.accept(BocekItem.getGunInstance());
//                                output.accept(M79Item.getGunInstance());
//                                output.accept(SecondaryCataclysm.getGunInstance());
//                                output.accept(RpgItem.getGunInstance());
//                                output.accept(JavelinItem.getGunInstance());
//                            }
//                    )
//                    .build());
//
public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PERK_TAB = TABS.register("perk",
        () -> CreativeModeTab.builder()
                .title(Component.translatable("item_group.superbwarfare.perk"))
                .icon(() -> new ItemStack(ModItems.AP_BULLET.get()))
//                    .withTabsBefore(GUN_TAB.getKey())
                .displayItems((param, output) -> ModItems.PERKS.getEntries().forEach(registryObject -> output.accept(registryObject.get())))
                .build());
//
public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AMMO_TAB = TABS.register("ammo",
        () -> CreativeModeTab.builder()
                .title(Component.translatable("item_group.superbwarfare.ammo"))
//                    .icon(() -> new ItemStack(ModItems.SHOTGUN_AMMO_BOX.get()))
                .withTabsBefore(PERK_TAB.getKey())
                .displayItems((param, output) -> {
                    ModItems.AMMO.getEntries().forEach(registryObject -> {
//                            if (registryObject.get() != ModItems.POTION_MORTAR_SHELL.get()) {
//                                output.accept(registryObject.get());
//
//                                if (registryObject.get() == ModItems.C4_BOMB.get()) {
//                                    output.accept(C4Bomb.makeInstance());
//                                }
//                            }
                    });

//                        param.holders().lookup(Registries.POTION)
//                                .ifPresent(potion -> generatePotionEffectTypes(output, potion, ModItems.POTION_MORTAR_SHELL.get()));
                })
                .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEM_TAB = TABS.register("item",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.superbwarfare.item"))
                    .icon(() -> new ItemStack(ModItems.FIRING_PARAMETERS.get()))
                    .displayItems((param, output) -> ModItems.ITEMS.getEntries().forEach(registryObject -> output.accept(registryObject.get())))
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOCK_TAB = TABS.register("block",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.superbwarfare.block"))
                    .icon(() -> new ItemStack(ModItems.SANDBAG.get()))
                    .withTabsBefore(ITEM_TAB.getKey())
                    .displayItems((param, output) -> ModItems.BLOCKS.getEntries().forEach(registryObject -> output.accept(registryObject.get())))
                    .build());

    @SubscribeEvent
    public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
//        if (tabData.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
//            tabData.accept(ModItems.SENPAI_SPAWN_EGG.get());
//        }
    }

//    private static void generatePotionEffectTypes(CreativeModeTab.Output output, HolderLookup<Potion> potions, Item potionItem) {
//        potions.listElements().filter(potion -> !potion.is(Potions.EMPTY_ID))
//                .map(potion -> PotionUtils.setPotion(new ItemStack(potionItem), potion.value()))
//                .forEach(output::accept);
//    }
}
