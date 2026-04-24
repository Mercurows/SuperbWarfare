package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.client.renderer.gun.IglaItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.entity.projectile.IglaMissileEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ShootClientMessage;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public class IglaItem extends GunGeoItem {

    public IglaItem() {
        super(new Properties().rarity(Rarity.EPIC));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return IglaItemRenderer::new;
    }

    @Override
    public boolean useSpecialFireProcedure(@NotNull GunData data) {
        return true;
    }

    @Override
    public void shoot(@NotNull ShootParameters parameters) {
        var data = parameters.data;
        var shooter = parameters.shooter;
        var targetUUID = parameters.targetEntityUUID;
        var zoom = parameters.zoom;

        if (shooter == null) return;
        if (!zoom || !data.hasEnoughAmmoToShoot(shooter)) return;

        Level level = shooter.level();

        float yRot = shooter.getYRot() + 360;
        yRot = (yRot + 90) % 360;

        var firePos = new Vector3d(0, -0.2, 0.15);
        firePos.rotateZ(-shooter.getXRot() * Mth.DEG_TO_RAD);
        firePos.rotateY(-yRot * Mth.DEG_TO_RAD);

        if (shooter.level() instanceof ServerLevel serverLevel) {
            Entity targetEntity = EntityFindUtil.findEntity(serverLevel, String.valueOf(targetUUID));

            IglaMissileEntity iglaMissileEntity = new IglaMissileEntity(shooter, level,
                    data.get(GunProp.DAMAGE).floatValue(),
                    data.get(GunProp.EXPLOSION_DAMAGE).floatValue(),
                    data.get(GunProp.EXPLOSION_RADIUS).floatValue()
            );

            for (Perk.Type type : Perk.Type.getEntries()) {
                var instance = data.perk.getInstances(type);
                instance.forEach(perk -> perk.perk().modifyProjectile(data, perk, iglaMissileEntity));
            }

            iglaMissileEntity.setPos(shooter.getX() + firePos.x, shooter.getEyeY() + firePos.y, shooter.getZ() + firePos.z);
            iglaMissileEntity.shoot(shooter.getLookAngle().x, shooter.getLookAngle().y + 0.3, shooter.getLookAngle().z, 3f, 1);
            if (targetEntity != null) {
                iglaMissileEntity.setTargetUuid(targetEntity.getStringUUID());
            }

            level.addFreshEntity(iglaMissileEntity);

            ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, shooter.getX() + 1.8 * shooter.getLookAngle().x,
                    shooter.getY() + shooter.getBbHeight() - 0.1 + 1.8 * shooter.getLookAngle().y,
                    shooter.getZ() + 1.8 * shooter.getLookAngle().z,
                    30, 0.4, 0.4, 0.4, 0.005, true);

            if (shooter instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.IGLA_FIRE_1P.get(), 2, 1);
                NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), ShootClientMessage.INSTANCE);
            }

            SoundTool.playDistantSound(serverLevel, ModSounds.IGLA_FIRE_3P.get(), shooter.position(), 4, 1, shooter);
            SoundTool.playDistantSound(serverLevel, ModSounds.IGLA_FAR.get(), shooter.position(), 10, 1, shooter);
        }

        data.ammo.set(data.ammo.get() - data.get(GunProp.AMMO_COST_PER_SHOOT));
        data.save();
    }
}