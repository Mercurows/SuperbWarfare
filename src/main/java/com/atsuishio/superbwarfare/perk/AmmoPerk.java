package com.atsuishio.superbwarfare.perk;

import com.atsuishio.superbwarfare.data.gun.DamageReduce;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.function.Supplier;

public class AmmoPerk extends Perk {

    public float bypassArmorRate;
    public float damageRate;
    public float speedRate;
    public boolean slug;
    public float[] rgb;
    public Supplier<ArrayList<Holder<MobEffect>>> mobEffects;

    public AmmoPerk(Builder builder) {
        super(builder.descriptionId, builder.type);
        this.bypassArmorRate = builder.bypassArmorRate;
        this.damageRate = builder.damageRate;
        this.speedRate = builder.speedRate;
        this.slug = builder.slug;
        this.rgb = builder.rgb;
        this.mobEffects = () -> builder.mobEffects;

        appendModification(GunProp.DAMAGE, (data, damage) -> {
            if (data.perk.get(Type.AMMO) instanceof AmmoPerk ammoPerk) {
                if (ammoPerk.slug) {
                    return damage * ammoPerk.damageRate * data.get(GunProp.PROJECTILE_AMOUNT);
                }
                return damage * ammoPerk.damageRate;
            }
            return damage;
        });

        appendModification(GunProp.PROJECTILE_AMOUNT, (data, amount) -> {
            var perk = data.perk.get(Perk.Type.AMMO);
            if (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug) {
                return 1;
            }
            return amount;
        });
    }

    public AmmoPerk(String descriptionId, Type type) {
        super(descriptionId, type);
        this.rgb = new float[]{1, 222 / 255f, 39 / 255f};
        this.mobEffects = ArrayList::new;
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        if (!(entity instanceof ProjectileEntity projectile)) return;
        projectile.setRGB(this.rgb);
        projectile.bypassArmorRate((float) Math.max(this.bypassArmorRate + data.get(GunProp.BYPASSES_ARMOR), 0));
        if (this.slug) {
            projectile.setDamage((float) (data.get(GunProp.DAMAGE) * data.get(GunProp.PROJECTILE_AMOUNT)));
        }
        if (!this.mobEffects.get().isEmpty()) {
            int amplifier = this.getEffectAmplifier(instance);
            int duration = this.getEffectDuration(instance);
            ArrayList<MobEffectInstance> mobEffectInstances = new ArrayList<>();
            for (var effect : this.mobEffects.get()) {
                mobEffectInstances.add(new MobEffectInstance(effect, duration, amplifier));
            }
            projectile.effect(mobEffectInstances);
        }
    }

    public int getEffectAmplifier(PerkInstance instance) {
        return instance.level() - 1;
    }

    public int getEffectDuration(PerkInstance instance) {
        return 70 + 30 * instance.level();
    }

    public double getModifiedVelocity(GunData data, PerkInstance instance) {
        return data.get(GunProp.VELOCITY) * this.speedRate;
    }

    @Override
    public double getModifiedDamageReduceRate(DamageReduce reduce) {
        if (this.slug && reduce.type == DamageReduce.ReduceType.SHOTGUN) {
            return 0.015;
        }
        return super.getModifiedDamageReduceRate(reduce);
    }

    @Override
    public double getModifiedDamageReduceMinDistance(DamageReduce reduce) {
        if (this.slug && reduce.type == DamageReduce.ReduceType.SHOTGUN) {
            return super.getModifiedDamageReduceMinDistance(reduce) * 2;
        }
        return super.getModifiedDamageReduceMinDistance(reduce);
    }

    public static class Builder {

        String descriptionId;
        Type type;
        float bypassArmorRate = 0.0f;
        float damageRate = 1.0f;
        float speedRate = 1.0f;
        boolean slug = false;
        float[] rgb = {1, 222 / 255f, 39 / 255f};
        public ArrayList<Holder<MobEffect>> mobEffects = new ArrayList<>();

        public Builder(String descriptionId, Type type) {
            this.descriptionId = descriptionId;
            this.type = type;
        }

        public Builder bypassArmorRate(float bypassArmorRate) {
            this.bypassArmorRate = Mth.clamp(bypassArmorRate, -1, 1);
            return this;
        }

        public Builder damageRate(float damageRate) {
            this.damageRate = Mth.clamp(damageRate, 0, Float.POSITIVE_INFINITY);
            return this;
        }

        public Builder speedRate(float speedRate) {
            this.speedRate = Mth.clamp(speedRate, 0, Float.POSITIVE_INFINITY);
            return this;
        }

        public Builder slug(boolean slug) {
            this.slug = slug;
            return this;
        }

        public Builder rgb(int r, int g, int b) {
            this.rgb[0] = r / 255f;
            this.rgb[1] = g / 255f;
            this.rgb[2] = b / 255f;
            return this;
        }

        public Builder mobEffect(Supplier<Holder<MobEffect>> mobEffect) {
            this.mobEffects.add(mobEffect.get());
            return this;
        }
    }
}
