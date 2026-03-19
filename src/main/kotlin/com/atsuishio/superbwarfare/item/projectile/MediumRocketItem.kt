package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import net.minecraft.core.Position
import net.minecraft.core.dispenser.BlockSource
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class MediumRocketItem(
    private val damage: Float,
    private val radius: Float,
    private val explosionDamage: Float,
    private val fireProbability: Float,
    private val fireTime: Int,
    @JvmField val type: MediumRocketEntity.Type,
    private val spreadAmount: Int
) : Item(
    Properties().stacksTo(4)
), DispenserLaunchable {
    fun createProjectile(level: Level, pos: Position): MediumRocketEntity {
        return MediumRocketEntity(
            ModEntities.MEDIUM_ROCKET.get(),
            pos.x(),
            pos.y(),
            pos.z(),
            level,
            damage,
            radius,
            explosionDamage,
            fireProbability,
            fireTime,
            type,
            spreadAmount,
            15
        )
    }

    override fun getLaunchBehavior(): AbstractProjectileDispenseBehavior {
        return object : AbstractProjectileDispenseBehavior() {
            override fun getPower(): Float {
                return 6f
            }

            override fun getProjectile(level: Level, position: Position, stack: ItemStack): Projectile {
                return createProjectile(level, position)
            }

            override fun playSound(source: BlockSource) {
                source.level
                    .playSound(null, source.pos, ModSounds.MEDIUM_ROCKET_FIRE.get(), SoundSource.BLOCKS, 4f, 1f)
            }
        }
    }
}