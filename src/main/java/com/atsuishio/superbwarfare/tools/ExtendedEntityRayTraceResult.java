package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Author: MrCrayFish
 */
public class ExtendedEntityRayTraceResult extends EntityHitResult {
    private final boolean headshot;
    private final boolean legShot;

    public ExtendedEntityRayTraceResult(ProjectileEntity.EntityResult result) {
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
