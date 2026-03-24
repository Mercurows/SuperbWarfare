package com.atsuishio.superbwarfare.entity.projectile

interface ExplosiveProjectile : CustomGravityEntity, CustomDamageProjectile {
    fun setExplosionDamage(explosionDamage: Float)

    fun setExplosionRadius(radius: Float)

    fun setLife(life: Int)
}
