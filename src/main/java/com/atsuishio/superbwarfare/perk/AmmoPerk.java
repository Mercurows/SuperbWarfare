package com.atsuishio.superbwarfare.perk;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.function.Supplier;

public class AmmoPerk extends Perk {

    public float bypassArmorRate;
    public float damageRate;
    public float speedRate;
    public boolean slug;
    public float[] rgb;
    public Supplier<ArrayList<MobEffect>> mobEffects;

    public AmmoPerk(Builder builder) {
        super(builder.descriptionId, builder.type);
        this.bypassArmorRate = builder.bypassArmorRate;
        this.damageRate = builder.damageRate;
        this.speedRate = builder.speedRate;
        this.slug = builder.slug;
        this.rgb = builder.rgb;
        this.mobEffects = () -> builder.mobEffects;
    }

    public static class Builder {

        String descriptionId;
        Type type;
        float bypassArmorRate = 0.0f;
        float damageRate = 1.0f;
        float speedRate = 1.0f;
        boolean slug = false;
        float[] rgb = {1, 222 / 255f, 39 / 255f};
        public ArrayList<MobEffect> mobEffects = new ArrayList<>();

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

        public Builder mobEffect(Supplier<MobEffect> mobEffect) {
            this.mobEffects.add(mobEffect.get());
            return this;
        }
    }
}
