package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;

public class RiotBullet extends AmmoPerk {

    public RiotBullet() {
        super(new Builder("riot_bullet", Type.AMMO).bypassArmorRate(-0.3f).damageRate(0.9f).speedRate(0.8f).slug(true).rgb(70, 35, 230)
                .mobEffect(() -> MobEffects.MOVEMENT_SLOWDOWN).mobEffect(() -> MobEffects.WEAKNESS));
    }

    @Override
    public int getEffectAmplifier(PerkInstance instance) {
        return (int) (instance.level() * 0.25);
    }

    @Override
    public int getEffectDuration(PerkInstance instance) {
        return 20 + instance.level() * 10;
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        super.modifyProjectile(data, instance, entity);
        if (!(entity instanceof ProjectileEntity projectile)) return;
        projectile.getDamageModifiers().put(ProjectileEntity.RAIDERS_PREDICATE, 1.0f + 0.5f * instance.level());
    }
}
