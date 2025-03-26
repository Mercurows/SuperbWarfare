package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.ModUtils;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@EventBusSubscriber(modid = ModUtils.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModLootModifier {
    // TODO loot table modifier
//    public static class TargetModLootTableModifier extends LootModifier {
//        public static final Supplier<Codec<TargetModLootTableModifier>> CODEC = Suppliers
//                .memoize(() -> RecordCodecBuilder.create(instance -> codecStart(instance).and(ResourceLocation.CODEC.fieldOf("lootTable").forGetter(m -> m.lootTable)).apply(instance, TargetModLootTableModifier::new)));
//        private final ResourceLocation lootTable;
//
//        public TargetModLootTableModifier(LootItemCondition[] conditions, ResourceLocation lootTable) {
//            super(conditions);
//            this.lootTable = lootTable;
//        }
//
//        @Override
//        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
//            context.getResolver().getLootTable(lootTable).getRandomItemsRaw(context, generatedLoot::add);
//            return generatedLoot;
//        }
//
//        @Override
//        public MapCodec<? extends IGlobalLootModifier> codec() {
//            return CODEC;
//        }
//
////        @Override
////        public Codec<? extends IGlobalLootModifier> codec() {
////            return CODEC.get();
////        }
//    }

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ModUtils.MODID);
//    public static final DeferredHolder<Codec<TargetModLootTableModifier>, Codec<TargetModLootTableModifier>> LOOT_MODIFIER = LOOT_MODIFIERS.register(ModUtils.MODID + "_loot_modifier", TargetModLootTableModifier.CODEC);

    @SubscribeEvent
    public static void register(FMLConstructModEvent event) {
//        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
//        event.enqueueWork(() -> LOOT_MODIFIERS.register(bus));
    }
}
