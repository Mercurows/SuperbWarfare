package com.atsuishio.superbwarfare.entity.vehicle.damage;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.util.function.Function;

public class DamageModify {
    public enum ModifyType {
        @SerializedName("Immunity")
        IMMUNITY,   // 完全免疫
        @SerializedName("Reduce")
        REDUCE,     // 固定数值减伤
        @SerializedName("Multiply")
        MULTIPLY,   // 乘以指定倍数
    }

    @SerializedName("Value")
    private float value;
    @SerializedName("Type")
    private ModifyType type;

    @SerializedName("Source")
    private String source;

    private transient TagKey<DamageType> sourceTagKey = null;
    private transient ResourceKey<DamageType> sourceKey = null;
    private transient Function<DamageSource, Boolean> condition = null;

    public DamageModify(ModifyType type, float value) {
        this.type = type;
        this.value = value;
    }


    public DamageModify(ModifyType type, float value, TagKey<DamageType> sourceTagKey) {
        this.type = type;
        this.value = value;
        this.sourceTagKey = sourceTagKey;
    }

    public DamageModify(ModifyType type, float value, ResourceKey<DamageType> sourceKey) {
        this.type = type;
        this.value = value;
        this.sourceKey = sourceKey;
    }

    public DamageModify(ModifyType type, float value, Function<DamageSource, Boolean> condition) {
        this.type = type;
        this.value = value;
        this.condition = condition;
    }

    /**
     * 判断指定伤害来源是否符合指定条件，若未指定条件则默认符合
     *
     * @param source 伤害来源
     * @return 伤害来源是否符合条件
     */
    public boolean match(DamageSource source) {
        if (condition != null) {
            return condition.apply(source);
        } else if (sourceTagKey != null) {
            return source.is(sourceTagKey);
        } else if (sourceKey != null) {
            return source.is(sourceKey);
        }
        return true;
    }

    /**
     * 计算减伤后的伤害值
     *
     * @param damage 原伤害值
     * @return 计算后的伤害值
     */
    public float compute(float damage) {
        return switch (type) {
            case IMMUNITY -> 0;
            case REDUCE -> Math.max(damage - value, 0);
            case MULTIPLY -> damage * value;
        };
    }
}
