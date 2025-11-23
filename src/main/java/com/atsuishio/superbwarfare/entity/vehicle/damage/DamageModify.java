package com.atsuishio.superbwarfare.entity.vehicle.damage;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.DeserializeFromString;
import com.atsuishio.superbwarfare.js.ScriptManager;
import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import java.util.function.Function;
import java.util.regex.Pattern;

public class DamageModify implements DeserializeFromString {
    private static final Pattern MODIFY_PATTERN = Pattern.compile("^(?<prefix>(@#|#|@)?)(?<id>\\w+(:\\w+)?)\\s*(?<operator>[-*]?)\\s*(?<value>([+-]?\\d+(\\.\\d*)?)?)$");

    private transient Context context;
    private transient Script script;
    private transient ScriptableObject scope;

    @Override
    public void deserializeFromString(String str) {
        if (str.trim().startsWith("$")) {
            var trimmed = str.trim().substring(1);
            this.context = ScriptManager.getContext();

            if (!context.stringIsCompilableUnit(trimmed)) {
                this.type = ModifyType.INVALID;
                Mod.LOGGER.warn("invalid damage modify: {}", str);
                return;
            }

            this.scope = this.context.initSafeStandardObjects();
            this.script = this.context.compileString(trimmed, "damageModifier", 1, null);
            this.type = ModifyType.CUSTOM;

            this.modifyFunction = (entity, source, damage) -> {
                try {
                    ScriptableObject.putProperty(scope, "entity", entity);
                    ScriptableObject.putProperty(scope, "source", source);
                    ScriptableObject.putProperty(scope, "damage", damage);

                    var result = script.exec(context, scope);
                    if (result instanceof Number num) {
                        return num.floatValue();
                    } else {
                        throw new IllegalArgumentException("damage modifier script result(" + result + ") is not a number!");
                    }
                } catch (Exception exception) {
                    Mod.LOGGER.error("error computing damage", exception);
                }

                return damage;
            };

            return;
        }

        var matcher = MODIFY_PATTERN.matcher(str.trim());
        if (!matcher.matches()) {
            Mod.LOGGER.warn("invalid damage modify: {}", str);
            return;
        }

        var prefix = matcher.group("prefix").trim();
        var id = matcher.group("id").trim();
        var operator = matcher.group("operator").trim();
        var value = matcher.group("value").trim();

        this.source = prefix + id;
        generateSourceType();

        this.type = switch (operator) {
            case "-" -> ModifyType.REDUCE;
            case "*" -> ModifyType.MULTIPLY;
            default -> value.equals("0") ? ModifyType.IMMUNITY : ModifyType.INVALID;
        };

        if (this.type == ModifyType.INVALID) {
            Mod.LOGGER.warn("invalid damage modify: {}", str);
            return;
        }

        this.value = value.isEmpty() ? 0 : Float.parseFloat(value);
    }

    public enum ModifyType {
        @SerializedName("Immunity")
        IMMUNITY,   // 完全免疫
        @SerializedName("Reduce")
        REDUCE,     // 固定数值减伤
        @SerializedName("Multiply")
        MULTIPLY,   // 乘以指定倍数
        @SerializedName("Custom")
        CUSTOM,     // 脚本计算
        @SerializedName("Invalid")
        INVALID     // 解析无效
    }

    @SerializedName("Value")
    public float value = 0;
    @SerializedName("Type")
    public ModifyType type = ModifyType.IMMUNITY;

    public transient DamageModifier.CustomDamageModifier modifyFunction;

    @SerializedName("Source")
    public String source = "All";

    public transient String entityId = "";

    // 必须默认为null，否则无法处理JSON读取Source的情况
    public transient SourceType sourceType = null;

    public enum SourceType {
        TAG_KEY,
        RESOURCE_KEY,
        FUNCTION,
        ENTITY_ID,
        ENTITY_TAG,
        ALL,
    }

    public transient TagKey<DamageType> sourceTagKey = null;
    public transient ResourceKey<DamageType> sourceKey = null;
    public transient TagKey<EntityType<?>> entityTag = null;
    public transient Function<DamageSource, Boolean> condition = null;

    @SuppressWarnings("unused")
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
            this.sourceTagKey = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(source.substring(1)));
        } else if (source.startsWith("@#")) {
            sourceType = SourceType.ENTITY_TAG;
            this.entityTag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(source.substring(2)));
        } else if (source.startsWith("@")) {
            sourceType = SourceType.ENTITY_ID;
            this.entityId = source.substring(1);
        } else if (!source.equals("All")) {
            sourceType = SourceType.RESOURCE_KEY;
            this.sourceKey = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(source));
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
            case TAG_KEY -> {
                if (sourceTagKey == null) yield false;
                yield source.is(sourceTagKey);
            }
            case RESOURCE_KEY -> {
                if (sourceKey == null) yield false;
                yield source.is(sourceKey);
            }
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
            case ENTITY_TAG -> {
                var directEntity = source.getDirectEntity();
                if (directEntity == null) yield false;
                if (entityTag == null) yield false;
                yield directEntity.getType().is(entityTag);
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
    public float compute(Entity entity, DamageSource source, float damage) {
        // 类型出错默认视为免疫
        if (type == null) return 0;

        return switch (type) {
            case IMMUNITY -> 0;
            case REDUCE -> Math.max(damage - value, 0);
            case MULTIPLY -> damage * value;
            case CUSTOM -> {
                if (this.modifyFunction != null) {
                    yield this.modifyFunction.compute(entity, source, damage);
                }
                yield damage;
            }
            case INVALID -> damage;
        };
    }
}
