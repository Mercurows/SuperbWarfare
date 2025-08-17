package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.particle.BulletDecalOption;
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.config.server.ProjectileConfig;
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback;
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.Beast;
import com.atsuishio.superbwarfare.item.Transcript;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

@SuppressWarnings({"unused", "UnusedReturnValue", "SuspiciousNameCombination"})
public class ProjectileEntity extends Projectile implements GeoEntity, CustomSyncMotionEntity {

    public static final EntityDataAccessor<Float> COLOR_R = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> COLOR_G = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> COLOR_B = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && input.isPickable() && !input.isSpectator() && input.isAlive();
    private static final Predicate<BlockState> IGNORE_LIST = input -> input != null && input.is(ModTags.Blocks.BULLET_IGNORE) && !(input.is(Blocks.IRON_DOOR) || input.is(Blocks.IRON_TRAPDOOR));

    // 子弹的颜色
    public static final float DEFAULT_R = 1.0f;
    public static final float DEFAULT_G = 222 / 255f;
    public static final float DEFAULT_B = 39 / 255f;

    // 子弹的发射者，可以为空
    @Nullable
    protected Entity shooter;
    // 子弹的发射者的ID
    protected int shooterId;
    // 子弹的伤害
    private float damage = 1f;
    // 子弹的爆头倍率
    private float headShot = 1f;
    // 子弹的打腿倍率
    private float legShot = 0.5f;
    // 是否为野兽弹
    private boolean beast = false;
    // 子弹是否是瞄准时发射的
    private boolean zoom = false;
    // 子弹的穿甲比例
    private float bypassArmorRate = 0.0f;
    // 高爆弹等级
    private int heLevel = 0;
    // 燃烧弹等级
    private int fireLevel = 0;
    // 是否为龙息弹
    private boolean dragonBreath = false;
    // 击退力度
    private float knockback = 0.05f;
    // 出膛速度
    private float velocity = 20f;
    // 是否强制击退生物
    private boolean forceKnockback = false;
    // 是否能穿墙
    private boolean penetrating = false;
    // 子弹造成的状态效果
    private final ArrayList<MobEffectInstance> mobEffects = new ArrayList<>();
    // 发射子弹的武器ID
    private String gunItemId;

    public ProjectileEntity(EntityType<? extends ProjectileEntity> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
    }

    public ProjectileEntity(Level level) {
        this(ModEntities.PROJECTILE.get(), level);
    }

    public ProjectileEntity(PlayMessages.SpawnEntity packet, Level level) {
        super(ModEntities.PROJECTILE.get(), level);
    }

    @Nullable
    protected EntityResult findEntityOnPath(Vec3 startVec, Vec3 endVec) {
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
            if (entity.equals(this.shooter) || this.shooter != null && entity.equals(this.shooter.getVehicle()))
                continue;
            if (this.shooter != null && entity.getRootVehicle() == this.shooter.getRootVehicle()) continue;

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
            if (this.shooter == null || entity != shooter && entity != this.shooter.getVehicle()) {
                EntityResult result = this.getHitResult(entity, startVec, endVec);
                if (result == null) continue;
                if (entity.getVehicle() != null && this.shooter != null && entity.getVehicle() == this.shooter.getVehicle())
                    continue;
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
                int ping = Mth.floor((serverPlayerOwner.latency / 1000.0) * 20.0 + 0.5);
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
        if ((eyeHeight - 0.25) < hitBoxPos.y && hitBoxPos.y < (eyeHeight + 0.3) && entity instanceof LivingEntity) {
            headshot = true;
        }
        if (hitBoxPos.y < (0.33 * bodyHeight) && entity instanceof LivingEntity) {
            legShot = true;
        }

        if (heLevel > 0) {
            explosionBullet(this, this.damage, heLevel, hitPos);
        }

        return new EntityResult(entity, hitPos, headshot, legShot);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR_R, DEFAULT_R);
        this.entityData.define(COLOR_G, DEFAULT_G);
        this.entityData.define(COLOR_B, DEFAULT_B);
    }

    @Override
    public void tick() {
        super.tick();
        this.updateHeading();

        Vec3 vec = this.getDeltaMovement();

        if (!this.level().isClientSide()) {
            Vec3 startVec = this.position();
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            HitResult result = rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this),
                    (this.penetrating || this.beast) ? state -> true :
                            ProjectileConfig.ALLOW_PROJECTILE_DESTROY_BLOCKS.get() ? IGNORE_LIST.and(input -> !input.is(ModTags.Blocks.BULLET_CAN_DESTROY)) : IGNORE_LIST);

