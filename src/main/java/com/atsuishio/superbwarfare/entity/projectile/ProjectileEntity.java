package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.block.BarbedWireBlock;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.config.server.ProjectileConfig;
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback;
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.Beast;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

@SuppressWarnings({"unused", "UnusedReturnValue", "SuspiciousNameCombination"})
public class ProjectileEntity extends Projectile implements IEntityWithComplexSpawn, GeoEntity, CustomSyncMotionEntity {

    public static final EntityDataAccessor<Float> COLOR_R = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> COLOR_G = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> COLOR_B = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && input.isPickable() && !input.isSpectator() && input.isAlive();
    private static final Predicate<BlockState> IGNORE_LIST = input -> input != null && (input.getBlock() instanceof LeavesBlock
            || input.getBlock() instanceof FenceBlock
            || input.is(Blocks.IRON_BARS)
            || input.getBlock() instanceof DoorBlock
            || input.getBlock() instanceof TrapDoorBlock
            || input.getBlock() instanceof BarbedWireBlock);

    @Nullable
    protected LivingEntity shooter;
    protected int shooterId;
    private float damage = 1f;
    private float headShot = 1f;
    private float monsterMultiplier = 0.0f;
    private float legShot = 0.5f;
    private boolean beast = false;
    private boolean zoom = false;
    private float bypassArmorRate = 0.0f;
    private float undeadMultiple = 1.0f;
    private float illagerMultiple = 1.0f;
    private int riotLevel = 0;
    private int jhpLevel = 0;
    private int heLevel = 0;
    private int fireLevel = 0;
    private boolean dragonBreath = false;
    private float knockback = 0.05f;
    private boolean forceKnockback = false;
    private final ArrayList<MobEffectInstance> mobEffects = new ArrayList<>();
    private String gunItemId;

    public ProjectileEntity(EntityType<? extends ProjectileEntity> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
    }

    public ProjectileEntity(Level level) {
        super(ModEntities.PROJECTILE.get(), level);
    }

