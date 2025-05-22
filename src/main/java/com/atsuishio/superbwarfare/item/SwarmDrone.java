package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class SwarmDrone extends Item implements ProjectileItem {

    public SwarmDrone() {
        super(new Properties());
    }

//    @Override
//    public AbstractProjectileDispenseBehavior getLaunchBehavior() {
//        return new AbstractProjectileDispenseBehavior() {
//
//            @Override
//            public ItemStack execute(BlockSource blockSource, ItemStack pStack) {
//                Level level = blockSource.getLevel();
//                Position position = DispenserBlock.getDispensePosition(blockSource);
//                Direction direction = blockSource.getBlockState().getValue(DispenserBlock.FACING);
//                Projectile projectile = this.getProjectile(level, position, pStack);
//
//                float yVec = direction.getStepY();
//                if (direction != Direction.DOWN) {
//                    yVec += 1F;
//                }
//
//                projectile.shoot(direction.getStepX(), yVec, direction.getStepZ(), this.getPower(), this.getUncertainty());
//
//                BlockHitResult result = level.clip(new ClipContext(new Vec3(position.x(), position.y(), position.z()),
//                        new Vec3(position.x(), position.y(), position.z()).add(new Vec3(direction.step().mul(128))),
//                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
//                Vec3 hitPos = result.getLocation();
//                ((SwarmDroneEntity) projectile).setGuideType(1);
//                ((SwarmDroneEntity) projectile).setTargetVec(hitPos);
//
//                level.addFreshEntity(projectile);
//                pStack.shrink(1);
//                return pStack;
//            }
//
//            @Override
//            @ParametersAreNonnullByDefault
//            protected @NotNull Projectile getProjectile(Level pLevel, Position pos, ItemStack pStack) {
//                return new SwarmDroneEntity(pos.x(), pos.y(), pos.z(), pLevel);
//            }
//
//            @Override
//            protected void playSound(BlockSource blockSource) {
//                blockSource.getLevel().playSound(null, blockSource.getPos(), ModSounds.DECOY_FIRE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
//            }
//        };
//    }

//    public class SwarmDroneDispenseBehavior extends ProjectileDispenseBehavior {
//
//        public SwarmDroneDispenseBehavior(Item projectile) {
//            super(projectile);
//        }
//
//        @Override
//        @ParametersAreNonnullByDefault
//        public @NotNull ItemStack execute(BlockSource blockSource, ItemStack stack) {
//            Level level = blockSource.level();
//            Position position = DispenserBlock.getDispensePosition(blockSource);
//            Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
//            Projectile projectile = this.getProjectile(level, position, stack);
//
//            float yVec = direction.getStepY();
//            if (direction != Direction.DOWN) {
//                yVec += 1F;
//            }
//
//            projectile.shoot(direction.getStepX(), yVec, direction.getStepZ(), this.getPower(), this.getUncertainty());
//
//            BlockHitResult result = level.clip(new ClipContext(new Vec3(position.x(), position.y(), position.z()),
//                    new Vec3(position.x(), position.y(), position.z()).add(new Vec3(direction.step().mul(128))),
//                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
//            Vec3 hitPos = result.getLocation();
//            ((SwarmDroneEntity) projectile).setGuideType(1);
//            ((SwarmDroneEntity) projectile).setTargetVec(hitPos);
//
//            level.addFreshEntity(projectile);
//            pStack.shrink(1);
//            return pStack;
//            return super.execute(blockSource, stack);
//        }
//    }

    // TODO 怎么发射？

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.swarm_drone").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NotNull DispenseConfig createDispenseConfig() {
        return ProjectileItem.DispenseConfig.builder()
                .uncertainty(1)
                .power(0.5f)
                .build();
    }


    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new SwarmDroneEntity(pos.x(), pos.y(), pos.z(), level);
    }
}