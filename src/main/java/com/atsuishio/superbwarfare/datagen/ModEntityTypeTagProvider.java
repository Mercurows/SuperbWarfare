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
import net.minecraftforge.common.data.ExistingFileHelper;
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
                ModEntities.WIRE_GUIDE_MISSILE.get()
        );

        this.tag(ModTags.EntityTypes.DECOY).add(
                ModEntities.SMOKE_DECOY.get(),
                ModEntities.FLARE_DECOY.get()
        );

        this.tag(ModTags.EntityTypes.NO_EXPERIENCE).add(ModEntities.TARGET.get(), ModEntities.DPS_GENERATOR.get())
                .addOptional(new ResourceLocation("dummmmmmy", "target_dummy"))
                .addOptional(new ResourceLocation("powerful_dummy", "test_dummy"));

        this.tag(ModTags.EntityTypes.CAN_REPAIR).add(
                EntityType.IRON_GOLEM
        ).addOptional(new ResourceLocation("touhou_little_maid", "maid"));

        this.tag(ModTags.EntityTypes.MINE).add(
                ModEntities.BLU_43.get(),
                ModEntities.TM_62.get(),
                ModEntities.PTKM_1R.get(),
                ModEntities.CLAYMORE.get(),
                ModEntities.PTKM_PROJECTILE.get()
        );

        this.tag(ModTags.EntityTypes.AT_ROCKET).add(
                ModEntities.RPG_ROCKET_STANDARD.get(),
                ModEntities.RPG_ROCKET_TBG.get()
        );

        this.tag(ModTags.EntityTypes.AA_MISSILE).add(
                ModEntities.IGLA_MISSILE.get(),
                ModEntities.RU_9K33_MISSILE.get()
        );

        this.tag(ModTags.EntityTypes.SEEK_BLACKLIST).add(
                        EntityType.ITEM,
                        EntityType.ARMOR_STAND,
                        EntityType.EXPERIENCE_ORB,
                        EntityType.ITEM_DISPLAY,
                        EntityType.FALLING_BLOCK,
                        EntityType.ITEM_FRAME,
                        EntityType.FIREWORK_ROCKET,
                        EntityType.GLOW_ITEM_FRAME,
                        EntityType.AREA_EFFECT_CLOUD,
                        ModEntities.CLAYMORE.get(),
                        ModEntities.C4.get()
                ).addOptional(new ResourceLocation("touhou_little_maid", "power_point"))
                .addOptional(new ResourceLocation("evilcraft", "vengeance_spirit"))
                .addOptional(new ResourceLocation("mts", "builder_rendering"))
                .addOptional(new ResourceLocation("create", "carriage_contraption"))
                .addOptional(new ResourceLocation("create", "stationary_contraption"))
                .addOptional(new ResourceLocation("create", "gantry_contraption"))
                .addOptional(new ResourceLocation("create", "super_glue"))
                .addOptional(new ResourceLocation("zombiekit", "flares"));
    }

    public static TagKey<EntityType<?>> forgeTag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge", name));
    }
}
