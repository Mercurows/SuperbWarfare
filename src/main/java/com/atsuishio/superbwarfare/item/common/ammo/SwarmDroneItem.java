package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.DispenserLaunchable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class SwarmDroneItem extends Item implements DispenserLaunchable {

    public SwarmDroneItem() {
        super(new Properties());
    }

    @Override
    public AbstractProjectileDispenseBehavior getLaunchBehavior() {
        return new AbstractProjectileDispenseBehavior() {

            @Override
            public ItemStack execute(BlockSource pSource, ItemStack pStack) {
                Level level = pSource.getLevel();
                Position position = DispenserBlock.getDispensePosition(pSource);
                Direction direction = pSource.getBlockState().getValue(DispenserBlock.FACING);
                Projectile projectile = this.getProjectile(level, position, pStack);

                float yVec = direction.getStepY();
                if (direction != Direction.DOWN) {
                    yVec += 1F;
                }

                projectile.shoot(direction.getStepX(), yVec, direction.getStepZ(), this.getPower(), this.getUncertainty());

                BlockHitResult result = level.clip(new ClipContext(new Vec3(position.x(), position.y(), position.z()),
                        new Vec3(position.x(), position.y(), position.z()).add(new Vec3(direction.step().mul(128))),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
                Vec3 hitPos = result.getLocation();
                ((SwarmDroneEntity) projectile).setGuideType(1);
                ((SwarmDroneEntity) projectile).setTargetVec(hitPos);

                level.addFreshEntity(projectile);
                pStack.shrink(1);
                return pStack;
            }

            @Override
            protected float getPower() {
                return 1.5F;
            }

            @Override
            protected float getUncertainty() {
                return 1F;
            }

            @Override
            @ParametersAreNonnullByDefault
            protected @NotNull Projectile getProjectile(Level pLevel, Position pPosition, ItemStack pStack) {
                return new SwarmDroneEntity(pPosition.x(), pPosition.y(), pPosition.z(), pLevel);
            }

            @Override
            protected void playSound(BlockSource pSource) {
                pSource.getLevel().playSound(null, pSource.getPos(), ModSounds.DECOY_FIRE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
            }
        };
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.swarm_drone").withStyle(ChatFormatting.GRAY));
    }
}