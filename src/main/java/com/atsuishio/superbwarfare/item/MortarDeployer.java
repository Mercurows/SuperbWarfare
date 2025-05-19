package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

public class MortarDeployer extends Item {

    public MortarDeployer() {
        super(new Properties().rarity(Rarity.RARE));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        } else {
            ItemStack stack = pContext.getItemInHand();
            BlockPos clickedPos = pContext.getClickedPos();
            Direction direction = pContext.getClickedFace();
            Player player = pContext.getPlayer();
            if (player == null) {
                return InteractionResult.PASS;
            }

            BlockState blockstate = level.getBlockState(clickedPos);
            BlockPos pos;
            if (blockstate.getCollisionShape(level, clickedPos).isEmpty()) {
                pos = clickedPos;
            } else {
                pos = clickedPos.relative(direction);
            }

            MortarEntity mortarEntity = new MortarEntity(level, player.getYRot());
            mortarEntity.setPos((double) pos.getX() + 0.5D, pos.getY() + 1, (double) pos.getZ() + 0.5D);
            double yOffset = this.getYOffset(level, pos, !Objects.equals(clickedPos, pos) && direction == Direction.UP, mortarEntity.getBoundingBox());
            mortarEntity.moveTo((double) pos.getX() + 0.5D, pos.getY() + yOffset, (double) pos.getZ() + 0.5D);
            level.addFreshEntity(mortarEntity);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.gameEvent(pContext.getPlayer(), GameEvent.ENTITY_PLACE, clickedPos);

            return InteractionResult.CONSUME;
        }
    }

    public double getYOffset(LevelReader pLevel, BlockPos pPos, boolean pShouldOffsetYMore, AABB pBox) {
        AABB aabb = new AABB(pPos);
        if (pShouldOffsetYMore) {
            aabb = aabb.expandTowards(0.0D, -1.0D, 0.0D);
        }

        Iterable<VoxelShape> iterable = pLevel.getCollisions(null, aabb);
        return 1.0D + Shapes.collide(Direction.Axis.Y, pBox, iterable, pShouldOffsetYMore ? -2.0D : -1.0D);
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        BlockHitResult blockhitresult = getPlayerPOVHitResult(pLevel, pPlayer, ClipContext.Fluid.SOURCE_ONLY);
        if (blockhitresult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack);
        } else if (!(pLevel instanceof ServerLevel)) {
            return InteractionResultHolder.success(itemstack);
        } else {
            BlockPos blockpos = blockhitresult.getBlockPos();
            if (!(pLevel.getBlockState(blockpos).getBlock() instanceof LiquidBlock)) {
                return InteractionResultHolder.pass(itemstack);
            } else if (pLevel.mayInteract(pPlayer, blockpos) && pPlayer.mayUseItemAt(blockpos, blockhitresult.getDirection(), itemstack)) {
                MortarEntity mortarEntity = new MortarEntity(pLevel, pPlayer.getYRot());
                mortarEntity.setPos((double) blockpos.getX() + 0.5D, blockpos.getY(), (double) blockpos.getZ() + 0.5D);
                pLevel.addFreshEntity(mortarEntity);

                if (!pPlayer.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }

                pPlayer.awardStat(Stats.ITEM_USED.get(this));
                pLevel.gameEvent(pPlayer, GameEvent.ENTITY_PLACE, mortarEntity.position());
                return InteractionResultHolder.consume(itemstack);
            } else {
                return InteractionResultHolder.fail(itemstack);
            }
        }
    }
}
