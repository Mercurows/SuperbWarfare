package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import static com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity.rayTraceBlocks;

public class SuperStarProjectileEntity extends FastThrowableProjectile {
    private Entity currentTarget = null;

    public SuperStarProjectileEntity(EntityType<? extends SuperStarProjectileEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return Items.AIR;
    }

    @Override
    public boolean shouldSyncMotion() {
        return true;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);

        var entity = result.getEntity();
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
            return;

        if (entity instanceof PartEntity<?> partEntity) {
            this.currentTarget = partEntity.getParent();
        } else {
            this.currentTarget = entity;
        }

        this.hitAndSlash(entity);
    }

    private void hitAndSlash(Entity entity) {
        Vec3 hitVec = entity.position();

        if (entity instanceof LivingEntity living) {
            hitVec = living.getEyePosition();
        }

        if (level() instanceof ServerLevel) {
            PrismaticBoltEntity PrismaticBoltEntity = new PrismaticBoltEntity(level());
            PrismaticBoltEntity.setPos(hitVec.x, hitVec.y, hitVec.z);
            level().addFreshEntity(PrismaticBoltEntity);
        }

        // 命中伤害
        DamageHandler.doDamage(entity, ModDamageTypes.causeSuperStarHitDamage(this.level().registryAccess(), this, this.getOwner()), damage);
        entity.invulnerableTime = 0;

        // 斩切伤害
        Mod.queueServerWork(2, () -> {
            DamageHandler.doDamage(entity, ModDamageTypes.causeSuperStarSlashDamage(this.level().registryAccess(), this, this.getOwner()), explosionDamage);
            entity.level().playSound(null, entity.getOnPos(), ModSounds.KNIFE_FLESH.get(), SoundSource.PLAYERS, 2, 1);

            entity.invulnerableTime = 0;

            if (this.getOwner() instanceof ServerPlayer player) {
                player.level().playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);
                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
            }
        });
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        BlockPos resultPos = result.getBlockPos();
        BlockState state = this.level().getBlockState(resultPos);

        SoundEvent event = state.getBlock().getSoundType(state, this.level(), resultPos, this).getBreakSound();
        var volume = Math.min(4, (float) getDeltaMovement().length() / 4F + 0.5F);
        this.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z, event, SoundSource.AMBIENT, volume, 1);
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
        for (int i = 0; i < 3; i++) {
            Vec3 vec3 = randomVec(dir, 80);
            ParticleTool.sendParticle(serverLevel, ModParticleTypes.WHITE_STAR.get(), pos.x, pos.y, pos.z, 0, vec3.x, vec3.y, vec3.z, 0.2 + 0.1 * Math.random(), true);
        }
        var soundType = state.getSoundType(serverLevel, BlockPos.containing(pos.x, pos.y, pos.z), null);
        if (soundType == SoundType.METAL || soundType == SoundType.ANVIL || soundType == SoundType.CHAIN || soundType == SoundType.COPPER || soundType == SoundType.NETHERITE_BLOCK) {
            serverLevel.playSound(null, pos.x, pos.y, pos.z, ModSounds.HIT.get(), SoundSource.BLOCKS, 2, 1);
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
                }
            }
        }
    }

    public Vec3 randomVec(Vec3 vec3, double spread) {
        return vec3.normalize().add(this.random.triangle(0, 0.0172275 * spread), this.random.triangle(0, 0.0172275 * spread), this.random.triangle(0, 0.0172275 * spread));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            Vec3 startVec = this.position();
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            BlockHitResult fluidResult = rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this), state -> false);
            this.onHitWater(fluidResult.getLocation(), fluidResult);

            if (this.currentTarget != null && this.getBoundingBox().intersects(this.currentTarget.getBoundingBox())) {
                this.hitAndSlash(this.currentTarget);
            }
        }

        if (tickCount > 1 && tickCount % 3 == 0 && level().isClientSide) {
            Vec3 vec3 = randomVec(getDeltaMovement(), 30).normalize().scale(0.4 + 0.05 * Math.random());
            level().addAlwaysVisibleParticle(ModParticleTypes.WHITE_STAR.get(), true, xo, yo, zo, vec3.x, vec3.y, vec3.z);
        }

        if (this.tickCount > 1200) {
            this.discard();
        }
    }

    @Override
    public boolean isFastMoving() {
        return false;
    }
}
