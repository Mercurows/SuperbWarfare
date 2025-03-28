package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.init.ModDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public class DamageTypeTool {

    public static boolean isGunDamage(DamageSource source) {
        return source.is(ModDamageTypes.GUN_FIRE) || source.is(ModDamageTypes.GUN_FIRE_HEADSHOT)
                || source.is(ModDamageTypes.GUN_FIRE_ABSOLUTE) || source.is(ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE)
                || source.is(ModDamageTypes.SHOCK) || source.is(ModDamageTypes.BURN)
                || source.is(ModDamageTypes.LASER) || source.is(ModDamageTypes.LASER_HEADSHOT);
    }

    public static boolean isGunDamage(ResourceKey<DamageType> damageType) {
        return damageType == ModDamageTypes.GUN_FIRE || damageType == ModDamageTypes.GUN_FIRE_HEADSHOT
                || damageType == ModDamageTypes.GUN_FIRE_ABSOLUTE || damageType == ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE;
    }

    public static boolean isExplosionDamage(DamageSource source) {
        return source.is(ModDamageTypes.CUSTOM_EXPLOSION) || source.is(ModDamageTypes.PROJECTILE_BOOM);
    }

    public static boolean isHeadshotDamage(DamageSource source) {
        return source.is(ModDamageTypes.GUN_FIRE_HEADSHOT) || source.is(ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE);
    }

    public static boolean isGunFireDamage(DamageSource source) {
        return source.is(ModDamageTypes.GUN_FIRE) || source.is(ModDamageTypes.GUN_FIRE_ABSOLUTE)
                || source.is(ModDamageTypes.SHOCK) || source.is(ModDamageTypes.BURN)
                || source.is(ModDamageTypes.LASER) || source.is(ModDamageTypes.LASER_HEADSHOT);
    }

    public static boolean isModDamage(DamageSource source) {
        return source.is(ModDamageTypes.GUN_FIRE_ABSOLUTE) || source.is(ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE)
                || source.is(ModDamageTypes.GUN_FIRE) || source.is(ModDamageTypes.GUN_FIRE_HEADSHOT)
                || source.is(ModDamageTypes.MINE) || source.is(ModDamageTypes.MINE) || source.is(ModDamageTypes.SHOCK)
                || source.is(ModDamageTypes.PROJECTILE_BOOM) || source.is(ModDamageTypes.CANNON_FIRE)
                || source.is(ModDamageTypes.BURN)
                || source.is(ModDamageTypes.LASER) || source.is(ModDamageTypes.LASER_HEADSHOT);
    }

}
