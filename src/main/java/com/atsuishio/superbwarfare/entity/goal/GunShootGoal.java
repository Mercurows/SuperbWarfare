package com.atsuishio.superbwarfare.entity.goal;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;

public class GunShootGoal<T extends Mob & RangedAttackMob> extends Goal {
    private final T mob;

    public GunShootGoal(T mob) {
        this.mob = mob;
    }


    public boolean canUse() {
        return this.mob.getTarget() != null && this.mob.getWeaponItem().getItem() instanceof GunItem;
    }

    @Override
    public boolean canContinueToUse() {
        return (this.canUse() || !this.mob.getNavigation().isDone()) && this.mob.getWeaponItem().getItem() instanceof GunItem;
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
//        boolean canSeeTarget = this.mob.getSensing().hasLineOfSight(target);

        this.mob.lookAt(target, 30.0F, 30.0F);
//            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (distance > 30) {
            this.mob.getNavigation().moveTo(target, 1);
        } else {
            this.mob.getNavigation().stop();
        }

        var data = GunData.from(this.mob.getWeaponItem());
        data.tick(this.mob, true);

        if (data.shouldStartReloading(this.mob)) {
            data.startReload();
        }

        if (data.shouldStartBolt()) {
            data.startBolt();
        }

        if (data.canShoot(this.mob)) {
            var currentTime = System.currentTimeMillis();
            double rps = (double) data.get(GunProp.RPM) / 60;

            // cooldown in ms
            int cooldown = (int) Math.round(1000 / rps);

            if (currentTime - lastShootTime < cooldown) {
                return;
            }

            lastShootTime = currentTime;

            data.shoot(this.mob, 1, true, target.getUUID());
        }

    }
}
