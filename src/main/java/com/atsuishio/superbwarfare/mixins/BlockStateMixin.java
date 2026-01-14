package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStateBase.class)
public class BlockStateMixin {

	@Inject(at = @At("HEAD"), method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", cancellable = true)
	private void getCollisionShape(BlockGetter worldIn, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> ci) {
		Entity entity = null;
		if (context instanceof EntityCollisionContext) {
			entity = ((EntityCollisionContext) context).getEntity();
		}
		if (entity instanceof VehicleEntity vehicle) {
			BlockState state = vehicle.level().getBlockState(pos);

			//TODO 未来添加更多能穿过的方块

			if (state.is(BlockTags.SWORD_EFFICIENT)) {
				ci.setReturnValue(Shapes.empty());
			}
		}
	}
}