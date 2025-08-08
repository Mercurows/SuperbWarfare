package com.atsuishio.superbwarfare.entity.goal;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.mob_guns.MobGunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

// TODO 正确处理追踪距离，正确计算开枪冷却等
public class GunShootGoal<T extends Mob> extends Goal {
    private final T mob;
    private final MobGunData data;
    private int aimTime = 0;

    public GunShootGoal(T mob, MobGunData data) {
        this.mob = mob;
        this.data = data;
    }

    public boolean canUse() {
        return this.mob.getTarget() != null
                && this.mob.getWeaponItem().getItem() instanceof GunItem
                && this.data.getGunData() != null
                && (this.data.getGunData().countBackupAmmo(mob) > 0 || this.data.getGunData().ammo.get() > 0);
    }

    @Override
    public boolean canContinueToUse() {
        return (this.canUse() || !this.mob.getNavigation().isDone())
                && this.mob.getWeaponItem().getItem() instanceof GunItem
                && this.data.getGunData() != null
                && (this.data.getGunData().countBackupAmmo(mob) > 0 || this.data.getGunData().ammo.get() > 0);
    }

    @Override
    public void start() {
        super.start();
        this.mob.setAggressive(true);
    }

    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.mob.stopUsingItem();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private long lastShootTime = 0;

    public void tick() {
        var target = this.mob.getTarget();
        if (target == null) return;

        double distance = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSeeTarget = this.mob.getSensing().hasLineOfSight(target);

        if (canSeeTarget) {
            aimTime = Math.min(data.aimTime(), aimTime + 1);
        } else {
            if (data.clearAimTimeWhenLostSight()) {
                aimTime = 0;
            } else {
                aimTime--;
            }
        }

        this.mob.lookAt(target, 30.0F, 30.0F);
//            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (distance > data.shootDistance()) {
            this.mob.getNavigation().moveTo(target, 1);
        } else {
            this.mob.getNavigation().stop();
        }

        var gunData = GunData.from(this.mob.getWeaponItem());
        gunData.tick(this.mob, true);

        if (gunData.shouldStartReloading(this.mob)) {
            gunData.startReload();
        }

        if (gunData.shouldStartBolt()) {
            gunData.startBolt();
        }

        if (gunData.canShoot(this.mob) && aimTime >= this.data.aimTime()) {
            var currentTime = System.currentTimeMillis();
            double rps = (double) gunData.get(GunProp.RPM) / 60;

            // cooldown in ms
            int cooldown = (int) Math.round(1000 / rps);

            if (currentTime - lastShootTime < cooldown) {
                return;
            }

            lastShootTime = currentTime;

            gunData.shoot(this.mob, 1, true, target.getUUID());
        }

    }
}
