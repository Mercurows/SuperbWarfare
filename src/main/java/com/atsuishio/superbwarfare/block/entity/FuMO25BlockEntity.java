package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.FuMO25Block;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class FuMO25BlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {

    public static SerializableDataTicket<Integer> FUMO25_TICK;

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

    private LazyOptional<EnergyStorage> energyHandler;

    public FuncType type = FuncType.NORMAL;
    public int time = 0;
    public boolean powered = false;
    public int tick = 0;
    public float yRot0 = 0;

    protected final ContainerEnergyData dataAccess = new ContainerEnergyData() {

        @Override
        public long get(int pIndex) {
            return switch (pIndex) {
                case 0 -> FuMO25BlockEntity.this.energyHandler.map(EnergyStorage::getEnergyStored).orElse(0);
                case 1 -> FuMO25BlockEntity.this.type.ordinal();
                case 2 -> FuMO25BlockEntity.this.time;
                case 3 -> FuMO25BlockEntity.this.powered ? 1 : 0;
                case 4 -> FuMO25BlockEntity.this.tick;
                default -> 0;
            };
        }

        @Override
        public void set(int pIndex, long pValue) {
            switch (pIndex) {
                case 0 ->
                        FuMO25BlockEntity.this.energyHandler.ifPresent(handler -> handler.receiveEnergy((int) pValue, false));
                case 1 -> FuMO25BlockEntity.this.type = FuncType.values()[(int) pValue];
                case 2 -> FuMO25BlockEntity.this.time = (int) pValue;
                case 3 -> FuMO25BlockEntity.this.powered = pValue == 1;
                case 4 -> FuMO25BlockEntity.this.tick = (int) pValue;
            }
        }

        @Override
        public int getCount() {
            return MAX_DATA_COUNT;
        }
    };

    public FuMO25BlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.FUMO_25.get(), pPos, pBlockState);
        this.energyHandler = LazyOptional.of(() -> new EnergyStorage(MAX_ENERGY));
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, FuMO25BlockEntity blockEntity) {
        int energy = blockEntity.energyHandler.map(EnergyStorage::getEnergyStored).orElse(0);

        if (pState.getValue(FuMO25Block.POWERED)) {
            blockEntity.tick++;
            blockEntity.setAnimData(FUMO25_TICK, blockEntity.tick);
        }

        FuncType funcType = blockEntity.type;
        int energyCost;
        if (funcType == FuncType.WIDER) {
            energyCost = MAX_ENERGY_COST;
        } else {
            energyCost = DEFAULT_ENERGY_COST;
        }

        if (energy < energyCost) {
            if (pState.getValue(FuMO25Block.POWERED)) {
                pLevel.setBlockAndUpdate(pPos, pState.setValue(FuMO25Block.POWERED, false));
                pLevel.playSound(null, pPos, ModSounds.RADAR_SEARCH_END.get(), SoundSource.BLOCKS, 1, 1);
                blockEntity.powered = false;
                setChanged(pLevel, pPos, pState);
            }
            if (blockEntity.time > 0) {
                blockEntity.time = 0;
                blockEntity.setChanged();
            }
        } else {
            if (!pState.getValue(FuMO25Block.POWERED)) {
                if (energy >= DEFAULT_MIN_ENERGY) {
                    pLevel.setBlockAndUpdate(pPos, pState.setValue(FuMO25Block.POWERED, true));
                    pLevel.playSound(null, pPos, ModSounds.RADAR_SEARCH_START.get(), SoundSource.BLOCKS, 1, 1);
                    blockEntity.powered = true;
                    setChanged(pLevel, pPos, pState);
                }
            } else {
                blockEntity.energyHandler.ifPresent(handler -> handler.extractEnergy(energyCost, false));
                if (blockEntity.tick == 200) {
                    pLevel.playSound(null, pPos, ModSounds.RADAR_SEARCH_IDLE.get(), SoundSource.BLOCKS, 1, 1);
                }

                if (blockEntity.time > 0) {
                    if (blockEntity.time % 100 == 0) {
                        blockEntity.setGlowEffect();
                    }
                    blockEntity.time--;
                    blockEntity.setChanged();
                }
            }
        }

        if (blockEntity.tick >= 200) {
            blockEntity.tick = 0;
        }

        if (blockEntity.time <= 0 && blockEntity.type != FuncType.NORMAL) {
            blockEntity.type = FuncType.NORMAL;
            blockEntity.setChanged();
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
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);

        if (pTag.contains("Energy")) {
            getCapability(ForgeCapabilities.ENERGY).ifPresent(handler -> ((EnergyStorage) handler).deserializeNBT(pTag.get("Energy")));
        }
        this.type = FuncType.values()[Mth.clamp(pTag.getInt("Type"), 0, 3)];
        this.time = pTag.getInt("Time");
        this.powered = pTag.getBoolean("Powered");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        super.saveAdditional(pTag);

        getCapability(ForgeCapabilities.ENERGY).ifPresent(handler -> pTag.put("Energy", ((EnergyStorage) handler).serializeNBT()));
        pTag.putInt("Type", this.type.ordinal());
        pTag.putInt("Time", this.time);
        pTag.putBoolean("Powered", this.powered);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.empty();
    }

    @Nullable
    @Override
    @ParametersAreNonnullByDefault
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        if (this.level == null) return null;
        return new FuMO25Menu(pContainerId, pPlayerInventory, ContainerLevelAccess.create(this.level, this.getBlockPos()), this.dataAccess);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.energyHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.energyHandler = LazyOptional.of(() -> new EnergyStorage(MAX_ENERGY));
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
