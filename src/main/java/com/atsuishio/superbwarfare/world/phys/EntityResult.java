package com.atsuishio.superbwarfare.world.phys;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityResult {
    private final Entity entity;
    private final Vec3 hitVec;
    private final boolean headshot;
    private final boolean legShot;

    public EntityResult(Entity entity, Vec3 hitVec, boolean headshot, boolean legShot) {
        this.entity = entity;
        this.hitVec = hitVec;
        this.headshot = headshot;
        this.legShot = legShot;
    }

    /**
     * Gets the entity that was hit by the projectile
     */
    public Entity getEntity() {
        return this.entity;
    }

    /**
     * Gets the position the projectile hit
     */
    public Vec3 getHitPos() {
        return this.hitVec;
    }

    /**
     * Gets if this was a headshot
     */
    public boolean isHeadshot() {
        return this.headshot;
    }

    public boolean isLegShot() {
        return this.legShot;
    }
}
