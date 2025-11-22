package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity.rayTraceBlocks;

public class GrapeshotEntity extends FastThrowableProjectile {

    public GrapeshotEntity(EntityType<? extends GrapeshotEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public GrapeshotEntity(@Nullable Entity entity, Level level, float damage) {
        super(ModEntities.GRAPESHOT.get(), entity, level);
        this.noCulling = true;
        this.damage = damage;
    }

    public GrapeshotEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.GRAPESHOT.get(), level);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.GS_5_INCHES.get();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
            return;
        if (this.level() instanceof ServerLevel) {
            if (entity == this.getOwner() || (this.getOwner() != null && entity == this.getOwner().getVehicle()))
                return;
            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                    NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                }
            }

            DamageHandler.doDamage(entity, ModDamageTypes.causeGrapeShotHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }
            this.discard();
        }
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        BlockPos resultPos = result.getBlockPos();
        BlockState state = this.level().getBlockState(resultPos);

        SoundEvent event = state.getBlock().getSoundType(state, this.level(), resultPos, this).getBreakSound();
        this.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z, event, SoundSource.AMBIENT, 1, 1);
        Vec3 hitVec = result.getLocation();

        this.hitBlock(hitVec, result);
        this.discard();
    }

    protected void hitBlock(Vec3 location, BlockHitResult result) {
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = result.getBlockPos();
            Direction face = result.getDirection();
            BlockState state = level().getBlockState(pos);

            double vx = face.getStepX();
            double vy = face.getStepY();
            double vz = face.getStepZ();
            Vec3 dir = new Vec3(vx, vy, vz);
            summonVectorParticle(serverLevel, state, location, dir);

            this.discard();
            serverLevel.playSound(null, new BlockPos((int) location.x, (int) location.y, (int) location.z), ModSounds.LAND.get(), SoundSource.BLOCKS, 1, 1);
        }
    }

    public void summonVectorParticle(ServerLevel serverLevel, BlockState state, Vec3 pos, Vec3 dir) {
        BlockParticleOption particleData = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < 7; i++) {
            Vec3 vec3 = randomVec(dir, 40);
            ParticleTool.sendParticle(serverLevel, particleData, pos.x + 0.05 * i * dir.x, pos.y + 0.05 * i * dir.y, pos.z + 0.05 * i * dir.z, 0, vec3.x, vec3.y, vec3.z, 10, true);
        }
        for (int i = 0; i < 3; i++) {
            Vec3 vec3 = randomVec(dir, 20);
            ParticleTool.sendParticle(serverLevel, ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0, vec3.x, vec3.y, vec3.z, 0.05, true);
        }
        if (state.getSoundType() == SoundType.METAL || state.getSoundType() == SoundType.ANVIL || state.getSoundType() == SoundType.CHAIN || state.getSoundType() == SoundType.COPPER || state.getSoundType() == SoundType.NETHERITE_BLOCK) {
            serverLevel.playSound(null, pos.x, pos.y, pos.z, ModSounds.HIT.get(), SoundSource.BLOCKS, 2, 1);
            for (int i = 0; i < 3; i++) {
                Vec3 vec3 = randomVec(dir, 80);
                ParticleTool.sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), pos.x, pos.y, pos.z, 0, vec3.x, vec3.y, vec3.z, 0.2 + 0.1 * Math.random(), true);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            Vec3 startVec = this.position();
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            BlockHitResult fluidResult = rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this), state -> false);
            this.onHitWater(fluidResult.getLocation(), fluidResult);
        }

        Vec3 vec31 = this.getDeltaMovement();
        this.setDeltaMovement(vec31.multiply(0.96, 0.96, 0.96));

        if (this.tickCount > 200 || this.isInWater()) {
            this.discard();
        }
    }

    protected void onHitWater(Vec3 location, BlockHitResult result) {
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = result.getBlockPos();
            Direction face = result.getDirection();
            BlockState state = level().getBlockState(pos);

            double vx = face.getStepX();
            double vy = face.getStepY();
            double vz = face.getStepZ();
            Vec3 dir = new Vec3(vx, vy, vz).add(getDeltaMovement().normalize().scale(-0.1));

            if (state.getBlock() == Blocks.WATER) {
                if (!isInWater()) {
                    CustomCloudOption particleData = new CustomCloudOption(1, 1, 1, 80, 0.5f, 1, false, false);
                    for (int i = 0; i < 10; i++) {
                        Vec3 vec3 = randomVec(dir, 40);
                        ParticleTool.sendParticle(serverLevel, particleData, location.x + 0.12 * i * dir.x, location.y + 0.12 * i * dir.y, location.z + 0.12 * i * dir.z, 0, vec3.x, vec3.y, vec3.z, 15, true);
                    }

                    ParticleTool.spawnBulletHitWaterParticles(serverLevel, location);
                    serverLevel.playSound(null, new BlockPos((int) location.x, (int) location.y, (int) location.z), ModSounds.HIT_WATER.get(), SoundSource.BLOCKS, 1, 1);
                    this.discard();
                }
            } else if (state.getBlock() == Blocks.LAVA) {
                if (!isInLava()) {
                    BlockParticleOption particleData = new BlockParticleOption(ParticleTypes.BLOCK, state);
                    for (int i = 0; i < 7; i++) {
                        Vec3 vec3 = randomVec(dir, 20);
                        ParticleTool.sendParticle(serverLevel, particleData, location.x + 0.1 * i * dir.x, location.y + 0.1 * i * dir.y, location.z + 0.1 * i * dir.z, 0, vec3.x, vec3.y, vec3.z, 10, true);
                    }
                    ParticleTool.sendParticle(serverLevel, ParticleTypes.LAVA, location.x, location.y, location.z,
                            4, 0, 0, 0, 0.6, true);
                    serverLevel.playSound(null, new BlockPos((int) location.x, (int) location.y, (int) location.z), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1, 1);
                    this.discard();
                }
            }
        }
    }

    public Vec3 randomVec(Vec3 vec3, double spread) {
        return vec3.normalize().add(this.random.triangle(0, 0.0172275 * spread), this.random.triangle(0, 0.0172275 * spread), this.random.triangle(0, 0.0172275 * spread));
    }
}
