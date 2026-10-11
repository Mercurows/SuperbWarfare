package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object RadiationConfig {
    @JvmField
    val PROCESS_INTERVAL = buildServerConfig {
        push("radiation")

        comment("How many ticks between two radiation processing steps, which cover natural decay, symptom damage and attribute update")
        comment("辐射处理间隔，单位为游戏刻，自然衰减、症状伤害与属性惩罚都按这个频率处理")
        defineInRange("process_interval", 10, 1, 200)
    }

    @JvmField
    val DECAY_RATE = buildServerConfig {
        comment("How much radiation dosage decays per tick, below the symptom threshold the rate is 1/5 of it")
        comment("Only applied while the entity has no radiation effect")
        comment("每游戏刻自然衰减的辐射剂量，剂量低于副作用阈值时按该值的1/5衰减")
        comment("仅在生物没有辐射效果时生效")
        defineInRange("decay_rate", 0.2, 0.0, 100.0)
    }

    @JvmField
    val DOSE_SYMPTOM_THRESHOLD = buildServerConfig {
        comment("Dosage at which radiation starts damaging the entity and reducing its attributes")
        comment("辐射副作用起点：剂量达到该值后开始持续受伤并受到属性惩罚")
        defineInRange("dose_symptom_threshold", 600.0, 0.0, 20000.0)
    }

    @JvmField
    val DOSE_HEAL_REDUCE_THRESHOLD = buildServerConfig {
        comment("Dosage at which healing starts being reduced, scaling up to DOSE_HEAL_BLOCK")
        comment("减疗起点：剂量达到该值后治疗量开始按比例降低，直到完全禁疗")
        defineInRange("dose_heal_reduce_threshold", 1000.0, 0.0, 20000.0)
    }

    @JvmField
    val DOSE_HEAL_BLOCK = buildServerConfig {
        comment("Dosage at which healing is completely blocked")
        comment("完全禁疗：剂量达到该值后无法通过任何方式治疗")
        defineInRange("dose_heal_block", 4000.0, 0.0, 20000.0)
    }

    @JvmField
    val DOSE_BLEED_THRESHOLD = buildServerConfig {
        comment("Dosage at which taking damage also causes extra bleeding damage")
        comment("额外流血起点：剂量达到该值后，受到伤害时会附加额外伤害")
        defineInRange("dose_bleed_threshold", 2000.0, 0.0, 20000.0)
    }

    @JvmField
    val DOSE_LETHAL_THRESHOLD = buildServerConfig {
        comment("Dosage at which radiation damage and attribute reduction reach their maximum")
        comment("致死剂量：剂量达到该值后辐射伤害与属性惩罚达到最大，流血伤害不再按倍率而是直接追加")
        defineInRange("dose_lethal_threshold", 8000.0, 0.0, 20000.0)
    }

    @JvmField
    val DOSE_SATURATION_ZERO = buildServerConfig {
        comment("Dosage at which the screen is fully desaturated by the radiation shader")
        comment("完全黑白：剂量达到该值后辐射画面的饱和度降到最低")
        defineInRange("dose_saturation_zero", 4500.0, 0.0, 20000.0).also { pop() }
    }
}