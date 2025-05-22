package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class SwarmDrone extends Item implements ProjectileItem {

    public SwarmDrone() {
        super(new Properties());
    }

    public static class SwarmDroneDispenseBehavior extends ProjectileDispenseBehavior {

        public SwarmDroneDispenseBehavior() {
            super(ModItems.SWARM_DRONE.get());
        }

        @Override
        @ParametersAreNonnullByDefault
        public @NotNull ItemStack execute(BlockSource blockSource, ItemStack stack) {
            Level level = blockSource.level();
            Position position = DispenserBlock.getDispensePosition(blockSource);
            Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
            SwarmDroneEntity projectile = createProjectile(level, position);

            float yVec = direction.getStepY();
            if (direction != Direction.DOWN) {
                yVec += 1F;
            }

            projectile.shoot(direction.getStepX(), yVec, direction.getStepZ(), 1.5f, 1);

            BlockHitResult result = level.clip(new ClipContext(new Vec3(position.x(), position.y(), position.z()),
                    new Vec3(position.x(), position.y(), position.z()).add(new Vec3(direction.step().mul(128))),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
            Vec3 hitPos = result.getLocation();
            projectile.setGuideType(1);
            projectile.setTargetVec(hitPos);

            level.addFreshEntity(projectile);
            stack.shrink(1);

            blockSource.level().playSound(null, blockSource.pos(), ModSounds.DECOY_FIRE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
            return stack;
        }
    }

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
                .power(1.5f)
                .build();
    }

    private static SwarmDroneEntity createProjectile(Level level, Position pos) {
        return new SwarmDroneEntity(pos.x(), pos.y(), pos.z(), level);
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return createProjectile(level, pos);
    }
}