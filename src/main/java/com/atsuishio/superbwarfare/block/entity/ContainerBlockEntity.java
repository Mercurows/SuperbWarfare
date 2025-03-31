package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.ContainerBlock;
import com.atsuishio.superbwarfare.entity.SenpaiEntity;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Random;

public class ContainerBlockEntity extends BlockEntity implements GeoBlockEntity {

    public EntityType<?> entityType;
    public Entity entity = null;
    public int tick = 0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ContainerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTAINER.get(), pos, state);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, ContainerBlockEntity blockEntity) {
        if (!pState.getValue(ContainerBlock.OPENED)) {
            return;
        }

        if (blockEntity.tick < 20) {
            blockEntity.tick++;
            blockEntity.setChanged();

            if (blockEntity.tick == 18) {
                ParticleTool.sendParticle((ServerLevel) pLevel, ParticleTypes.EXPLOSION, pPos.getX(), pPos.getY() + 1, pPos.getZ(), 40, 1.5, 1.5, 1.5, 1, false);
                pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.2F) * 0.7F);
            }
        } else {
            if (blockEntity.entity != null) {
                double random = new Random().nextDouble();
                if (random < 0.114) {
                    blockEntity.entity = new SenpaiEntity(ModEntities.SENPAI.get(), pLevel);
                }

                blockEntity.entity.setPos(pPos.getX() + 0.5 + (2 * Math.random() - 1) * 0.1f, pPos.getY() + 0.5 + (2 * Math.random() - 1) * 0.1f, pPos.getZ() + 0.5 + (2 * Math.random() - 1) * 0.1f);
                pLevel.addFreshEntity(blockEntity.entity);
            } else if (blockEntity.entityType != null) {
                var entity = blockEntity.entityType.create(pLevel);

                double random = new Random().nextDouble();
                if (random < 0.114) {
                    entity = new SenpaiEntity(ModEntities.SENPAI.get(), pLevel);
                }

                if (entity != null) {
                    entity.setPos(pPos.getX() + 0.5 + (2 * Math.random() - 1) * 0.1f, pPos.getY() + 0.5 + (2 * Math.random() - 1) * 0.1f, pPos.getZ() + 0.5 + (2 * Math.random() - 1) * 0.1f);
                    pLevel.addFreshEntity(entity);
                }
            }

            pLevel.setBlockAndUpdate(pPos, Blocks.AIR.defaultBlockState());
        }
    }

    private PlayState predicate(AnimationState<ContainerBlockEntity> event) {
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
    public void load(CompoundTag compound) {
        super.load(compound);
        if (compound.contains("EntityType")) {
            this.entityType = EntityType.byString(compound.getString("EntityType")).orElse(null);
        }
        if (compound.contains("Entity") && this.entityType != null && this.level != null) {
            this.entity = this.entityType.create(this.level);
            if (entity != null) {
                entity.load(compound.getCompound("Entity"));
            }
        }
        this.tick = compound.getInt("Tick");
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        if (this.entity != null) {
            compound.put("Entity", this.entity.serializeNBT());
        }
        if (this.entityType != null) {
            compound.putString("EntityType", EntityType.getKey(this.entityType).toString());
        }
        compound.putInt("Tick", this.tick);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithFullMetadata();
    }

    @Override
    public void saveToItem(ItemStack pStack) {
        CompoundTag tag = new CompoundTag();
        if (this.entityType != null) {
            tag.putString("EntityType", EntityType.getKey(this.entityType).toString());
        }
        BlockItem.setBlockEntityData(pStack, this.getType(), tag);
    }
}
