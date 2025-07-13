package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.ContainerBlock;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;

public class LuckyContainerBlockEntity extends BlockEntity implements GeoBlockEntity {

    public int tick = 0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LuckyContainerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTAINER.get(), pos, state);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, LuckyContainerBlockEntity blockEntity) {
        if (!pState.getValue(ContainerBlock.OPENED)) {
            return;
        }

        if (blockEntity.tick < 20) {
            blockEntity.tick++;
            blockEntity.setChanged();

            if (blockEntity.tick == 18) {
                ParticleTool.sendParticle((ServerLevel) pLevel, ParticleTypes.EXPLOSION, pPos.getX(), pPos.getY() + 1, pPos.getZ(), 40, 1.5, 1.5, 1.5, 1, false);
                pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, (1.0F + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.2F) * 0.7F);
            }
        } else {
            var direction = pState.getValue(ContainerBlock.FACING);

            pLevel.setBlockAndUpdate(pPos, Blocks.AIR.defaultBlockState());
        }
    }

    private PlayState predicate(AnimationState<LuckyContainerBlockEntity> event) {
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
    @ParametersAreNonnullByDefault
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tick = tag.getInt("Tick");
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Tick", this.tick);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return this.saveWithFullMetadata(registries);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        BlockItem.setBlockEntityData(stack, this.getType(), tag);
    }
}
