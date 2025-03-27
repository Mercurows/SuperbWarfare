package com.atsuishio.superbwarfare.compat;

//@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CompatHolder {

    public static final String DMV = "dreamaticvoyage";
    public static final String VRC = "virtuarealcraft";
    public static final String CLOTH_CONFIG = "cloth_config";

//    @ObjectHolder(registryName = "minecraft:mob_effect", value = DMV + ":bleeding")
//    public static final MobEffect DMV_BLEEDING = null;
//
//    @ObjectHolder(registryName = "minecraft:mob_effect", value = VRC + ":curse_flame")
//    public static final MobEffect VRC_CURSE_FLAME = null;
//
//    @ObjectHolder(registryName = "minecraft:entity_type", value = VRC + ":rain_shower_butterfly")
//    public static final EntityType<? extends Projectile> VRC_RAIN_SHOWER_BUTTERFLY = null;

//    @SubscribeEvent
//    public static void onInterModEnqueue(final InterModEnqueueEvent event) {
//        event.enqueueWork(() -> hasMod(CLOTH_CONFIG, () -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClothConfigHelper::registerScreen)));
//    }

//    public static void hasMod(String modid, Runnable runnable) {
//        if (ModList.get().isLoaded(modid)) {
//            runnable.run();
//        }
//    }
}
