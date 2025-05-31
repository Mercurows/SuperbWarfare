package com.atsuishio.superbwarfare.entity.vehicle.damage;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;

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
    private float value = 0;
    @SerializedName("Type")
    private ModifyType type = ModifyType.IMMUNITY;

    public ModifyType getType() {
        return type;
    }

    @SerializedName("Source")
    private String source = "All";

    private transient String entityId = "";

    // 必须默认为null，否则无法处理JSON读取Source的情况
    private transient SourceType sourceType = null;

    private enum SourceType {
        TAG_KEY,
        RESOURCE_KEY,
        FUNCTION,
        ENTITY_ID,
        ALL,
    }

    private transient TagKey<DamageType> sourceTagKey = null;
    private transient ResourceKey<DamageType> sourceKey = null;
    private transient Function<DamageSource, Boolean> condition = null;

    public DamageModify() {
    }

    public DamageModify(ModifyType type, float value) {
        this.type = type;
        this.value = value;
        this.sourceType = SourceType.ALL;
    }


    public DamageModify(ModifyType type, float value, TagKey<DamageType> sourceTagKey) {
        this.type = type;
        this.value = value;
        this.sourceTagKey = sourceTagKey;
        this.sourceType = SourceType.TAG_KEY;
    }

    public DamageModify(ModifyType type, float value, ResourceKey<DamageType> sourceKey) {
        this.type = type;
        this.value = value;
        this.sourceKey = sourceKey;
        this.sourceType = SourceType.RESOURCE_KEY;
    }

    public DamageModify(ModifyType type, float value, Function<DamageSource, Boolean> condition) {
        this.type = type;
        this.value = value;
        this.condition = condition;
        this.sourceType = SourceType.FUNCTION;
    }

    public DamageModify(ModifyType type, float value, String entityId) {
        this.type = type;
        this.value = value;
        this.entityId = entityId;
        this.sourceType = SourceType.ENTITY_ID;
    }

    private void generateSourceType() {
        if (source.startsWith("#")) {
            sourceType = SourceType.TAG_KEY;
            var location = ResourceLocation.tryParse(source.substring(1));

            if (location != null) {
                this.sourceTagKey = TagKey.create(Registries.DAMAGE_TYPE, location);
            }
        } else if (source.startsWith("@")) {
            sourceType = SourceType.ENTITY_ID;
            this.entityId = source.substring(1);
        } else if (!source.equals("All")) {
            sourceType = SourceType.RESOURCE_KEY;
            var location = ResourceLocation.tryParse(source);

            if (location != null) {
                this.sourceKey = ResourceKey.create(Registries.DAMAGE_TYPE, location);
            }
        } else {
            sourceType = SourceType.ALL;
        }
    }

    /**
     * 判断指定伤害来源是否符合指定条件，若未指定条件则默认符合
     *
     * @param source 伤害来源
     * @return 伤害来源是否符合条件
     */
    public boolean match(DamageSource source) {
        if (source == null) return false;

        if (sourceType == null) {
            generateSourceType();
        }

        return switch (sourceType) {
            case TAG_KEY -> source.is(sourceTagKey);
            case RESOURCE_KEY -> source.is(sourceKey);
            case FUNCTION -> condition.apply(source);
            case ENTITY_ID -> {
                var directEntity = source.getDirectEntity();
                var entity = source.getEntity();

                // TODO 是否考虑优先处理Entity而不是DirectEntity？
                if (directEntity != null) {
                    yield EntityType.getKey(directEntity.getType()).toString().equals(this.entityId);
                } else if (entity != null) {
                    yield EntityType.getKey(entity.getType()).toString().equals(this.entityId);
                } else {
                    yield false;
                }
            }
            case ALL -> true;
        };
    }

    /**
     * 计算减伤后的伤害值
     *
     * @param damage 原伤害值
     * @return 计算后的伤害值
     */
    public float compute(float damage) {
        // 类型出错默认视为免疫
        if (type == null) return 0;

        return switch (type) {
            case IMMUNITY -> 0;
            case REDUCE -> Math.max(damage - value, 0);
            case MULTIPLY -> damage * value;
        };
    }
}
