package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.AircraftCatapultBlock;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AircraftCatapultBlockEntity extends BlockEntity {

    public AircraftCatapultBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.AIRCRAFT_CATAPULT.get(), pPos, pBlockState);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AircraftCatapultBlockEntity blockEntity) {
        var direction = pState.getValue(AircraftCatapultBlock.FACING);
        int power = pState.getValue(AircraftCatapultBlock.POWER);
        if (power == 0) return;

        var list = pLevel.getEntitiesOfClass(Entity.class, new AABB(pPos.above()));
        list.forEach(entity -> {
            float rate = power / 1200f;
            if (entity instanceof LivingEntity) {
                rate = power / 100f;
            }
            if (entity.getDeltaMovement().dot(new Vec3(direction.getStepX(), 0, direction.getStepZ())) < 0.2 * power) {
                entity.addDeltaMovement(new Vec3(direction.getStepX() * rate, 0, direction.getStepZ() * rate));
            }
        });
    }

    public static void clientTick(Level pLevel, BlockPos pPos, BlockState pState, AircraftCatapultBlockEntity blockEntity) {
        var direction = pState.getValue(AircraftCatapultBlock.FACING);
        int power = pState.getValue(AircraftCatapultBlock.POWER);
        if (power == 0) return;

        var list = pLevel.getEntitiesOfClass(Player.class, new AABB(pPos.above()));
        list.forEach(entity -> {
            if (entity.getAbilities().flying) return;
            entity.addDeltaMovement(new Vec3(direction.getStepX() * power / 100f, 0, direction.getStepZ() * power / 100f));
        });
    }
}
