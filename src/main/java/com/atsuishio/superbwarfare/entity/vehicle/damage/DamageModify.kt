package com.atsuishio.superbwarfare.entity.vehicle.damage

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.DeserializeFromString
import com.google.gson.annotations.SerializedName
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.EntityType
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.max

class DamageModify : DeserializeFromString {
    override fun deserializeFromString(str: String) {
        val matcher: Matcher = MODIFY_PATTERN.matcher(str.trim())
        if (!matcher.matches()) {
            Mod.LOGGER.warn("invalid damage modify: {}", str)
            return
        }

        val prefix = matcher.group("prefix").trim()
        val id = matcher.group("id").trim()
        val operator = matcher.group("operator").trim()
        val value = matcher.group("value").trim()

        this.source = prefix + id
        generateSourceType()

        this.type = when (operator) {
            "-", "+" -> ModifyType.REDUCE
            "*" -> ModifyType.MULTIPLY
            else -> if (value == "0") ModifyType.IMMUNITY else ModifyType.INVALID
        }

        if (this.type == ModifyType.INVALID) {
            Mod.LOGGER.warn("invalid damage modify: {}", str)
            return
        }

        this.value = if (value.isEmpty()) 0f else value.toFloat() * (if (operator == "+") -1 else 1)
    }

    enum class ModifyType {
        @SerializedName("Immunity")
        IMMUNITY,  // 完全免疫

        @SerializedName("Reduce")
        REDUCE,  // 固定数值减伤

        @SerializedName("Multiply")
        MULTIPLY,  // 乘以指定倍数

        @SerializedName("Invalid")
        INVALID // 解析无效
    }

    @SerializedName("Value")
    var value: Float = 0f

    @SerializedName("Type")
    var type: ModifyType? = ModifyType.IMMUNITY

    @SerializedName("Source")
    var source: String = "All"

    @Transient
    var entityId: String? = ""

    // 必须默认为null，否则无法处理JSON读取Source的情况
    @Transient
    var sourceType: SourceType? = null

    enum class SourceType {
        TAG_KEY,
        RESOURCE_KEY,
        FUNCTION,
        ENTITY_ID,
        ENTITY_TAG,
        ALL,
    }

    @Transient
    var sourceTagKey: TagKey<DamageType>? = null

    @Transient
    var sourceKey: ResourceKey<DamageType>? = null

    @Transient
    var entityTag: TagKey<EntityType<*>>? = null

    @Transient
    var condition: Function<DamageSource, Boolean>? = null

    @Suppress("unused")
    constructor()

    constructor(type: ModifyType?, value: Float) {
        this.type = type
        this.value = value
        this.sourceType = SourceType.ALL
    }


    constructor(type: ModifyType?, value: Float, sourceTagKey: TagKey<DamageType>?) {
        this.type = type
        this.value = value
        this.sourceTagKey = sourceTagKey
        this.sourceType = SourceType.TAG_KEY
    }

    constructor(type: ModifyType?, value: Float, sourceKey: ResourceKey<DamageType>?) {
        this.type = type
        this.value = value
        this.sourceKey = sourceKey
        this.sourceType = SourceType.RESOURCE_KEY
    }

    constructor(type: ModifyType?, value: Float, condition: Function<DamageSource, Boolean>?) {
        this.type = type
        this.value = value
        this.condition = condition
        this.sourceType = SourceType.FUNCTION
    }

    constructor(type: ModifyType, value: Float, entityId: String?) {
        this.type = type
        this.value = value
        this.entityId = entityId
        this.sourceType = SourceType.ENTITY_ID
    }

    private fun generateSourceType() {
        if (source.startsWith("#")) {
            sourceType = SourceType.TAG_KEY
            this.sourceTagKey = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(source.substring(1)))
        } else if (source.startsWith("@#")) {
            sourceType = SourceType.ENTITY_TAG
            this.entityTag =
                TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(source.substring(2)))
        } else if (source.startsWith("@")) {
            sourceType = SourceType.ENTITY_ID
            this.entityId = source.substring(1)
        } else if (source != "All") {
            sourceType = SourceType.RESOURCE_KEY
            this.sourceKey = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(source))
        } else {
            sourceType = SourceType.ALL
        }
    }

    /**
     * 判断指定伤害来源是否符合指定条件，若未指定条件则默认符合
     * 
     * @param source 伤害来源
     * @return 伤害来源是否符合条件
     */
    fun match(source: DamageSource?): Boolean {
        if (source == null) return false

        if (sourceType == null) {
            generateSourceType()
        }

        val type = sourceType ?: return false
        return when (type) {
            SourceType.TAG_KEY -> {
                source.`is`(sourceTagKey ?: return false)
            }

            SourceType.RESOURCE_KEY -> {
                source.`is`(sourceKey ?: return false)
            }

            SourceType.FUNCTION -> condition!!.apply(source)
            SourceType.ENTITY_ID -> {
                val directEntity = source.directEntity
                val entity = source.entity

                // TODO 是否考虑优先处理Entity而不是DirectEntity？
                if (directEntity != null) {
                    EntityType.getKey(directEntity.type).toString() == this.entityId
                } else if (entity != null) {
                    EntityType.getKey(entity.type).toString() == this.entityId
                } else {
                    false
                }
            }

            SourceType.ENTITY_TAG -> {
                source.directEntity?.type?.`is`(entityTag ?: return false) ?: false
            }

            SourceType.ALL -> true
        }
    }

    /**
     * 计算减伤后的伤害值
     * 
     * @param damage 原伤害值
     * @return 计算后的伤害值
     */
    fun compute(damage: Float): Float {
        // 类型出错默认视为免疫
        if (type == null) return 0f

        return when (type) {
            ModifyType.IMMUNITY -> 0F
            ModifyType.REDUCE -> max(damage - value, 0f)
            ModifyType.MULTIPLY -> damage * value
            ModifyType.INVALID -> damage
            else -> error("invalid type!")
        }
    }

    companion object {
        private val MODIFY_PATTERN: Pattern =
            Pattern.compile("^(?<prefix>(@#|#|@)?)(?<id>\\w+(:\\w+)?)\\s*(?<operator>[-*+]?)\\s*(?<value>([+-]?\\d+(\\.\\d*)?)?)$")
    }
}
