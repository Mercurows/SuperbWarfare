package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.customExplode
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

open class RgoGrenadeEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    constructor(type: EntityType<out RgoGrenadeEntity>, level: Level) : super(type, level)

    constructor(type: EntityType<out RgoGrenadeEntity>, x: Double, y: Double, z: Double, level: Level) :
            super(type, x, y, z, level)

    constructor(entity: LivingEntity?, level: Level) : super(ModEntities.RGO_GRENADE.get(), entity, level)

    @Suppress("unused")
    constructor(entity: LivingEntity?, level: Level, life: Int) : this(entity, level) {
        this.lifeValue = life
    }

    init {
        this.explosionDamageValue = ExplosionConfig.RGO_GRENADE_EXPLOSION_DAMAGE.get().toFloat()
        this.explosionRadiusValue = ExplosionConfig.RGO_GRENADE_EXPLOSION_RADIUS.get().toFloat()
    }

    override fun getDefaultItem(): Item {
        return ModItems.RGO_GRENADE.get()
    }

    override fun performOnHit(
        entity: Entity,
        damage: Float,
        headshot: Boolean,
        knockback: Double
    ) {
        this.customExplode(this.explosionDamageValue, this.explosionRadiusValue)
    }

    override fun tick() {
        super.tick()
        val level = this.level() as? ServerLevel ?: return
        ParticleTool.sendParticle(
            level, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
            1, 0.0, 0.0, 0.0, 0.01, true
        )
    }
}
