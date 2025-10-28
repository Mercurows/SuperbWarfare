package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.client.particle.CustomSmokeOption;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class M18SmokeGrenadeEntity extends FastThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int count = 8;
    private int fuse = 100;
    private float rColor = 1.0f;
    private float gColor = 1.0f;
    private float bColor = 1.0f;

    public M18SmokeGrenadeEntity(EntityType<? extends M18SmokeGrenadeEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public M18SmokeGrenadeEntity(EntityType<? extends M18SmokeGrenadeEntity> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world);
        this.noCulling = true;
    }

    public M18SmokeGrenadeEntity(LivingEntity entity, Level level, int fuse) {
        super(ModEntities.M18_SMOKE_GRENADE.get(), entity, level);
        this.noCulling = true;
        this.fuse = fuse;
    }

    public M18SmokeGrenadeEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.M18_SMOKE_GRENADE.get(), level);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Fuse", this.fuse);
        pCompound.putInt("Count", this.count);
        pCompound.putFloat("RColor", this.rColor);
        pCompound.putFloat("GColor", this.gColor);
        pCompound.putFloat("BColor", this.bColor);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Fuse")) {
            this.fuse = pCompound.getInt("Fuse");
        }
        if (pCompound.contains("Count")) {
            this.count = Mth.clamp(pCompound.getInt("Count"), 1, 64);
        }
        if (pCompound.contains("RColor")) {
            this.rColor = pCompound.getFloat("RColor");
        }
        if (pCompound.contains("GColor")) {
            this.gColor = pCompound.getFloat("GColor");
        }
        if (pCompound.contains("BColor")) {
            this.bColor = pCompound.getFloat("BColor");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.M18_SMOKE_GRENADE.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    protected void onHit(HitResult result) {
        switch (result.getType()) {
            case BLOCK:
                BlockHitResult blockResult = (BlockHitResult) result;
                BlockPos resultPos = blockResult.getBlockPos();
                BlockState state = this.level().getBlockState(resultPos);
                SoundEvent event = state.getBlock().getSoundType(state, this.level(), resultPos, this).getBreakSound();
                double speed = this.getDeltaMovement().length();
                if (speed > 0.1) {
                    this.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z, event, SoundSource.AMBIENT, 1.0F, 1.0F);
                }
                this.bounce(blockResult.getDirection());

                if (state.getBlock() instanceof BellBlock bell) {
                    bell.attemptToRing(this.level(), resultPos, blockResult.getDirection());
                }

                break;
            case ENTITY:
                EntityHitResult entityResult = (EntityHitResult) result;
                Entity entity = entityResult.getEntity();
                if (entity == this.getOwner() || entity == this.getVehicle()) return;
                double speed_e = this.getDeltaMovement().length();
                if (speed_e > 0.1) {
                    if (this.getOwner() instanceof LivingEntity living) {
                        if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                            living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                            NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                        }
                    }
                    entity.hurt(entity.damageSources().thrown(this, this.getOwner()), 1);
                }
                this.bounce(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.25, 1.0, 0.25));
                break;
            default:
                break;
        }
    }

    private void bounce(Direction direction) {
        switch (direction.getAxis()) {
            case X:
                this.setDeltaMovement(this.getDeltaMovement().multiply(-0.5, 0.75, 0.75));
                break;
            case Y:
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.75, -0.25, 0.75));
                if (this.getDeltaMovement().y() < this.getGravity()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0, 1));
                }
                break;
            case Z:
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.75, 0.75, -0.5));
                break;
        }
    }

    @Override
    public void tick() {
        super.tick();
        --this.fuse;

        if (tickCount > 200) {
            this.discard();
        }

        if (fuse == -20) {
            releaseSmoke();
        }

        if (fuse == 0) {
            this.level().playSound(null, this, ModSounds.SM0KE_GRENADE_RELEASE.get(), this.getSoundSource(), 2, 1);
        }

        if (fuse <= 0 && tickCount % 2 == 0) {
            if (this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, new CustomSmokeOption(this.rColor, this.gColor, this.bColor), this.getX(), this.getY() + getBbHeight(), this.getZ(),
                        8, 0.075, 0.01, 0.075, 0.08, true);
            }
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            ParticleTool.sendParticle(serverLevel, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
                    1, 0, 0, 0, 0.01, true);
        }
    }

    public void releaseSmoke() {
        var vec3 = new Vec3(1, 0.05, 0);

        for (int i = 0; i < this.count; i++) {
            var decoy = new SmokeDecoyEntity(ModEntities.SMOKE_DECOY.get(), this.level(), false);
            decoy.setPos(this.getX(), this.getY() + getBbHeight(), this.getZ());
            decoy.decoyShoot(this, vec3.yRot(i * (360f / this.count) * Mth.DEG_TO_RAD), 1.5f, 5);
            this.level().addFreshEntity(decoy);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public float getGravity() {
        return 0.07F;
    }

    public float getRed() {
        return rColor;
    }

    public float getGreen() {
        return gColor;
    }

    public float getBlue() {
        return bColor;
    }

    public M18SmokeGrenadeEntity setColor(float r, float g, float b) {
        this.rColor = r;
        this.gColor = g;
        this.bColor = b;
        return this;
    }
}
