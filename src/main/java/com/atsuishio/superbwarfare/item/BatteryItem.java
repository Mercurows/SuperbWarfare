package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BatteryItem extends Item {

    public int maxEnergy;

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    private final IEnergyStorage energy;

    public BatteryItem(int maxEnergy, Properties properties) {
        super(properties.stacksTo(1));
        this.maxEnergy = maxEnergy;
        this.energy = new EnergyStorage(maxEnergy);
    }

    @Override
    public boolean isBarVisible(ItemStack pStack) {
        var cap = pStack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap == null) return false;
        return cap.getEnergyStored() != cap.getMaxEnergyStored();
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        var energy = 0;
        var cap = pStack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap != null) {
            energy = cap.getEnergyStored();
        }

        return Math.round((float) energy * 13.0F / maxEnergy);
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        return 0xFFFF00;
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new CellImageComponent(pStack));
    }

    public ItemStack makeFullEnergyStack() {
        ItemStack stack = new ItemStack(this);
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap == null) return stack;

        cap.receiveEnergy(maxEnergy, false);
        return stack;
    }

}
