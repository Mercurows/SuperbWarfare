package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

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
    private float damageMultiplier = 1;

    public CustomExplosion(Level pLevel, @Nullable Entity pSource, @Nullable DamageSource source, @Nullable ExplosionDamageCalculator pDamageCalculator,
                           float damage, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius,
                           Explosion.BlockInteraction pBlockInteraction) {
        super(pLevel, pSource, source, null, pToBlowX, pToBlowY, pToBlowZ, pRadius, false, pBlockInteraction);
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

    public CustomExplosion(Level pLevel, @Nullable Entity pSource, float damage, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius, Explosion.BlockInteraction pBlockInteraction) {
        this(pLevel, pSource, null, null, damage, pToBlowX, pToBlowY, pToBlowZ, pRadius, pBlockInteraction);
    }

    public CustomExplosion(Level pLevel, @Nullable Entity pSource, @Nullable DamageSource source, float damage, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius, Explosion.BlockInteraction pBlockInteraction) {
        this(pLevel, pSource, source, null, damage, pToBlowX, pToBlowY, pToBlowZ, pRadius, pBlockInteraction);
        ShakeClientMessage.sendToNearbyPlayers(level, pToBlowX, pToBlowY, pToBlowZ, 4 * radius, 20 + 0.2 * radius, 50 + 0.5 * radius);
    }

    public CustomExplosion(Level pLevel, @Nullable Entity pSource, @Nullable DamageSource source, float damage, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius) {
        this(pLevel, pSource, source, null, damage, pToBlowX, pToBlowY, pToBlowZ, pRadius, BlockInteraction.KEEP);
        ShakeClientMessage.sendToNearbyPlayers(level, pToBlowX, pToBlowY, pToBlowZ, radius, 5 + 0.2 * radius, 2 + 0.02 * radius);
    }

    public CustomExplosion setFireTime(int fireTime) {
        this.fireTime = fireTime;
        return this;
    }

    public CustomExplosion setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
        return this;
    }

    @Override
    public void explode() {
        this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
        Set<BlockPos> set = Sets.newHashSet();

        // 这个效果更好但是性能损耗巨大
//        int sampleCount = (int) Mth.clamp(Math.PI * this.radius * this.radius, 64, 4096);
//
//        for (int i = 0; i < sampleCount; ++i) {
//            double theta = 2 * Math.PI * this.level.random.nextDouble();
//            double phi = Math.acos(2 * this.level.random.nextDouble() - 1);
//
//            double d0 = Math.sin(phi) * Math.cos(theta);
//            double d1 = Math.sin(phi) * Math.sin(theta);
//            double d2 = Math.cos(phi);
//
//            d0 += (this.level.random.nextDouble() - 0.5) * 0.2;
//            d1 += (this.level.random.nextDouble() - 0.5) * 0.2;
//            d2 += (this.level.random.nextDouble() - 0.5) * 0.2;
//
//            double length = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
//            d0 /= length;
//            d1 /= length;
//            d2 /= length;
//
//            float rayStrength = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
//            double currentX = this.x;
//            double currentY = this.y;
//            double currentZ = this.z;
//
//            for (; rayStrength > 0.0F; rayStrength -= 0.22500001F) {
//                BlockPos blockpos = BlockPos.containing(currentX, currentY, currentZ);
//                BlockState blockstate = this.level.getBlockState(blockpos);
//                FluidState fluidstate = this.level.getFluidState(blockpos);
//
//                if (!this.level.isInWorldBounds(blockpos)) {
//                    break;
//                }
//
//                Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(
//                        this, this.level, blockpos, blockstate, fluidstate
//                );
//
//                if (optional.isPresent()) {
//                    rayStrength -= (optional.get() + 0.3F) * 0.3F;
//                }
//
//                if (rayStrength > 0.0F && this.damageCalculator.shouldBlockExplode(
//                        this, this.level, blockpos, blockstate, rayStrength
//                )) {
//                    set.add(blockpos);
//                }
//
//                currentX += d0 * 0.3;
//                currentY += d1 * 0.3;
//                currentZ += d2 * 0.3;
//            }
//        }

        Vec3 center = new Vec3(this.x, this.y, this.z);
        RandomSource random = level.random;

        AABB aabb = new AABB(x - 0.5 * radius, y - 0.5 * radius, z - 0.5 * radius, x + 0.5 * radius, y + 0.5 * radius, z + 0.5 * radius);

        BlockPos minPos = new BlockPos(
                (int) Math.floor(aabb.minX),
                (int) Math.floor(aabb.minY),
                (int) Math.floor(aabb.minZ)
        );

        BlockPos maxPos = new BlockPos(
                (int) Math.floor(aabb.maxX),
                (int) Math.floor(aabb.maxY),
                (int) Math.floor(aabb.maxZ)
        );

        BlockPos.betweenClosedStream(minPos, maxPos).forEach(blockpos -> {
            double effectiveRadius = 0.4 * radius;
            float distanceSqr = (float) blockpos.getCenter().distanceToSqr(center);
            float force = this.radius * (0.25F + random.nextFloat() * 0.15F) * 0.02f * damage;

            if(distanceSqr > radius * radius * 0.15) {
                effectiveRadius += (random.nextDouble() - 0.5) * radius * 0.2;
            }

            if (level.isInWorldBounds(blockpos) && blockpos.getCenter().distanceToSqr(center) <= effectiveRadius * effectiveRadius) {
                BlockState blockstate = this.level.getBlockState(blockpos);
                float resistance = blockstate.getBlock().defaultDestroyTime();
                force *= (float) (1 - (distanceSqr / (effectiveRadius * effectiveRadius)));

                if (resistance != -1 && force > resistance && this.damageCalculator.shouldBlockExplode(this, this.level, blockpos, blockstate, force)) {
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.destroyBlock(blockpos, true);
                    }
                }
            }
        });

        this.getToBlow().addAll(set);

        float diameter = this.radius * 2;
        int x0 = Mth.floor(this.x - (double) diameter - 1);
        int x1 = Mth.floor(this.x + (double) diameter + 1);
        int y0 = Mth.floor(this.y - (double) diameter - 1);
        int y1 = Mth.floor(this.y + (double) diameter + 1);
        int z0 = Mth.floor(this.z - (double) diameter - 1);
        int z1 = Mth.floor(this.z + (double) diameter + 1);
        List<Entity> list = this.level.getEntities(this.source, new AABB(x0, y0, z0, x1, y1, z1));
        net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.level, this, list, diameter);
        Vec3 position = new Vec3(this.x, this.y, this.z);

        boolean hit = false;

        for (Entity entity : list) {
            if (!entity.ignoreExplosion()) {
                double distanceRate = Math.sqrt(entity.distanceToSqr(position)) / (double) diameter;
                if (distanceRate <= 1) {
                    double xDistance = entity.getX() - this.x;
                    double yDistance = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - this.y;
                    double zDistance = entity.getZ() - this.z;
                    double distance = Math.sqrt(xDistance * xDistance + yDistance * yDistance + zDistance * zDistance);

                    if (distance != 0) {
                        double seenPercent = Mth.clamp(getSeenPercent(position, entity), 0.01 * ExplosionConfig.EXPLOSION_PENETRATION_RATIO.get(), Double.POSITIVE_INFINITY);
                        double damagePercent = (1 - distanceRate) * seenPercent;
                        double damageFinal = (damagePercent * damagePercent + damagePercent) / 2 * damage;

                        if (entity instanceof Monster monster) {
                            DamageHandler.doDamage(monster, this.damageSource, (float) damageFinal * (1 + 0.2f * this.damageMultiplier));
                        } else {
                            DamageHandler.doDamage(entity, this.damageSource, (float) damageFinal);
                        }

                        if (entity instanceof LivingEntity living) {
                            double force = damageFinal * 0.015;
                            force = ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, force);
                            Vec3 vec31 = position.vectorTo(living.getBoundingBox().getCenter()).normalize();
                            if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
                                entity.setDeltaMovement(entity.getDeltaMovement().add(vec31.scale(force)));
                            } else {
                                entity.setDeltaMovement(entity.getDeltaMovement().add(vec31.scale(force)));
                            }
                        }

                        if (entity instanceof LivingEntity || entity instanceof VehicleEntity) {
                            hit = true;
                        }

                        entity.invulnerableTime = 1;

                        if (fireTime > 0) {
                            entity.setSecondsOnFire(fireTime);
                        }
                    }
                }
            }
        }

        if (hit) {
            if (this.damageSource.getEntity() instanceof ServerPlayer player) {
                SoundTool.playLocalSound(player, ModSounds.INDICATION.get());
                NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
            }
        }
    }

    public static class Builder {
        private final Level level;
        private Entity directSource;
        private @Nullable Entity sourceEntity;
        private @Nullable Entity attackerEntity;
        private float damage;
        private float radius;
        private @Nullable ParticleTool.ParticleType particleType = ParticleTool.ParticleType.MINI;
        private Supplier<BlockInteraction> destroyBlock = () -> ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP;
        private int fireTime = 0;
        private float damageMultiplier = 1;
        private DamageSource damageSource = null;
        private Vec3 particlePosition = null;
        public Vec3 position;

        public Builder(@NotNull Entity target) {
            this.level = target.level();
            this.directSource = target;
            this.sourceEntity = target;
            this.attackerEntity = target;
            this.position = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        }

        public Builder directSource(@NotNull Entity directSource) {
            this.directSource = directSource;
            return this;
        }

        public Builder source(Entity source) {
            this.sourceEntity = source;
            return this;
        }

        public Builder attacker(Entity attacker) {
            this.attackerEntity = attacker;
            return this;
        }

        public Builder damage(float damage) {
            this.damage = damage;
            return this;
        }

        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }

        public Builder withParticleType(@Nullable ParticleTool.ParticleType particleType) {
            this.particleType = particleType;
            return this;
        }

        public Builder destroyBlock(Supplier<BlockInteraction> destroyBlock) {
            this.destroyBlock = destroyBlock;
            return this;
        }

        public Builder keepBlock() {
            this.destroyBlock = () -> Explosion.BlockInteraction.KEEP;
            return this;
        }

        public Builder fireTime(int fireTime) {
            this.fireTime = fireTime;
            return this;
        }

        public Builder damageMultiplier(float damageMultiplier) {
            this.damageMultiplier = damageMultiplier;
            return this;
        }

        public Builder damageSource(DamageSource damageSource) {
            this.damageSource = damageSource;
            return this;
        }

        public Builder position(Vec3 position) {
            this.position = position;
            return this;
        }

        public Builder particlePosition(Vec3 particlePosition) {
            this.particlePosition = particlePosition;
            return this;
        }

        public void explode() {
            if (level.isClientSide) return;

            var source = this.damageSource != null ? this.damageSource : ModDamageTypes.causeCustomExplosionDamage(level.registryAccess(), sourceEntity, attackerEntity);

            var customExplosion = new CustomExplosion(level, directSource,
                    source, damage,
                    position.x, position.y, position.z, radius, destroyBlock.get())
                    .setFireTime(fireTime)
                    .setDamageMultiplier(damageMultiplier);
            customExplosion.explode();
            ForgeEventFactory.onExplosionStart(directSource.level(), customExplosion);
            customExplosion.finalizeExplosion(false);

            ParticleTool.spawnExplosionParticles(particleType, directSource.level(), particlePosition != null ? particlePosition : position);
        }
    }
}
