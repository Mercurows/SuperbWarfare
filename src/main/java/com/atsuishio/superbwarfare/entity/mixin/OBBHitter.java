package com.atsuishio.superbwarfare.entity.mixin;

import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.world.entity.Entity;

public interface OBBHitter {

    static OBBHitter getInstance(Entity entity) {
        return (OBBHitter) entity;
    }

    /**
     * 获取当前命中部分
     */
    OBB.Part sbw$getCurrentHitPart();

    /**
     * 设置当前命中部分
     */
    void sbw$setCurrentHitPart(OBB.Part part);
}
