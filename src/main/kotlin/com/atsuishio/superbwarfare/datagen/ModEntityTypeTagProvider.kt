package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.EntityTypeTagsProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraftforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class ModEntityTypeTagProvider(
    pOutput: PackOutput,
    pProvider: CompletableFuture<HolderLookup.Provider>,
    existingFileHelper: ExistingFileHelper
) : EntityTypeTagsProvider(pOutput, pProvider, Mod.MODID, existingFileHelper) {
    override fun addTags(pProvider: HolderLookup.Provider) {
        this.tag(ModTags.EntityTypes.AERIAL_BOMB).add(
            ModEntities.MELON_BOMB.get(),
            ModEntities.MK_82.get(),
            ModEntities.MK_84.get(),
            ModEntities.BOR_57.get(),
            ModEntities.SC_50.get(),
            ModEntities.SC_250.get(),
            ModEntities.RU_3M14_MISSILE.get()
        )

        this.tag(ModTags.EntityTypes.DESTROYABLE_PROJECTILE).add(
            ModEntities.AGM_65.get(),
            ModEntities.JAVELIN_MISSILE.get(),
            ModEntities.MELON_BOMB.get(),
            ModEntities.MK_82.get(),
            ModEntities.MK_84.get(),
            ModEntities.BOR_57.get(),
            ModEntities.SWARM_DRONE.get(),
            ModEntities.WIRE_GUIDE_MISSILE.get(),
            ModEntities.RU_3M14_MISSILE.get()

        )

        this.tag(ModTags.EntityTypes.DECOY).add(
            ModEntities.SMOKE_DECOY.get(),
            ModEntities.FLARE_DECOY.get()
        )

        this.tag(ModTags.EntityTypes.NO_EXPERIENCE).add(ModEntities.TARGET.get(), ModEntities.DPS_GENERATOR.get())
            .addOptional(ResourceLocation("dummmmmmy", "target_dummy"))
            .addOptional(ResourceLocation("powerful_dummy", "test_dummy"))

        this.tag(ModTags.EntityTypes.CAN_REPAIR).add(
            EntityType.IRON_GOLEM
        ).addOptional(ResourceLocation("touhou_little_maid", "maid"))

        this.tag(ModTags.EntityTypes.MINE).add(
            ModEntities.BLU_43.get(),
            ModEntities.TM_62.get(),
            ModEntities.PTKM_1R.get(),
            ModEntities.CLAYMORE.get(),
            ModEntities.PTKM_PROJECTILE.get()
        )

        this.tag(ModTags.EntityTypes.AT_ROCKET).add(
            ModEntities.RPG_ROCKET_STANDARD.get(),
            ModEntities.RPG_ROCKET_TBG.get()
        )

        this.tag(ModTags.EntityTypes.AA_MISSILE).add(
            ModEntities.IGLA_MISSILE.get(),
            ModEntities.RU_9M336_MISSILE.get(),
            ModEntities.RU_9M100_MISSILE.get()
        )

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
        ).addOptional(ResourceLocation("touhou_little_maid", "power_point"))
            .addOptional(ResourceLocation("evilcraft", "vengeance_spirit"))
            .addOptional(ResourceLocation("mts", "builder_rendering"))
            .addOptional(ResourceLocation("create", "carriage_contraption"))
            .addOptional(ResourceLocation("create", "stationary_contraption"))
            .addOptional(ResourceLocation("create", "gantry_contraption"))
            .addOptional(ResourceLocation("create", "super_glue"))
            .addOptional(ResourceLocation("zombiekit", "flares"))

        this.tag(ModTags.EntityTypes.BIOGAS_GENERATOR_WHITELIST).add(
            EntityType.PLAYER,
            EntityType.VILLAGER,
            EntityType.WANDERING_TRADER,
            ModEntities.SENPAI.get()
        ).addOptional(ResourceLocation("touhou_little_maid", "maid"))
    }

    companion object {
        fun forgeTag(name: String): TagKey<EntityType<*>> {
            return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation("forge", name))
        }
    }
}
