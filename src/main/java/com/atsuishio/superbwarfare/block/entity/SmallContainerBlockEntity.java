package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.ContainerBlock;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class SmallContainerBlockEntity extends BlockEntity implements GeoBlockEntity {

    @Nullable
    public ResourceLocation lootTable;
    public long lootTableSeed;
    public int tick = 0;
    @Nullable
    private Player player;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SmallContainerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMALL_CONTAINER.get(), pos, state);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, SmallContainerBlockEntity blockEntity) {
        if (!pState.getValue(ContainerBlock.OPENED)) {
            return;
        }

        if (blockEntity.tick < 20) {
            blockEntity.tick++;
            blockEntity.setChanged();

            if (blockEntity.tick == 18) {
                ParticleTool.sendParticle((ServerLevel) pLevel, ParticleTypes.EXPLOSION, pPos.getX(), pPos.getY() + 1, pPos.getZ(), 40, 1.5, 1.5, 1.5, 1, false);
                pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4F, (1F + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.2F) * 0.7F);
            }
        } else {
            var items = blockEntity.unpackLootTable(blockEntity.player);
            if (!items.isEmpty()) {
                for (var item : items) {
                    ItemEntity entity = new ItemEntity(pLevel, pPos.getX() + 0.5, pPos.getY() + 0.85, pPos.getZ() + 0.5, item);
                    entity.setDeltaMovement(new Vec3(pLevel.random.nextDouble() * 0.1, 0.1, pLevel.random.nextDouble() * 0.1));
                    pLevel.addFreshEntity(entity);
                }
            }
            pLevel.setBlockAndUpdate(pPos, Blocks.AIR.defaultBlockState());
        }
    }

    private PlayState predicate(AnimationState<SmallContainerBlockEntity> event) {
        if (this.getBlockState().getValue(ContainerBlock.OPENED)) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.container.open"));
        }
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void load(@NotNull CompoundTag compound) {
        super.load(compound);
        if (compound.contains("LootTable", 8)) {
            this.lootTable = new ResourceLocation(compound.getString("LootTable"));
            this.lootTableSeed = compound.getLong("LootTableSeed");
        }
        this.tick = compound.getInt("Tick");
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag compound) {
        super.saveAdditional(compound);
        if (this.lootTable != null) {
            compound.putString("LootTable", this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                compound.putLong("LootTableSeed", this.lootTableSeed);
            }
        }
        compound.putInt("Tick", this.tick);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return this.saveWithFullMetadata();
    }

    @Override
    public void saveToItem(@NotNull ItemStack pStack) {
        CompoundTag tag = new CompoundTag();
        if (this.lootTable != null) {
            tag.putString("LootTable", this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                tag.putLong("LootTableSeed", this.lootTableSeed);
            }
        }
        BlockItem.setBlockEntityData(pStack, this.getType(), tag);
    }

    public void setLootTable(ResourceLocation pLootTable, long pLootTableSeed) {
        this.lootTable = pLootTable;
        this.lootTableSeed = pLootTableSeed;
    }

    public List<ItemStack> unpackLootTable(@Nullable Player pPlayer) {
        if (this.lootTable != null && this.level != null && this.level.getServer() != null) {
            LootTable loottable = this.level.getServer().getLootData().getLootTable(this.lootTable);
            if (pPlayer instanceof ServerPlayer) {
                CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer) pPlayer, this.lootTable);
            }

            this.lootTable = null;
            LootParams.Builder builder = (new LootParams.Builder((ServerLevel) this.level))
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition));
            if (pPlayer != null) {
                builder.withLuck(pPlayer.getLuck()).withParameter(LootContextParams.THIS_ENTITY, pPlayer);
            }

            return loottable.getRandomItems(builder.create(LootContextParamSets.CHEST), this.lootTableSeed).stream().toList();
        }
        return Collections.emptyList();
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(@Nullable Player player) {
        this.player = player;
    }
}
