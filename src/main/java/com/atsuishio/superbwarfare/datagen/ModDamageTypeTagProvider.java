package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModDamageTypeTagProvider extends DamageTypeTagsProvider {

    public ModDamageTypeTagProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(pOutput, pLookupProvider, Mod.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider pProvider) {
        this.tag(ModTags.DamageTypes.PROJECTILE).add(ModDamageTypes.GUN_FIRE, ModDamageTypes.GUN_FIRE_HEADSHOT,
                DamageTypes.ARROW, DamageTypes.TRIDENT, DamageTypes.THROWN);
//                .addOptional(new ResourceLocation("tacz", "bullet"))
//                .addOptional(new ResourceLocation("tacz", "bullet_void"))
//                .addOptional(new ResourceLocation("virtuarealcraft", "rain_crystal"))
//                .addOptional(new ResourceLocation("virtuarealcraft", "rain_shower_butterfly"))
//                .addOptional(new ResourceLocation("virtuarealcraft", "sparkle_butterfly"))
//                .addOptional(new ResourceLocation("dreamaticvoyage", "blood_crystal"))
//                .addOptional(new ResourceLocation("dreamaticvoyage", "leviy_beam"));
        this.tag(ModTags.DamageTypes.PROJECTILE_ABSOLUTE).add(ModDamageTypes.GUN_FIRE_ABSOLUTE, ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE);
//                .addOptional(new ResourceLocation("tacz", "bullet_ignore_armor"))
//                .addOptional(new ResourceLocation("tacz", "bullet_void_ignore_armor"))
//                .addOptional(new ResourceLocation("dreamaticvoyage", "leviy_beam_absolute"));
    }

}
