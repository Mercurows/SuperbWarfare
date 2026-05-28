package com.atsuishio.superbwarfare.item.gun.special;

import com.atsuishio.superbwarfare.client.renderer.gun.TaserItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

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
    public void afterShoot(@NotNull ShootParameters parameters) {
        super.afterShoot(parameters);

        var data = parameters.data;

        var stack = data.stack;
        int perkLevel = data.perk.getLevel(ModPerks.INSTANCE.getVOLT_OVERLOAD());
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> energy.extractEnergy(400 + 100 * perkLevel, false));
    }

    @Override
    public boolean canShoot(GunData data, @Nullable Entity shooter) {
        var stack = data.stack;

        int perkLevel = data.perk.getLevel(ModPerks.INSTANCE.getVOLT_OVERLOAD());
        var hasEnoughEnergy = stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> storage.getEnergyStored() >= 400 + 100 * perkLevel)
                .orElse(false);

        if (!hasEnoughEnergy) return false;

        return super.canShoot(data, shooter);
    }
}
