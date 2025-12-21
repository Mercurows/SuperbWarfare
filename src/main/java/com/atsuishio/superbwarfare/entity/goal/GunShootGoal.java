package com.atsuishio.superbwarfare.entity.goal;

import com.atsuishio.superbwarfare.data.gun.FireMode;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.mob_guns.MobGunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.MillisTimer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

// TODO 正确处理追踪距离
public class GunShootGoal<T extends Mob> extends Goal {
    private final T mob;
    private final MobGunData data;
    private int aimTime = 0;
    private final MillisTimer shootTimer = new MillisTimer();

    public GunShootGoal(T mob, MobGunData data) {
        this.mob = mob;
        this.data = data;
    }

    public boolean canUse() {
        return this.mob.getTarget() != null
                && this.mob.getMainHandItem().getItem() instanceof GunItem
                && this.data.getGunData() != null
                && (this.data.getGunData().countBackupAmmo(mob) > 0 || this.data.getGunData().hasEnoughAmmoToShoot(this.mob));
    }

    @Override
    public boolean canContinueToUse() {
        return (this.canUse() || !this.mob.getNavigation().isDone())
                && this.mob.getMainHandItem().getItem() instanceof GunItem
                && this.data.getGunData() != null
                && (this.data.getGunData().countBackupAmmo(mob) > 0 || this.data.getGunData().hasEnoughAmmoToShoot(this.mob));
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

        this.mob.lookAt(target, 30F, 30F);
//            this.mob.getLookControl().setLookAt(target, 30F, 30F);

        if (distance > data.shootDistance()) {
            this.mob.getNavigation().moveTo(target, 1);
        } else {
            this.mob.getNavigation().stop();
        }

        var gunData = GunData.from(this.mob.getMainHandItem());
        gunData.tick(this.mob, true);

        if (gunData.shouldStartReloading(this.mob)) {
            gunData.startReload();
        }

        if (gunData.shouldStartBolt()) {
            gunData.startBolt();
        }

        if (gunData.canShoot(this.mob) && aimTime >= this.data.aimTime()) {
            double rps = (double) gunData.get(GunProp.RPM) / 60;

            // cooldown in ms
            long cooldown = Math.round(1000 / rps);

            var fireMode = gunData.selectedFireModeInfo().mode;
            // 半自动或连发开火时，添加额外的开火冷却时间
            if (fireMode == FireMode.SEMI || fireMode == FireMode.BURST && gunData.burstAmount.get() == 0) {
                cooldown += data.semiFireInterval();
            }

            if (!shootTimer.started()) {
                shootTimer.start();
                // 首发瞬间发射
                shootTimer.setProgress(cooldown + 1);
            }

            if (shootTimer.getProgress() >= cooldown) {
                var newProgress = shootTimer.getProgress();

                // 低帧率下的开火次数补偿
                do {
                    gunData.shoot(this.mob, data.spread(), data.zoom(), target.getUUID());
                    newProgress -= cooldown;
                } while (newProgress - cooldown > 0);

                shootTimer.setProgress(newProgress);
            }
        } else {
            shootTimer.stop();
        }

    }
}