    @Nullable
    protected EntityResult findEntityOnPath(Vec3 startVec, Vec3 endVec) {
        if (this.shooter == null) return null;

        Vec3 hitVec = null;
        Entity hitEntity = null;
        boolean headshot = false;
        boolean legShot = false;
        List<Entity> entities = this.level()
                .getEntities(this,
                        this.getBoundingBox()
                                .expandTowards(this.getDeltaMovement())
                                .inflate(this.beast ? 3 : 1),
                        PROJECTILE_TARGETS
                );
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (entity.equals(this.shooter)) continue;
            if (entity.equals(this.shooter.getVehicle())) continue;

            if (entity instanceof TargetEntity && entity.getEntityData().get(TargetEntity.DOWN_TIME) > 0) continue;
            if (entity instanceof DPSGeneratorEntity && entity.getEntityData().get(DPSGeneratorEntity.DOWN_TIME) > 0)
                continue;

            EntityResult result = this.getHitResult(entity, startVec, endVec);
            if (result == null) continue;

            Vec3 hitPos = result.getHitPos();
            if (hitPos == null) continue;

            double distanceToHit = startVec.distanceTo(hitPos);
            if (distanceToHit < closestDistance) {
                hitVec = hitPos;
                hitEntity = entity;
                closestDistance = distanceToHit;
                headshot = result.isHeadshot();
                legShot = result.isLegShot();
            }
        }
        return hitEntity != null ? new EntityResult(hitEntity, hitVec, headshot, legShot) : null;
    }

    @Nullable
    protected List<EntityResult> findEntitiesOnPath(Vec3 startVec, Vec3 endVec) {
        List<EntityResult> hitEntities = new ArrayList<>();
        List<Entity> entities = this.level().getEntities(
                this,
                this.getBoundingBox()
                        .expandTowards(this.getDeltaMovement())
                        .inflate(1),
                PROJECTILE_TARGETS
        );
        for (Entity entity : entities) {
            if (shooter != null && entity != shooter && entity != shooter.getVehicle()) {
                EntityResult result = this.getHitResult(entity, startVec, endVec);
                if (result == null) continue;
                if (entity.getVehicle() != null && entity.getVehicle() == shooter.getVehicle()) continue;
                hitEntities.add(result);
            }
        }
        return hitEntities;
    }

    /**
     * From TaC-Z
     */
    @Nullable
    private EntityResult getHitResult(Entity entity, Vec3 startVec, Vec3 endVec) {
        double expandHeight = entity instanceof Player && !entity.isCrouching() ? 0.0625 : 0.0;

        Vec3 hitPos = null;
        if (entity instanceof OBBEntity obbEntity) {
            for (OBB obb : obbEntity.getOBBs()) {
                var obbVec = obb.clip(startVec.toVector3f(), endVec.toVector3f()).orElse(null);
                if (obbVec != null) {
                    hitPos = new Vec3(obbVec);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        this.level().playSound(null, BlockPos.containing(hitPos), ModSounds.HIT.get(), SoundSource.PLAYERS, 1, 1);
                        sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), hitPos.x, hitPos.y, hitPos.z, 2, 0, 0, 0, 0.2, false);
                        sendParticle(serverLevel, ParticleTypes.SMOKE, hitPos.x, hitPos.y, hitPos.z, 2, 0, 0, 0, 0.01, false);
                    }

                    var acc = OBBHitter.getInstance(this);
                    acc.sbw$setCurrentHitPart(obb.part());
                }
            }
        } else {
            AABB boundingBox = entity.getBoundingBox();
            Vec3 velocity = new Vec3(entity.getX() - entity.xOld, entity.getY() - entity.yOld, entity.getZ() - entity.zOld);

            if (entity instanceof ServerPlayer player && this.shooter instanceof ServerPlayer serverPlayerOwner) {
                int ping = Mth.floor((serverPlayerOwner.connection.latency() / 1000.0) * 20.0 + 0.5);
                boundingBox = HitboxHelper.getBoundingBox(player, ping);
                velocity = HitboxHelper.getVelocity(player, ping);
            }
            boundingBox = boundingBox.expandTowards(0, expandHeight, 0);
            boundingBox = boundingBox.expandTowards(velocity.x, velocity.y, velocity.z);

            double playerHitboxOffset = 3;
            if (entity instanceof ServerPlayer) {
                if (entity.getVehicle() != null) {
                    boundingBox = boundingBox.move(velocity.multiply(playerHitboxOffset / 2, playerHitboxOffset / 2, playerHitboxOffset / 2));
                }
                boundingBox = boundingBox.move(velocity.multiply(playerHitboxOffset, playerHitboxOffset, playerHitboxOffset));
            }

            if (entity.getVehicle() != null) {
                boundingBox = boundingBox.move(velocity.multiply(-2.5, -2.5, -2.5));
            }
            boundingBox = boundingBox.move(velocity.multiply(-5, -5, -5));

            if (this.beast) {
                boundingBox = boundingBox.inflate(3);
            }

            hitPos = boundingBox.clip(startVec, endVec).orElse(null);

        }

        if (hitPos == null) {
            return null;
        }
        Vec3 hitBoxPos = hitPos.subtract(entity.position());
        boolean headshot = false;
        boolean legShot = false;
        float eyeHeight = entity.getEyeHeight();
        float bodyHeight = entity.getBbHeight();
        if ((eyeHeight - 0.35) < hitBoxPos.y && hitBoxPos.y < (eyeHeight + 0.4) && entity instanceof LivingEntity) {
            headshot = true;
        }
        if (hitBoxPos.y < (0.33 * bodyHeight) && entity instanceof LivingEntity) {
            legShot = true;
        }

        if (heLevel > 0) {
            explosionBullet(this, this.damage, heLevel, monsterMultiplier + 1, hitPos);
        }

        return new EntityResult(entity, hitPos, headshot, legShot);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(COLOR_R, 1.0f)
                .define(COLOR_G, 222 / 255f)
                .define(COLOR_B, 39 / 255f);

    }

    @Override
    public void tick() {
        super.tick();
        this.updateHeading();

        Vec3 vec = this.getDeltaMovement();

        if (!this.level().isClientSide() && this.shooter != null) {
            Vec3 startVec = this.position();
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            HitResult result = rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this),
                    ProjectileConfig.ALLOW_PROJECTILE_DESTROY_GLASS.get() ? IGNORE_LIST : IGNORE_LIST.or(input -> input.is(Tags.Blocks.GLASS_PANES)));
            if (result.getType() != HitResult.Type.MISS) {
                endVec = result.getLocation();
            }

            List<EntityResult> entityResults = new ArrayList<>();
            var temp = findEntitiesOnPath(startVec, endVec);
            if (temp != null) entityResults.addAll(temp);
            entityResults.sort(Comparator.comparingDouble(e -> e.getHitPos().distanceTo(this.shooter.position())));

            for (EntityResult entityResult : entityResults) {
                result = new ExtendedEntityRayTraceResult(entityResult);
                if (((EntityHitResult) result).getEntity() instanceof Player player) {
                    if (this.shooter instanceof Player p && !p.canHarmPlayer(player)) {
                        result = null;
                    }
                }
                if (result != null) {
                    this.onHit(result);
                }

                if (!this.beast) {
                    this.bypassArmorRate -= 0.2F;
                    if (this.bypassArmorRate < 0.8F) {
                        if (result != null && !(((EntityHitResult) result).getEntity() instanceof TargetEntity target && target.getEntityData().get(TargetEntity.DOWN_TIME) > 0)
                                && !(((EntityHitResult) result).getEntity() instanceof DPSGeneratorEntity dpsGeneratorEntity && dpsGeneratorEntity.getEntityData().get(DPSGeneratorEntity.DOWN_TIME) > 0)) {
                            break;
                        }
                    }
                }
            }
            if (entityResults.isEmpty()) {
                this.onHit(result);
            }

            this.setPos(this.getX() + vec.x, this.getY() + vec.y, this.getZ() + vec.z);
        } else {
            this.setPosRaw(this.getX() + vec.x, this.getY() + vec.y, this.getZ() + vec.z);
        }

        this.setDeltaMovement(vec.x, vec.y - 0.02, vec.z);

        if (this.tickCount > (fireLevel > 0 ? 10 : 40)) {
            this.discard();
        }

        if (fireLevel > 0 && dragonBreath && this.level() instanceof ServerLevel serverLevel) {
            double randomPos = this.tickCount * 0.08 * (Math.random() - 0.5);
            ParticleTool.sendParticle(serverLevel, ParticleTypes.FLAME,
                    (this.xo + this.getX()) / 2 + randomPos, (this.yo + this.getY()) / 2 + randomPos, (this.zo + this.getZ()) / 2 + randomPos,
                    0,
                    this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z,
                    Math.max(this.getDeltaMovement().length() - 1.1 * this.tickCount, 0.2), true
            );
        }

        this.syncMotion();
    }

    @Override
    public void syncMotion() {
        if (!this.level().isClientSide) {
            PacketDistributor.sendToAllPlayers(new ClientMotionSyncMessage(this));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.damage = tag.getFloat("Damage");
        this.headShot = tag.getFloat("HeadShot");
        this.monsterMultiplier = tag.getFloat("MonsterMultiplier");
        this.legShot = tag.getFloat("LegShot");
        this.bypassArmorRate = tag.getFloat("BypassArmorRate");
        this.undeadMultiple = tag.getFloat("UndeadMultiple");
        this.illagerMultiple = tag.getFloat("illagerMultiple");
        this.knockback = tag.getFloat("Knockback");

        this.beast = tag.getBoolean("Beast");
        this.forceKnockback = tag.getBoolean("ForceKnockback");

        if (tag.contains("GunId")) {
            this.gunItemId = tag.getString("GunId");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", this.damage);
        tag.putFloat("HeadShot", this.headShot);
        tag.putFloat("MonsterMultiplier", this.monsterMultiplier);
        tag.putFloat("LegShot", this.legShot);
        tag.putFloat("BypassArmorRate", this.bypassArmorRate);
        tag.putFloat("UndeadMultiple", this.undeadMultiple);
        tag.putFloat("illagerMultiple", this.illagerMultiple);
        tag.putFloat("Knockback", this.knockback);

        tag.putBoolean("Beast", this.beast);
        tag.putBoolean("ForceKnockback", this.forceKnockback);

        if (this.gunItemId != null) {
            tag.putString("GunId", this.gunItemId);
        }
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        if (result instanceof BlockHitResult blockHitResult) {
            if (blockHitResult.getType() == HitResult.Type.MISS) {
                return;
            }
            BlockPos resultPos = blockHitResult.getBlockPos();
            BlockState state = this.level().getBlockState(resultPos);
            SoundEvent event = state.getBlock().getSoundType(state, this.level(), resultPos, this).getBreakSound();
            this.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z, event, SoundSource.AMBIENT, 1.0F, 1.0F);
            Vec3 hitVec = result.getLocation();

            if (state.getBlock() instanceof BellBlock bell) {
                bell.attemptToRing(this.level(), resultPos, blockHitResult.getDirection());
            }

            if (ProjectileConfig.ALLOW_PROJECTILE_DESTROY_GLASS.get()) {
                if (state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)) {
                    this.level().destroyBlock(resultPos, false, this.getShooter());
                }
            }

            if (state.getBlock() instanceof TargetBlock) {
                if (this.shooter == null) return;

                int rings = getRings(blockHitResult, hitVec);
                double dis = this.shooter.position().distanceTo(hitVec);
                recordHitScore(rings, dis);
            }

            this.onHitBlock(hitVec);
            if (heLevel > 0) {
                explosionBullet(this, this.damage, heLevel, monsterMultiplier + 1, hitVec);
            }
            if (fireLevel > 0 && this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.LAVA, hitVec.x, hitVec.y, hitVec.z,
                        3, 0, 0, 0, 0.5, true);
            }
        }

        if (result instanceof ExtendedEntityRayTraceResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            if (entity.getId() == this.shooterId) {
                return;
            }

            if (this.shooter instanceof Player player) {
                if (entity.hasIndirectPassenger(player)) {
                    return;
                }
            }

            this.onHitEntity(entity, entityHitResult.isHeadshot(), entityHitResult.isLegShot());
            entity.invulnerableTime = 0;
        }
    }

    private static int getRings(@NotNull BlockHitResult blockHitResult, @NotNull Vec3 hitVec) {
        Direction direction = blockHitResult.getDirection();
        double x = Math.abs(Mth.frac(hitVec.x) - 0.5);
        double y = Math.abs(Mth.frac(hitVec.y) - 0.5);
        double z = Math.abs(Mth.frac(hitVec.z) - 0.5);
        Direction.Axis axis = direction.getAxis();
        double v;
        if (axis == Direction.Axis.Y) {
            v = Math.max(x, z);
        } else if (axis == Direction.Axis.Z) {
            v = Math.max(x, y);
        } else {
            v = Math.max(y, z);
        }

        return Math.max(1, Mth.ceil(10.0 * Mth.clamp((0.5 - v) / 0.5, 0.0, 1.0)));
    }

    private void recordHitScore(int score, double distance) {
        if (!(shooter instanceof Player player)) {
            return;
        }

        player.displayClientMessage(Component.literal(String.valueOf(score))
                .append(Component.translatable("tips.superbwarfare.shoot.rings"))
                .append(Component.literal(" " + FormatTool.format1D(distance, "m"))), false);

        if (!this.shooter.level().isClientSide() && this.shooter instanceof ServerPlayer serverPlayer) {
            var holder = score == 10 ? Holder.direct(ModSounds.HEADSHOT.get()) : Holder.direct(ModSounds.INDICATION.get());
            serverPlayer.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.level().random.nextLong()));
            PacketDistributor.sendToPlayer(serverPlayer, new ClientIndicatorMessage(score == 10 ? 1 : 0, 5));
        }

        ItemStack stack = player.getOffhandItem();

        if (stack.is(ModItems.TRANSCRIPT.get())) {
            final int size = 10;

            var scores = stack.get(ModDataComponents.TRANSCRIPT_SCORE);
            if (scores == null) scores = List.of();

            Queue<Pair<Integer, Double>> queue = new ArrayDeque<>(scores);
            queue.offer(new Pair<>(score, distance));

            while (queue.size() > size) {
                queue.poll();
            }

            stack.set(ModDataComponents.TRANSCRIPT_SCORE, List.copyOf(queue));
        }
    }

    protected void onHitBlock(Vec3 location) {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.beast) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.END_ROD, location.x, location.y, location.z, 15, 0.1, 0.1, 0.1, 0.05, true);
            } else {
                ParticleTool.sendParticle(serverLevel, ModParticleTypes.BULLET_HOLE.get(), location.x, location.y, location.z, 1, 0, 0, 0, 0, true);
                ParticleTool.sendParticle(serverLevel, ParticleTypes.SMOKE, location.x, location.y, location.z, 3, 0, 0.1, 0, 0.01, true);
                this.discard();
            }
            serverLevel.playSound(null, new BlockPos((int) location.x, (int) location.y, (int) location.z), ModSounds.LAND.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    protected void onHitEntity(Entity entity, boolean headshot, boolean legShot) {
        if (this.shooter == null) return;

        float mMultiple = 1 + this.monsterMultiplier;

        if (entity == null) return;

        if (entity instanceof PartEntity<?> part) {
            entity = part.getParent();
        }

        if (entity instanceof LivingEntity living) {
            living.level().playSound(null, living.getOnPos(), ModSounds.MELEE_HIT.get(), SoundSource.PLAYERS, 1, (float) ((2 * org.joml.Math.random() - 1) * 0.1f + 1.0f));
        }

        if (beast && entity instanceof LivingEntity living) {
            Beast.beastKill(this.shooter, living);
            return;
        }

        if (entity instanceof Monster) {
            this.damage *= mMultiple;
        }

        if (entity instanceof LivingEntity living && living.getType().is(EntityTypeTags.UNDEAD)) {
            this.damage *= this.undeadMultiple;
        }

        if (entity instanceof LivingEntity living && (living.getType().is(EntityTypeTags.ILLAGER) || living instanceof Vex || living instanceof Ravager || living instanceof Witch)) {
            this.damage *= this.illagerMultiple;
        }

        if (entity instanceof LivingEntity living && riotLevel > 0 && !entity.level().isClientSide()) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 + riotLevel * 10, (int) (riotLevel * 0.25), false, false), this.shooter);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 + riotLevel * 10, (int) (riotLevel * 0.25), false, false), this.shooter);
        }

        if (entity instanceof LivingEntity living && jhpLevel > 0) {
            this.damage *= (1.0f + 0.12f * jhpLevel) * ((float) (10 / (living.getAttributeValue(Attributes.ARMOR) + 10)) + 0.25f);
        }

        if (fireLevel > 0) {
            if (!entity.level().isClientSide() && entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(ModMobEffects.BURN, 60 + fireLevel * 20, fireLevel, false, false), this.shooter);
            }
        }

        if (headshot) {
            if (!this.shooter.level().isClientSide() && this.shooter instanceof ServerPlayer player) {
                var holder = Holder.direct(ModSounds.HEADSHOT.get());
                player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.level().random.nextLong()));
                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(1, 5));
            }
            performOnHit(entity, this.damage, true, this.knockback);
        } else {
            if (!this.shooter.level().isClientSide() && this.shooter instanceof ServerPlayer player) {
                var holder = Holder.direct(ModSounds.INDICATION.get());
                player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.level().random.nextLong()));
                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
            }

            if (legShot) {
                if (entity instanceof LivingEntity living) {
                    if (living instanceof Player player && player.isCreative()) {
                        return;
                    }
                    if (!living.level().isClientSide()) {
                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false));
                    }
                }
                this.damage *= this.legShot;
            }

            performOnHit(entity, this.damage, false, this.knockback);
        }

        if (!this.mobEffects.isEmpty() && entity instanceof LivingEntity living) {
            for (MobEffectInstance instance : this.mobEffects) {
                living.addEffect(instance, this.shooter);
            }
        }

        this.discard();
    }

    public void performOnHit(Entity entity, float damage, boolean headshot, double knockback) {
        if (entity instanceof LivingEntity living) {
            if (this.forceKnockback) {
                Vec3 vec3 = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize();
                living.addDeltaMovement(vec3.scale(knockback));
                performDamage(entity, damage, headshot);
            } else {
                ICustomKnockback iCustomKnockback = ICustomKnockback.getInstance(living);
                iCustomKnockback.superbWarfare$setKnockbackStrength(knockback);
                performDamage(entity, damage, headshot);
                iCustomKnockback.superbWarfare$resetKnockbackStrength();
            }
        } else {
            performDamage(entity, damage, headshot);
        }
    }

    protected void explosionBullet(Entity projectile, float damage, int heLevel, float monsterMultiple, Vec3 hitVec) {
        CustomExplosion explosion = new CustomExplosion(projectile.level(), projectile,
                ModDamageTypes.causeProjectileBoomDamage(projectile.level().registryAccess(), projectile, this.getShooter()), (float) ((0.9 * damage) * (1 + 0.1 * heLevel)),
                hitVec.x, hitVec.y, hitVec.z, (float) ((1.5 + 0.02 * damage) * (1 + 0.05 * heLevel))).setDamageMultiplier(monsterMultiple).bulletExplode();
        explosion.explode();
        EventHooks.onExplosionStart(projectile.level(), explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnMiniExplosionParticles(this.level(), hitVec);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return this.damage;
    }

    public void shoot(Player player, double vecX, double vecY, double vecZ, float velocity, float spread) {
        Vec3 vec3 = (new Vec3(vecX, vecY, vecZ)).normalize().
                add(this.random.triangle(0.0D, 0.0172275D * (double) spread), this.random.triangle(0.0D, 0.0172275D * (double) spread), this.random.triangle(0.0D, 0.0172275D * (double) spread)).
                scale(velocity);
        this.setDeltaMovement(vec3);
        double d0 = vec3.horizontalDistance();
        this.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)));
        this.setXRot((float) (Mth.atan2(vec3.y, d0) * (double) (180F / (float) Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @SuppressWarnings("SameParameterValue")
    private static BlockHitResult rayTraceBlocks(Level world, ClipContext context, Predicate<BlockState> ignorePredicate) {
        return performRayTrace(context, (rayTraceContext, blockPos) -> {
            BlockState blockState = world.getBlockState(blockPos);
            if (ignorePredicate.test(blockState)) return null;
            FluidState fluidState = world.getFluidState(blockPos);
            Vec3 startVec = rayTraceContext.getFrom();
            Vec3 endVec = rayTraceContext.getTo();
            VoxelShape blockShape = rayTraceContext.getBlockShape(blockState, world, blockPos);
            BlockHitResult blockResult = world.clipWithInteractionOverride(startVec, endVec, blockPos, blockShape, blockState);
            VoxelShape fluidShape = rayTraceContext.getFluidShape(fluidState, world, blockPos);
            BlockHitResult fluidResult = fluidShape.clip(startVec, endVec, blockPos);
            double blockDistance = blockResult == null ? Double.MAX_VALUE : rayTraceContext.getFrom().distanceToSqr(blockResult.getLocation());
            double fluidDistance = fluidResult == null ? Double.MAX_VALUE : rayTraceContext.getFrom().distanceToSqr(fluidResult.getLocation());
            return blockDistance <= fluidDistance ? blockResult : fluidResult;
        }, (rayTraceContext) -> {
            Vec3 Vector3d = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
            return BlockHitResult.miss(rayTraceContext.getTo(), Direction.getNearest(Vector3d.x, Vector3d.y, Vector3d.z), BlockPos.containing(rayTraceContext.getTo()));
        });
    }

    private static <T> T performRayTrace(ClipContext context, BiFunction<ClipContext, BlockPos, T> hitFunction, Function<ClipContext, T> p_217300_2_) {
        Vec3 startVec = context.getFrom();
        Vec3 endVec = context.getTo();
        if (!startVec.equals(endVec)) {
            double startX = Mth.lerp(-0.0000001, endVec.x, startVec.x);
            double startY = Mth.lerp(-0.0000001, endVec.y, startVec.y);
            double startZ = Mth.lerp(-0.0000001, endVec.z, startVec.z);
            double endX = Mth.lerp(-0.0000001, startVec.x, endVec.x);
            double endY = Mth.lerp(-0.0000001, startVec.y, endVec.y);
            double endZ = Mth.lerp(-0.0000001, startVec.z, endVec.z);
            int blockX = Mth.floor(endX);
            int blockY = Mth.floor(endY);
            int blockZ = Mth.floor(endZ);
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(blockX, blockY, blockZ);
            T t = hitFunction.apply(context, mutablePos);
            if (t != null) {
                return t;
            }

            double deltaX = startX - endX;
            double deltaY = startY - endY;
            double deltaZ = startZ - endZ;
            int signX = Mth.sign(deltaX);
            int signY = Mth.sign(deltaY);
            int signZ = Mth.sign(deltaZ);
            double d9 = signX == 0 ? Double.MAX_VALUE : (double) signX / deltaX;
            double d10 = signY == 0 ? Double.MAX_VALUE : (double) signY / deltaY;
            double d11 = signZ == 0 ? Double.MAX_VALUE : (double) signZ / deltaZ;
            double d12 = d9 * (signX > 0 ? 1.0D - Mth.frac(endX) : Mth.frac(endX));
            double d13 = d10 * (signY > 0 ? 1.0D - Mth.frac(endY) : Mth.frac(endY));
            double d14 = d11 * (signZ > 0 ? 1.0D - Mth.frac(endZ) : Mth.frac(endZ));

            while (d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D) {
                if (d12 < d13) {
                    if (d12 < d14) {
                        blockX += signX;
                        d12 += d9;
                    } else {
                        blockZ += signZ;
                        d14 += d11;
                    }
                } else if (d13 < d14) {
                    blockY += signY;
                    d13 += d10;
                } else {
                    blockZ += signZ;
                    d14 += d11;
                }

                T t1 = hitFunction.apply(context, mutablePos.set(blockX, blockY, blockZ));
                if (t1 != null) {
                    return t1;
                }
            }
        }
        return p_217300_2_.apply(context);
    }

    public @Nullable LivingEntity getShooter() {
        return this.shooter;
    }

    public int getShooterId() {
        return this.shooterId;
    }

    public float getBypassArmorRate() {
        return this.bypassArmorRate;
    }

    public void updateHeading() {
        double horizontalDistance = this.getDeltaMovement().horizontalDistance();
        this.setYRot((float) (Mth.atan2(this.getDeltaMovement().x(), this.getDeltaMovement().z()) * (180D / Math.PI)));
        this.setXRot((float) (Mth.atan2(this.getDeltaMovement().y(), horizontalDistance) * (180D / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private void performDamage(Entity entity, float damage, boolean isHeadshot) {
        float rate = Mth.clamp(this.bypassArmorRate, 0, 1);

        float normalDamage = damage * Mth.clamp(1 - rate, 0, 1);
        float absoluteDamage = damage * Mth.clamp(rate, 0, 1);

        entity.invulnerableTime = 0;

        float headShotModifier = isHeadshot ? this.headShot : 1;
        if (normalDamage > 0) {
            entity.hurt(isHeadshot ? ModDamageTypes.causeGunFireHeadshotDamage(this.level().registryAccess(), this, this.shooter)
                    : ModDamageTypes.causeGunFireDamage(this.level().registryAccess(), this, this.shooter), normalDamage * headShotModifier);
            entity.invulnerableTime = 0;
        }
        if (absoluteDamage > 0) {
            entity.hurt(isHeadshot ? ModDamageTypes.causeGunFireHeadshotAbsoluteDamage(this.level().registryAccess(), this, this.shooter)
                    : ModDamageTypes.causeGunFireAbsoluteDamage(this.level().registryAccess(), this, this.shooter), absoluteDamage * headShotModifier);
            entity.invulnerableTime = 0;

            // 大于1的穿甲对载具造成额外伤害
            if (entity instanceof VehicleEntity vehicle && this.bypassArmorRate > 1) {
                vehicle.hurt(ModDamageTypes.causeGunFireAbsoluteDamage(this.level().registryAccess(), this, this.shooter), absoluteDamage * (this.bypassArmorRate - 1) * 0.5f);
            }
        }
    }


    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf additionalData) {
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
    }

    public static class EntityResult {
        private final Entity entity;
        private final Vec3 hitVec;
        private final boolean headshot;
        private final boolean legShot;

        public EntityResult(Entity entity, Vec3 hitVec, boolean headshot, boolean legShot) {
            this.entity = entity;
            this.hitVec = hitVec;
            this.headshot = headshot;
            this.legShot = legShot;
        }

        /**
         * Gets the entity that was hit by the projectile
         */
        public Entity getEntity() {
            return this.entity;
        }

        /**
         * Gets the position the projectile hit
         */
        public Vec3 getHitPos() {
            return this.hitVec;
        }

        /**
         * Gets if this was a headshot
         */
        public boolean isHeadshot() {
            return this.headshot;
        }

        public boolean isLegShot() {
            return this.legShot;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public boolean isZoom() {
        return this.zoom;
    }

    @Nullable
    public String getGunItemId() {
        return this.gunItemId;
    }

    /**
     * Builders
     */
    public ProjectileEntity shooter(LivingEntity shooter) {
        this.shooter = shooter;
        return this;
    }

    public ProjectileEntity damage(float damage) {
        this.damage = damage;
        return this;
    }

    public ProjectileEntity headShot(float headShot) {
        this.headShot = headShot;
        return this;
    }

    public ProjectileEntity setMonsterMultiplier(float monsterMultiplier) {
        this.monsterMultiplier = monsterMultiplier;
        return this;
    }

    public ProjectileEntity legShot(float legShot) {
        this.legShot = legShot;
        return this;
    }

    public ProjectileEntity beast() {
        this.beast = true;
        return this;
    }

    public ProjectileEntity riotBullet(int riotLevel) {
        this.riotLevel = riotLevel;
        return this;
    }

    public ProjectileEntity jhpBullet(int jhpLevel) {
        this.jhpLevel = jhpLevel;
        return this;
    }

    public ProjectileEntity heBullet(int heLevel) {
        this.heLevel = heLevel;
        return this;
    }

    public ProjectileEntity fireBullet(int fireLevel, boolean dragonBreath) {
        this.fireLevel = fireLevel;
        this.dragonBreath = dragonBreath;
        return this;
    }

    public ProjectileEntity zoom(boolean zoom) {
        this.zoom = zoom;
        return this;
    }

    public ProjectileEntity bypassArmorRate(float bypassArmorRate) {
        this.bypassArmorRate = bypassArmorRate;
        return this;
    }

    public ProjectileEntity undeadMultiple(float undeadMultiple) {
        this.undeadMultiple = undeadMultiple;
        return this;
    }

    public ProjectileEntity illagerMultiple(float illagerMultiple) {
        this.illagerMultiple = illagerMultiple;
        return this;
    }

    public ProjectileEntity effect(ArrayList<MobEffectInstance> mobEffectInstances) {
        this.mobEffects.addAll(mobEffectInstances);
        return this;
    }

    public void setRGB(float[] rgb) {
        this.entityData.set(COLOR_R, rgb[0]);
        this.entityData.set(COLOR_G, rgb[1]);
        this.entityData.set(COLOR_B, rgb[2]);
    }

    public ProjectileEntity knockback(float knockback) {
        this.knockback = knockback;
        return this;
    }

    public ProjectileEntity forceKnockback() {
        this.forceKnockback = true;
        return this;
    }

    public ProjectileEntity setGunItemId(ItemStack stack) {
        this.gunItemId = stack.getDescriptionId();
        return this;
    }

    public ProjectileEntity setGunItemId(String id) {
        this.gunItemId = id;
        return this;
    }
}
