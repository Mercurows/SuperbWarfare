package com.atsuishio.superbwarfare.item.gun.special;

import com.atsuishio.superbwarfare.client.renderer.gun.TaserItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.BatteryItem;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

public class TaserItem extends GunGeoItem {

    public TaserItem() {
        super(new Item.Properties());
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return TaserItemRenderer::new;
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
    public void afterShoot(@NotNull ShootParameters parameters) {
        super.afterShoot(parameters);

        var data = parameters.data();

        var stack = data.stack;
        int perkLevel = data.perk.getLevel(ModPerks.VOLT_OVERLOAD);
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> energy.extractEnergy(400 + 100 * perkLevel, false));
    }

    @Override
    public boolean canShoot(GunData data, @Nullable Entity shooter) {
        var stack = data.stack;

        int perkLevel = data.perk.getLevel(ModPerks.VOLT_OVERLOAD);
        var hasEnoughEnergy = stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> storage.getEnergyStored() >= 400 + 100 * perkLevel)
                .orElse(false);

        if (!hasEnoughEnergy) return false;

        return super.canShoot(data, shooter);
    }
}
