package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import net.minecraft.core.Position
import net.minecraft.core.dispenser.BlockSource
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

open class MortarShellItem : Item(Properties().stacksTo(8)), DispenserLaunchable {
    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : AbstractProjectileDispenseBehavior() {
            override fun getPower(): Float {
                return 0.5f
            }

            override fun getProjectile(level: Level, position: Position, stack: ItemStack): Projectile {
                return MortarShellEntity(
                    ModEntities.MORTAR_SHELL.get(),
                    position.x(),
                    position.y(),
                    position.z(),
                    level,
                    0.13f
                )
            }

            override fun playSound(source: BlockSource) {
                source.level.playSound(null, source.pos, ModSounds.MORTAR_FIRE.get(), SoundSource.BLOCKS, 1f, 1f)
            }
        }
    }

    companion object {
        @JvmStatic
        fun createShell(
            entity: LivingEntity?,
            level: Level,
            stack: ItemStack,
            gravity: Float,
            damage: Float,
            explosionDamage: Float,
            explosionRadius: Float
        ): MortarShellEntity {
            val shellEntity = MortarShellEntity(entity, level, damage, explosionDamage, explosionRadius)
            shellEntity.setGravity(gravity)
            shellEntity.setEffectsFromItem(stack)
            shellEntity.setType(if (stack.`is`(ModItems.MORTAR_SHELL_WP.get())) MortarShellEntity.Type.WP else MortarShellEntity.Type.NORMAL)
            return shellEntity
        }
    }
}
