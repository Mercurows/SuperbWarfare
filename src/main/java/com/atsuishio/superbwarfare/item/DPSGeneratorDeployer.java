package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class DPSGeneratorDeployer extends Item {

    public DPSGeneratorDeployer() {
        super(new Properties());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.dps_generator_deployer").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
    }

    private static final Predicate<Entity> IS_GENERATOR = e -> e instanceof DPSGeneratorEntity;

    @Override
    public @NotNull InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        } else {
            ItemStack itemstack = pContext.getItemInHand();
            BlockPos blockpos = pContext.getClickedPos();
            Direction direction = pContext.getClickedFace();
            BlockState blockstate = level.getBlockState(blockpos);
            BlockPos pos;
            if (blockstate.getCollisionShape(level, blockpos).isEmpty()) {
                pos = blockpos;
            } else {
                pos = blockpos.relative(direction);
            }

            // 禁止堆叠
            if (!level.getEntities(
                    (Entity) null,
                    ModEntities.DPS_GENERATOR.get().getSpawnAABB(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                    IS_GENERATOR
            ).isEmpty()) {
                return InteractionResult.FAIL;
            }

            if (ModEntities.DPS_GENERATOR.get().spawn((ServerLevel) level, itemstack, pContext.getPlayer(), pos, MobSpawnType.SPAWN_EGG, true, !Objects.equals(blockpos, pos) && direction == Direction.UP) != null) {
                itemstack.shrink(1);
                level.gameEvent(pContext.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
            }

            return InteractionResult.CONSUME;
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        BlockHitResult blockhitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (blockhitresult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack);
        } else if (!(level instanceof ServerLevel)) {
            return InteractionResultHolder.success(itemstack);
        } else {
            BlockPos blockpos = blockhitresult.getBlockPos();
            if (!(level.getBlockState(blockpos).getBlock() instanceof LiquidBlock)) {
                return InteractionResultHolder.pass(itemstack);
            } else if (level.mayInteract(player, blockpos) && player.mayUseItemAt(blockpos, blockhitresult.getDirection(), itemstack)) {
                // 禁止堆叠
                if (!level.getEntities(
                        (Entity) null,
                        ModEntities.DPS_GENERATOR.get().getSpawnAABB(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 0.5),
                        IS_GENERATOR
                ).isEmpty()) {
                    return InteractionResultHolder.fail(itemstack);
                }

                DPSGeneratorEntity entity = ModEntities.DPS_GENERATOR.get().spawn((ServerLevel) level, itemstack, player, blockpos, MobSpawnType.SPAWN_EGG, false, false);
                if (entity == null) {
                    return InteractionResultHolder.pass(itemstack);
                } else {
                    if (!player.getAbilities().instabuild) {
                        itemstack.shrink(1);
                    }

                    player.awardStat(Stats.ITEM_USED.get(this));
                    level.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position());
                    return InteractionResultHolder.consume(itemstack);
                }
            } else {
                return InteractionResultHolder.fail(itemstack);
            }
        }
    }
}
