package com.atsuishio.superbwarfare.block.property;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum BlockPart implements StringRepresentable {
    FLB("flb"),
    FLU("flu"),
    FRB("frb"),
    FRU("fru"),
    BLB("blb"),
    BLU("blu"),
    BRB("brb"),
    BRU("bru");

    private final String name;

    BlockPart(String pName) {
        this.name = pName;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}
