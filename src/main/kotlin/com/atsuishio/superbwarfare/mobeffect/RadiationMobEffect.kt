package com.atsuishio.superbwarfare.mobeffect

import com.atsuishio.superbwarfare.capability.living.RadiationCapability
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.entity.living.LivingHealEvent
import net.minecraftforge.event.entity.living.LivingHurtEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object RadiationMobEffect : MobEffect(MobEffectCategory.HARMFUL, 0x55FF55) {
    const val MAX_EFFECTS_LEVEL = 20

    const val DOSE_INTERVAL = 5
    const val DOSE_SYMPTOM_THRESHOLD = 600f        // 副作用起点
    const val DOSE_HEAL_REDUCE_THRESHOLD = 1000f   // 减疗起点
    const val DOSE_HEAL_BLOCK = 4000f              // 完全禁疗
    const val DOSE_BLEED_THRESHOLD = 2000f         // 额外流血起点
    const val DOSE_LETHAL_THRESHOLD = 8000f        // 超高额外伤害起点
    const val DOSE_SATURATION_ZERO = 4500f         // 完全黑白

    const val BLEED_COOLDOWN_MAX = 40              // 额外伤害冷却上限
    const val BLEED_COOLDOWN_MIN = 4               // 额外伤害冷却下限

    private const val MAX_HEALTH_MODIFIER_UUID = "E560C8B4-8E5D-4C59-BD01-8C50E9C1EDB7"
    private const val MOVEMENT_SPEED_MODIFIER_UUID = "7D5F7E7E-67DD-48D7-90E3-64C3410E1A80"
    private const val ATTACK_SPEED_MODIFIER_UUID = "B49D7F68-566A-48C6-BFB4-19F7D4AC25EB"
    private const val ATTACK_DAMAGE_MODIFIER_UUID = "AD6EE9CB-6037-40AF-B122-10A4590A55E3"

    private const val LAST_BLEED_TIME_TAG = "SbwRadiationLastBleedTime"

    init {
        addAttributeModifier(
            Attributes.MAX_HEALTH,
            MAX_HEALTH_MODIFIER_UUID,
            -0.9,
            AttributeModifier.Operation.MULTIPLY_TOTAL
        )
        addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            MOVEMENT_SPEED_MODIFIER_UUID,
            -0.9,
            AttributeModifier.Operation.MULTIPLY_TOTAL
        )
        addAttributeModifier(
            Attributes.ATTACK_SPEED,
            ATTACK_SPEED_MODIFIER_UUID,
            -0.9,
            AttributeModifier.Operation.MULTIPLY_TOTAL
        )
        addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            ATTACK_DAMAGE_MODIFIER_UUID,
            -0.9,
            AttributeModifier.Operation.MULTIPLY_TOTAL
        )
    }

    override fun getAttributeModifierValue(amplifier: Int, modifier: AttributeModifier): Double {
        return -getAttributeReduction(getLevel(amplifier))
    }

    override fun getCurativeItems(): List<ItemStack> = emptyList()

    override fun isDurationEffectTick(duration: Int, amplifier: Int): Boolean {
        return duration % DOSE_INTERVAL == 0
    }

    override fun applyEffectTick(entity: LivingEntity, amplifier: Int) {
        if (entity.level().isClientSide) return

        val level = getLevel(amplifier)
        val dose = RadiationCapability.addDosage(entity, DOSE_INTERVAL * (2 + 0.25f * level))
        if (dose < DOSE_SYMPTOM_THRESHOLD) return

        if (entity is Player) {
            entity.causeFoodExhaustion((0.005f * level).coerceAtMost(0.1f))
        }

        entity.forceHurt(
            ModDamageTypes.causeRadiationDamage(entity.level().registryAccess(), null),
            getRadiationDamage(dose, level)
        )
    }

    @SubscribeEvent
    fun onLivingHeal(event: LivingHealEvent) {
        val dose = RadiationCapability.getDosage(event.entity)
        if (dose < DOSE_HEAL_REDUCE_THRESHOLD) return

        val rate = ((dose - DOSE_HEAL_REDUCE_THRESHOLD) / (DOSE_HEAL_BLOCK - DOSE_HEAL_REDUCE_THRESHOLD))
            .coerceIn(0f, 1f)
        if (rate >= 1f) {
            event.isCanceled = true
        } else {
            event.amount *= 1f - rate
        }
    }

    @SubscribeEvent
    fun onLivingHurt(event: LivingHurtEvent) {
        val entity = event.entity
        val source = event.source
        if (source.`is`(ModDamageTypes.RADIATION)) return

        val dose = RadiationCapability.getDosage(entity)
        if (dose < DOSE_BLEED_THRESHOLD) return

        val gameTime = entity.level().gameTime
        val lastBleedTime = entity.persistentData.getLong(LAST_BLEED_TIME_TAG)
        if (gameTime - lastBleedTime < getBleedCooldown(dose)) return

        val damage = if (dose > DOSE_LETHAL_THRESHOLD) {
            event.amount + dose / 40f
        } else {
            event.amount * ((dose - DOSE_BLEED_THRESHOLD) / DOSE_LETHAL_THRESHOLD + 1f)
        }

        entity.persistentData.putLong(LAST_BLEED_TIME_TAG, gameTime)
        entity.forceHurt(
            ModDamageTypes.causeRadiationDamage(entity.level().registryAccess(), source.entity),
            damage
        )
    }

    @JvmStatic
    fun reduceLevel(entity: LivingEntity): Boolean {
        val instance = entity.getEffect(ModMobEffects.RADIATION.get()) ?: return false

        val level = getLevel(instance.amplifier)
        if (level <= 1) {
            return entity.removeEffect(ModMobEffects.RADIATION.get())
        } else {
            entity.forceAddEffect(
                MobEffectInstance(
                    ModMobEffects.RADIATION.get(),
                    MobEffectInstance.INFINITE_DURATION,
                    level - 2,
                    false,
                    true,
                    true
                ),
                null
            )
        }
        return true
    }

    @JvmStatic
    fun getBleedCooldown(dose: Float): Int {
        val ratio = ((dose - DOSE_BLEED_THRESHOLD) / (DOSE_LETHAL_THRESHOLD - DOSE_BLEED_THRESHOLD))
            .coerceIn(0f, 1f)
        return (BLEED_COOLDOWN_MAX - (BLEED_COOLDOWN_MAX - BLEED_COOLDOWN_MIN) * ratio).toInt()
    }

    @JvmStatic
    fun getRadiationDamage(dose: Float, level: Int): Float {
        if (dose < DOSE_SYMPTOM_THRESHOLD) return 0f

        val base = 0.25f + 0.05f * (level - 1)
        val doseFactor = ((dose - DOSE_SYMPTOM_THRESHOLD) / (DOSE_SYMPTOM_THRESHOLD * 4f)).coerceIn(0f, 1f)
        return base * (0.5f + 2f * doseFactor)
    }

    @JvmStatic
    fun getSaturation(dose: Float): Float {
        return ((dose - DOSE_SYMPTOM_THRESHOLD) / (DOSE_SATURATION_ZERO - DOSE_SYMPTOM_THRESHOLD)).coerceIn(0f, 1f)
    }

    @JvmStatic
    fun getAttributeReduction(level: Int): Double {
        if (level <= 1) return 0.0
        return 0.9 * (level.coerceAtMost(MAX_EFFECTS_LEVEL) - 1) / (MAX_EFFECTS_LEVEL - 1)
    }

    private fun getLevel(amplifier: Int): Int {
        return (amplifier + 1).coerceIn(1, MAX_EFFECTS_LEVEL)
    }
}
