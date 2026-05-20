package com.atsuishio.superbwarfare.entity.vehicle.damage

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify.SourceType
import com.atsuishio.superbwarfare.tools.FormatTool.format2D
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import java.util.function.Function

class DamageModifier {
    private val immuneList = mutableListOf<DamageModify>()
    private val modifyList = mutableListOf<DamageModify>()
    private val customList = mutableListOf<DamageModify>()

    fun interface CustomDamageModifier {
        fun compute(entity: Entity, source: DamageSource, damage: Float): Float
    }

    /**
     * 免疫所有伤害
     */
    fun immuneTo(): DamageModifier {
        immuneList.add(DamageModify(DamageModify.ModifyType.IMMUNITY, 0f))
        return this
    }

    /**
     * 免疫指定类型的伤害
     *
     * @param sourceTagKey 伤害类型
     */
    fun immuneTo(sourceTagKey: TagKey<DamageType>): DamageModifier {
        immuneList.add(DamageModify(DamageModify.ModifyType.IMMUNITY, 0f, sourceTagKey))
        return this
    }

    /**
     * 免疫指定类型的伤害
     *
     * @param sourceKey 伤害类型
     */
    fun immuneTo(sourceKey: ResourceKey<DamageType>): DamageModifier {
        immuneList.add(DamageModify(DamageModify.ModifyType.IMMUNITY, 0f, sourceKey))
        return this
    }

    /**
     * 免疫指定类型的伤害
     *
     * @param condition 伤害来源判定条件
     */
    fun immuneTo(condition: Function<DamageSource, Boolean>): DamageModifier {
        immuneList.add(DamageModify(DamageModify.ModifyType.IMMUNITY, 0f, condition))
        return this
    }

    /**
     * 免疫指定类型的伤害
     *
     * @param entityId 伤害来源实体ID
     */
    fun immuneTo(entityId: String): DamageModifier {
        immuneList.add(DamageModify(DamageModify.ModifyType.IMMUNITY, 0f, entityId))
        return this
    }

    /**
     * 免疫指定类型的伤害
     *
     * @param type 伤害来源实体类型
     */
    fun immuneTo(type: EntityType<*>) = immuneTo(EntityType.getKey(type).toString())

    /**
     * 固定减少所有伤害一定数值
     *
     * @param value 要减少的数值
     */
    fun reduce(value: Float): DamageModifier {
        modifyList.add(DamageModify(DamageModify.ModifyType.REDUCE, value))
        return this
    }

    /**
     * 固定减少指定类型的伤害一定数值
     *
     * @param value        要减少的数值
     * @param sourceTagKey 伤害类型
     */
    fun reduce(value: Float, sourceTagKey: TagKey<DamageType>): DamageModifier {
        modifyList.add(DamageModify(DamageModify.ModifyType.REDUCE, value, sourceTagKey))
        return this
    }

    /**
     * 固定减少指定类型的伤害一定数值
     *
     * @param value     要减少的数值
     * @param sourceKey 伤害类型
     */
    fun reduce(value: Float, sourceKey: ResourceKey<DamageType>): DamageModifier {
        modifyList.add(DamageModify(DamageModify.ModifyType.REDUCE, value, sourceKey))
        return this
    }

    /**
     * 固定减少指定类型的伤害一定数值
     *
     * @param value     要减少的数值
     * @param condition 伤害来源判定条件
     */
    fun reduce(value: Float, condition: Function<DamageSource, Boolean>): DamageModifier {
        modifyList.add(DamageModify(DamageModify.ModifyType.REDUCE, value, condition))
        return this
    }

    /**
     * 固定减少指定类型的伤害一定数值
     *
     * @param value    要减少的数值
     * @param entityId 伤害来源实体ID
     */
    fun reduce(value: Float, entityId: String): DamageModifier {
        modifyList.add(DamageModify(DamageModify.ModifyType.REDUCE, value, entityId))
        return this
    }

