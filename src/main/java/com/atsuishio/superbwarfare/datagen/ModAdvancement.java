package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Codes Based on @Create
 */
@SuppressWarnings("unused")
public class ModAdvancement {

    public static final ResourceLocation MAIN_BACKGROUND = Mod.loc("textures/block/sandbag.png");
    public static final ResourceLocation LEGENDARY_BACKGROUND = Mod.loc("textures/block/steel_block.png");

    private final Advancement.Builder builder;
    private ModAdvancement parent;
    public AdvancementHolder result;
    private final String id;
    private final Group group;

    public ModAdvancement(String id, UnaryOperator<Builder> b) {
        this.builder = Advancement.Builder.advancement();
        this.id = id;

        Builder builtInBuilder = new Builder();
        b.apply(builtInBuilder);
        this.group = builtInBuilder.group;

        Optional<ResourceLocation> bg = Optional.empty();
        if (id.equals("root")) {
            if (group == Group.MAIN) {
                bg = Optional.of(MAIN_BACKGROUND);
            }
            if (group == Group.LEGENDARY) {
                bg = Optional.of(LEGENDARY_BACKGROUND);
            }
        }

        builder.display(new DisplayInfo(builtInBuilder.icon, titleComponent(),
                Component.translatable(description()), bg,
                builtInBuilder.type.frame, builtInBuilder.type.toast, builtInBuilder.type.announce, builtInBuilder.type.hide));

        ModAdvancementProvider.ADVANCEMENTS.add(this);
    }

    private String title() {
        return Mod.MODID + ".advancement." + group.path + "." + id;
    }

    private Component titleComponent() {
        if (this.group == Group.LEGENDARY && !this.id.equals("root")) {
            return Component.translatable(title()).withStyle(ChatFormatting.GOLD);
        }
        return Component.translatable(title());
    }

    private String description() {
        return title() + ".des";
    }

    public void save(Consumer<AdvancementHolder> t) {
        if (parent != null) {
            builder.parent(parent.result);
        }

        AdvancementHolder advancementholder = builder.build(Mod.loc(group.path + "/" + id));
        t.accept(advancementholder);
        result = advancementholder;
    }

    enum Type {
        DEFAULT(AdvancementType.TASK, true, true, false),
        DEFAULT_NO_ANNOUNCE(AdvancementType.TASK, true, false, false),
        DEFAULT_CHALLENGE(AdvancementType.CHALLENGE, true, true, false),
        SILENT(AdvancementType.TASK, false, false, false),
        GOAL(AdvancementType.GOAL, true, true, false),
        SECRET(AdvancementType.TASK, true, true, true),
        SECRET_CHALLENGE(AdvancementType.CHALLENGE, true, true, true);

        private final AdvancementType frame;
        private final boolean toast;
        private final boolean announce;
        private final boolean hide;

        Type(AdvancementType frame, boolean toast, boolean announce, boolean hide) {
            this.frame = frame;
            this.toast = toast;
            this.announce = announce;
            this.hide = hide;
        }
    }

    enum Group {
        MAIN("main"),
        LEGENDARY("legendary");

        public final String path;

        Group(String path) {
            this.path = path;
        }
    }

    public class Builder {

        private Type type = Type.DEFAULT;
        private int keyIndex;
        private ItemStack icon;
        private Group group = Group.MAIN;

        Builder type(Type type) {
            this.type = type;
            return this;
        }

        Builder parent(ModAdvancement other) {
            ModAdvancement.this.parent = other;
            return this;
        }

        Builder icon(ItemLike item) {
            return icon(new ItemStack(item));
        }

        Builder icon(ItemStack stack) {
            icon = stack;
            return this;
        }

        Builder whenBlockPlaced(Block block) {
            return externalTrigger(ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block));
        }

        Builder whenIconCollected() {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(icon.getItem()));
        }

        Builder whenItemCollected(ItemLike itemProvider) {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(itemProvider));
        }

        Builder whenItemCollected(TagKey<Item> tag) {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance
                    .hasItems(ItemPredicate.Builder.item().of(tag).build()));
        }

        Builder whenItemConsumed(ItemLike itemProvider) {
            return externalTrigger(ConsumeItemTrigger.TriggerInstance.usedItem(itemProvider));
        }

        Builder whenIconConsumed() {
            return externalTrigger(ConsumeItemTrigger.TriggerInstance.usedItem(icon.getItem()));
        }

        Builder awardedForFree() {
            return externalTrigger(PlayerTrigger.TriggerInstance.tick());
        }

        Builder whenEffectChanged(MobEffectsPredicate.Builder predicate) {
            return externalTrigger(EffectsChangedTrigger.TriggerInstance.hasEffects(predicate));
        }

        Builder externalTrigger(Criterion<?> trigger) {
            builder.addCriterion(String.valueOf(keyIndex), trigger);
            keyIndex++;
            return this;
        }

        Builder requirement(AdvancementRequirements requirements) {
            builder.requirements(requirements);
            return this;
        }

        Builder group(Group group) {
            this.group = group;
            return this;
        }

        Builder rewardExp(int exp) {
            builder.rewards(AdvancementRewards.Builder.experience(exp).build());
            return this;
        }

        Builder rewardLootTable(ResourceLocation location) {
            builder.rewards(AdvancementRewards.Builder.loot(ResourceKey.create(Registries.LOOT_TABLE, location)).build());
            return this;
        }
    }
}
