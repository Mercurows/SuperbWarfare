package com.atsuishio.superbwarfare.mobeffect

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.capability.living.RadiationCapability
import com.atsuishio.superbwarfare.config.server.RadiationConfig
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
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.EffectCure
import net.neoforged.neoforge.event.entity.living.LivingHealEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber
object RadiationMobEffect : MobEffect(MobEffectCategory.HARMFUL, 0x55FF55) {
    const val MAX_EFFECTS_LEVEL = 20

    const val DOSE_INTERVAL = 5

    const val SYMPTOM_DECAY_RATE = 1f
    const val SUBSYMPTOM_DECAY_RATE = SYMPTOM_DECAY_RATE / 5f

    const val MIN_ATTRIBUTE_REDUCTION = 0.0
    const val MAX_ATTRIBUTE_REDUCTION = 0.9

    const val BLEED_COOLDOWN_MAX = 40
    const val BLEED_COOLDOWN_MIN = 4

    private const val LAST_BLEED_TIME_TAG = "SbwRadiationLastBleedTime"

    private const val FOOD_EXHAUSTION_PER_STEP = 0.01f

    override fun fillEffectCures(
        cures: Set<EffectCure?>,
        effectInstance: MobEffectInstance
    ) {
    }

    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return duration % DOSE_INTERVAL == 0
    }

    override fun applyEffectTick(entity: LivingEntity, amplifier: Int): Boolean {
        if (entity.level().isClientSide) return false

        RadiationCapability.addDosage(entity, 2f * (1f + (getLevel(amplifier) + 1) / 2f))
        return true
    }

    @SubscribeEvent
    fun onLivingTick(event: EntityTickEvent.Post) {
        val living = event.entity as? LivingEntity ?: return
        if (living.level().isClientSide || living.isDeadOrDying) return

        val interval = RadiationConfig.PROCESS_INTERVAL.get()
        if ((living.tickCount + living.id) % interval != 0) return

        val capability = RadiationCapability.getOrNull(living) ?: return
        val before = capability.dosage

        val symptomThreshold = getSymptomThreshold()
        val symptomatic = before >= symptomThreshold

        val dose = if (before <= 0f) {
            0f
        } else if (living.hasEffect(ModMobEffects.RADIATION)) {
            before
        } else {
            val decayRate = if (symptomatic) SYMPTOM_DECAY_RATE else SUBSYMPTOM_DECAY_RATE
            val decayed = (before - (RadiationConfig.DECAY_RATE.get() * decayRate * interval).toFloat())
                .coerceAtLeast(0f)
            RadiationCapability.setDosage(living, decayed)
            decayed
        }

        if (dose >= symptomThreshold) {
            if (living is Player) {
                living.causeFoodExhaustion(FOOD_EXHAUSTION_PER_STEP * interval)
            }

            living.forceHurt(
                ModDamageTypes.causeRadiationDamage(living.level().registryAccess(), null),
                getRadiationDamage(dose)
            )
            living.invulnerableTime = 0

            setRadiationAttributes(living, dose)
        } else {
            setRadiationAttributes(living, 0f)
        }
    }

    @SubscribeEvent
    fun onLivingHeal(event: LivingHealEvent) {
        val dose = RadiationCapability.getDosage(event.entity)
        val reduceThreshold = getHealReduceThreshold()
        if (dose < reduceThreshold) return

        val rate = ((dose - reduceThreshold) / (getHealBlockThreshold() - reduceThreshold)).coerceIn(0f, 1f)
        if (rate >= 1f) {
            event.isCanceled = true
        } else {
            event.amount *= 1f - rate
        }
    }

    @SubscribeEvent
    fun onLivingHurt(event: LivingIncomingDamageEvent) {
        val entity = event.entity
        val source = event.source
        if (source.`is`(ModDamageTypes.RADIATION)) return

        val dose = RadiationCapability.getDosage(entity)
        val bleedThreshold = getBleedThreshold()
        if (dose < bleedThreshold) return

        val gameTime = entity.level().gameTime
        val lastBleedTime = entity.persistentData.getLong(LAST_BLEED_TIME_TAG)
        if (gameTime - lastBleedTime < getBleedCooldown(dose)) return

        val lethalThreshold = getLethalThreshold()
        val damage = if (dose > lethalThreshold) {
            event.amount + dose / 40f
        } else {
            event.amount * ((dose - bleedThreshold) / lethalThreshold + 1f)
        }

        entity.persistentData.putLong(LAST_BLEED_TIME_TAG, gameTime)
        event.amount = damage
    }

    private val RADIATION_ATTRIBUTES = listOf(
        Pair(Attributes.MAX_HEALTH, loc("radiation_max_health")),
        Pair(Attributes.MOVEMENT_SPEED, loc("radiation_movement_speed")),
        Pair(Attributes.ATTACK_SPEED, loc("radiation_attack_speed")),
        Pair(Attributes.ATTACK_DAMAGE, loc("radiation_attack_damage")),
    )

    private fun setRadiationAttributes(entity: LivingEntity, dose: Float) {
        val reduction = if (dose >= getSymptomThreshold()) getAttributeReduction(dose) else 0.0

        for ((attribute, id) in RADIATION_ATTRIBUTES) {
            val instance = entity.getAttribute(attribute) ?: continue

            instance.removeModifier(id)
            if (reduction > 0.0) {
                instance.addTransientModifier(
                    AttributeModifier(id, -reduction, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                )
            }
        }
    }

    private fun ratio(value: Float, from: Float, to: Float): Float {
        return ((value - from) / (to - from)).coerceIn(0f, 1f)
    }

    @JvmStatic
    fun clear(entity: LivingEntity) {
        entity.removeEffect(ModMobEffects.RADIATION)
        RadiationCapability.setDosage(entity, 0f)
        setRadiationAttributes(entity, 0f)
        entity.persistentData.remove(LAST_BLEED_TIME_TAG)
    }

    @JvmStatic
    fun reduceLevel(entity: LivingEntity): Boolean {
        val instance = entity.getEffect(ModMobEffects.RADIATION) ?: return false

        val level = getLevel(instance.amplifier)
        if (level <= 1) {
            return entity.removeEffect(ModMobEffects.RADIATION)
        } else {
            entity.forceAddEffect(
                MobEffectInstance(
                    ModMobEffects.RADIATION,
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
    fun getSymptomThreshold() = try {
        RadiationConfig.DOSE_SYMPTOM_THRESHOLD.get().toFloat()
    } catch (_: Exception) {
        600f
    }

    @JvmStatic
    fun getHealReduceThreshold() = try {
        RadiationConfig.DOSE_HEAL_REDUCE_THRESHOLD.get().toFloat()
    } catch (_: Exception) {
        1000f
    }

    @JvmStatic
    fun getHealBlockThreshold() = try {
        RadiationConfig.DOSE_HEAL_BLOCK.get().toFloat()
    } catch (_: Exception) {
        4000f
    }

    @JvmStatic
    fun getBleedThreshold() = try {
        RadiationConfig.DOSE_BLEED_THRESHOLD.get().toFloat()
    } catch (_: Exception) {
        2000f
    }

    @JvmStatic
    fun getLethalThreshold() = try {
        RadiationConfig.DOSE_LETHAL_THRESHOLD.get().toFloat()
    } catch (_: Exception) {
        8000f
    }

    @JvmStatic
    fun getSaturationZero() = try {
        RadiationConfig.DOSE_SATURATION_ZERO.get().toFloat()
    } catch (_: Exception) {
        4500f
    }

    @JvmStatic
    fun getBleedCooldown(dose: Float): Int {
        val ratio = ratio(dose, getBleedThreshold(), getLethalThreshold())
        return (BLEED_COOLDOWN_MAX - (BLEED_COOLDOWN_MAX - BLEED_COOLDOWN_MIN) * ratio).toInt()
    }

    @JvmStatic
    fun getRadiationDamage(dose: Float): Float {
        val symptomThreshold = getSymptomThreshold()
        if (dose < symptomThreshold) return 0f

        return 0.5f + 3.5f * ratio(dose, symptomThreshold, getLethalThreshold())
    }

    @JvmStatic
    fun getAttributeReduction(dose: Float): Double {
        val ratio = ratio(dose, getSymptomThreshold(), getLethalThreshold())
        return MIN_ATTRIBUTE_REDUCTION + (MAX_ATTRIBUTE_REDUCTION - MIN_ATTRIBUTE_REDUCTION) * ratio
    }

    @JvmStatic
    fun getSaturation(dose: Float): Float {
        return ratio(dose, getSymptomThreshold(), getSaturationZero())
    }

    private fun getLevel(amplifier: Int): Int {
        return (amplifier + 1).coerceIn(1, MAX_EFFECTS_LEVEL)
    }
}
