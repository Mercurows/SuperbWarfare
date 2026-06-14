package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import com.atsuishio.superbwarfare.item.IDyeableSmokeItem
import net.minecraft.core.BlockSource
import net.minecraft.core.Position
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior
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

            override fun getProjectile(pLevel: Level, pPosition: Position, pStack: ItemStack): Projectile {
                return MortarShellEntity(
                    ModEntities.MORTAR_SHELL.get(),
                    pPosition.x(),
                    pPosition.y(),
                    pPosition.z(),
                    pLevel,
                    0.13f
                )
            }

            override fun playSound(pSource: BlockSource) {
                pSource.level.playSound(null, pSource.pos, ModSounds.MORTAR_FIRE.get(), SoundSource.BLOCKS, 1f, 1f)
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
            shellEntity.setCustomGravity(gravity)
            shellEntity.setEffectsFromItem(stack)
            shellEntity.setType(
                when {
                    stack.`is`(ModItems.MORTAR_SHELL_WP.get()) -> MortarShellEntity.Type.WP
                    stack.`is`(ModItems.MORTAR_SHELL_SMOKE.get()) -> {
                        val tag = stack.tag
                        if (tag != null && tag.contains(IDyeableSmokeItem.TAG_COLOR)) {
                            val color = tag.getInt(IDyeableSmokeItem.TAG_COLOR)
                            shellEntity.setRGB(
                                floatArrayOf(
                                    ((color shr 16) and 255).toFloat(),
                                    ((color shr 8) and 255).toFloat(),
                                    (color and 255).toFloat()
                                )
                            )
                        }
                        MortarShellEntity.Type.SMOKE
                    }

                    else -> MortarShellEntity.Type.NORMAL
                }
            )
            return shellEntity
        }
    }
}
