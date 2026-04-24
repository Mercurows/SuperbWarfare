package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.model.item.M79ItemModel;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public class M79Item extends GunGeoItem {

    public M79Item() {
        super(new Item.Properties().rarity(Rarity.RARE));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return GunRendererBuilder.simple(M79ItemModel::new);
    }

    @Override
    public boolean shootBullet(@NotNull ShootParameters parameters) {
        if (!super.shootBullet(parameters)) return false;

        var shooter = parameters.shooter;
        var level = parameters.level;

        if (shooter != null) {
            ParticleTool.sendParticle(level, ParticleTypes.CLOUD, shooter.getX() + 1.8 * shooter.getLookAngle().x,
                    shooter.getY() + shooter.getBbHeight() - 0.1 + 1.8 * shooter.getLookAngle().y,
                    shooter.getZ() + 1.8 * shooter.getLookAngle().z,
                    4, 0.1, 0.1, 0.1, 0.002, true);
        }

        return true;
    }
}