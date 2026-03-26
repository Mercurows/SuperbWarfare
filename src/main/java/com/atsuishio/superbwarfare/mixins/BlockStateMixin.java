package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStateBase.class)
public abstract class BlockStateMixin {

    @Shadow
    public abstract Block getBlock();

    @Shadow
    public abstract VoxelShape getShape(BlockGetter level, BlockPos pos);

    @Inject(at = @At("HEAD"), method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", cancellable = true)
    private void getCollisionShape(BlockGetter worldIn, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> ci) {
        Entity entity = null;
        if (context instanceof EntityCollisionContext) {
            entity = ((EntityCollisionContext) context).getEntity();
        }
        if (entity instanceof VehicleEntity vehicle) {
            BlockState state = vehicle.level().getBlockState(pos);

            if (state.is(ModTags.Blocks.VEHICLE_PASS_THROUGH) && !state.is(BlockTags.MINEABLE_WITH_AXE)) {
                ci.setReturnValue(Shapes.empty());
            }

            if (state.is(ModBlocks.DRAGON_TEETH.get())) {
                ci.setReturnValue(this.getShape(worldIn, pos).move(0, Mth.clamp(vehicle.getBbHeight() - 0.25, 0, 1), 0));
            }
        }
    }
}