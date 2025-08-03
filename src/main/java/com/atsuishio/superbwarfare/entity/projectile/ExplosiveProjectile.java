package com.atsuishio.superbwarfare.entity.projectile;

public interface ExplosiveProjectile extends CustomGravityEntity {
    void setDamage(float damage);

    void setExplosionDamage(float explosionDamage);

    void setExplosionRadius(float radius);
}
