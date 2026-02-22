package com.atsuishio.superbwarfare.block.property

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.StringRepresentable

enum class BlockPart(private val partName: String, val x: Int, val y: Int, val z: Int) : StringRepresentable {
    FLB("flb", 0, 0, 0),
    FRB("frb", 0, 0, 1),
    FLU("flu", 0, 1, 0),
    FRU("fru", 0, 1, 1),
    BLB("blb", 1, 0, 0),
    BRB("brb", 1, 0, 1),
    BLU("blu", 1, 1, 0),
    BRU("bru", 1, 1, 1);

    fun relative(pos: BlockPos, direction: Direction): BlockPos {
        return BlockPos(
            when (direction) {
                Direction.WEST, Direction.DOWN, Direction.UP -> pos.offset(x, y, z)
                Direction.NORTH -> pos.offset(-z, y, x)
                Direction.EAST -> pos.offset(-x, y, -z)
                Direction.SOUTH -> pos.offset(z, y, -x)
            }
        )
    }

    fun relativeNegative(pos: BlockPos, direction: Direction): BlockPos {
        return BlockPos(
            when (direction) {
                Direction.WEST, Direction.DOWN, Direction.UP -> pos.offset(-x, -y, -z)
                Direction.NORTH -> pos.offset(z, -y, -x)
                Direction.EAST -> pos.offset(x, -y, z)
                Direction.SOUTH -> pos.offset(-z, -y, x)
            }
        )
    }

    override fun toString(): String {
        return this.partName
    }

    override fun getSerializedName(): String {
        return this.partName
    }
}
