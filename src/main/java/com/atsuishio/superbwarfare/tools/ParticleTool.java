package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ParticleTool {
    public static <T extends ParticleOptions> void sendParticle(ServerLevel level, T particle, double x, double y, double z, int count,
                                                                double xOffset, double yOffset, double zOffset, double speed, boolean force) {
        for (ServerPlayer serverPlayer : level.players()) {
            sendParticle(level, particle, x, y, z, count, xOffset, yOffset, zOffset, speed, force, serverPlayer);
        }
    }

    public static <T extends ParticleOptions> void sendParticle(ServerLevel level, T particle, double x, double y, double z, int count,
                                                                double xOffset, double yOffset, double zOffset, double speed, boolean force, ServerPlayer viewer) {
        level.sendParticles(viewer, particle, force, x, y, z, count, xOffset, yOffset, zOffset, speed);
    }

    public static void spawnMiniExplosionParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (!level.isClientSide()) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_WATER.get(), SoundSource.BLOCKS, 2, 1);
            }
            level.playSound(null, BlockPos.containing(x, y + 1, z), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 4, 1);
        } else {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                level.playLocalSound(x, (y + 1), z, ModSounds.EXPLOSION_WATER.get(), SoundSource.BLOCKS, 1, 1, false);
            }
            level.playLocalSound(x, (y + 1), z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 2, 1, false);
        }

        if (level instanceof ServerLevel serverLevel) {
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 3, 0.1, 0.1, 0.1, 0.02, true);
            sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, x, y, z, 4, 0.2, 0.2, 0.2, 0.02, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y, z, 6, 0, 0, 0, 0.2, true);
        }
    }

    public static void spawnSmallExplosionParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (!level.isClientSide()) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_WATER.get(), SoundSource.BLOCKS, 2, 1);
            }
            level.playSound(null, BlockPos.containing(x, y + 1, z), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 4, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_CLOSE.get(), SoundSource.BLOCKS, 3, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_FAR.get(), SoundSource.BLOCKS, 6, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_VERY_FAR.get(), SoundSource.BLOCKS, 12, 1);
        } else {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                level.playLocalSound(x, (y + 1), z, ModSounds.EXPLOSION_WATER.get(), SoundSource.BLOCKS, 1, 1, false);
            }
            level.playLocalSound(x, (y + 1), z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 2, 1, false);
            level.playLocalSound(x, (y + 1), z, ModSounds.EXPLOSION_CLOSE.get(), SoundSource.BLOCKS, 1, 1, false);
            level.playLocalSound(x, (y + 1), z, ModSounds.EXPLOSION_FAR.get(), SoundSource.BLOCKS, 1, 1, false);
            level.playLocalSound(x, (y + 1), z, ModSounds.EXPLOSION_VERY_FAR.get(), SoundSource.BLOCKS, 1, 1, false);
        }

        if (level instanceof ServerLevel serverLevel) {
            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y, z, 2, 0.05, 0.05, 0.05, 1, true);
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 3, 0.1, 0.1, 0.1, 0.02, true);
            sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, x, y, z, 4, 0.2, 0.2, 0.2, 0.02, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y, z, 20, 0, 0, 0, 0.6, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y, z, 5, 0.1, 0.1, 0.1, 20, true);
        }
    }

    public static void spawnMediumExplosionParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (!level.isClientSide()) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_WATER.get(), SoundSource.BLOCKS, 3, 1);
            }
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_CLOSE.get(), SoundSource.BLOCKS, 6, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_FAR.get(), SoundSource.BLOCKS, 12, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_VERY_FAR.get(), SoundSource.BLOCKS, 32, 1);
        } else {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                level.playLocalSound(x, (y + 1), z, ModSounds.EXPLOSION_WATER.get(), SoundSource.BLOCKS, 1, 1, false);
            }
            level.playLocalSound(x, (y + 1), z, ModSounds.EXPLOSION_CLOSE.get(), SoundSource.BLOCKS, 1, 1, false);
            level.playLocalSound(x, (y + 1), z, ModSounds.EXPLOSION_FAR.get(), SoundSource.BLOCKS, 1, 1, false);
            level.playLocalSound(x, (y + 1), z, ModSounds.EXPLOSION_VERY_FAR.get(), SoundSource.BLOCKS, 1, 1, false);
        }

        if (level instanceof ServerLevel serverLevel) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 20, 1, 3, 1, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 30, 2, 1, 2, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.FALLING_WATER, x, y + 3, z, 50, 1.5, 4, 1.5, 1, true);
                sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 60, 3, 0.5, 3, 0.1, true);
            }
            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y + 1, z, 5, 0.7, 0.7, 0.7, 1, true);
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 20, 0.2, 1, 0.2, 0.02, true);
            sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, x, y + 1, z, 10, 0.4, 1, 0.4, 0.02, true);
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.25, z, 40, 2, 0.001, 2, 0.01, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y + 0.2, z, 50, 0, 0, 0, 0.9, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y + 0.5, z, 50, 0.2, 0.2, 0.2, 20, true);
        }
    }

    public static void spawnHugeExplosionParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (!level.isClientSide()) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_WATER.get(), SoundSource.BLOCKS, 3, 1);
            }
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.HUGE_EXPLOSION_CLOSE.get(), SoundSource.BLOCKS, 8, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.HUGE_EXPLOSION_FAR.get(), SoundSource.BLOCKS, 16, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), SoundSource.BLOCKS, 32, 1);
        } else {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                level.playLocalSound(x, (y + 1), z, ModSounds.EXPLOSION_WATER.get(), SoundSource.BLOCKS, 1, 1, false);
            }
            level.playLocalSound(x, (y + 1), z, ModSounds.HUGE_EXPLOSION_CLOSE.get(), SoundSource.BLOCKS, 1, 1, false);
            level.playLocalSound(x, (y + 1), z, ModSounds.HUGE_EXPLOSION_FAR.get(), SoundSource.BLOCKS, 1, 1, false);
            level.playLocalSound(x, (y + 1), z, ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), SoundSource.BLOCKS, 1, 1, false);
        }

        if (level instanceof ServerLevel serverLevel) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 100, 2, 6, 2, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 200, 4, 2, 4, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.FALLING_WATER, x, y + 3, z, 500, 3, 8, 3, 1, true);
                sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 350, 6, 1, 6, 0.1, true);
            }

            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y + 1, z, 75, 2.5, 2.5, 2.5, 1, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y + 1, z, 200, 5, 5, 5, 20, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y + 1, z, 400, 0, 0, 0, 1.5, true);
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 75, 2, 3, 2, 0.005, true);
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 150, 7, 0.1, 7, 0.005, true);
            sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 1, z, 200, 3, 4, 3, 0.4, true);
        }

    }

    public static void cannonHitParticles(Level level, Vec3 pos, Entity entity) {
        double x = pos.x + 0.5 * entity.getDeltaMovement().x;
        double y = pos.y + 0.5 * entity.getDeltaMovement().y;
        double z = pos.z + 0.5 * entity.getDeltaMovement().z;

        if (level instanceof ServerLevel serverLevel) {
            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y, z, 2, 0.5, 0.5, 0.5, 1, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y, z, 2, 0.2, 0.2, 0.2, 10, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y, z, 40, 0, 0, 0, 1.5, true);
        }

    }
}
