package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public class MortarShellEntity extends FastThrowableProjectile implements GeoEntity {

    public enum Type {
        NORMAL, WP
    }

    private Type type = Type.NORMAL;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Potion potion = Potions.EMPTY;
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

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

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
        Entity entity = entityHitResult.getEntity();
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
            return;
        if (this.level() instanceof ServerLevel && this.tickCount > 1) {
            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                    NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                }
            }

            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);

            if (type == Type.WP) {
                findNearEntity(entityHitResult.getLocation(), getOwner());
            }

            if (this.level() instanceof ServerLevel) {
                causeExplode(entityHitResult.getLocation());
                this.createAreaCloud(this.level(), entityHitResult.getLocation());
            }
            this.discard();
        }
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        BlockPos resultPos = blockHitResult.getBlockPos();
        BlockState state = this.level().getBlockState(resultPos);

        if (state.getBlock() instanceof BellBlock bell) {
            bell.attemptToRing(this.level(), resultPos, blockHitResult.getDirection());
        }

        if (type == Type.WP) {
            findNearEntity(blockHitResult.getLocation(), getOwner());
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel) {
            if (this.tickCount > 1) {
                causeExplode(blockHitResult.getLocation());
                this.createAreaCloud(this.level(), blockHitResult.getLocation());
            }
        }
        this.discard();
    }

    public void findNearEntity(Vec3 pos, Entity shooter) {
        if (this.level() instanceof ServerLevel) {

            var entities = new SeekTool.Builder(shooter)
                    .withinRange(pos, explosionRadius)
                    .notItsVehicle()
                    .baseFilter()
                    .noVehicle()
                    .build();

            for (Entity e : entities) {
                var dis = pos.distanceTo(e.position());

                if (e instanceof LivingEntity living && checkNoClip(e, pos)) {
                    if (living instanceof Player player && player.isCreative()) {
                        return;
                    }
                    if (!living.level().isClientSide()) {
                        living.addEffect(new MobEffectInstance(ModMobEffects.PHOSPHORUS_FIRE.get(), (int) (300 - 30 * dis), (int) Math.max(explosionRadius - dis, 0)), this.getOwner());
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (getDeltaMovement().lengthSqr() > 25) {
            mediumTrail();
        }

        if (type == Type.WP) {
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

    public void createAreaCloud(Level level, Vec3 pos) {
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
