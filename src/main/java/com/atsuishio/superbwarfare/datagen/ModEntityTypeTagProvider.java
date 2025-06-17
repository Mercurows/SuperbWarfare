package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModEntityTypeTagProvider extends EntityTypeTagsProvider {

    public ModEntityTypeTagProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(pOutput, pProvider, Mod.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider pProvider) {
        this.tag(ModTags.EntityTypes.AERIAL_BOMB).add(
                ModEntities.MELON_BOMB.get(),
                ModEntities.MK_82.get()
        );

        this.tag(ModTags.EntityTypes.DESTROYABLE_PROJECTILE).add(
                ModEntities.AGM_65.get(),
                ModEntities.JAVELIN_MISSILE.get(),
                ModEntities.MELON_BOMB.get(),
                ModEntities.MK_82.get(),
                ModEntities.SWARM_DRONE.get(),
                ModEntities.WG_MISSILE.get()
        );

        this.tag(ModTags.EntityTypes.DECOY).add(
                ModEntities.SMOKE_DECOY.get(),
                ModEntities.FLARE_DECOY.get()
        );
    }

    public static TagKey<EntityType<?>> cTag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", name));
    }
}
