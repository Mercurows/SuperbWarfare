package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import thedarkcolour.kotlinforforge.forge.ForgeKt;

import java.util.function.Supplier;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
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
        protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
            context.getResolver().getLootTable(lootTable).getRandomItemsRaw(context, generatedLoot::add);
            return generatedLoot;
        }

        @Override
        public Codec<? extends IGlobalLootModifier> codec() {
            return CODEC.get();
        }
    }

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mod.MODID);
    public static final RegistryObject<Codec<TargetModLootTableModifier>> LOOT_MODIFIER = LOOT_MODIFIERS.register(Mod.MODID + "_loot_modifier", TargetModLootTableModifier.CODEC);

    @SubscribeEvent
    public static void register(FMLConstructModEvent event) {
        IEventBus bus = ForgeKt.getMOD_BUS();
        event.enqueueWork(() -> LOOT_MODIFIERS.register(bus));
    }
}
