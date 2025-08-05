package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public class DamageTypeTool {

    public static boolean isGunDamage(DamageSource source) {
        return source.is(ModTags.DamageTypes.GUN_DAMAGE);
    }

    public static boolean isGunDamage(ResourceKey<DamageType> damageType, RegistryAccess registryAccess) {
        var damageTypeRegistry = registryAccess.registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = damageTypeRegistry.getHolder(damageType).orElse(null);
        return holder != null && holder.is(ModTags.DamageTypes.GUN_DAMAGE);
    }

    public static boolean isExplosionDamage(DamageSource source) {
        return source.is(ModDamageTypes.CUSTOM_EXPLOSION) || source.is(ModDamageTypes.PROJECTILE_EXPLOSION);
    }

    public static boolean isHeadshotDamage(DamageSource source) {
        return source.is(ModDamageTypes.GUN_FIRE_HEADSHOT) || source.is(ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE) || source.is(ModDamageTypes.LASER_HEADSHOT);
    }

    public static boolean isGunFireDamage(DamageSource source) {
        return source.is(ModDamageTypes.GUN_FIRE) || source.is(ModDamageTypes.GUN_FIRE_ABSOLUTE)
                || source.is(ModDamageTypes.SHOCK) || source.is(ModDamageTypes.BURN)
                || source.is(ModDamageTypes.LASER);
    }

    public static boolean isModDamage(DamageSource source) {
        return source.typeHolder().unwrapKey().map(s -> s.location().getNamespace().equals(Mod.MODID)).orElse(false);
    }

    public static boolean isCompatGunDamage(ResourceKey<DamageType> damageType, RegistryAccess registryAccess) {
        return isGunDamage(damageType, registryAccess)
                || damageType == ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("tacz", "bullet"))
                || damageType == ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("tacz", "bullet_void"))
                || damageType == ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("tacz", "bullet_ignore_armor"))
                || damageType == ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("tacz", "bullet_void_ignore_armor"));
    }
}
