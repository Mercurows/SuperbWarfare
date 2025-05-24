package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.advancement.criteria.OttoSprintTrigger;
import com.atsuishio.superbwarfare.advancement.criteria.RPGMeleeExplosionTrigger;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Codes Based on @Create
 */
public class ModAdvancementProvider implements DataProvider {

    private final PackOutput packOutput;
    private final ExistingFileHelper existingFileHelper;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public static final List<ModAdvancement> ADVANCEMENTS = new ArrayList<>();

    @SuppressWarnings("unused")
    public static ModAdvancement START = null,
    /**
     * Main
     */
    MAIN_ROOT = advancement("root", builder -> builder.icon(ModItems.TASER.get())
            .type(ModAdvancement.Type.SILENT)
            .awardedForFree()
            .rewardLootTable(Mod.loc("grant_manual"))),

    BEST_FRIEND = advancement("best_friend", builder -> builder.icon(ModItems.CLAYMORE_MINE.get())
            .whenIconCollected()
            .type(ModAdvancement.Type.SECRET)
            .parent(MAIN_ROOT)),

    BANZAI = advancement("banzai", builder -> builder.icon(ModItems.LUNGE_MINE.get())
            .whenIconCollected()
            .parent(MAIN_ROOT)),

    HAMMER = advancement("hammer", builder -> builder.icon(ModItems.HAMMER.get())
            .whenIconCollected()
            .parent(MAIN_ROOT)),

    PHYSICS_EXCALIBUR = advancement("physics_excalibur", builder -> builder.icon(ModItems.CROWBAR.get())
            .whenIconCollected()
            .parent(MAIN_ROOT)),

    CLEAN_ENERGY = advancement("clean_energy", builder -> builder.icon(ModItems.CHARGING_STATION.get())
            .whenIconCollected()
            .parent(PHYSICS_EXCALIBUR)),

    SUPER_CONTAINER = advancement("super_container", builder -> builder.icon(ModItems.CONTAINER.get())
            .whenIconCollected()
            .parent(CLEAN_ENERGY)),

    // 蓝图
    BLUEPRINT = advancement("blueprint", builder -> builder.icon(ModItems.TRACHELIUM_BLUEPRINT.get())
            .whenItemCollected(ModTags.Items.BLUEPRINT)
            .parent(MAIN_ROOT)),

    COMMON_BLUEPRINT = advancement("common_blueprint", builder -> builder.icon(ModItems.TRACHELIUM_BLUEPRINT.get())
            .whenItemCollected(ModTags.Items.COMMON_BLUEPRINT)
            .parent(BLUEPRINT)),

    RARE_BLUEPRINT = advancement("rare_blueprint", builder -> builder.icon(ModItems.TRACHELIUM_BLUEPRINT.get())
            .whenItemCollected(ModTags.Items.RARE_BLUEPRINT)
            .parent(COMMON_BLUEPRINT)),

    EPIC_BLUEPRINT = advancement("epic_blueprint", builder -> builder.icon(ModItems.TRACHELIUM_BLUEPRINT.get())
            .whenItemCollected(ModTags.Items.EPIC_BLUEPRINT)
            .parent(RARE_BLUEPRINT)),

    LEGENDARY_BLUEPRINT = advancement("legendary_blueprint", builder -> builder.icon(ModItems.TRACHELIUM_BLUEPRINT.get())
            .whenItemCollected(ModTags.Items.LEGENDARY_BLUEPRINT)
            .parent(EPIC_BLUEPRINT)),

    CANNON_BLUEPRINT = advancement("cannon_blueprint", builder -> builder.icon(ModItems.MK_42_BLUEPRINT.get())
            .whenItemCollected(ModTags.Items.CANNON_BLUEPRINT)
            .parent(BLUEPRINT)),

    // 古代芯片
    ANCIENT_TECHNOLOGY = advancement("ancient_technology", builder -> builder.icon(ModItems.ANCIENT_CPU.get())
            .whenIconCollected()
            .type(ModAdvancement.Type.GOAL)
            .parent(MAIN_ROOT)),

    ENCLAVE = advancement("enclave", builder -> builder.icon(ModItems.REFORGING_TABLE.get())
            .whenIconCollected()
            .type(ModAdvancement.Type.GOAL)
            .parent(ANCIENT_TECHNOLOGY)),

    // 哑弹棒（？）
    BOOMSTICK_MELEE = advancement("boomstick_melee", builder -> builder.icon(ModItems.ROCKET.get())
            .externalTrigger(RPGMeleeExplosionTrigger.TriggerInstance.get())
            .type(ModAdvancement.Type.SECRET_CHALLENGE)
            .parent(MAIN_ROOT)),

    RUSH_RUSH_RUN = advancement("rush_rush_run", builder -> builder.icon(ModItems.ELECTRIC_BATON.get())
            .externalTrigger(OttoSprintTrigger.TriggerInstance.get())
            .type(ModAdvancement.Type.SECRET_CHALLENGE)
            .parent(MAIN_ROOT)),

    END = null;


    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        this.packOutput = output;
        this.existingFileHelper = existingFileHelper;
        this.registries = lookupProvider;
    }

    private static ModAdvancement advancement(String id, UnaryOperator<ModAdvancement.Builder> b) {
        return new ModAdvancement(id, b);
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        PackOutput.PathProvider pathProvider = packOutput.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");

        return this.registries.thenCompose(provider -> {
            Consumer<AdvancementHolder> consumer = advancementHolder -> {
                ResourceLocation id = advancementHolder.id();
                if (existingFileHelper.exists(id, PackType.SERVER_DATA, ".json", "advancement")) {
                    throw new IllegalStateException("Duplicate advancement " + id);
                }
                Path path = pathProvider.json(advancementHolder.id());
                futures.add(DataProvider.saveStable(output, provider, Advancement.CODEC, advancementHolder.value(), path));
            };

            for (ModAdvancement advancement : ADVANCEMENTS) {
                advancement.save(consumer);
            }

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public @NotNull String getName() {
        return "Superb Warfare Advancements";
    }
}
