package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.DamageTypeTagsProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.DamageTypeTags
import net.minecraft.tags.TagKey
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.damagesource.DamageTypes
import net.minecraftforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class ModDamageTypeTagProvider(
    pOutput: PackOutput,
    pLookupProvider: CompletableFuture<HolderLookup.Provider>,
    existingFileHelper: ExistingFileHelper
) : DamageTypeTagsProvider(pOutput, pLookupProvider, Mod.MODID, existingFileHelper) {
    override fun addTags(pProvider: HolderLookup.Provider) {
        this.tag(ModTags.DamageTypes.PROJECTILE).add(
            ModDamageTypes.GUN_FIRE,
            ModDamageTypes.GUN_FIRE_HEADSHOT,
            DamageTypes.ARROW,
            DamageTypes.TRIDENT,
            DamageTypes.THROWN,
            ModDamageTypes.SUPER_STAR_HIT,
            ModDamageTypes.SUPER_STAR_SLASH
        )
            .addOptional(ResourceLocation("tacz", "bullet"))
            .addOptional(ResourceLocation("tacz", "bullet_void"))
            .addOptional(ResourceLocation("virtuarealcraft", "rain_crystal"))
            .addOptional(ResourceLocation("virtuarealcraft", "rain_shower_butterfly"))
            .addOptional(ResourceLocation("virtuarealcraft", "sparkle_butterfly"))
            .addOptional(ResourceLocation("dreamaticvoyage", "blood_crystal"))
            .addOptional(ResourceLocation("dreamaticvoyage", "leviy_beam"))
        this.tag(ModTags.DamageTypes.PROJECTILE_ABSOLUTE)
            .add(ModDamageTypes.GUN_FIRE_ABSOLUTE, ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE)
            .addOptional(ResourceLocation("tacz", "bullet_ignore_armor"))
            .addOptional(ResourceLocation("tacz", "bullet_void_ignore_armor"))
            .addOptional(ResourceLocation("dreamaticvoyage", "leviy_beam_absolute"))
        this.tag(ModTags.DamageTypes.VEHICLE_IGNORE)
            .addOptional(ResourceLocation("sona", "injury"))
        this.tag(ModTags.DamageTypes.VEHICLE_NOT_ABSORB)
            .add(
                DamageTypes.EXPLOSION,
                DamageTypes.PLAYER_EXPLOSION,
                ModDamageTypes.CUSTOM_EXPLOSION,
                ModDamageTypes.MINE,
                ModDamageTypes.PROJECTILE_EXPLOSION,
                ModDamageTypes.AMMO_CONSUMPTION
            )
        this.tag(ModTags.DamageTypes.VEHICLE_IMMUNE)
            .add(DamageTypes.CACTUS, DamageTypes.SWEET_BERRY_BUSH, DamageTypes.IN_WALL)
            .addOptional(ResourceLocation("iceandfire", "gorgon"))
        this.tag(ModTags.DamageTypes.GUN_DAMAGE).add(
            ModDamageTypes.GUN_FIRE,
            ModDamageTypes.GUN_FIRE_HEADSHOT,
            ModDamageTypes.GUN_FIRE_ABSOLUTE,
            ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE,
            ModDamageTypes.LASER,
            ModDamageTypes.LASER_HEADSHOT,
            ModDamageTypes.SHOCK,
            ModDamageTypes.BURN,
            ModDamageTypes.REPAIR_TOOL,
            ModDamageTypes.PROJECTILE_HIT,
            ModDamageTypes.PROJECTILE_EXPLOSION,
            ModDamageTypes.SUPER_STAR_HIT,
            ModDamageTypes.SUPER_STAR_SLASH,
            ModDamageTypes.PHOSPHORUS_FIRE,
            ModDamageTypes.CUSTOM_EXPLOSION,
            ModDamageTypes.PROJECTILE_EXPLOSION
        )
        this.tag(ModTags.DamageTypes.SBW_GUN_FIRE_DAMAGE).add(
            ModDamageTypes.GUN_FIRE,
            ModDamageTypes.GUN_FIRE_HEADSHOT,
            ModDamageTypes.GUN_FIRE_ABSOLUTE,
            ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE
        )

        this.tag(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS).add(
            ModDamageTypes.PROJECTILE_EXPLOSION,
            ModDamageTypes.CUSTOM_EXPLOSION,
            ModDamageTypes.PROJECTILE_HIT,
            ModDamageTypes.GRAPESHOT_HIT,
            ModDamageTypes.LASER,
            ModDamageTypes.LASER_HEADSHOT,
            ModDamageTypes.LASER_STATIC,
            ModDamageTypes.REPAIR_TOOL,
            ModDamageTypes.SUPER_STAR_HIT,
            ModDamageTypes.SUPER_STAR_SLASH,
            ModDamageTypes.PHOSPHORUS_FIRE
        )
        this.tag(DamageTypeTags.BYPASSES_ARMOR).add(
            ModDamageTypes.GUN_FIRE_ABSOLUTE,
            ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE,
            ModDamageTypes.SHOCK,
            ModDamageTypes.PROJECTILE_HIT,
            ModDamageTypes.GRAPESHOT_HIT,
            ModDamageTypes.LASER,
            ModDamageTypes.LASER_HEADSHOT,
            ModDamageTypes.LASER_STATIC,
            ModDamageTypes.VEHICLE_STRIKE,
            ModDamageTypes.VEHICLE_EXPLOSION,
            ModDamageTypes.AIR_CRASH,
            ModDamageTypes.REPAIR_TOOL,
            ModDamageTypes.SUPER_STAR_HIT,
            ModDamageTypes.SUPER_STAR_SLASH,
            ModDamageTypes.PHOSPHORUS_FIRE,
            ModDamageTypes.AMMO_CONSUMPTION
        )
        this.tag(DamageTypeTags.BYPASSES_EFFECTS).add(
            ModDamageTypes.SHOCK,
            ModDamageTypes.PHOSPHORUS_FIRE,
            ModDamageTypes.AMMO_CONSUMPTION
        )
        this.tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(
            ModDamageTypes.GUN_FIRE_ABSOLUTE,
            ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE,
            ModDamageTypes.SHOCK,
            ModDamageTypes.PROJECTILE_HIT,
            ModDamageTypes.GRAPESHOT_HIT,
            ModDamageTypes.LASER,
            ModDamageTypes.LASER_HEADSHOT,
            ModDamageTypes.LASER_STATIC,
            ModDamageTypes.VEHICLE_STRIKE,
            ModDamageTypes.VEHICLE_EXPLOSION,
            ModDamageTypes.AIR_CRASH,
            ModDamageTypes.SUPER_STAR_HIT,
            ModDamageTypes.SUPER_STAR_SLASH,
            ModDamageTypes.PHOSPHORUS_FIRE,
            ModDamageTypes.AMMO_CONSUMPTION
        )
        this.tag(DamageTypeTags.IS_EXPLOSION).add(
            ModDamageTypes.PROJECTILE_EXPLOSION,
            ModDamageTypes.CUSTOM_EXPLOSION,
            ModDamageTypes.LUNGE_MINE,
            ModDamageTypes.AMMO_CONSUMPTION
        )
        this.tag(DamageTypeTags.IS_FIRE).add(ModDamageTypes.BURN)
        this.tag(ModTags.DamageTypes.BYPASSES_VEHICLE).add(ModDamageTypes.REPAIR_TOOL)
        this.tag(ModTags.DamageTypes.NO_HURT_EFFECT).add(ModDamageTypes.AMMO_CONSUMPTION)

        this.tag(otherModTag("cataclysm", "bypasses_hurt_time")).add(
            ModDamageTypes.GUN_FIRE_ABSOLUTE,
            ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE,
            ModDamageTypes.AIR_CRASH,
            ModDamageTypes.BURN,
            ModDamageTypes.REPAIR_TOOL,
            ModDamageTypes.PROJECTILE_HIT,
            ModDamageTypes.GRAPESHOT_HIT,
            ModDamageTypes.CUSTOM_EXPLOSION,
            ModDamageTypes.DRONE_HIT,
            ModDamageTypes.LASER,
            ModDamageTypes.LASER_HEADSHOT,
            ModDamageTypes.LASER_STATIC,
            ModDamageTypes.LUNGE_MINE,
            ModDamageTypes.MINE,
            ModDamageTypes.PROJECTILE_EXPLOSION,
            ModDamageTypes.SHOCK,
            ModDamageTypes.VEHICLE_EXPLOSION,
            ModDamageTypes.VEHICLE_STRIKE,
            ModDamageTypes.SUPER_STAR_HIT,
            ModDamageTypes.SUPER_STAR_SLASH,
            ModDamageTypes.PHOSPHORUS_FIRE
        )
    }

    companion object {
        fun otherModTag(modId: String, name: String): TagKey<DamageType> {
            return TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation(modId, name))
        }
    }
}
