package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class PrismTankEntity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PrismTankEntity(EntityType<PrismTankEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.4f) * damage);
    }

    public void hitBlock(Vec3 pos, GunData gunData, Entity shooter) {
        if (level() instanceof ServerLevel serverLevel) {
            if (gunData.compute().explosionRadius > 0) {
                findNearEntity(pos, gunData, shooter);
                sendParticle(serverLevel, ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 24, 0, 0, 0, 0.2, true);
                sendParticle(serverLevel, ParticleTypes.LAVA, pos.x, pos.y, pos.z, 8, 0, 0, 0, 0.4, true);
            } else {
                sendParticle(serverLevel, ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 4, 0, 0, 0, 0.05, true);
                sendParticle(serverLevel, ParticleTypes.LAVA, pos.x, pos.y, pos.z, 2, 0, 0, 0, 0.15, true);
            }
        }
    }

    public void hitEntity(Vec3 pos, GunData gunData, Entity shooter) {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (gunData.compute().explosionRadius > 0) {
                findNearEntity(pos, gunData, shooter);
                sendParticle(serverLevel, ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 24, 0, 0, 0, 0.2, true);
                sendParticle(serverLevel, ParticleTypes.LAVA, pos.x, pos.y, pos.z, 8, 0, 0, 0, 0.4, true);
            } else {
                sendParticle(serverLevel, ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 4, 0, 0, 0, 0.05, true);
                sendParticle(serverLevel, ParticleTypes.LAVA, pos.x, pos.y, pos.z, 2, 0, 0, 0, 0.15, true);
            }
        }
    }

    public void findNearEntity(Vec3 vec, GunData gunData, Entity shooter) {
        double aoeDamage = gunData.compute().explosionDamage;
        double range = gunData.compute().explosionRadius;
        if (level() instanceof ServerLevel serverLevel) {
            List<Entity> entities = new SeekTool.Builder(this)
                    .withinRange(vec, range)
                    .notItsVehicle()
                    .baseFilter()
                    .smokeFilter()
                    .noVehicle()
                    .differentTeam()
                    .build();

            for (var e : entities) {
                double dis = vec.distanceTo(e.getEyePosition());
                for (float i = 0; i < dis; i += 0.2f) {
                    Vec3 toVec = vec.vectorTo(e.getEyePosition()).normalize();
                    Vec3 pos = vec.add(toVec.scale(i));
                    sendParticle(serverLevel, ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0, true);
                }

                sendParticle(serverLevel, ParticleTypes.LAVA, e.getX(), e.getEyeY(), e.getZ(), 4, 0, 0, 0, 0.15, true);
                DamageHandler.doDamage(e, ModDamageTypes.causeLaserDamage(this.level().registryAccess(), this, shooter), (float) (aoeDamage - Mth.clamp(dis / range, 0, 0.75) * aoeDamage));

                if (shooter instanceof ServerPlayer player) {
                    var holder = Holder.direct(ModSounds.INDICATION.get());
                    player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.level().random.nextLong()));
                    PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
                }
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public float getWheelMaxHealth() {
        return 100;
    }

    @Override
    public float getEngineMaxHealth() {
        return 150;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Component firstPersonAmmoComponent(GunData data, Player player) {
        var name = data.compute().name;
        if (name == null || name.isBlank()) return Component.empty();

        return Component.translatable(name, (int) (25 + data.heat.get()) + " " + "°C");
    }
}