            BlockHitResult fluidResult = rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this),
                    (this.penetrating || this.beast) ? state -> true :
                            ProjectileConfig.ALLOW_PROJECTILE_DESTROY_BLOCKS.get() ? IGNORE_LIST.and(input -> !input.is(ModTags.Blocks.BULLET_CAN_DESTROY)) : IGNORE_LIST);

            if (result.getType() != HitResult.Type.MISS) {
                endVec = result.getLocation();
            }

            List<EntityResult> entityResults = new ArrayList<>();
            var temp = findEntitiesOnPath(startVec, endVec);
            if (temp != null) entityResults.addAll(temp);

            if (this.shooter != null) {
                entityResults.sort(Comparator.comparingDouble(e -> e.getHitPos().distanceTo(this.shooter.position())));
            }

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

            this.onHitWater(fluidResult.getLocation(), fluidResult);

            this.setPos(this.getX() + vec.x, this.getY() + vec.y, this.getZ() + vec.z);
        } else {
            this.setPosRaw(this.getX() + vec.x, this.getY() + vec.y, this.getZ() + vec.z);
        }

        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.05, 0));

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

        if (this.level() instanceof ServerLevel serverLevel) {
            if (VectorTool.isInLiquid(serverLevel, position())) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.75, 0.75, 0.75));
            }
            if (isInWater()) {
                double l = getDeltaMovement().length();
                for (double i = 0; i < l; i++) {
                    Vec3 startPos = new Vec3(this.xo, this.yo, this.zo);
                    Vec3 pos = startPos.add(getDeltaMovement().normalize().scale(i));
                    ParticleTool.sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, pos.x, pos.y, pos.z,
                            1, 0, 0, 0, 0.001, true);
                }
            }
        }

        this.syncMotion();
    }


    @Override
    public void syncMotion() {
        if (!this.level().isClientSide) {
            Mod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new ClientMotionSyncMessage(this));
        }
    }

    @Override
    protected void onHit(@Nullable HitResult result) {
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

            if (ProjectileConfig.ALLOW_PROJECTILE_DESTROY_BLOCKS.get() && state.is(ModTags.Blocks.BULLET_CAN_DESTROY)) {
                this.level().destroyBlock(resultPos, false, this.getShooter());
            }

            if (state.getBlock() instanceof TargetBlock && this.shooter != null) {
                int rings = getRings(blockHitResult, hitVec);
                double dis = this.shooter.position().distanceTo(hitVec);
                recordHitScore(rings, dis);
            }

            this.onHitBlock(hitVec, blockHitResult);
            if (heLevel > 0) {
                explosionBullet(this, this.damage, heLevel, hitVec);
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

        if (!this.level().isClientSide() && this.shooter instanceof ServerPlayer serverPlayer) {
            var holder = score == 10 ? Holder.direct(ModSounds.HEADSHOT.get()) : Holder.direct(ModSounds.INDICATION.get());
            serverPlayer.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.level().random.nextLong()));
            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new ClientIndicatorMessage(score == 10 ? 1 : 0, 5));
        }

        ItemStack stack = player.getOffhandItem();
        if (stack.is(ModItems.TRANSCRIPT.get())) {
            final int size = 10;

            ListTag tags = stack.getOrCreateTag().getList(Transcript.TAG_SCORES, Tag.TAG_COMPOUND);

            Queue<CompoundTag> queue = new ArrayDeque<>();
            for (int i = 0; i < tags.size(); i++) {
                queue.add(tags.getCompound(i));
            }

            CompoundTag tag = new CompoundTag();
            tag.putInt("Score", score);
            tag.putDouble("Distance", distance);
            queue.offer(tag);

            while (queue.size() > size) {
                queue.poll();
            }

            ListTag newTags = new ListTag();
            newTags.addAll(queue);

            stack.getOrCreateTag().put(Transcript.TAG_SCORES, newTags);
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
                    serverLevel.playSound(null, new BlockPos((int) location.x, (int) location.y, (int) location.z), ModSounds.HIT_WATER.get(), SoundSource.BLOCKS, 1.0F, 1.0F);

                    // 水下路径气泡
                    double l = getDeltaMovement().length();
                    for (double i = 0; i < l; i++) {
                        Vec3 p = location.add(getDeltaMovement().normalize().scale(i));
                        ParticleTool.sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, p.x, p.y, p.z,
                                1, 0, 0, 0, 0.001, false);
                    }

                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.1, 0.1, 0.1));
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
                    serverLevel.playSound(null, new BlockPos((int) location.x, (int) location.y, (int) location.z), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.0F, 1.0F);
                    this.discard();
                }
            }
        }
    }

    protected void onHitBlock(Vec3 location, BlockHitResult result) {
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = result.getBlockPos();
            Direction face = result.getDirection();
            BlockState state = level().getBlockState(pos);

            double vx = face.getStepX();
            double vy = face.getStepY();
            double vz = face.getStepZ();
            Vec3 dir = new Vec3(vx, vy, vz);

            if (this.beast) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.END_ROD, location.x, location.y, location.z, 15, 0.1, 0.1, 0.1, 0.05, true);
            } else {
                BulletDecalOption bulletDecalOption;
                if (this.entityData.get(COLOR_R) == DEFAULT_R && this.entityData.get(COLOR_G) == DEFAULT_G && this.entityData.get(COLOR_B) == DEFAULT_B) {
                    bulletDecalOption = new BulletDecalOption(result.getDirection(), result.getBlockPos());
                } else {
                    bulletDecalOption = new BulletDecalOption(result.getDirection(), result.getBlockPos(),
                            this.entityData.get(COLOR_R), this.entityData.get(COLOR_G), this.entityData.get(COLOR_B));
                }
                ParticleTool.sendParticle(serverLevel, bulletDecalOption, location.x, location.y, location.z, 1, 0, 0, 0, 0, true);
                summonVectorParticle(serverLevel, state, location, dir);

                this.discard();
            }
            serverLevel.playSound(null, new BlockPos((int) location.x, (int) location.y, (int) location.z), ModSounds.LAND.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
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

    public Vec3 randomVec(Vec3 vec3, double spread) {
        return vec3.normalize().add(this.random.triangle(0.0D, 0.0172275D * spread), this.random.triangle(0.0D, 0.0172275D * spread), this.random.triangle(0.0D, 0.0172275D * spread));
    }

    protected void onHitEntity(Entity entity, boolean headshot, boolean legShot) {
        if (entity == null) return;

        if (entity instanceof PartEntity<?> part) {
            entity = part.getParent();
        }

        if (entity instanceof LivingEntity living) {
            living.level().playSound(null, living.getOnPos(), ModSounds.MELEE_HIT.get(), SoundSource.PLAYERS, 1, (float) (2 * Math.random() - 1) * 0.1f + 1.0f);

            if (beast) {
                Beast.beastKill(this.shooter, living);
                return;
            }
        }

        this.damage *= (float) (getDeltaMovement().length() / velocity);

        if (headshot) {
            if (!this.level().isClientSide() && this.shooter instanceof ServerPlayer player) {
                var holder = Holder.direct(ModSounds.HEADSHOT.get());
                player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.level().random.nextLong()));

                Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(1, 5));
            }
            performOnHit(entity, this.damage, true, this.knockback);
        } else {
            if (!this.level().isClientSide() && this.shooter instanceof ServerPlayer player) {
                var holder = Holder.direct(ModSounds.INDICATION.get());
                player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.level().random.nextLong()));
                Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
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

    protected void explosionBullet(Entity projectile, float damage, int heLevel, Vec3 hitVec) {
        new CustomExplosion.Builder(projectile)
                .attacker(this.getShooter())
                .damage((float) ((0.9 * damage) * (1 + 0.1 * heLevel)))
                .radius((float) ((1.5 + 0.02 * damage) * (1 + 0.05 * heLevel)))
                .position(hitVec)
//                .bulletExplode()
                .explode();
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
    public static BlockHitResult rayTraceBlocks(Level world, ClipContext context, Predicate<BlockState> ignorePredicate) {
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

    public @Nullable Entity getShooter() {
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
            DamageHandler.doDamage(entity, isHeadshot ? ModDamageTypes.causeGunFireHeadshotAbsoluteDamage(this.level().registryAccess(), this, this.shooter)
                    : ModDamageTypes.causeGunFireAbsoluteDamage(this.level().registryAccess(), this, this.shooter), absoluteDamage * headShotModifier);
            entity.invulnerableTime = 0;

            // 大于1的穿甲对载具造成额外伤害
            if (entity instanceof VehicleEntity vehicle && this.bypassArmorRate > 1) {
                vehicle.hurt(ModDamageTypes.causeGunFireAbsoluteDamage(this.level().registryAccess(), this, this.shooter), absoluteDamage * (this.bypassArmorRate - 1) * 0.5f);
            }
        }
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

    public boolean isPenetrating() {
        return this.penetrating;
    }

    public void setPenetrating(boolean penetrating) {
        this.penetrating = penetrating;
    }

    /**
     * Builders
     */
    public ProjectileEntity shooter(@Nullable Entity shooter) {
        this.shooter = shooter;
        return this;
    }

    public ProjectileEntity damage(float damage) {
        this.damage = damage;
        return this;
    }

    public ProjectileEntity velocity(float velocity) {
        this.velocity = velocity;
        return this;
    }

    public ProjectileEntity headShot(float headShot) {
        this.headShot = headShot;
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
