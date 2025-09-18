package com.atsuishio.superbwarfare.world.phys;

import net.minecraft.world.phys.EntityHitResult;

/**
 * @author MrCrayFish
 */
public class ExtendedEntityRayTraceResult extends EntityHitResult {
    private final boolean headshot;
    private final boolean legShot;

    public ExtendedEntityRayTraceResult(EntityResult result) {
        super(result.getEntity(), result.getHitPos());
        this.headshot = result.isHeadshot();
        this.legShot = result.isLegShot();
    }

    public boolean isHeadshot() {
        return this.headshot;
    }

    public boolean isLegShot() {
        return this.legShot;
    }
}
