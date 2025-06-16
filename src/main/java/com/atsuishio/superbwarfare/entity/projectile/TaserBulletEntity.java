package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class TaserBulletEntity extends AbstractArrow implements GeoEntity {

    private float damage = 1f;
    private int volt = 0;
    private int wireLength = 0;
    private boolean stop = false;
    public static final ItemStack PROJECTILE_ITEM = new ItemStack(Items.AIR);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TaserBulletEntity(LivingEntity entity, Level level, float damage, int volt, int wireLength) {
        super(ModEntities.TASER_BULLET.get(), level);
        this.damage = damage;
        this.volt = volt;
        this.wireLength = wireLength;
    }

    public TaserBulletEntity(LivingEntity entity, Level level, float damage) {
        super(ModEntities.TASER_BULLET.get(), level);
        this.damage = damage;
    }

    public TaserBulletEntity(EntityType<? extends TaserBulletEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public int getVolt() {
        return volt;
    }

    public void setVolt(int volt) {
        this.volt = volt;
    }

    public int getWireLength() {
        return wireLength;
    }

    public void setWireLength(int wireLength) {
        this.wireLength = wireLength;
    }

    @Override
    public void playerTouch(@NotNull Player pEntity) {
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return PROJECTILE_ITEM;
    }

    @Override
    protected @NotNull ItemStack getDefaultPickupItem() {
        return PROJECTILE_ITEM;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
            return;
        if (this.getOwner() instanceof LivingEntity living) {
            if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
            }
        }
        if (entity instanceof LivingEntity living) {
            entity.invulnerableTime = 0;
            entity.hurt(ModDamageTypes.causeShockDamage(this.level().registryAccess(), this.getOwner()), this.damage);
            if (living instanceof Player player && player.isCreative()) {
                return;
            }
            if (!living.level().isClientSide()) {
                if (living instanceof Creeper creeper && living.level() instanceof ServerLevel serverLevel) {
                    creeper.thunderHit(serverLevel, new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel));
                } else {
                    living.addEffect(new MobEffectInstance(ModMobEffects.SHOCK, 100 + volt * 30, volt), this.getOwner());
                }
            }
        }
        this.discard();
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        BlockPos resultPos = blockHitResult.getBlockPos();
        BlockState state = this.level().getBlockState(resultPos);
        if (state.getBlock() instanceof BellBlock bell) {
            bell.attemptToRing(this.level(), resultPos, blockHitResult.getDirection());
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getOwner() != null && this.position().distanceTo(this.getOwner().position()) > 10 + 4 * wireLength && !stop) {
            stop = true;
            this.setDeltaMovement(new Vec3(0, 0, 0));
        }

        if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}

