package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.goal.GunShootGoal;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Skeleton;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = Mod.MODID)
public class EntityUseGun {

    @SubscribeEvent
    public static void entityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) return;

        var entity = event.getEntity();
        if (entity instanceof Skeleton skeleton) {
            skeleton.goalSelector.addGoal(30, new GunShootGoal<>(skeleton));

            var data = GunData.from(ModItems.M_2_HB.get());
            data.virtualAmmo.set(114514);
            data.reloadAmmo(skeleton);
            data.save();

            skeleton.setItemInHand(InteractionHand.MAIN_HAND, data.stack);
        }
    }
}
