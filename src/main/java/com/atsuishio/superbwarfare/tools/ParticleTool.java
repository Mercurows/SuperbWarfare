package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.particle.CannonMuzzleFlareOption;
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.google.gson.annotations.SerializedName;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    public enum ParticleType {
        @SerializedName("Mini") MINI,
        @SerializedName("Small") SMALL,
        @SerializedName("Medium") MEDIUM,
        @SerializedName("Large") LARGE,
        @SerializedName("Huge") HUGE,
        @SerializedName("Giant") GIANT,
    }

    public static void spawnExplosionParticles(ParticleType type, Level level, Vec3 pos) {
        if (type == null) {
            type = ParticleType.MINI;
        }

        switch (type) {
            case MINI -> ParticleTool.spawnMiniExplosionParticles(level, pos);
            case SMALL -> ParticleTool.spawnSmallExplosionParticles(level, pos);
            case MEDIUM -> ParticleTool.spawnMediumExplosionParticles(level, pos);
            case LARGE -> ParticleTool.spawnLargeExplosionParticles(level, pos);
            case HUGE -> ParticleTool.spawnHugeExplosionParticles(level, pos);
            case GIANT -> ParticleTool.spawnGiantExplosionParticles(level, pos);
        }
    }

    public static void spawnMiniExplosionParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (!level.isClientSide()) {
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.MINI_EXPLOSION.get(), SoundSource.BLOCKS, 4, 1);
        }

        if (level instanceof ServerLevel serverLevel) {
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 2, 0.1, 0.1, 0.1, 0.02, true);
            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y, z, 2, 0.05, 0.05, 0.05, 1, true);
            sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.2, 0.2, 0.2, 0.02, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y, z, 7, 0, 0, 0, 0.4, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 20, true);
        }
    }

    public static void spawnSmallExplosionParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (level instanceof ServerLevel serverLevel) {
            SoundTool.playDistantSound(serverLevel, ModSounds.EXPLOSION_CLOSE.get(), pos, 2, 1, null);
            SoundTool.playDistantSound(serverLevel, ModSounds.EXPLOSION_FAR.get(), pos, 8, 1, null);
            SoundTool.playDistantSound(serverLevel, ModSounds.EXPLOSION_VERY_FAR.get(), pos, 32, 1, null);

            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y, z, 2, 0.05, 0.05, 0.05, 1, true);
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 3, 0.1, 0.1, 0.1, 0.02, true);
            sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, x, y, z, 4, 0.2, 0.2, 0.2, 0.02, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y, z, 12, 0, 0, 0, 0.6, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y, z, 3, 0.1, 0.1, 0.1, 20, true);
        }
    }

    public static void spawnMediumExplosionParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (level instanceof ServerLevel serverLevel) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 20, 1, 3, 1, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 30, 2, 1, 2, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.FALLING_WATER, x, y + 3, z, 50, 1.5, 4, 1.5, 1, true);
                sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 60, 3, 0.5, 3, 0.1, true);
            }

            SoundTool.playDistantSound(serverLevel, ModSounds.EXPLOSION_CLOSE.get(), pos, 4, 1, null);
            SoundTool.playDistantSound(serverLevel, ModSounds.EXPLOSION_FAR.get(), pos, 16, 1, null);
            SoundTool.playDistantSound(serverLevel, ModSounds.EXPLOSION_VERY_FAR.get(), pos, 32, 1, null);

            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y + 1, z, 5, 0.7, 0.7, 0.7, 1, true);
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 20, 0.2, 1, 0.2, 0.02, true);
            sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, x, y + 1, z, 10, 0.4, 1, 0.4, 0.02, true);
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.25, z, 40, 2, 0.001, 2, 0.01, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y + 0.2, z, 50, 0, 0, 0, 0.8, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y + 0.5, z, 50, 0.2, 0.2, 0.2, 20, true);
        }
    }

    public static void spawnLargeExplosionParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (level instanceof ServerLevel serverLevel) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 100, 2, 6, 2, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 200, 4, 2, 4, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.FALLING_WATER, x, y + 3, z, 500, 3, 8, 3, 1, true);
                sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 350, 6, 1, 6, 0.1, true);
            }

            SoundTool.playDistantSound(serverLevel, ModSounds.HUGE_EXPLOSION_CLOSE.get(), pos, 6, 1, null);
            SoundTool.playDistantSound(serverLevel, ModSounds.HUGE_EXPLOSION_FAR.get(), pos, 20, 1, null);
            SoundTool.playDistantSound(serverLevel, ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), pos, 64, 1, null);

            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 60, 0.5, 2, 0.5, 0.02, true);
            sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.25, z, 120, 5, 0.001, 5, 0.01, true);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), x, y + 0.2, z, 100, 0, 0, 0, 1.2, true);
            sendParticle(serverLevel, ParticleTypes.EXPLOSION, x, y + 1, z, 35, 1.5, 1.5, 1.5, 1, true);
            sendParticle(serverLevel, ParticleTypes.FLASH, x, y + 1, z, 120, 3, 3, 3, 20, true);

            for (int h = 0; h < 2; h++) {
                for (int i = 0; i < 150; i++) {
                    Vec3 v = new Vec3(1, 0, 0).yRot((float) (i * Math.random()));
                    sendParticle(serverLevel, new CustomCloudOption(0xFFFFFF, 25, 2, 0, false, false), x, y + 0.2, z,
                            0, v.x, v.y, v.z, 140 - 2 * h, true);
                }
            }
        }
    }

    public static void spawnHugeExplosionParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (level instanceof ServerLevel serverLevel) {
            SoundTool.playDistantSound(serverLevel, ModSounds.HUGE_EXPLOSION_CLOSE.get(), pos, 8, 1, null);
            SoundTool.playDistantSound(serverLevel, ModSounds.HUGE_EXPLOSION_FAR.get(), pos, 24, 1, null);
            SoundTool.playDistantSound(serverLevel, ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), pos, 128, 1, null);

            if ((level.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.WATER) {
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 100, 2, 6, 2, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.CLOUD, x, y + 3, z, 200, 4, 2, 4, 0.01, true);
                sendParticle(serverLevel, ParticleTypes.FALLING_WATER, x, y + 3, z, 500, 3, 8, 3, 1, true);
                sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 350, 6, 1, 6, 0.1, true);
            }

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

            ShakeClientMessage.sendToNearbyPlayers(level, x, y, z, 192, 30, 12);
        }
    }

    public static void spawnGiantExplosionParticles(Level level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;


        if (level instanceof ServerLevel serverLevel) {

            SoundTool.playDistantSound(serverLevel, ModSounds.HUGE_EXPLOSION_CLOSE.get(), pos, 12, 1, null);
            SoundTool.playDistantSound(serverLevel, ModSounds.HUGE_EXPLOSION_FAR.get(), pos, 32, 1, null);
            SoundTool.playDistantSound(serverLevel, ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), pos, 192, 1, null);

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
            ShakeClientMessage.sendToNearbyPlayers(level, x, y, z, 384, 30, 16);
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

    public static void spawnMediumCannonMuzzleParticles(Vec3 direct, Vec3 pos, ServerLevel serverLevel, Entity entity) {
        spawnDirectionalParticles(4, 0.1, serverLevel , new CannonMuzzleFlareOption(1, 1, 1, 8, 0.7f, 1, 0.2f), direct, pos, 0.3);
        spawnDirectionalParticles(3, 0.06, serverLevel , new CannonMuzzleFlareOption(1, 1, 1, 8, 0.72f, 1, 0.15f), direct, pos, 0.2);
        spawnDirectionalParticles(1, 0, serverLevel , new CannonMuzzleFlareOption(0.4f, 0.4f, 0.4f, 45, 0.88f, 2, 0.05f), direct, pos, 0.15);
        spawnDirectionalParticles(1, 0, serverLevel , new CannonMuzzleFlareOption(0.45f, 0.45f, 0.45f, 47, 0.90f, 2, 0.03f), direct, pos, 0.125);
        spawnDirectionalParticles(1, 0, serverLevel , new CannonMuzzleFlareOption(0.5f, 0.5f, 0.5f, 48, 0.92f, 2, 0.01f), direct, pos, 0.1);
    }

    public static void spawnBigCannonMuzzleParticles(Vec3 direct, Vec3 pos, ServerLevel serverLevel, Entity entity) {
        spawnDirectionalParticles(10, 0.1, serverLevel , new CannonMuzzleFlareOption(1, 1, 1, 8, 0.7f, 1, 2.2f), direct, pos, 1);
        spawnDirectionalParticles(8, 0.06, serverLevel , new CannonMuzzleFlareOption(1, 1, 1, 8, 0.72f, 1, 1.5f), direct, pos, 0.8);
        spawnDirectionalParticles(1, 0, serverLevel , new CannonMuzzleFlareOption(0.4f, 0.4f, 0.4f, 36, 0.84f, 2, 1.1f), direct, pos, 0.6);
        spawnDirectionalParticles(1, 0, serverLevel , new CannonMuzzleFlareOption(0.4f, 0.4f, 0.4f, 39, 0.87f, 2, 0.9f), direct, pos, 0.5);
        spawnDirectionalParticles(1, 0, serverLevel , new CannonMuzzleFlareOption(0.4f, 0.4f, 0.4f, 42, 0.87f, 2, 0.7f), direct, pos, 0.35);
        spawnDirectionalParticles(1, 0, serverLevel , new CannonMuzzleFlareOption(0.4f, 0.4f, 0.4f, 45, 0.88f, 2, 0.5f), direct, pos, 0.25);
        spawnDirectionalParticles(1, 0, serverLevel , new CannonMuzzleFlareOption(0.45f, 0.45f, 0.45f, 47, 0.90f, 2, 0.3f), direct, pos, 0.17);
        spawnDirectionalParticles(1, 0, serverLevel , new CannonMuzzleFlareOption(0.5f, 0.5f, 0.5f, 48, 0.92f, 2, 0.1f), direct, pos, 0.1);
    }

    public static void spawnDirectionalParticles(int count, double radius, ServerLevel level, ParticleOptions particle, Vec3 direct, Vec3 pos, double speed) {
        Vec3 direction = direct.normalize();
        Vec3 randomPerp = getRandomPerpendicular(direction);
        Vec3 u = randomPerp.normalize();
        Vec3 v = direction.cross(u).normalize();

        spawnCircularParticles(level, pos, u, v, count, radius, particle, direct, speed);
    }

    public static void spawnDirectionalParticles(int count, double radius, Level level, ParticleOptions particle, Vec3 direct, Vec3 pos, double speed) {
        Vec3 direction = direct.normalize();
        Vec3 randomPerp = getRandomPerpendicular(direction);
        Vec3 u = randomPerp.normalize();
        Vec3 v = direction.cross(u).normalize();

        spawnCircularParticles(level, pos, u, v, count, radius, particle, direct, speed);
    }

    private static Vec3 getRandomPerpendicular(Vec3 dir) {
        Vec3 candidate1 = new Vec3(dir.y, -dir.x, 0); // 在XY平面垂直
        if (candidate1.lengthSqr() > 1e-4) return candidate1;
        return new Vec3(0, dir.z, -dir.y); // 备用垂直向量
    }

    private static void spawnCircularParticles(ServerLevel level, Vec3 center, Vec3 u, Vec3 v, int count, double radius, ParticleOptions particle, Vec3 direct, double speed) {
        for (int i = 0; i < count; i++) {
            double theta = 2 * Math.PI * i / count;
            double xOffset = radius * (Math.cos(theta) * u.x + Math.sin(theta) * v.x);
            double yOffset = radius * (Math.cos(theta) * u.y + Math.sin(theta) * v.y);
            double zOffset = radius * (Math.cos(theta) * u.z + Math.sin(theta) * v.z);

            Vec3 pos = center.add(xOffset, yOffset, zOffset);
            spawnParticle(level, pos, particle, direct, center, speed);
        }
    }

    private static void spawnCircularParticles(Level level, Vec3 center, Vec3 u, Vec3 v, int count, double radius, ParticleOptions particle, Vec3 direct, double speed) {
        for (int i = 0; i < count; i++) {
            double theta = 2 * Math.PI * i / count;
            double xOffset = radius * (Math.cos(theta) * u.x + Math.sin(theta) * v.x);
            double yOffset = radius * (Math.cos(theta) * u.y + Math.sin(theta) * v.y);
            double zOffset = radius * (Math.cos(theta) * u.z + Math.sin(theta) * v.z);

            Vec3 pos = center.add(xOffset, yOffset, zOffset);
            spawnParticle(level, pos, particle, direct, center, speed);
        }
    }

    private static void spawnParticle(ServerLevel level, Vec3 pos, ParticleOptions particle, Vec3 direct, Vec3 originPos, double speed) {
        Vec3 v0 = originPos.vectorTo(pos).normalize().add(direct.scale(6));
        sendParticle(level, particle, pos.x, pos.y, pos.z,
                0, v0.x, v0.y, v0.z, speed, true);
    }

    private static void spawnParticle(Level level, Vec3 pos, ParticleOptions particle, Vec3 direct, Vec3 originPos, double speed) {
        Vec3 v0 = originPos.vectorTo(pos).normalize().add(direct.scale(6));
        sendParticle(level, particle, pos.x, pos.y, pos.z, v0.x, v0.y, v0.z, speed);
    }

    public static void sendParticle(Level level, ParticleOptions particle, double x, double y, double z, double xOffset, double yOffset, double zOffset, double speed) {
        Vec3 vec3 = new Vec3(xOffset, yOffset, zOffset).normalize().scale(speed * (0.75 + Math.random() * 0.5));
        level.addAlwaysVisibleParticle(particle,
                true,
                x, y, z,
                vec3.x,
                vec3.y,
                vec3.z);
    }

    public static void spawnBarrelSmoke(int count, ServerLevel level, Vec3 v0, Vec3 pos) {
        for (int i = 0; i < count; i++) {
            sendParticle(level, ModParticleTypes.RISING_SMOKE.get(), pos.x, pos.y, pos.z,
                    0, v0.x, v0.y, v0.z, 0.22, true);
        }
    }
}
