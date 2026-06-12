package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent
import com.atsuishio.superbwarfare.api.event.ReloadEvent
import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.projectile.GrapeshotEntity
import com.atsuishio.superbwarfare.entity.projectile.IAdvancedHitDetection
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.entity.projectile.SuperStarProjectileEntity
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.Perk
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.level.block.TargetBlock
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries

@Mod.EventBusSubscriber
object CustomEventHandler {
    @SubscribeEvent
    fun onPreReload(event: ReloadEvent.Pre) {
        val shooter = event.entity ?: return
        val stack = event.stack
        if (stack.item !is GunItem || shooter.level().isClientSide) return

        val data = GunData.from(stack)
        for (type in Perk.Type.entries) {
            val instance = data.perk.getInstances(type)
            instance.forEach { it.perk.preReload(data, it, shooter) }
        }
    }

    @SubscribeEvent
    fun onPostReload(event: ReloadEvent.Post) {
        val shooter = event.entity ?: return
        val stack = event.stack
        if (stack.item !is GunItem || shooter.level().isClientSide) return

        val data = GunData.from(stack)
        for (type in Perk.Type.entries) {
            val instance = data.perk.getInstances(type)
            instance.forEach { it.perk.postReload(data, it, shooter) }
        }
    }

    @SubscribeEvent
    fun onProjectileHitEntity(event: ProjectileHitEvent.HitEntity) {
        val entity = event.owner
        if (entity !is LivingEntity) return

        val stack = entity.mainHandItem
        if (stack.item !is GunItem) return

        val projectile = event.projectile
        val data = GunData.from(stack)
        val key = ForgeRegistries.ENTITY_TYPES.getKey(projectile.type) ?: return

        if (data.get(GunProp.PROJECTILE).itemId != key.toString()) return

        for (type in Perk.Type.entries) {
            val instance = data.perk.getInstances(type)
            instance.forEach { it.perk.onHit(entity, data, it, event.target) }
        }
    }

    @SubscribeEvent
    fun onProjectileHitBlock(event: ProjectileHitEvent.HitBlock) {
        val projectile = event.projectile
        val state = event.state
        val pos = event.pos
        val face = event.face
        val block = state.block

        if (block is BellBlock) {
            if (projectile is ProjectileEntity || projectile is GrapeshotEntity || projectile is SuperStarProjectileEntity) {
                block.attemptToRing(projectile.level(), pos, face)
            }
        }

        if (projectile is IAdvancedHitDetection && block is TargetBlock) {
            projectile.recordHitScore(face, event.hitVec)
        }

        if (projectile is ProjectileEntity) {
            if (ProjectileConfig.PROJECTILE_DESTROY_BLOCKS.get() && state.`is`(ModTags.Blocks.BULLET_CAN_DESTROY)) {
                projectile.level().destroyBlock(pos, false, projectile.owner)
            }
        }

        if (projectile is GrapeshotEntity) {
            if (ProjectileConfig.PROJECTILE_DESTROY_BLOCKS.get() && state.`is`(ModTags.Blocks.CANNON_SHOT_CAN_DESTROY)) {
                projectile.level().destroyBlock(pos, false, projectile.owner)
            }
        }
    }
}