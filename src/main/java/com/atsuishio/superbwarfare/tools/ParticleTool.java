package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
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
            level.playSound(null, BlockPos.containing(x, y + 1, z), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 2, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_CLOSE.get(), SoundSource.BLOCKS, 2, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_FAR.get(), SoundSource.BLOCKS, 8, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_VERY_FAR.get(), SoundSource.BLOCKS, 32, 1);
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
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_CLOSE.get(), SoundSource.BLOCKS, 4, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_FAR.get(), SoundSource.BLOCKS, 16, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_VERY_FAR.get(), SoundSource.BLOCKS, 32, 1);
        }

        if (level instanceof ServerLevel serverLevel) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 20, 1, 3, 1, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 30, 2, 1, 2, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.FALLING_WATER, x, y + 3, z, 50, 1.5, 4, 1.5, 1, true);
                sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 60, 3, 0.5, 3, 0.1, true);
            }
            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y + 1, z, 30, 1, 1, 1, 1, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y + 1, z, 30, 1, 1, 1, 1, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y + 1, z, 100, 0, 0, 0, 2, true);

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
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.HUGE_EXPLOSION_FAR.get(), SoundSource.BLOCKS, 24, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), SoundSource.BLOCKS, 128, 1);
        }

        if (level instanceof ServerLevel serverLevel) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 100, 2, 6, 2, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 200, 4, 2, 4, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.FALLING_WATER, x, y + 3, z, 500, 3, 8, 3, 1, true);
                sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 350, 6, 1, 6, 0.1, true);
            }

            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y + 3, z, 80, 3, 3, 3, 1, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y + 4, z, 100, 4, 4, 4, 1, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y + 1, z, 400, 0, 0, 0, 2, true);

            for (int h = 0; h < 4; h++) {
                for (int i = 0; i < 200; i++) {
                    Vec3 v = new Vec3(1, 0, 0).yRot((float) (i * Math.random()));
                    sendParticle(serverLevel, new CustomCloudOption(0xFFFFFF, 25, 4, 0, false, false), x, y + 0.5, z,
                            0, v.x, v.y, v.z, 200 - 2 * h, true);
                }
            }

            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y + 3, z, 75, 2.5, 2.5, 2.5, 1, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y + 3, z, 200, 5, 5, 5, 20, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y + 1, z, 400, 0, 0, 0, 1.5, true);
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 3, z, 75, 2, 3, 2, 0.005, true);
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 150, 7, 0.1, 7, 0.005, true);
            sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 1, z, 200, 3, 4, 3, 0.4, true);

            ShakeClientMessage.sendToNearbyPlayers(level, x, y, z, 192, 30, 192, 12);
        }
    }

    public static void spawnGiantExplosionParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (!level.isClientSide()) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.EXPLOSION_WATER.get(), SoundSource.BLOCKS, 3, 1);
            }
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.HUGE_EXPLOSION_CLOSE.get(), SoundSource.BLOCKS, 12, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.HUGE_EXPLOSION_FAR.get(), SoundSource.BLOCKS, 32, 1);
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), SoundSource.BLOCKS, 192, 1);
        }

        if (level instanceof ServerLevel serverLevel) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 100, 2, 6, 2, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 200, 4, 2, 4, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.FALLING_WATER, x, y + 3, z, 500, 3, 8, 3, 1, true);
                sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 350, 6, 1, 6, 0.1, true);
            }

            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y + 6, z, 100, 6, 6, 6, 1, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y + 7, z, 200, 7, 7, 7, 1, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y + 3, z, 800, 0, 0, 0, 2, true);
            for (int h = 0; h < 5; h++) {
                for (int i = 0; i < 200; i++) {
                    Vec3 v = new Vec3(1, 0, 0).yRot((float) (i * Math.random()));
                    sendParticle(serverLevel, new CustomCloudOption(1, 1, 1, 25, 4, 0, false, false), x, y + 1, z,
                            0, v.x, v.y, v.z, 500 - 3 * h, true);
                }
            }
            for (int i = 0; i < 24; i++) {
                int j = i;
                Mod.queueServerWork(i, () -> {
                    if (j < 12) {
                        sendParticle(serverLevel, new CustomCloudOption(1 - ((float) j / 24), 0.5f - ((float) j / 48), 0, 100, 4, 0, true, true), x, y + 2 * j, z, 35, 2 - 0.1 * j, 0.5, 2 - 0.1 * j, 0.005, true);
                        sendParticle(serverLevel, new CustomCloudOption(0.8f - ((float) j / 24), 0.4f - ((float) j / 48), 0, 100, 4, 0, true, true), x, y + 0.5, z, 55, 3 + 0.5 * j, 0.3, 3 + 0.5 * j, 0.005, true);
                    }

                    if (j >= 8 && j < 16) {
                        int k = j - 8;
                        sendParticle(serverLevel, new CustomCloudOption(1, 0.5f, 0, 100, 6, 0, true, true), x, y + 20, z, 20 * k, 1 + 0.5 * k, 1 + 0.2 * k, 1 + 0.5 * k, 0.005, true);
                        sendParticle(serverLevel, new CustomCloudOption(0.5f, 0.25f, 0, 100, 6, 0, true, true), x, y + 20, z, 10 * k, 1 + 0.5 * k, 1 + 0.2 * k, 1 + 0.5 * k, 0.005, true);
                        sendParticle(serverLevel, new CustomCloudOption(0.25f, 0.125f, 0, 100, 8, 0, true, true), x, y + 20, z, 10 * k, 1 + 0.5 * k, 1 + 0.2 * k, 1 + 0.5 * k, 0.005, true);
                    }
                    sendParticle(serverLevel, new CustomCloudOption(0.667f, 0.631f, 0.592f, 100, 4, 0, false, false), x, y + 0.2, z, 4 * j, j, 0.1, j, 0.0003 * j, true);
                });
            }
            ShakeClientMessage.sendToNearbyPlayers(level, x, y, z, 384, 30, 384, 16);
        }
    }

    public static void spawnBulletHitWaterParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 80; i++) {
                Vec3 v = new Vec3(1, 0, 0).yRot((float) (i * Math.random()));
                sendParticle(serverLevel, new CustomCloudOption(1, 1, 1, 20, 0.3f, 0, false, false), x, y, z,
                        0, v.x, v.y, v.z, 8, true);
            }
            Mod.queueServerWork(8, () -> {
                for (int i = 0; i < 60; i++) {
                    Vec3 v = new Vec3(1, 0, 0).yRot((float) (i * Math.random()));
                    sendParticle(serverLevel, new CustomCloudOption(1, 1, 1, 17, 0.3f, 0, false, false), x, y, z,
                            0, v.x, v.y, v.z, 8, true);
                }
            });
            Mod.queueServerWork(14, () -> {
                for (int i = 0; i < 40; i++) {
                    Vec3 v = new Vec3(1, 0, 0).yRot((float) (i * Math.random()));
                    sendParticle(serverLevel, new CustomCloudOption(1, 1, 1, 15, 0.3f, 0, false, false), x, y, z,
                            0, v.x, v.y, v.z, 4, true);
                }
            });
            sendParticle(serverLevel, ParticleTypes.CLOUD, x, y - 0.2, z, 3, 0.2, 0, 0.2, 0.002, false);
            sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, x, y - 0.5, z, 5, 0.4, 0.2, 0.4, 0.005, false);
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
