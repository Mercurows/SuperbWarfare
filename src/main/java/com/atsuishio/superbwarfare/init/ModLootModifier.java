package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;
import thedarkcolour.kotlinforforge.neoforge.forge.ForgeKt;

import javax.annotation.ParametersAreNonnullByDefault;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModLootModifier {
    public static class TargetModLootTableModifier extends LootModifier {
        public static final MapCodec<TargetModLootTableModifier> CODEC =
                RecordCodecBuilder.mapCodec((instance) -> instance.group(IGlobalLootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter((glm) -> glm.conditions), ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter(TargetModLootTableModifier::table)).apply(instance, TargetModLootTableModifier::new));

        private final ResourceKey<LootTable> lootTable;

        public TargetModLootTableModifier(LootItemCondition[] conditions, ResourceKey<LootTable> lootTable) {
            super(conditions);
            this.lootTable = lootTable;
        }

        @Override
        @ParametersAreNonnullByDefault
        protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
            if (context.getLevel().getGameRules().getBoolean(ModGameRules.MOD_RULE_DO_GENERATE_LOOTS)) {
                context.getResolver().get(Registries.LOOT_TABLE, this.lootTable).ifPresent(table ->
                        table.value().getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), generatedLoot::add)));
            }
            return generatedLoot;
        }

        @Override
        public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
            return TARGET_LOOT_MODIFIER.get();
        }

        public ResourceKey<LootTable> table() {
            return this.lootTable;
        }
    }

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mod.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<TargetModLootTableModifier>> TARGET_LOOT_MODIFIER =
            LOOT_MODIFIERS.register(Mod.MODID + "_loot_modifier", () -> TargetModLootTableModifier.CODEC);

    @SubscribeEvent
    public static void register(FMLConstructModEvent event) {
        var bus = ForgeKt.getMOD_BUS();
        event.enqueueWork(() -> LOOT_MODIFIERS.register(bus));
    }
}
