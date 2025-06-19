package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CustomExplosion extends Explosion {

    private final Level level;
    private final double x;
    private final double y;
    private final double z;
    @Nullable
    private final Entity source;
    private final float radius;
    private final DamageSource damageSource;
    private final ExplosionDamageCalculator damageCalculator;
    private final float damage;
    private int fireTime;
    private float damageMultiplier;

    public CustomExplosion(Level pLevel, @Nullable Entity pSource, @Nullable DamageSource source,
                           @Nullable ExplosionDamageCalculator pDamageCalculator,
                           float damage, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius,
                           BlockInteraction pBlockInteraction, ParticleOptions smallParticle, ParticleOptions bigParticle, Holder<SoundEvent> sound) {
        super(pLevel, pSource, source, null, pToBlowX, pToBlowY, pToBlowZ, pRadius, false, pBlockInteraction, smallParticle, bigParticle, sound);

        this.level = pLevel;
        this.source = pSource;
        this.radius = pRadius;
        this.damageSource = source == null ? pLevel.damageSources().explosion(this) : source;
        this.damageCalculator = pDamageCalculator == null ? new ExplosionDamageCalculator() : pDamageCalculator;
        this.x = pToBlowX;
        this.y = pToBlowY;
        this.z = pToBlowZ;
        this.damage = damage;
    }

    public CustomExplosion(Level pLevel, @Nullable Entity pSource, @Nullable DamageSource source,
                           @Nullable ExplosionDamageCalculator pDamageCalculator,
                           float damage, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius,
                           BlockInteraction pBlockInteraction) {
        this(pLevel, pSource, source, pDamageCalculator, damage, pToBlowX, pToBlowY, pToBlowZ, pRadius, pBlockInteraction, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
    }

    public CustomExplosion(Level pLevel, @Nullable Entity pSource, float damage, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius, BlockInteraction pBlockInteraction) {
        this(pLevel, pSource, null, null, damage, pToBlowX, pToBlowY, pToBlowZ, pRadius, pBlockInteraction);
    }

    public CustomExplosion(Level pLevel, @Nullable Entity pSource, @Nullable DamageSource source, float damage, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius, Explosion.BlockInteraction pBlockInteraction, boolean vanillaExplode) {
        this(pLevel, pSource, source, null, damage, pToBlowX, pToBlowY, pToBlowZ, pRadius, pBlockInteraction);

        if (pLevel instanceof ServerLevel && vanillaExplode) {
            pLevel.explode(source == null ? null : source.getEntity(), pToBlowX, pToBlowY, pToBlowZ, 0.4f * pRadius, ExplosionConfig.EXPLOSION_DESTROY.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        }

        ShakeClientMessage.sendToNearbyPlayers(level, pToBlowX, pToBlowY, pToBlowZ, 4 * radius, 20 + 0.02 * damage, 3 * pRadius, 50 + 0.05 * damage);
    }

    public CustomExplosion(Level pLevel, @Nullable Entity pSource, @Nullable DamageSource source, float damage, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius, Explosion.BlockInteraction pBlockInteraction) {
        this(pLevel, pSource, source, null, damage, pToBlowX, pToBlowY, pToBlowZ, pRadius, pBlockInteraction);

        ShakeClientMessage.sendToNearbyPlayers(level, pToBlowX, pToBlowY, pToBlowZ, 4 * radius, 5 + 0.02 * damage, 0.75 * pRadius, 4 + 0.02 * damage);
    }

    public CustomExplosion(Level pLevel, @Nullable Entity pSource, @Nullable DamageSource source, float damage, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius) {
        this(pLevel, pSource, source, null, damage, pToBlowX, pToBlowY, pToBlowZ, pRadius, BlockInteraction.KEEP);

        ShakeClientMessage.sendToNearbyPlayers(level, pToBlowX, pToBlowY, pToBlowZ, radius, 20 + 0.02 * damage, pRadius, 10 + 0.03 * damage);
    }

    public CustomExplosion setFireTime(int fireTime) {
        this.fireTime = fireTime;
        return this;
    }

    public CustomExplosion setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
        return this;
    }

    public CustomExplosion bulletExplode() {
        return this;
    }

    @Override
    public void explode() {
        this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
        Set<BlockPos> set = Sets.newHashSet();

        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                for (int l = 0; l < 16; ++l) {
                    if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
                        double d0 = (float) j / 15.0F * 2.0F - 1.0F;
                        double d1 = (float) k / 15.0F * 2.0F - 1.0F;
                        double d2 = (float) l / 15.0F * 2.0F - 1.0F;
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 /= d3;
                        d1 /= d3;
                        d2 /= d3;
                        float f = this.radius * (0.2F + this.level.random.nextFloat() * 0.15F);
                        double d4 = this.x;
                        double d6 = this.y;
                        double d8 = this.z;

                        for (; f > 0.0F; f -= 0.22500001F) {
                            BlockPos blockpos = BlockPos.containing(d4, d6, d8);
                            BlockState blockstate = this.level.getBlockState(blockpos);
                            FluidState fluidstate = this.level.getFluidState(blockpos);
                            if (!this.level.isInWorldBounds(blockpos)) {
                                break;
                            }

                            Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(this, this.level, blockpos, blockstate, fluidstate);
                            if (optional.isPresent()) {
                                f -= (optional.get() + 1F) * 0.3F;
                            }

                            if (f > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, blockpos, blockstate, f)) {
                                set.add(blockpos);
                            }

                            d4 += d0 * (double) 0.3F;
                            d6 += d1 * (double) 0.3F;
                            d8 += d2 * (double) 0.3F;
                        }
                    }
                }
            }
        }

        this.getToBlow().addAll(set);

        float diameter = this.radius * 2.0F;
        int x0 = Mth.floor(this.x - (double) diameter - 1.0D);
        int x1 = Mth.floor(this.x + (double) diameter + 1.0D);
        int y0 = Mth.floor(this.y - (double) diameter - 1.0D);
        int y1 = Mth.floor(this.y + (double) diameter + 1.0D);
        int z0 = Mth.floor(this.z - (double) diameter - 1.0D);
        int z1 = Mth.floor(this.z + (double) diameter + 1.0D);
        List<Entity> list = this.level.getEntities(this.source, new AABB(x0, y0, z0, x1, y1, z1));
        EventHooks.onExplosionDetonate(this.level, this, list, diameter);
        Vec3 position = new Vec3(this.x, this.y, this.z);

        for (Entity entity : list) {
            if (!entity.ignoreExplosion(this)) {
                double distanceRate = Math.sqrt(entity.distanceToSqr(position)) / (double) diameter;
                if (distanceRate <= 1.0D) {
                    double xDistance = entity.getX() - this.x;
                    double yDistance = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - this.y;
                    double zDistance = entity.getZ() - this.z;
                    double distance = Math.sqrt(xDistance * xDistance + yDistance * yDistance + zDistance * zDistance);

                    if (distance != 0.0D) {
                        double seenPercent = Mth.clamp(getSeenPercent(position, entity), 0.01 * ExplosionConfig.EXPLOSION_PENETRATION_RATIO.get(), Double.POSITIVE_INFINITY);
                        double damagePercent = (1.0D - distanceRate) * seenPercent;
                        double damageFinal = (damagePercent * damagePercent + damagePercent) / 2.0D * damage;

                        if (entity instanceof Monster monster) {
                            monster.hurt(this.damageSource, (float) damageFinal * (1 + 0.2f * this.damageMultiplier));
                        } else {
                            entity.hurt(this.damageSource, (float) damageFinal);
                        }
                        entity.invulnerableTime = 1;

                        if (fireTime > 0) {
                            entity.setRemainingFireTicks(fireTime);
                        }
                    }
                }
            }
        }
    }
}
