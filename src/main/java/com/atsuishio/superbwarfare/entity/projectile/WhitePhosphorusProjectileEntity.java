package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class WhitePhosphorusProjectileEntity extends FastThrowableProjectile {

    public WhitePhosphorusProjectileEntity(EntityType<? extends WhitePhosphorusProjectileEntity> type, Level world) {
        super(type, world);
    }

    public WhitePhosphorusProjectileEntity(Entity entity, Level level) {
        super(ModEntities.WHITE_PHOSPHORUS_PROJECTILE.get(), entity, level);
        this.noCulling = true;
    }


    @Override
    protected @NotNull Item getDefaultItem() {
        return Items.AIR;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.getOwner() instanceof LivingEntity living) {
            if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
            }
        }
        if (entity instanceof LivingEntity living) {
            DamageHandler.doDamage(entity, ModDamageTypes.causeBurnDamage(entity.level().registryAccess(), getOwner()), 1);
            entity.invulnerableTime = 0;
            if (living instanceof Player player && player.isCreative()) {
                return;
            }
            if (!living.level().isClientSide()) {
                living.addEffect(new MobEffectInstance(ModMobEffects.PHOSPHORUS_FIRE, 200, 4), this.getOwner());
            }
        }

        this.discard();
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        findNearEntity(blockHitResult.getLocation(), getOwner());
        this.discard();
    }

    public void findNearEntity(Vec3 pos, Entity shooter) {
        if (this.level() instanceof ServerLevel) {

            var entities = new SeekTool.Builder(shooter)
                    .withinRange(pos, 5)
                    .notItsVehicle()
                    .baseFilter()
                    .noVehicle()
                    .build();

            for (Entity e : entities) {
                var dis = pos.distanceTo(e.position());

                if (e instanceof LivingEntity living) {
                    DamageHandler.doDamage(living, ModDamageTypes.causeBurnDamage(this.level().registryAccess(), getOwner()), 1);
                    e.invulnerableTime = 0;
                    if (living instanceof Player player && player.isCreative()) {
                        return;
                    }
                    if (!living.level().isClientSide()) {
                        living.addEffect(new MobEffectInstance(ModMobEffects.PHOSPHORUS_FIRE, (int) (200 - 30 * dis), (int) Math.max(4 - dis, 0)), this.getOwner());
                    }
                }

                if (this.getOwner() instanceof LivingEntity living) {
                    if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                        living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);
                        PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.02, 0));
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (level().isClientSide()) {
            level().addAlwaysVisibleParticle(ParticleTypes.END_ROD, true, this.xo, this.yo, this.zo, 0, 0, 0);
            level().addAlwaysVisibleParticle(ParticleTypes.CLOUD, true, this.xo, this.yo, this.zo, 0, 0, 0);
        }
        if (this.tickCount > 200 || this.isInWater()) {
            this.discard();
        }
    }
}
