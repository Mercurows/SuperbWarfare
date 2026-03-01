package com.atsuishio.superbwarfare.item.projectile

import net.minecraft.core.Direction
import net.minecraft.core.Position
import net.minecraft.core.dispenser.BlockSource
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DispenserBlock

// Modified from Minecraft 1.20.1 source code
abstract class AbstractProjectileDispenseBehavior : DefaultDispenseItemBehavior() {

    override fun execute(source: BlockSource, stack: ItemStack): ItemStack {
        val level: Level = source.level
        val position = DispenserBlock.getDispensePosition(source)
        val direction: Direction = source.state.getValue(DispenserBlock.FACING)
        val projectile = this.getProjectile(level, position, stack)
        projectile.shoot(
            direction.stepX.toDouble(),
            direction.stepY.toDouble() + 0.1,
            direction.stepZ.toDouble(),
            getPower(),
            getUncertainty()
        )
        level.addFreshEntity(projectile)
        stack.shrink(1)
        return stack
    }

    override fun playSound(source: BlockSource) {
        source.level.levelEvent(1002, source.pos, 0)
    }

    protected abstract fun getProjectile(level: Level, position: Position, stack: ItemStack): Projectile

    protected open fun getPower() = 1.1F
    protected open fun getUncertainty() = 6F
}