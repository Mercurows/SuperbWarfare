package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.LuckyContainerBlock;
import com.atsuishio.superbwarfare.data.container.ContainerDataManager;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class LuckyContainerBlockEntity extends BlockEntity implements GeoBlockEntity {

    @Nullable
    public ResourceLocation location;
    @Nullable
    public ResourceLocation icon;
    public int tick = 0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LuckyContainerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LUCKY_CONTAINER.get(), pos, state);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, LuckyContainerBlockEntity blockEntity) {
        if (!pState.getValue(LuckyContainerBlock.OPENED)) {
            return;
        }

        if (blockEntity.tick < 20) {
            blockEntity.tick++;
            blockEntity.setChanged();

            if (blockEntity.tick == 18) {
                ParticleTool.sendParticle((ServerLevel) pLevel, ParticleTypes.EXPLOSION, pPos.getX(), pPos.getY() + 1, pPos.getZ(), 40, 1.5, 1.5, 1.5, 1, false);
                pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4, (1 + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.2F) * 0.7F);
            }
        } else {
            var direction = pState.getValue(LuckyContainerBlock.FACING);
            var type = blockEntity.unpackEntities();

            if (type != null) {
                var entity = type.create(pLevel);
                if (entity != null) {
                    entity.setPos(pPos.getX() + 0.5 + (2 * Math.random() - 1) * 0.1f, pPos.getY() + 0.5 + (2 * Math.random() - 1) * 0.1f, pPos.getZ() + 0.5 + (2 * Math.random() - 1) * 0.1f);
                    entity.setYRot(direction.toYRot());
                    if (entity instanceof VehicleEntity vehicle) {
                        vehicle.setServerYaw(direction.toYRot());
                    }
                    pLevel.addFreshEntity(entity);
                }
            }

            pLevel.setBlockAndUpdate(pPos, Blocks.AIR.defaultBlockState());
        }
    }

    @Nullable
    public EntityType<?> unpackEntities() {
        if (this.location != null && this.level != null && this.level.getServer() != null) {
            ContainerDataManager dataManager = ContainerDataManager.INSTANCE;
            var list = dataManager.getEntityTypes(this.location);
            if (list.isPresent()) {
                var pool = list.get();
                int sum = pool.stream().mapToInt(Pair::second).sum();
                if (sum <= 0) return null;

                int rand = this.level.random.nextInt(sum);

                int cumulativeWeight = 0;
                for (var entry : pool) {
                    cumulativeWeight += entry.second();
                    if (rand < cumulativeWeight) {
                        return EntityType.byString(entry.first()).orElse(null);
                    }
                }
            }
        }
        return null;
    }

    private PlayState predicate(AnimationState<LuckyContainerBlockEntity> event) {
        if (this.getBlockState().getValue(LuckyContainerBlock.OPENED)) {
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
        if (compound.contains("Location", 8)) {
            this.location = new ResourceLocation(compound.getString("Location"));
        }
        if (compound.contains("Icon", 8)) {
            this.icon = new ResourceLocation(compound.getString("Icon"));
        }
        this.tick = compound.getInt("Tick");
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag compound) {
        super.saveAdditional(compound);
        if (this.location != null) {
            compound.putString("Location", this.location.toString());
        }
        if (this.icon != null) {
            compound.putString("Icon", this.icon.toString());
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
        if (this.location != null) {
            tag.putString("Location", this.location.toString());
        }
        if (this.icon != null) {
            tag.putString("Icon", this.icon.toString());
        }
        BlockItem.setBlockEntityData(pStack, this.getType(), tag);
    }
}
