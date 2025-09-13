package com.atsuishio.superbwarfare.item.gun.special;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.energy.ItemEnergyProvider;
import com.atsuishio.superbwarfare.client.renderer.gun.RepairToolItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.EnergyImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.BatteryItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class RepairToolItem extends GunItem {

    public static final int MAX_ENERGY = 100000;

    private final Supplier<Integer> energyCapacity;

    public RepairToolItem() {
        super(new Properties().rarity(Rarity.COMMON));
        this.energyCapacity = () -> MAX_ENERGY;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack pStack) {
        if (!pStack.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
            return false;
        }

        int[] energy = {0};
        pStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                e -> energy[0] = e.getEnergyStored()
        );
        return energy[0] != 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack pStack) {
        int[] energy = {0};
        pStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                e -> energy[0] = e.getEnergyStored()
        );

        return Math.round((float) energy[0] * 13.0F / MAX_ENERGY);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag tag) {
        return new ItemEnergyProvider(stack, energyCapacity.get());
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        return 0xFFFF00;
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return RepairToolItemRenderer::new;
    }

//    private PlayState idlePredicate(AnimationState<RepairToolItem> event) {
//        LocalPlayer player = Minecraft.getInstance().player;
//        if (player == null) return PlayState.STOP;
//        ItemStack stack = player.getMainHandItem();
//        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
//        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
//            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.taser.idle"));
//
//        var data = GunData.from(stack);
//        if (data.reload.empty()) {
//            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.taser.reload"));
//        }
//
//        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.taser.idle"));
//    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
//        AnimationController<RepairToolItem> idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
//        data.add(idleController);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (entity instanceof Player player) {
            for (var cell : player.getInventory().items) {
                if (cell.getItem() instanceof BatteryItem) {
                    assert stack.getCapability(ForgeCapabilities.ENERGY).resolve().isPresent();
                    var stackStorage = stack.getCapability(ForgeCapabilities.ENERGY).resolve().get();
                    int stackMaxEnergy = stackStorage.getMaxEnergyStored();
                    int stackEnergy = stackStorage.getEnergyStored();

                    assert cell.getCapability(ForgeCapabilities.ENERGY).resolve().isPresent();
                    var cellStorage = cell.getCapability(ForgeCapabilities.ENERGY).resolve().get();
                    int cellEnergy = cellStorage.getEnergyStored();

                    int stackEnergyNeed = Math.min(cellEnergy, stackMaxEnergy - stackEnergy);

                    if (cellEnergy > 0) {
                        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                                iEnergyStorage -> iEnergyStorage.receiveEnergy(stackEnergyNeed, false)
                        );
                    }
                    cell.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                            cEnergy -> cEnergy.extractEnergy(stackEnergyNeed, false)
                    );
                }
            }
        }
    }

    @Override
    public ResourceLocation getGunIcon(GunData data) {
        return Mod.loc("textures/gun_icon/repair_tool_icon.png");
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new EnergyImageComponent(pStack));
    }

    @Override
    public void afterShoot(
            @Nullable Entity shooter,
            @NotNull ServerLevel level,
            @NotNull Vec3 shootPosition,
            @NotNull Vec3 shootDirection,
            @NotNull GunData data,
            double spread,
            boolean zoom,
            @Nullable UUID uuid
    ) {
        super.afterShoot(shooter, level, shootPosition, shootDirection, data, spread, zoom, uuid);

        var stack = data.stack;
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> energy.extractEnergy(50, false));
    }

    @Override
    public boolean canShoot(GunData data, @Nullable Entity shooter) {
        var stack = data.stack;

        var hasEnoughEnergy = stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> storage.getEnergyStored() >= 50)
                .orElse(false);

        if (!hasEnoughEnergy) return false;

        return super.canShoot(data, shooter);
    }
}
