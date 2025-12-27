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
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;
import java.util.Set;

public class MortarShellEntity extends FastThrowableProjectile implements GeoEntity {

    public enum Type {
        NORMAL, WP
    }

    private Type type = Type.NORMAL;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Potion potion = Potions.WATER.value();
    private final Set<MobEffectInstance> effects = Sets.newHashSet();

    public MortarShellEntity(EntityType<? extends MortarShellEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.damage = 60;
        this.explosionDamage = 100;
        this.explosionRadius = 8;
    }

    public MortarShellEntity(EntityType<? extends MortarShellEntity> type, double x, double y, double z, Level level, float gravity) {
        super(type, x, y, z, level);
        this.noCulling = true;
        this.damage = 60;
        this.explosionDamage = 100;
        this.explosionRadius = 8;
        this.gravity = gravity;
    }

    public MortarShellEntity(LivingEntity entity, Level level, float damage, float explosionDamage, float explosionRadius) {
        super(ModEntities.MORTAR_SHELL.get(), entity, level);
        this.noCulling = true;
        this.damage = damage;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
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

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

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
    public void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (this.tickCount > 1) {
            Entity entity = entityHitResult.getEntity();
            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);
            if (this.level() instanceof ServerLevel) {
                causeExplode(entityHitResult.getLocation());
                this.createAreaCloud(this.level(), position());
            }
            this.discard();
        }
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
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
                this.createAreaCloud(this.level(), position());
            }
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        mediumTrail();

        if (type == Type.WP) {
            // 使用Minecraft内置的光线追踪进行碰撞检测
            BlockHitResult hitResult = level().clip(new ClipContext(
                    position(),
                    position().add(getDeltaMovement().scale(8)),
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.ANY,
                    this
            ));

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                releaseWp(getOwner());
            }
        }
    }

    private void releaseWp(Entity shooter) {
        if (level() instanceof ServerLevel serverLevel) {
            ParticleTool.spawnMediumExplosionParticles(serverLevel, position());
            for (int index0 = 0; index0 < 32; index0++) {
                WhitePhosphorusProjectileEntity whitePhosphorusProjectileEntity = new WhitePhosphorusProjectileEntity(shooter, serverLevel);

                whitePhosphorusProjectileEntity.setPos(position().x, position().y, position().z);
                whitePhosphorusProjectileEntity.shoot(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z, (float) (random.nextFloat() * 0.05f + 0.1f * getDeltaMovement().length()),
                        35);
                serverLevel.addFreshEntity(whitePhosphorusProjectileEntity);
            }
            discard();
        }
    }

    @Override
    public CustomExplosion.@NotNull Builder buildExplosion(@NotNull Vec3 vec3) {
        return super.buildExplosion(vec3).damageMultiplier(1.25F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public void createAreaCloud(Level level, Vec3 pos) {
        if (this.potion == Potions.WATER.value()) return;

        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
        for (MobEffectInstance effect : this.effects) {
            cloud.addEffect(effect);
        }
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
