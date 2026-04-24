package com.atsuishio.superbwarfare.item.gun.shotgun;

import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.model.item.HomemadeShotgunItemModel;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public class HomemadeShotgunItem extends GunGeoItem {

    public HomemadeShotgunItem() {
        super(new Item.Properties());
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return GunRendererBuilder.simple(HomemadeShotgunItemModel::new);
    }

    @Override
    public boolean isOpenBolt(GunData data) {
        return true;
    }

    @Override
    public void beforeShoot(@NotNull ShootParameters parameters) {
        super.beforeShoot(parameters);

        var shooter = parameters.shooter;
        var level = parameters.level;

        if (shooter instanceof ServerPlayer serverPlayer) {
            ParticleTool.sendParticle(level, ParticleTypes.CLOUD, shooter.getX() + 1.8 * shooter.getLookAngle().x, shooter.getY() + shooter.getBbHeight() - 0.1 + 1.8 * shooter.getLookAngle().y,
                    shooter.getZ() + 1.8 * shooter.getLookAngle().z, 30, 0.4, 0.4, 0.4, 0.005, true, serverPlayer);
        }
    }
}
