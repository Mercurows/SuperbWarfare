package com.atsuishio.superbwarfare.entity.vehicle.damage

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.DeserializeFromString
import com.atsuishio.superbwarfare.data.STOFactory
import com.atsuishio.superbwarfare.data.StringInstanceBuilder
import com.atsuishio.superbwarfare.script.ScriptManager
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.max

@STOFactory(DamageModify.DamageModifyInstanceBuilder::class)
@Serializable
class DamageModify : DeserializeFromString {
    @kotlinx.serialization.Transient
    @Transient
    private var script: ScriptManager.CustomScript? = null

    @kotlinx.serialization.Transient
    @Transient
    var modifyFunction: DamageModifier.CustomDamageModifier? = null

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

    object DamageModifyInstanceBuilder : StringInstanceBuilder<DamageModify> {
        override fun fromString(value: String) = DamageModify().apply {
            if (value.trim().startsWith("$")) {
                val trimmed = value.trim().substring(1)

                val script = ScriptManager.createSafeScript("damageModifier", trimmed)
                if (script == null) {
                    this.type = ModifyType.INVALID
                    Mod.LOGGER.warn("invalid damage modify script: {}", value)
                    return@apply
                }

                this.script = script
                this.type = ModifyType.CUSTOM

                this.modifyFunction = DamageModifier.CustomDamageModifier { entity, source, damage ->
                    try {
                        this.script!!.putProperty("entity", entity)
                        this.script!!.putProperty("source", source)
                        this.script!!.putProperty("damage", damage)

                        val result = this.script!!.exec()
                        if (result is Number) {
                            return@CustomDamageModifier result.toFloat()
                        } else {
                            throw IllegalArgumentException("damage modifier script result($result) is not a number!")
                        }
                    } catch (exception: Exception) {
                        Mod.LOGGER.error("error computing damage", exception)
                    }
                    damage
                }

                return@apply
            }

            val matcher: Matcher = MODIFY_PATTERN.matcher(value.trim())
            if (!matcher.matches()) {
                Mod.LOGGER.warn("invalid damage modify: {}", value)
                return@apply
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
                Mod.LOGGER.warn("invalid damage modify: {}", value)
                return@apply
            }

            this.value = if (value.isEmpty()) 0f else value.toFloat() * (if (operator == "+") -1 else 1)
        }
    }

    @Serializable
    enum class ModifyType {
        @SerializedName("Immunity")
        @SerialName("Immunity")
        IMMUNITY,  // 完全免疫

        @SerializedName("Reduce")
        @SerialName("Reduce")
        REDUCE,  // 固定数值减伤

        @SerializedName("Multiply")
        @SerialName("Multiply")
        MULTIPLY,  // 乘以指定倍数

        @SerializedName("Custom")
        @SerialName("Custom")
        CUSTOM, // 脚本计算

        @SerializedName("Invalid")
        @SerialName("Invalid")
        INVALID // 解析无效
    }

    @SerializedName("Value")
    @SerialName("Value")
    var value: Float = 0f

    @SerializedName("Type")
    @SerialName("Type")
    var type: ModifyType? = ModifyType.IMMUNITY

    @SerializedName("Source")
    @SerialName("Source")
    var source: String = "All"

    @Transient
    @kotlinx.serialization.Transient
    var entityId: String? = ""

    // 必须默认为null，否则无法处理JSON读取Source的情况
    @Transient
    @kotlinx.serialization.Transient
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
    @kotlinx.serialization.Transient
    var sourceTagKey: TagKey<DamageType>? = null

    @Transient
    @kotlinx.serialization.Transient
    var sourceKey: ResourceKey<DamageType>? = null

    @Transient
    @kotlinx.serialization.Transient
    var entityTag: TagKey<EntityType<*>>? = null

    @Transient
    @kotlinx.serialization.Transient
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
            this.sourceTagKey = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.tryParse(source.substring(1)))
        } else if (source.startsWith("@#")) {
            sourceType = SourceType.ENTITY_TAG
            this.entityTag =
                TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.tryParse(source.substring(2)))
        } else if (source.startsWith("@")) {
            sourceType = SourceType.ENTITY_ID
            this.entityId = source.substring(1)
        } else if (source != "All") {
            sourceType = SourceType.RESOURCE_KEY
            this.sourceKey = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.tryParse(source))
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
    fun compute(entity: Entity, source: DamageSource, damage: Float): Float {
        // 类型出错默认视为免疫
        if (type == null) return 0f

        return when (type) {
            ModifyType.IMMUNITY -> 0F
            ModifyType.REDUCE -> max(damage - value, 0f)
            ModifyType.MULTIPLY -> damage * value
            ModifyType.CUSTOM -> {
                if (this.modifyFunction == null) damage
                else this.modifyFunction!!.compute(entity, source, damage)
            }

            ModifyType.INVALID -> damage
            else -> error("invalid type!")
        }
    }

    companion object {
        private val MODIFY_PATTERN: Pattern =
            Pattern.compile("^(?<prefix>(@#|#|@)?)(?<id>\\w+(:\\w+)?)\\s*(?<operator>[-*+]?)\\s*(?<value>([+-]?\\d+(\\.\\d*)?)?)$")
    }
}
