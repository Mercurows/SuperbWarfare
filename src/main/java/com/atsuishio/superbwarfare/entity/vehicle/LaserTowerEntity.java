package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;

public class LaserTowerEntity extends AutoAimableEntity {

    public LaserTowerEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.LASER_TOWER.get(), world);
    }

    public LaserTowerEntity(EntityType<LaserTowerEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }
}
