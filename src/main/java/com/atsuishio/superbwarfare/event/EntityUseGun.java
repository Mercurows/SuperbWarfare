package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.SpawnConfig;
import com.atsuishio.superbwarfare.data.mob_guns.MobGunData;
import com.atsuishio.superbwarfare.entity.goal.GunShootGoal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = Mod.MODID)
public class EntityUseGun {

    @SubscribeEvent
    public static void entityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !SpawnConfig.SPAWN_MOB_WITH_GUNS.get()) return;

        var entity = event.getEntity();
        if (!(entity instanceof Mob mob)) return;

        var data = MobGunData.from(mob);

        if (data.probability() <= 0 || data.probability() < entity.level().random.nextDouble()) {
            return;
        }

        var gunData = data.getGunData();
        if (gunData == null) {
            return;
        }

        // TODO 正确处理权重
        mob.goalSelector.addGoal(data.goalWeight(), new GunShootGoal<>(mob, data));

        if (data.backupAmmoCount() > 0) {
            gunData.virtualAmmo.set(data.backupAmmoCount());
        }

        if (data.spawnWithLoadedAmmo()) {
            gunData.reloadAmmo(mob);
        }

        gunData.save();

        mob.setItemInHand(InteractionHand.MAIN_HAND, gunData.stack);
    }
}
