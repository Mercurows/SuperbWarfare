package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.client.renderer.gun.RpgItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RpgItem extends GunGeoItem {

    public RpgItem() {
        super(new Item.Properties().rarity(Rarity.RARE));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return RpgItemRenderer::new;
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
                    30, 0.4, 0.4, 0.4, 0.005, true);
        }

        return true;
    }

    @Override
    public void whenNoAmmo(GunData data) {
        data.isEmpty.set(true);
        data.closeHammer.set(true);
    }

    @Override
    public void addReloadTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
        super.addReloadTimeBehavior(behaviors);
        behaviors.put(84, data -> data.isEmpty.set(false));
        behaviors.put(9, data -> data.closeHammer.set(false));
    }

    @Override
    public boolean canEditAttachments(GunData data) {
        return true;
    }
}