package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import net.minecraft.world.entity.LivingEntity;

public class ProjectileWeapon extends VehicleWeapon {

    public float headShot, damage, bypassArmorRate;
    public boolean zoom;
    public int jhpLevel, heLevel;

    public ProjectileWeapon headShot(float headShot) {
        this.headShot = headShot;
        return this;
    }

    public ProjectileWeapon damage(float damage) {
        this.damage = damage;
        return this;
    }

    public ProjectileWeapon damage(double damage) {
        this.damage = (float) damage;
        return this;
    }

    public ProjectileWeapon bypassArmorRate(float bypassArmorRate) {
        this.bypassArmorRate = bypassArmorRate;
        return this;
    }

    public ProjectileWeapon zoom(boolean zoom) {
        this.zoom = zoom;
        return this;
    }

    public ProjectileWeapon jhpBullet(int jhpLevel) {
        this.jhpLevel = jhpLevel;
        return this;
    }

    public ProjectileWeapon heBullet(int heLevel) {
        this.heLevel = heLevel;
        return this;
    }

    public ProjectileEntity create(LivingEntity shooter) {
        return new ProjectileEntity(shooter.level())
                .shooter(shooter)
                .headShot(headShot)
                .damage(damage)
                .bypassArmorRate(bypassArmorRate)
                .zoom(zoom)
                .jhpBullet(jhpLevel)
                .heBullet(heLevel);
    }
}
