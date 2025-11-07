package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent;
import com.atsuishio.superbwarfare.api.event.ReloadEvent;
import com.atsuishio.superbwarfare.config.server.ProjectileConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.GrapeshotEntity;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber
public class CustomEventHandler {

    @SubscribeEvent
    public static void onPreReload(ReloadEvent.Pre event) {
        var shooter = event.shooter;
        ItemStack stack = event.stack;
        if (shooter == null || !(stack.getItem() instanceof GunItem) || shooter.level().isClientSide) return;

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
        if (shooter == null || !(stack.getItem() instanceof GunItem) || shooter.level().isClientSide) {
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
        var entity = event.getOwner();
        if (!(entity instanceof LivingEntity attacker)) return;

        ItemStack stack = attacker.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }
        var projectile = event.getProjectile();

        GunData data = GunData.from(stack);
        var key = ForgeRegistries.ENTITY_TYPES.getKey(projectile.getType());
        if (key == null) return;
        if (!data.compute().projectile().type.equals(key.toString())) return;

        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().onHit(attacker, data, instance, event.getTarget());
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileHitBlock(ProjectileHitEvent.HitBlock event) {
        var projectile = event.getProjectile();
        var state = event.getState();
        var pos = event.getPos();
        var face = event.getFace();

        if (state.getBlock() instanceof BellBlock bell) {
            if (projectile instanceof ProjectileEntity || projectile instanceof GrapeshotEntity) {
                bell.attemptToRing(projectile.level(), pos, face);
            }
        }

        if (projectile instanceof ProjectileEntity p) {
            if (ProjectileConfig.ALLOW_PROJECTILE_DESTROY_BLOCKS.get() && state.is(ModTags.Blocks.BULLET_CAN_DESTROY)) {
                p.level().destroyBlock(pos, false, p.getShooter());
            }

            if (state.getBlock() instanceof TargetBlock) {
                p.recordHitScore(face, event.getHitVec());
            }
        }
        if (projectile instanceof GrapeshotEntity grapeshotEntity) {
            if (ProjectileConfig.ALLOW_PROJECTILE_DESTROY_BLOCKS.get() && state.is(ModTags.Blocks.CANNON_SHOT_CAN_DESTROY)) {
                grapeshotEntity.level().destroyBlock(pos, false, grapeshotEntity.getOwner());
            }
        }
    }
}
