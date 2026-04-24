package com.atsuishio.superbwarfare.item.gun.special;

import com.atsuishio.superbwarfare.client.renderer.gun.TaserItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public class TaserItem extends GunGeoItem {

    public TaserItem() {
        super(new Properties().rarity(Rarity.COMMON));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return TaserItemRenderer::new;
    }

    @Override
    public void afterShoot(@NotNull ShootParameters parameters) {
        super.afterShoot(parameters);

        var data = parameters.data;

        var stack = data.stack;
        int perkLevel = data.perk.getLevel(ModPerks.VOLT_OVERLOAD);

        var energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage != null) {
            energyStorage.extractEnergy(400 + 100 * perkLevel, false);
        }
    }

    @Override
    public boolean canShoot(GunData data, @Nullable Entity shooter) {
        int perkLevel = data.perk.getLevel(ModPerks.VOLT_OVERLOAD);

        var energyStorage = data.stack.getCapability(Capabilities.EnergyStorage.ITEM);
        var hasEnoughEnergy = energyStorage != null && energyStorage.getEnergyStored() >= 400 + 100 * perkLevel;

        if (!hasEnoughEnergy) return false;

        return super.canShoot(data, shooter);
    }

}
