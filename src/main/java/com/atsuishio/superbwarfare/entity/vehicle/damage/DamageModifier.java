package com.atsuishio.superbwarfare.entity.vehicle.damage;

import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class DamageModifier {

    private final List<DamageModify> immuneList = new ArrayList<>();
    private final List<DamageModify> reduceList = new ArrayList<>();
    private final List<DamageModify> multiplyList = new ArrayList<>();
    private final List<DamageModify> customList = new ArrayList<>();

    @FunctionalInterface
    public interface CustomDamageModifier {
        float compute(Entity entity, DamageSource source, float damage);
    }

    public static DamageModifier createDefaultModifier() {
        return new DamageModifier()
                .immuneTo(EntityType.POTION)
                .immuneTo(EntityType.AREA_EFFECT_CLOUD)
                .immuneTo(DamageTypes.FALL)
                .immuneTo(DamageTypes.CACTUS)
                .immuneTo(DamageTypes.DROWN)
                .immuneTo(DamageTypes.DRAGON_BREATH)
                .immuneTo(DamageTypes.WITHER)
                .immuneTo(DamageTypes.WITHER_SKULL);
    }

    /**
     * 免疫所有伤害
     */
    public DamageModifier immuneTo() {
        immuneList.add(new DamageModify(DamageModify.ModifyType.IMMUNITY, 0));
        return this;
    }

    /**
     * 免疫指定类型的伤害
     *
     * @param sourceTagKey 伤害类型
     */
    public DamageModifier immuneTo(TagKey<DamageType> sourceTagKey) {
        immuneList.add(new DamageModify(DamageModify.ModifyType.IMMUNITY, 0, sourceTagKey));
        return this;
    }

    /**
     * 免疫指定类型的伤害
     *
     * @param sourceKey 伤害类型
     */
    public DamageModifier immuneTo(ResourceKey<DamageType> sourceKey) {
        immuneList.add(new DamageModify(DamageModify.ModifyType.IMMUNITY, 0, sourceKey));
        return this;
    }

    /**
     * 免疫指定类型的伤害
     *
     * @param condition 伤害来源判定条件
     */
    public DamageModifier immuneTo(Function<DamageSource, Boolean> condition) {
        immuneList.add(new DamageModify(DamageModify.ModifyType.IMMUNITY, 0, condition));
        return this;
    }

    /**
     * 免疫指定类型的伤害
     *
     * @param entityId 伤害来源实体ID
     */
    public DamageModifier immuneTo(String entityId) {
        immuneList.add(new DamageModify(DamageModify.ModifyType.IMMUNITY, 0, entityId));
        return this;
    }

    /**
     * 免疫指定类型的伤害
     *
     * @param type 伤害来源实体类型
     */
    public DamageModifier immuneTo(EntityType<?> type) {
        return immuneTo(EntityType.getKey(type).toString());
    }

    /**
     * 固定减少所有伤害一定数值
     *
     * @param value 要减少的数值
     */
    public DamageModifier reduce(float value) {
        reduceList.add(new DamageModify(DamageModify.ModifyType.REDUCE, value));
        return this;
    }

    /**
     * 固定减少指定类型的伤害一定数值
     *
     * @param value        要减少的数值
     * @param sourceTagKey 伤害类型
     */
    public DamageModifier reduce(float value, TagKey<DamageType> sourceTagKey) {
        reduceList.add(new DamageModify(DamageModify.ModifyType.REDUCE, value, sourceTagKey));
        return this;
    }

    /**
     * 固定减少指定类型的伤害一定数值
     *
     * @param value     要减少的数值
     * @param sourceKey 伤害类型
     */
    public DamageModifier reduce(float value, ResourceKey<DamageType> sourceKey) {
        reduceList.add(new DamageModify(DamageModify.ModifyType.REDUCE, value, sourceKey));
        return this;
    }

    /**
     * 固定减少指定类型的伤害一定数值
     *
     * @param value     要减少的数值
     * @param condition 伤害来源判定条件
     */
    public DamageModifier reduce(float value, Function<DamageSource, Boolean> condition) {
        reduceList.add(new DamageModify(DamageModify.ModifyType.REDUCE, value, condition));
        return this;
    }

    /**
     * 固定减少指定类型的伤害一定数值
     *
     * @param value    要减少的数值
     * @param entityId 伤害来源实体ID
     */
    public DamageModifier reduce(float value, String entityId) {
        reduceList.add(new DamageModify(DamageModify.ModifyType.REDUCE, value, entityId));
        return this;
    }

    /**
     * 固定减少指定类型的伤害一定数值
     *
     * @param value 要减少的数值
     * @param type  伤害来源实体类型
     */
    public DamageModifier reduce(float value, EntityType<?> type) {
        return reduce(value, EntityType.getKey(type).toString());
    }

    /**
     * 将所有类型的伤害值乘以指定数值
     *
     * @param value 要乘以的数值
     */
    public DamageModifier multiply(float value) {
        multiplyList.add(new DamageModify(DamageModify.ModifyType.MULTIPLY, value));
        return this;
    }

    /**
     * 将指定类型的伤害值乘以指定数值
     *
     * @param value        要乘以的数值
     * @param sourceTagKey 伤害类型
     */
    public DamageModifier multiply(float value, TagKey<DamageType> sourceTagKey) {
        multiplyList.add(new DamageModify(DamageModify.ModifyType.MULTIPLY, value, sourceTagKey));
        return this;
    }

    /**
     * 将指定类型的伤害值乘以指定数值
     *
     * @param value     要乘以的数值
     * @param sourceKey 伤害类型
     */
    public DamageModifier multiply(float value, ResourceKey<DamageType> sourceKey) {
        multiplyList.add(new DamageModify(DamageModify.ModifyType.MULTIPLY, value, sourceKey));
        return this;
    }

    /**
     * 将指定类型的伤害值乘以指定数值
     *
     * @param value     要乘以的数值
     * @param condition 伤害来源判定条件
     */
    public DamageModifier multiply(float value, Function<DamageSource, Boolean> condition) {
        multiplyList.add(new DamageModify(DamageModify.ModifyType.MULTIPLY, value, condition));
        return this;
    }

    /**
     * 将指定类型的伤害值乘以指定数值
     *
     * @param value    要乘以的数值
     * @param entityId 伤害来源实体ID
     */
    public DamageModifier multiply(float value, String entityId) {
        multiplyList.add(new DamageModify(DamageModify.ModifyType.MULTIPLY, value, entityId));
        return this;
    }

    /**
     * 将指定类型的伤害值乘以指定数值
     *
     * @param value 要乘以的数值
     * @param type  伤害来源实体类型
     */
    public DamageModifier multiply(float value, EntityType<?> type) {
        return multiply(value, EntityType.getKey(type).toString());
    }

    /**
     * 自定义伤害值计算
     *
     * @param damageModifyFunction 自定义伤害值计算函数
     */
    public DamageModifier custom(CustomDamageModifier damageModifyFunction) {
        var modifier = new DamageModify();
        modifier.type = DamageModify.ModifyType.CUSTOM;
        modifier.modifyFunction = damageModifyFunction;
        customList.add(modifier);
        return this;
    }

    public DamageModifier addAll(List<DamageModify> list) {
        for (var damageModify : list) {
            switch (damageModify.type) {
                case IMMUNITY -> immuneList.add(damageModify);
                case REDUCE -> reduceList.add(damageModify);
                case MULTIPLY -> multiplyList.add(damageModify);
                case CUSTOM -> customList.add(damageModify);
            }
        }
        return this;
    }

    public List<DamageModify> toList() {
        var list = new ArrayList<DamageModify>();

        // 计算优先级 免疫 > 固定减伤 > 乘
        list.addAll(immuneList);
        list.addAll(reduceList);
        list.addAll(multiplyList);

        return list;
    }

    public List<DamageModify> match(DamageSource source) {
        return toList().stream().filter(m -> m.match(source)).toList();
    }

    public record ModifyResult(@Nullable DamageModify modify, float damage) {

        public MutableComponent getDamageInfo() {
            if (modify == null) {
                return Component.translatable("tips.superbwarfare.modify_result.function").withStyle(style -> style.withColor(0xe1ff6b))
                        .append(Component.literal(" " + FormatTool.format2D(damage)).withStyle(ChatFormatting.WHITE));
            }
            int color;
            var sourceString = switch (modify.sourceType) {
                case TAG_KEY -> {
                    color = 0xff987e;
                    yield modify.sourceTagKey.location().toString();
                }
                case ENTITY_TAG -> {
                    color = 0xffd07e;
                    yield modify.entityTag.location().toString();
                }
                case FUNCTION -> {
                    color = 0xe1ff6b;
                    yield "";
                }
                case ENTITY_ID -> {
                    color = 0x6be6ff;
                    yield modify.entityId;
                }
                case RESOURCE_KEY -> {
                    color = 0x6b7aff;
                    yield modify.sourceKey.location().toString();
                }
                case ALL -> {
                    color = 0xff6bdf;
                    yield "";
                }
            };
            MutableComponent typeString = switch (modify.type) {
                case IMMUNITY -> Component.literal(" 0").withStyle(ChatFormatting.GRAY);
                case REDUCE -> Component.literal(" - ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("" + modify.value).withStyle(ChatFormatting.RESET))
                        .append(Component.literal(" = " + FormatTool.format2D(damage)).withStyle(ChatFormatting.WHITE));
                case MULTIPLY -> Component.literal(" * ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("" + modify.value).withStyle(ChatFormatting.RESET))
                        .append(Component.literal(" = " + FormatTool.format2D(damage)).withStyle(ChatFormatting.WHITE));
                case CUSTOM -> Component.empty();
                case INVALID -> Component.literal("INVALID!").withStyle(ChatFormatting.RED);
            };
            var component = Component.translatable("tips.superbwarfare.modify_result." + modify.sourceType.name().toLowerCase(Locale.ROOT), sourceString)
                    .withStyle(style -> style.withColor(color));
            return component.append(typeString);
        }
    }

    /**
     * 获取调试用的详细减伤结果
     */
    public List<ModifyResult> matchResult(Entity entity, DamageSource source, float damage) {
        var matchList = match(source);
        var list = new ArrayList<ModifyResult>();

        for (var damageModify : matchList) {
            damage = damageModify.compute(entity, source, damage);
            list.add(new ModifyResult(damageModify, damage));

            if (damage <= 0) return list;
        }

        for (var func : customList) {
            damage = func.compute(entity, source, damage);
            list.add(new ModifyResult(null, damage));

            if (damage <= 0) break;
        }

        return list;
    }

    /**
     * 计算减伤后的伤害值
     *
     * @param source 伤害来源
     * @param damage 原伤害值
     * @return 减伤后的伤害值
     */
    public float compute(Entity entity, DamageSource source, float damage) {
        var matchList = match(source);

        for (var damageModify : matchList) {
            damage = damageModify.compute(entity, source, damage);
            if (damage <= 0) return 0;
        }

        // 最后计算自定义伤害
        for (var func : customList) {
            damage = func.compute(entity, source, damage);
            if (damage <= 0) return 0;
        }

        return damage;
    }
}
