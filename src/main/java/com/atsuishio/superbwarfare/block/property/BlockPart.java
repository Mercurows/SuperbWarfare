package com.atsuishio.superbwarfare.block.property;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum BlockPart implements StringRepresentable {
    FLB("flb", 0, 0, 0),
    FRB("frb", 0, 0, 1),
    FLU("flu", 0, 1, 0),
    FRU("fru", 0, 1, 1),
    BLB("blb", 1, 0, 0),
    BRB("brb", 1, 0, 1),
    BLU("blu", 1, 1, 0),
    BRU("bru", 1, 1, 1);

    private final String name;
    final int x, y, z;

    BlockPart(String name, int x, int y, int z) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockPos relative(BlockPos pos, Direction direction) {
        return new BlockPos(switch (direction) {
            case WEST, DOWN, UP -> pos.offset(x, y, z);
            case NORTH -> pos.offset(-z, y, x);
            case EAST -> pos.offset(-x, y, -z);
            case SOUTH -> pos.offset(z, y, -x);
        });
    }

    public BlockPos relativeNegative(BlockPos pos, Direction direction) {
        return new BlockPos(switch (direction) {
            case WEST, DOWN, UP -> pos.offset(-x, -y, -z);
            case NORTH -> pos.offset(z, -y, -x);
            case EAST -> pos.offset(x, -y, z);
            case SOUTH -> pos.offset(-z, -y, x);
        });
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
