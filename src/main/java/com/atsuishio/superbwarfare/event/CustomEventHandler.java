package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent;
import com.atsuishio.superbwarfare.api.event.ReloadEvent;
import com.atsuishio.superbwarfare.config.server.ProjectileConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class CustomEventHandler {

    @SubscribeEvent
    public static void onPreReload(ReloadEvent.Pre event) {
        var shooter = event.shooter;
        ItemStack stack = event.stack;
        if (shooter == null
                || !(stack.getItem() instanceof GunItem)
                || shooter.level().isClientSide
        ) return;

        GunData data = GunData.from(stack);
        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().preReload(data, instance, shooter);
            }
        }
    }

    @SubscribeEvent
    public static void onPostReload(ReloadEvent.Post event) {
        var shooter = event.shooter;
        ItemStack stack = event.stack;
        if (shooter == null || !(stack.getItem() instanceof GunItem)) {
            return;
        }

        if (shooter.level().isClientSide) {
            return;
        }

        GunData data = GunData.from(stack);
        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().postReload(data, instance, shooter);
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileHitEntity(ProjectileHitEvent.HitEntity event) {

    }

    @SubscribeEvent
    public static void onProjectileHitBlock(ProjectileHitEvent.HitBlock event) {
        var projectile = event.getProjectile();
        var state = event.getState();
        var pos = event.getPos();
        var face = event.getFace();

        if (projectile instanceof ProjectileEntity p) {
            if (state.getBlock() instanceof BellBlock bell) {
                bell.attemptToRing(p.level(), pos, face);
            }

            if (ProjectileConfig.ALLOW_PROJECTILE_DESTROY_BLOCKS.get() && state.is(ModTags.Blocks.BULLET_CAN_DESTROY)) {
                p.level().destroyBlock(pos, false, p.getShooter());
            }

            if (state.getBlock() instanceof TargetBlock) {
                p.recordHitScore(face, event.getHitVec());
            }
        }
    }
}