    /**
     * 固定减少指定类型的伤害一定数值
     *
     * @param value 要减少的数值
     * @param type  伤害来源实体类型
     */
    fun reduce(value: Float, type: EntityType<*>): DamageModifier {
        return reduce(value, EntityType.getKey(type).toString())
    }

    /**
     * 将所有类型的伤害值乘以指定数值
     *
     * @param value 要乘以的数值
     */
    fun multiply(value: Float): DamageModifier {
        modifyList.add(DamageModify(DamageModify.ModifyType.MULTIPLY, value))
        return this
    }

    /**
     * 将指定类型的伤害值乘以指定数值
     *
     * @param value        要乘以的数值
     * @param sourceTagKey 伤害类型
     */
    fun multiply(value: Float, sourceTagKey: TagKey<DamageType>): DamageModifier {
        modifyList.add(DamageModify(DamageModify.ModifyType.MULTIPLY, value, sourceTagKey))
        return this
    }

    /**
     * 将指定类型的伤害值乘以指定数值
     *
     * @param value     要乘以的数值
     * @param sourceKey 伤害类型
     */
    fun multiply(value: Float, sourceKey: ResourceKey<DamageType>): DamageModifier {
        modifyList.add(DamageModify(DamageModify.ModifyType.MULTIPLY, value, sourceKey))
        return this
    }

    /**
     * 将指定类型的伤害值乘以指定数值
     *
     * @param value     要乘以的数值
     * @param condition 伤害来源判定条件
     */
    fun multiply(value: Float, condition: Function<DamageSource, Boolean>): DamageModifier {
        modifyList.add(DamageModify(DamageModify.ModifyType.MULTIPLY, value, condition))
        return this
    }

    /**
     * 将指定类型的伤害值乘以指定数值
     *
     * @param value    要乘以的数值
     * @param entityId 伤害来源实体ID
     */
    fun multiply(value: Float, entityId: String): DamageModifier {
        modifyList.add(DamageModify(DamageModify.ModifyType.MULTIPLY, value, entityId))
        return this
    }

    /**
     * 将指定类型的伤害值乘以指定数值
     *
     * @param value 要乘以的数值
     * @param type  伤害来源实体类型
     */
    fun multiply(value: Float, type: EntityType<*>): DamageModifier {
        return multiply(value, EntityType.getKey(type).toString())
    }

    /**
     * 自定义伤害值计算
     *
     * @param damageModifyFunction 自定义伤害值计算函数
     */
    fun custom(damageModifyFunction: CustomDamageModifier): DamageModifier {
        val modifier = DamageModify()
        modifier.type = DamageModify.ModifyType.CUSTOM
        modifier.modifyFunction = damageModifyFunction
        customList.add(modifier)
        return this
    }

    fun addAll(list: MutableList<DamageModify>): DamageModifier {
        for (damageModify in list) {
            when (damageModify.type) {
                DamageModify.ModifyType.IMMUNITY -> immuneList.add(damageModify)
                DamageModify.ModifyType.REDUCE -> modifyList.add(damageModify)
                DamageModify.ModifyType.MULTIPLY -> modifyList.add(damageModify)
                DamageModify.ModifyType.CUSTOM -> customList.add(damageModify)
                else -> Mod.LOGGER.error("unknown modify type ${damageModify.type}")
            }
        }
        return this
    }

    // 计算优先级 免疫 > 固定减伤/乘算 > 自定义减伤
    fun toList(): List<DamageModify> = buildList {
        addAll(immuneList)
        addAll(modifyList)
        addAll(customList)
    }

    fun match(source: DamageSource): List<DamageModify> {
        return toList().filter { it.match(source) }
    }

