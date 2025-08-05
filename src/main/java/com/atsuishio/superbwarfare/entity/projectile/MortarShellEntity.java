package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public class MortarShellEntity extends FastThrowableProjectile implements GeoEntity, ExplosiveProjectile {

    private float damage = 50;
    private float explosionDamage = ExplosionConfig.MORTAR_SHELL_EXPLOSION_DAMAGE.get();
    private int life = 600;
    private float radius = ExplosionConfig.MORTAR_SHELL_EXPLOSION_RADIUS.get();
    private float gravity = 0.13f;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private Potion potion = Potions.EMPTY;
    private final Set<MobEffectInstance> effects = Sets.newHashSet();

    public MortarShellEntity(EntityType<? extends MortarShellEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public MortarShellEntity(EntityType<? extends MortarShellEntity> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world);
        this.noCulling = true;
    }

    public MortarShellEntity(LivingEntity entity, Level level) {
        super(ModEntities.MORTAR_SHELL.get(), entity, level);
        this.noCulling = true;
    }

    public MortarShellEntity(LivingEntity entity, Level world, float explosionDamage) {
        super(ModEntities.MORTAR_SHELL.get(), entity, world);
        this.noCulling = true;

        this.explosionDamage = explosionDamage;
    }

    public MortarShellEntity(LivingEntity entity, Level world, float explosionDamage, float radius) {
        super(ModEntities.MORTAR_SHELL.get(), entity, world);
        this.noCulling = true;

        this.explosionDamage = explosionDamage;
        this.radius = radius;
    }

    public MortarShellEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.MORTAR_SHELL.get(), level);
    }

    public void setEffectsFromItem(ItemStack pStack) {
        if (pStack.is(ModItems.POTION_MORTAR_SHELL.get())) {
            this.potion = PotionUtils.getPotion(pStack);
            Collection<MobEffectInstance> collection = PotionUtils.getCustomEffects(pStack);
            if (!collection.isEmpty()) {
                for (MobEffectInstance mobeffectinstance : collection) {
                    this.effects.add(new MobEffectInstance(mobeffectinstance));
                }
            }
        } else if (pStack.is(ModItems.MORTAR_SHELL.get())) {
            this.potion = Potions.EMPTY;
            this.effects.clear();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", this.damage);
        pCompound.putFloat("ExplosionDamage", this.explosionDamage);
        pCompound.putInt("Life", this.life);
        pCompound.putFloat("Radius", this.radius);

        if (this.potion != Potions.EMPTY) {
            pCompound.putString("Potion", Objects.requireNonNullElse(ForgeRegistries.POTIONS.getKey(this.potion), "empty").toString());
        }

        if (!this.effects.isEmpty()) {
            ListTag listtag = new ListTag();
            for (MobEffectInstance mobeffectinstance : this.effects) {
                listtag.add(mobeffectinstance.save(new CompoundTag()));
            }
            pCompound.put("CustomPotionEffects", listtag);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        if (pCompound.contains("Damage")) {
            this.damage = pCompound.getFloat("Damage");
        } else {
            this.damage = 50f;
        }

        if (pCompound.contains("ExplosionDamage")) {
            this.explosionDamage = pCompound.getFloat("ExplosionDamage");
        } else {
            this.explosionDamage = ExplosionConfig.MORTAR_SHELL_EXPLOSION_DAMAGE.get();
        }

        if (pCompound.contains("Life")) {
            this.life = pCompound.getInt("Life");
        } else {
            this.life = 600;
        }

        if (pCompound.contains("Radius")) {
            this.radius = pCompound.getFloat("Radius");
        } else {
            this.radius = ExplosionConfig.MORTAR_SHELL_EXPLOSION_RADIUS.get();
        }

        if (pCompound.contains("Potion", 8)) {
            this.potion = PotionUtils.getPotion(pCompound);
        }

        this.effects.addAll(PotionUtils.getCustomEffects(pCompound));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.MORTAR_SHELL.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    public void onHitEntity(EntityHitResult entityHitResult) {
        if (this.tickCount > 1) {
            Entity entity = entityHitResult.getEntity();
            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);
            if (this.level() instanceof ServerLevel) {
                causeExplode(entityHitResult.getLocation());
                this.createAreaCloud(this.level(), entityHitResult.getLocation());
            }
            this.discard();
        }
    }

    @Override
    public void onHitBlock(BlockHitResult blockHitResult) {
        BlockPos resultPos = blockHitResult.getBlockPos();
        BlockState state = this.level().getBlockState(resultPos);

        if (this.level() instanceof ServerLevel) {
            float hardness = this.level().getBlockState(resultPos).getBlock().defaultDestroyTime();
            if (hardness != -1) {
                if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
                    this.level().destroyBlock(resultPos, true);
                }
            }
        }

        if (state.getBlock() instanceof BellBlock bell) {
            bell.attemptToRing(this.level(), resultPos, blockHitResult.getDirection());
        }
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel) {
            if (this.tickCount > 1) {
                causeExplode(blockHitResult.getLocation());
                this.createAreaCloud(this.level(), blockHitResult.getLocation());
            }
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel && tickCount > 1) {
            double l = getDeltaMovement().length();
            for (double i = 0; i < l; i++) {
                Vec3 startPos = new Vec3(this.xo, this.yo, this.zo);
                Vec3 pos = startPos.add(getDeltaMovement().normalize().scale(i));
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0.001, true);
            }
        }
        if (this.tickCount > this.life || this.isInWater()) {
            if (this.level() instanceof ServerLevel) {
                causeExplode(position());
                this.createAreaCloud(this.level(), position());
            }
            this.discard();
        }
    }

    @Override
    public void causeExplode(Vec3 vec3) {
        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeProjectileExplosionDamage(this.level().registryAccess(),
                        this,
                        this.getOwner()),
                explosionDamage,
                vec3.x,
                vec3.y,
                vec3.z,
                radius,
                ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).
                setDamageMultiplier(1.25f);
        explosion.explode();
        net.minecraftforge.event.ForgeEventFactory.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnMediumExplosionParticles(this.level(), vec3);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private void createAreaCloud(Level level, Vec3 pos) {
        if (this.potion == Potions.EMPTY) return;

        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
        cloud.setPotion(this.potion);
        cloud.setDuration((int) this.explosionDamage);
        cloud.setRadius(this.radius);
        if (this.getOwner() instanceof LivingEntity living) {
            cloud.setOwner(living);
        }
        level.addFreshEntity(cloud);
    }

    @Override
    public SoundEvent getSound() {
        return ModSounds.SHELL_FLY.get();
    }

    @Override
    public float getVolume() {
        return 0.06f;
    }

    @Override
    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void setExplosionDamage(float explosionDamage) {
        this.explosionDamage = explosionDamage;
    }

    @Override
    public void setExplosionRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public float getGravity() {
        return this.gravity;
    }

    @Override
    public void setGravity(float gravity) {
        this.gravity = gravity;
    }
}
