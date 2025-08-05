package com.atsuishio.superbwarfare.entity.projectile;

public interface ExplosiveProjectile extends CustomGravityEntity, CustomDamageProjectile {
    void setExplosionDamage(float explosionDamage);

    void setExplosionRadius(float radius);
}