    @JvmRecord
    data class ModifyResult(val modify: DamageModify?, val damage: Float) {
        fun getDamageInfo(): MutableComponent {
            if (modify == null) {
                return Component.translatable("tips.superbwarfare.modify_result.function")
                    .withStyle { style -> style.withColor(0xe1ff6b) }
                    .append(
                        Component.literal(" " + format2D(damage.toDouble()))
                            .withStyle(ChatFormatting.WHITE)
                    )
            }
            val color: Int
            val sourceString = when (modify.sourceType) {
                SourceType.TAG_KEY -> {
                    color = 0xff987e
                    modify.sourceTagKey!!.location().toString()
                }

                SourceType.ENTITY_TAG -> {
                    color = 0xffd07e
                    modify.entityTag!!.location().toString()
                }

                SourceType.FUNCTION -> {
                    color = 0xe1ff6b
                    ""
                }

                SourceType.ENTITY_ID -> {
                    color = 0x6be6ff
                    modify.entityId
                }

                SourceType.RESOURCE_KEY -> {
                    color = 0x6b7aff
                    modify.sourceKey!!.location().toString()
                }

                else -> {
                    color = 0xff6bdf
                    ""
                }
            }
            val typeString = when (modify.type) {
                DamageModify.ModifyType.IMMUNITY -> Component.literal(" 0").withStyle(ChatFormatting.GRAY)

                DamageModify.ModifyType.REDUCE -> Component.literal(" - ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal("" + modify.value).withStyle(ChatFormatting.RESET))
                    .append(Component.literal(" = " + format2D(damage.toDouble())).withStyle(ChatFormatting.WHITE))

                DamageModify.ModifyType.MULTIPLY -> Component.literal(" * ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("" + modify.value).withStyle(ChatFormatting.RESET))
                    .append(Component.literal(" = " + format2D(damage.toDouble())).withStyle(ChatFormatting.WHITE))

                DamageModify.ModifyType.CUSTOM -> Component.empty()

                else -> Component.literal("INVALID!").withStyle(ChatFormatting.RED)
            }
            val component = Component.translatable(
                "tips.superbwarfare.modify_result." + modify.sourceType!!.name.lowercase(),
                sourceString
            ).withStyle { style -> style.withColor(color) }
            return component.append(typeString)
        }
    }

    /**
     * 获取调试用的详细减伤结果
     */
    fun matchResult(entity: Entity, source: DamageSource, damage: Float): MutableList<ModifyResult> {
        var damage = damage
        val matchList = match(source)
        val list = ArrayList<ModifyResult>()

        for (damageModify in matchList) {
            damage = damageModify.compute(entity, source, damage)
            list.add(ModifyResult(damageModify, damage))

            if (damage <= 0) return list
        }

        for (func in customList) {
            damage = func.compute(entity, source, damage)
            list.add(ModifyResult(null, damage))

            if (damage <= 0) break
        }

        return list
    }

    /**
     * 计算减伤后的伤害值
     *
     * @param source 伤害来源
     * @param damage 原伤害值
     * @return 减伤后的伤害值
     */
    fun compute(entity: Entity, source: DamageSource, damage: Float): Float {
        var damage = damage
        val matchList = match(source)

        for (damageModify in matchList) {
            damage = damageModify.compute(entity, source, damage)
            if (damage <= 0) return 0f
        }

        // 最后计算自定义伤害
        for (func in customList) {
            damage = func.compute(entity, source, damage)
            if (damage <= 0) return 0f
        }

        return damage
    }

    companion object {
        @JvmStatic
        fun createDefaultModifier(): DamageModifier {
            return DamageModifier()
                .immuneTo(EntityType.POTION)
                .immuneTo(EntityType.AREA_EFFECT_CLOUD)
                .immuneTo(DamageTypes.FALL)
                .immuneTo(DamageTypes.CACTUS)
                .immuneTo(DamageTypes.DROWN)
                .immuneTo(DamageTypes.DRAGON_BREATH)
                .immuneTo(DamageTypes.WITHER)
                .immuneTo(DamageTypes.WITHER_SKULL)
        }
    }
}
