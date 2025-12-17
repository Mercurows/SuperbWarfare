package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModLootModifier {
    public static class TargetModLootTableModifier extends LootModifier {
        public static final Supplier<Codec<TargetModLootTableModifier>> CODEC = Suppliers
                .memoize(() -> RecordCodecBuilder.create(instance -> codecStart(instance).and(ResourceLocation.CODEC.fieldOf("lootTable").forGetter(m -> m.lootTable)).apply(instance, TargetModLootTableModifier::new)));
        private final ResourceLocation lootTable;

        public TargetModLootTableModifier(LootItemCondition[] conditions, ResourceLocation lootTable) {
            super(conditions);
            this.lootTable = lootTable;
        }

        @Override
        @ParametersAreNonnullByDefault
        protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
            // TODO 如何正确实现这个？
//            if (context.getLevel().getGameRules().getBoolean(ModGameRules.MOD_RULE_DO_GENERATE_LOOTS)) {
//                context.getResolver().getLootTable(lootTable).getRandomItemsRaw(context, generatedLoot::add);
//            }
            return generatedLoot;
        }

        @Override
        public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
            // TODO 如何正确实现这个？
            return null;
//            return CODEC.get();
        }
    }

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mod.MODID);
    // TODO 如何正确实现这个？
//    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER = LOOT_MODIFIERS.register(Mod.MODID + "_loot_modifier", () -> TargetModLootTableModifier.CODEC);

    @SubscribeEvent
    public static void register(FMLConstructModEvent event) {
//        var bus = ForgeKt.getMOD_BUS();
//        event.enqueueWork(() -> LOOT_MODIFIERS.register(bus));
    }
}
