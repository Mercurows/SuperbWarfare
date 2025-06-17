package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.ChunkLoadTool;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MortarShellEntity extends FastThrowableProjectile implements GeoEntity, ExplosiveProjectile {

    private float damage = 50;
    private float explosionDamage = ExplosionConfig.MORTAR_SHELL_EXPLOSION_DAMAGE.get();
    private int life = 600;
    private float radius = ExplosionConfig.MORTAR_SHELL_EXPLOSION_RADIUS.get();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public Set<Long> loadedChunks = new HashSet<>();

    private Potion potion = Potions.WATER.value();
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
    }

    public MortarShellEntity(LivingEntity entity, Level world, float explosionDamage) {
        super(ModEntities.MORTAR_SHELL.get(), entity, world);
        this.explosionDamage = explosionDamage;
    }

    public MortarShellEntity(LivingEntity entity, Level world, float explosionDamage, float radius) {
        super(ModEntities.MORTAR_SHELL.get(), entity, world);
        this.explosionDamage = explosionDamage;
        this.radius = radius;
    }

    public void setEffectsFromItem(ItemStack stack) {
        if (stack.is(ModItems.POTION_MORTAR_SHELL.get())) {
            var potionContents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            this.potion = potionContents.potion().orElse(Potions.WATER).value();

            for (MobEffectInstance mobeffectinstance : potionContents.getAllEffects()) {
                this.effects.add(new MobEffectInstance(mobeffectinstance));
            }
        } else if (stack.is(ModItems.MORTAR_SHELL.get())) {
            this.potion = Potions.WATER.value();
            this.effects.clear();
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", this.explosionDamage);
        pCompound.putInt("Life", this.life);
        pCompound.putFloat("Radius", this.radius);

        ListTag listTag = new ListTag();
        for (long chunkPos : this.loadedChunks) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Pos", chunkPos);
            listTag.add(tag);
        }
        pCompound.put("Chunks", listTag);

        if (this.potion != Potions.WATER.value()) {
            pCompound.putString("Potion", Objects.requireNonNullElse(BuiltInRegistries.POTION.getKey(this.potion), "empty").toString());
        }

        if (!this.effects.isEmpty()) {
            ListTag listtag = new ListTag();
            for (MobEffectInstance mobeffectinstance : this.effects) {
                listtag.add(mobeffectinstance.save());
            }
            pCompound.put("CustomPotionEffects", listtag);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Damage")) {
            this.explosionDamage = pCompound.getFloat("Damage");
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

        if (pCompound.contains("Chunks")) {
            ListTag listTag = pCompound.getList("Chunks", 10);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag tag = listTag.getCompound(i);
                this.loadedChunks.add(tag.getLong("Pos"));
            }
        }

        if (pCompound.contains("Potion", 8)) {
            var tagName = pCompound.getString("Potion");
            this.potion = BuiltInRegistries.POTION.get(ResourceLocation.tryParse(tagName));
        }

        var listTag = pCompound.getList("CustomPotionEffects", 10);
        for (int i = 0; i < listTag.size(); ++i) {
            CompoundTag compoundtag = listTag.getCompound(i);
            MobEffectInstance instance = MobEffectInstance.load(compoundtag);
            if (instance != null) {
                this.effects.add(instance);
            }
        }
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.MORTAR_SHELL.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    public void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        if (this.tickCount > 1) {
            Entity entity = entityHitResult.getEntity();
            entity.hurt(ModDamageTypes.causeCannonFireDamage(this.level().registryAccess(), this, this.getOwner()), 50);
            if (this.level() instanceof ServerLevel) {
                causeExplode(entityHitResult.getLocation());
                this.createAreaCloud(this.level());
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
                this.createAreaCloud(this.level());
            }
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.xo, this.yo, this.zo,
                    1, 0, 0, 0, 0.001, true);

            // 更新需要加载的区块
            ChunkLoadTool.updateLoadedChunks(serverLevel, this, this.loadedChunks);
        }
        if (this.tickCount > this.life || this.isInWater()) {
            if (this.level() instanceof ServerLevel) {
                causeExplode(position());
                this.createAreaCloud(this.level());
            }
            this.discard();
        }
    }

    private void causeExplode(Vec3 vec3) {
        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(),
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
        EventHooks.onExplosionStart(this.level(), explosion);
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

    @Override
    protected double getDefaultGravity() {
        return 0.146F;
    }

    @Override
    public void onRemovedFromLevel() {
        if (this.level() instanceof ServerLevel serverLevel) {
            ChunkLoadTool.unloadAllChunks(serverLevel, this, this.loadedChunks);
        }
        super.onRemovedFromLevel();
    }

    private void createAreaCloud(Level level) {
        if (this.potion == Potions.WATER.value()) return;

        AreaEffectCloud cloud = new AreaEffectCloud(level, this.getX() + 0.75 * getDeltaMovement().x, this.getY() + 0.5 * getBbHeight() + 0.75 * getDeltaMovement().y, this.getZ() + 0.75 * getDeltaMovement().z);

        for (MobEffectInstance effect : this.effects) {
            cloud.addEffect(effect);
        }
        cloud.setDuration((int) this.explosionDamage);
        cloud.setRadius(this.radius);
        if (this.getOwner() instanceof LivingEntity living) {
            cloud.setOwner(living);
        }
        level.addFreshEntity(cloud);
    }

    @Override
    public @NotNull SoundEvent getSound() {
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
}
