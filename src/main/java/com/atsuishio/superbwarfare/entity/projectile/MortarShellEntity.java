package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public class MortarShellEntity extends FastThrowableProjectile implements GeoEntity, ExplosiveProjectile {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int life = 600;
    private Potion potion = Potions.EMPTY;
    private final Set<MobEffectInstance> effects = Sets.newHashSet();

    public MortarShellEntity(EntityType<? extends MortarShellEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.damage = 50;
        this.explosionDamage = ExplosionConfig.MORTAR_SHELL_EXPLOSION_DAMAGE.get();
        this.explosionRadius = ExplosionConfig.MORTAR_SHELL_EXPLOSION_RADIUS.get();
        this.gravity = 0.13f;
    }

    public MortarShellEntity(EntityType<? extends MortarShellEntity> type, double x, double y, double z, Level level, float gravity) {
        super(type, x, y, z, level);
        this.noCulling = true;
        this.damage = 50;
        this.explosionDamage = ExplosionConfig.MORTAR_SHELL_EXPLOSION_DAMAGE.get();
        this.explosionRadius = ExplosionConfig.MORTAR_SHELL_EXPLOSION_RADIUS.get();
        this.gravity = gravity;
    }

    public MortarShellEntity(LivingEntity entity, Level level, float gravity) {
        super(ModEntities.MORTAR_SHELL.get(), entity, level);
        this.noCulling = true;
        this.damage = 50;
        this.explosionDamage = ExplosionConfig.MORTAR_SHELL_EXPLOSION_DAMAGE.get();
        this.explosionRadius = ExplosionConfig.MORTAR_SHELL_EXPLOSION_RADIUS.get();
        this.gravity = gravity;
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
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Life", this.life);

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
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        if (pCompound.contains("Life")) {
            this.life = pCompound.getInt("Life");
        } else {
            this.life = 600;
        }

        if (pCompound.contains("Potion", 8)) {
            this.potion = PotionUtils.getPotion(pCompound);
        }

        this.effects.addAll(PotionUtils.getCustomEffects(pCompound));
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.MORTAR_SHELL.get();
    }

    @Override
    public void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
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
        super.onHitBlock(blockHitResult);
        BlockPos resultPos = blockHitResult.getBlockPos();
        BlockState state = this.level().getBlockState(resultPos);

        if (this.level() instanceof ServerLevel && ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
            float hardness = this.level().getBlockState(resultPos).getBlock().defaultDestroyTime();
            if (hardness != -1) {
                this.level().destroyBlock(resultPos, true);
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

        mediumTrail();

        if (this.tickCount > this.life || this.isInWater()) {
            if (this.level() instanceof ServerLevel) {
                causeExplode(position());
                this.createAreaCloud(this.level(), position());
            }
            this.discard();
        }
    }

    @Override
    public CustomExplosion.Builder buildExplosion(Vec3 vec3) {
        return super.buildExplosion(vec3).damageMultiplier(1.25F);
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
        cloud.setRadius(this.explosionRadius);
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
    public boolean forceLoadChunk() {
        return true;
    }
}
