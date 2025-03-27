package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class FuMO25BlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final int MAX_ENERGY = 1000000;

    // 固定距离，以后有人改动这个需要自行解决GUI渲染问题
    public static final int DEFAULT_RANGE = 96;
    public static final int MAX_RANGE = 128;
    public static final int GLOW_RANGE = 64;

    public static final int DEFAULT_ENERGY_COST = 256;
    public static final int MAX_ENERGY_COST = 1024;

    public static final int DEFAULT_MIN_ENERGY = 64000;

    public static final int MAX_DATA_COUNT = 5;

//    private LazyOptional<EnergyStorage> energyHandler;

    public FuncType type = FuncType.NORMAL;
    public int time = 0;
    public boolean powered = false;
    public int tick = 0;

    protected final ContainerEnergyData dataAccess = new ContainerEnergyData() {
        // TODO energy
        @Override
        public int get(int pIndex) {
            return switch (pIndex) {
//                case 0 -> FuMO25BlockEntity.this.energyHandler.map(EnergyStorage::getEnergyStored).orElse(0);
                case 1 -> FuMO25BlockEntity.this.type.ordinal();
                case 2 -> FuMO25BlockEntity.this.time;
                case 3 -> FuMO25BlockEntity.this.powered ? 1 : 0;
                case 4 -> FuMO25BlockEntity.this.tick;
                default -> 0;
            };
        }

        @Override
        public void set(int pIndex, int pValue) {
            switch (pIndex) {
                case 0 -> {
                }
//                        FuMO25BlockEntity.this.energyHandler.ifPresent(handler -> handler.receiveEnergy((int) pValue, false));
                case 1 -> FuMO25BlockEntity.this.type = FuncType.values()[pValue];
                case 2 -> FuMO25BlockEntity.this.time = pValue;
                case 3 -> FuMO25BlockEntity.this.powered = pValue == 1;
                case 4 -> FuMO25BlockEntity.this.tick = pValue;
            }
        }

        @Override
        public int getCount() {
            return MAX_DATA_COUNT;
        }
    };

    public FuMO25BlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.FUMO_25.get(), pPos, pBlockState);
//        this.energyHandler = LazyOptional.of(() -> new EnergyStorage(MAX_ENERGY));
    }

    public static <T extends BlockEntity> void serverTick(Level pLevel, BlockPos pPos, BlockState pState, T blockEntity) {
        var radar = (FuMO25BlockEntity) blockEntity;

//        int energy = radar.energyHandler.map(EnergyStorage::getEnergyStored).orElse(0);
        radar.tick++;

        FuncType funcType = radar.type;
        int energyCost;
        if (funcType == FuncType.WIDER) {
            energyCost = MAX_ENERGY_COST;
        } else {
            energyCost = DEFAULT_ENERGY_COST;
        }

//        if (energy < energyCost) {
//            if (pState.getValue(FuMO25Block.POWERED)) {
//                pLevel.setBlockAndUpdate(pPos, pState.setValue(FuMO25Block.POWERED, false));
//                pLevel.playSound(null, pPos, ModSounds.RADAR_SEARCH_END.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
//                radar.powered = false;
//                setChanged(pLevel, pPos, pState);
//            }
//            if (radar.time > 0) {
//                radar.time = 0;
//                radar.setChanged();
//            }
//        } else {
//            if (!pState.getValue(FuMO25Block.POWERED)) {
//                if (energy >= DEFAULT_MIN_ENERGY) {
//                    pLevel.setBlockAndUpdate(pPos, pState.setValue(FuMO25Block.POWERED, true));
//                    pLevel.playSound(null, pPos, ModSounds.RADAR_SEARCH_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
//                    radar.powered = true;
//                    setChanged(pLevel, pPos, pState);
//                }
//            } else {
//                radar.energyHandler.ifPresent(handler -> handler.extractEnergy(energyCost, false));
//                if (radar.tick == 200) {
//                    pLevel.playSound(null, pPos, ModSounds.RADAR_SEARCH_IDLE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
//                }
//
//                if (radar.time > 0) {
//                    if (radar.time % 100 == 0) {
//                        radar.setGlowEffect();
//                    }
//                    radar.time--;
//                    radar.setChanged();
//                }
//            }
//        }

        if (radar.tick >= 200) {
            radar.tick = 0;
        }

        if (radar.time <= 0 && radar.type != FuncType.NORMAL) {
            radar.type = FuncType.NORMAL;
            radar.setChanged();
        }
    }

    private void setGlowEffect() {
        if (this.type != FuncType.GLOW) return;

        Level level = this.level;
        if (level == null) return;
        BlockPos pos = this.getBlockPos();
        List<Entity> entities = SeekTool.getEntitiesWithinRange(pos, level, GLOW_RANGE);
        entities.forEach(e -> {
            if (e instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false));
            }
        });
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("Energy")) {
//            getCapability(Capabilities.ENERGY).ifPresent(handler -> ((EnergyStorage) handler).deserializeNBT(tag.get("Energy")));
        }
        this.type = FuncType.values()[Mth.clamp(tag.getInt("Type"), 0, 3)];
        this.time = tag.getInt("Time");
        this.powered = tag.getBoolean("Powered");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

//        getCapability(Capabilities.ENERGY).ifPresent(handler -> tag.put("Energy", ((EnergyStorage) handler).serializeNBT()));
        tag.putInt("Type", this.type.ordinal());
        tag.putInt("Time", this.time);
        tag.putBoolean("Powered", this.powered);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        if (this.level == null) return null;
        return new FuMO25Menu(pContainerId, pPlayerInventory, ContainerLevelAccess.create(this.level, this.getBlockPos()), this.dataAccess);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

//    @Override
//    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
//        if (cap == Capabilities.ENERGY) {
//            return energyHandler.cast();
//        }
//        return super.getCapability(cap, side);
//    }
//
//    @Override
//    public void invalidateCapabilities() {
//        this.energyHandler.invalidate();
//        super.invalidateCapabilities();
//    }
//
//
//    @Override
//    public void reviveCaps() {
//        super.reviveCaps();
//        this.energyHandler = LazyOptional.of(() -> new EnergyStorage(MAX_ENERGY));
//    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public enum FuncType {
        NORMAL,
        WIDER,
        GLOW,
        GUIDE
    }
}
