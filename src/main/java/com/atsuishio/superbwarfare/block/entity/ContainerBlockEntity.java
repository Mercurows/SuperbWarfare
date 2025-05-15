package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.ContainerBlock;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ContainerBlockEntity extends BlockEntity implements GeoBlockEntity {

    public EntityType<?> entityType;
    public CompoundTag entityTag = null;
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
                pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, (1.0F + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.2F) * 0.7F);
            }
        } else {
            var direction = pState.getValue(ContainerBlock.FACING);

            var entity = blockEntity.entityType.create(pLevel);
            if (entity == null) return;

            if (blockEntity.entityTag != null) {
                entity.load(blockEntity.entityTag);
            }

            entity.setPos(pPos.getX() + 0.5 + (2 * Math.random() - 1) * 0.1f, pPos.getY() + 0.5 + (2 * Math.random() - 1) * 0.1f, pPos.getZ() + 0.5 + (2 * Math.random() - 1) * 0.1f);
            entity.setYRot(direction.toYRot());
            pLevel.addFreshEntity(entity);

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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // 保存额外DataComponent以确保正确生成掉落物
    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder components) {
        super.collectImplicitComponents(components);

        components.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(saveToTag()));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        loadFromTag(tag);
    }

    private void loadFromTag(CompoundTag tag) {
        if (tag.contains("EntityType")) {
            this.entityType = EntityType.byString(tag.getString("EntityType")).orElse(null);
        }
        if (tag.contains("Entity") && this.entityTag == null && this.entityType != null) {
            this.entityTag = tag.getCompound("Entity");
        }
        this.tick = tag.getInt("Tick");
    }

    private CompoundTag saveToTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "superbwarfare:container");
        saveDataToTag(tag);
        return tag;
    }

    private void saveDataToTag(CompoundTag tag) {
        if (this.entityType != null) {
            tag.putString("EntityType", EntityType.getKey(this.entityType).toString());
        }
        if (this.entityTag != null) {
            tag.put("Entity", this.entityTag);
        }
        tag.putInt("Tick", this.tick);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        saveDataToTag(tag);
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
    public void saveToItem(@NotNull ItemStack stack, HolderLookup.@NotNull Provider registries) {
        super.saveToItem(stack, registries);

        CompoundTag tag = new CompoundTag();
        if (this.entityType != null) {
            tag.putString("EntityType", EntityType.getKey(this.entityType).toString());
        }
        BlockItem.setBlockEntityData(stack, this.getType(), tag);
    }

}
