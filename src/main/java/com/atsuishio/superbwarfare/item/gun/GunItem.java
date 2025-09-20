package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.particle.BulletDecalOption;
import com.atsuishio.superbwarfare.client.screens.WeaponEditScreen;
import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.data.Prop;
import com.atsuishio.superbwarfare.data.gun.*;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.data.launchable.LaunchableEntityTool;
import com.atsuishio.superbwarfare.data.launchable.ShootData;
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback;
import com.atsuishio.superbwarfare.entity.projectile.CustomDamageProjectile;
import com.atsuishio.superbwarfare.entity.projectile.CustomGravityEntity;
import com.atsuishio.superbwarfare.entity.projectile.ExplosiveProjectile;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.EnergyStorageItem;
import com.atsuishio.superbwarfare.item.ItemScreenProvider;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.RangeTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.atsuishio.superbwarfare.world.phys.EntityResult;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.atsuishio.superbwarfare.tools.EntityFindUtil.findEntity;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public abstract class GunItem extends Item implements ItemScreenProvider, GunPropertyModifier, EnergyStorageItem {

    protected final RandomSource random = RandomSource.create();

    @Override
    public int getMaxEnergy(ItemStack stack) {
        return GunData.from(stack).get(GunProp.MAX_ENERGY);
    }

    @Override
    public int getMaxReceiveEnergy(ItemStack stack) {
        return GunData.from(stack).get(GunProp.MAX_RECEIVE_ENERGY);
    }

    @Override
    public int getMaxExtractEnergy(ItemStack stack) {
        return GunData.from(stack).get(GunProp.MAX_EXTRACT_ENERGY);
    }

    public GunItem(Properties properties) {
        super(properties.stacksTo(1));

        addReloadTimeBehavior(this.reloadTimeBehaviors);
        addBoltTimeBehavior(this.boltTimeBehaviors);

        setProperty(GunProp.DAMAGE, (data, v) -> v + getCustomDamage(data));
        setProperty(GunProp.HEADSHOT, (data, v) -> v + getCustomHeadshot(data));
        setProperty(GunProp.BYPASSES_ARMOR, (data, v) -> v + getCustomBypassArmor(data));
        setProperty(GunProp.MAGAZINE, (data, v) -> v + getCustomMagazine(data));
        setProperty(GunProp.DEFAULT_ZOOM, (data, v) -> v + getCustomZoom(data));
        setProperty(GunProp.RPM, (data, v) -> v + getCustomRPM(data));
        setProperty(GunProp.WEIGHT, (data, v) -> v + getCustomWeight(data));
        setProperty(GunProp.VELOCITY, (data, v) -> v + getCustomVelocity(data));
        setProperty(GunProp.SOUND_RADIUS, (data, v) -> v + getCustomSoundRadius(data));
        setProperty(GunProp.BOLT_ACTION_TIME, (data, v) -> v + getCustomBoltActionTime(data));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        var data = GunData.from(stack);
        if (data.get(GunProp.MAX_DURABILITY) > 0) return super.isBarVisible(stack);

        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.getEnergyStored() > 0 && cap.getMaxEnergyStored() > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var data = GunData.from(stack);
        if (data.get(GunProp.MAX_DURABILITY) > 0) {
            return super.getBarWidth(stack);
        }

        if (data.get(GunProp.MAX_ENERGY) > 0) {
            var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            return Math.round((float) (cap != null ? cap.getEnergyStored() : 0) * 13.0F / GunData.from(stack).get(GunProp.MAX_ENERGY));
        }

        return super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        var data = GunData.from(stack);
        if (data.get(GunProp.MAX_DURABILITY) > 0) {
            return super.getBarColor(stack);
        }

        if (data.get(GunProp.MAX_ENERGY) > 0) {
            return getEnergyBarColor(data);
        }

        return super.getBarColor(stack);
    }

    public int getEnergyBarColor(GunData data) {
        return 0x95E9FF;
    }

    protected final Map<GunProp<?>, Prop.PropModifyContext<GunData, DefaultGunData, ?>> propertyModifiers = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Map<GunProp<?>, Prop.PropModifyContext<GunData, DefaultGunData, ?>> getPropModifiers() {
        return this.propertyModifiers;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(stack.getItem() instanceof GunItem) || level.isClientSide) return;

        if (level instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
        }

        var data = GunData.from(stack);

        var inMainHand = entity instanceof LivingEntity living && living.getMainHandItem() == stack;
        data.tick(entity, inMainHand);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    private static final ResourceLocation SPEED_ID = Mod.loc("gun_movement_speed");

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        var list = new ArrayList<>(super.getDefaultAttributeModifiers(stack).modifiers());
        var data = GunData.from(stack);

        // 移速
        list.add(new ItemAttributeModifiers.Entry(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(SPEED_ID,
                        -0.01f - 0.005f * data.get(GunProp.WEIGHT),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        ));

        // 近战伤害
        if (data.get(GunProp.MELEE_DAMAGE) > 0) {
            list.add(new ItemAttributeModifiers.Entry(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(BASE_ATTACK_DAMAGE_ID, data.get(GunProp.MELEE_DAMAGE), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
            ));
        }

        return new ItemAttributeModifiers(list, true);
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new GunImageComponent(pStack));
    }

    public Set<SoundEvent> getReloadSound() {
        return Set.of();
    }

    public ResourceLocation getGunIcon(ItemStack stack) {
        return getGunIcon(GunData.from(stack));
    }

    public ResourceLocation getGunIcon(GunData data) {
        return Mod.loc("textures/gun_icon/default_icon.png");
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public int getMaxDamage(@NotNull ItemStack stack) {
        var maxDurability = GunData.from(stack).get(GunProp.MAX_DURABILITY);

        if (maxDurability > 0) {
            if (!stack.has(DataComponents.MAX_DAMAGE) || !stack.has(DataComponents.DAMAGE)) {
                stack.set(DataComponents.MAX_DAMAGE, maxDurability);
                stack.set(DataComponents.DAMAGE, 0);
            }
        } else {
            stack.remove(DataComponents.MAX_DAMAGE);
        }
        return maxDurability;
    }

    /**
     * 开膛待击
     */
    public boolean isOpenBolt(GunData data) {
        return false;
    }

    /**
     * 是否允许额外往枪管里塞入一发子弹
     */
    public boolean hasBulletInBarrel(GunData data) {
        return false;
    }

    /**
     * 武器是否能更换枪管配件
     */
    public boolean hasCustomBarrel(GunData data) {
        return false;
    }

    public int[] getValidBarrels() {
        return new int[]{0, 1, 2};
    }

    /**
     * 武器是否能更换枪托配件
     */
    public boolean hasCustomGrip(GunData data) {
        return false;
    }

    public int[] getValidGrips() {
        return new int[]{0, 1, 2, 3};
    }

    /**
     * 武器是否能更换弹匣配件
     */
    public boolean hasCustomMagazine(GunData data) {
        return false;
    }

    public int[] getValidMagazines() {
        return new int[]{0, 1, 2};
    }

    /**
     * 武器是否能更换瞄具配件
     */
    public boolean hasCustomScope(GunData data) {
        return false;
    }

    public int[] getValidScopes() {
        return new int[]{0, 1, 2, 3};
    }

    /**
     * 武器是否能更换枪托配件
     */
    public boolean hasCustomStock(GunData data) {
        return false;
    }

    public int[] getValidStocks() {
        return new int[]{0, 1, 2};
    }

    /**
     * 武器是否有脚架
     */
    public boolean hasBipod(GunData data) {
        return false;
    }

    /**
     * 武器是否会抛壳
     */
    public boolean canEjectShell(GunData data) {
        return false;
    }

    /**
     * 武器是否能进行近战攻击
     */
    public boolean hasMeleeAttack(GunData data) {
        return data.get(GunProp.MELEE_DAMAGE) > 0;
    }

    /**
     * 获取额外伤害加成
     */
    public double getCustomDamage(GunData data) {
        return 0;
    }

    /**
     * 获取额外爆头伤害加成
     */
    public double getCustomHeadshot(GunData data) {
        return 0;
    }

    /**
     * 获取额外护甲穿透加成
     */
    public double getCustomBypassArmor(GunData data) {
        return 0;
    }

    /**
     * 获取额外弹匣容量加成
     */
    public int getCustomMagazine(GunData data) {
        return 0;
    }

    /**
     * 获取额外缩放倍率加成
     */
    public double getCustomZoom(GunData data) {
        return 0;
    }

    /**
     * 获取额外RPM加成
     */
    public int getCustomRPM(GunData data) {
        return 0;
    }

    /**
     * 获取额外总重量加成
     */
    public double getCustomWeight(GunData data) {
        var attachment = data.attachment;

        double scopeWeight = switch (attachment.get(AttachmentType.SCOPE)) {
            case 1 -> 0.5;
            case 2 -> 1;
            case 3 -> 1.5;
            default -> 0;
        };

        double barrelWeight = switch (attachment.get(AttachmentType.BARREL)) {
            case 1 -> 0.5;
            case 2 -> 1;
            default -> 0;
        };

        double magazineWeight = switch (attachment.get(AttachmentType.MAGAZINE)) {
            case 1 -> 1;
            case 2 -> 2;
            default -> 0;
        };

        double stockWeight = switch (attachment.get(AttachmentType.STOCK)) {
            case 1 -> -2;
            case 2 -> 1.5;
            default -> 0;
        };

        double gripWeight = switch (attachment.get(AttachmentType.GRIP)) {
            case 1, 2 -> 0.25;
            case 3 -> 1;
            default -> 0;
        };

        return scopeWeight + barrelWeight + magazineWeight + stockWeight + gripWeight;
    }

    /**
     * 获取额外弹速加成
     */
    public double getCustomVelocity(GunData data) {
        return 0;
    }

    /**
     * 获取额外音效半径加成
     */
    public double getCustomSoundRadius(GunData data) {
        return data.attachment.get(AttachmentType.BARREL) == 2 ? 0.6 : 1;
    }

    public int getCustomBoltActionTime(GunData data) {
        return 0;
    }

    /**
     * 是否允许缩放
     */
    public boolean canAdjustZoom(GunData data) {
        return false;
    }

    /**
     * 是否允许切换瞄具
     */
    public boolean canSwitchScope(GunData data) {
        return false;
    }

    public final Map<Integer, Consumer<GunData>> reloadTimeBehaviors = new HashMap<>();
    public final Map<Integer, Consumer<GunData>> boltTimeBehaviors = new HashMap<>();

    /**
     * 添加达到指定换弹时间时的额外行为
     */
    public void addReloadTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
    }

    /**
     * 添加达到指定拉栓/泵动时间时的额外行为
     */
    public void addBoltTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
    }

    /**
     * 判断武器能否开火
     */
    public boolean canShoot(GunData data, @Nullable Entity shooter) {
        return data.get(GunProp.PROJECTILE_AMOUNT) > 0
                && !data.overHeat.get()
                && !data.reloading()
                && !data.charging()
                && !data.bolt.needed.get()
                && data.hasEnoughAmmoToShoot(shooter);
    }

    /**
     * 判断武器能否瞄准
     */
    public boolean canZoom(GunData data, @Nullable Entity shooter) {
        return true;
    }

    /**
     * 服务端在开火前的额外行为
     */
    public void beforeShoot(
            @Nullable Entity shooter,
            @NotNull ServerLevel level,
            @NotNull Vec3 shootPosition,
            @NotNull Vec3 shootDirection,
            @NotNull GunData data,
            double spread,
            boolean zoom
    ) {
        // 空仓挂机
        if (data.currentAvailableShots(shooter) == 1) {
            data.holdOpen.set(true);
        }

        // 判断是否为栓动武器（BoltActionTime > 0），并在开火后给一个需要上膛的状态
        if (data.get(GunProp.BOLT_ACTION_TIME) > 0 && data.hasEnoughAmmoToShoot(shooter)) {
            data.bolt.needed.set(true);
        }
    }

    /**
     * 服务端在开火后的额外行为
     */
    public void afterShoot(
            @Nullable Entity shooter,
            @NotNull ServerLevel level,
            @NotNull Vec3 shootPosition,
            @NotNull Vec3 shootDirection,
            @NotNull GunData data,
            double spread,
            boolean zoom,
            @Nullable UUID uuid
    ) {
        if (!data.useBackpackAmmo()) {
            data.ammo.set(data.ammo.get() - data.get(GunProp.AMMO_COST_PER_SHOOT));
            data.isEmpty.set(true);
            data.closeStrike.set(true);
        } else {
            data.consumeBackupAmmo(shooter, data.get(GunProp.AMMO_COST_PER_SHOOT));
        }

        if (!data.hasEnoughAmmoToShoot(shooter)) {
            data.burstAmount.reset();
        }

        var stack = data.stack();
        if (this.getMaxDamage(stack) > 0) {
            if (shooter instanceof LivingEntity living) {
                stack.hurtAndBreak(data.get(GunProp.DURABILITY_PER_SHOOT), living, EquipmentSlot.MAINHAND);
            } else {
                stack.hurtAndBreak(data.get(GunProp.DURABILITY_PER_SHOOT), level, (LivingEntity) null, item -> {
                });
            }
        }

        // 真实后座（
        if (shooter != null && data.get(GunProp.RECOIL) != 0) {
            shooter.setDeltaMovement(shooter.getDeltaMovement().add(shooter.getViewVector(1).scale(-data.get(GunProp.RECOIL))));
        }

        data.clearTempModifications();
    }

    public void shoot(@NotNull ServerLevel level, @NotNull Vec3 shootPosition, @NotNull Vec3 shootDirection, @NotNull GunData data, double spread, boolean zoom, @Nullable UUID uuid) {
        shoot(null, level, shootPosition, shootDirection, data, spread, zoom, uuid);
    }

    public void shoot(@NotNull GunData data, @NotNull Entity shooter, double spread, boolean zoom, UUID uuid) {
        if (shooter.level() instanceof ServerLevel server) {
            shoot(shooter, server, new Vec3(shooter.getX(), shooter.getEyeY(), shooter.getZ()), shooter.getLookAngle(), data, spread, zoom, uuid);
        }
    }

    /**
     * 服务端处理单次开火
     *
     * @param shooter        射击者
     * @param level          ServerLevel
     * @param shootPosition  子弹位置
     * @param shootDirection 射击方向
     * @param data           GunData
     * @param spread         子弹散布
     * @param zoom           是否开镜
     * @param uuid           已锁定实体UUID
     */
    public void shoot(
            @Nullable Entity shooter,
            @NotNull ServerLevel level,
            @NotNull Vec3 shootPosition,
            @NotNull Vec3 shootDirection,
            @NotNull GunData data,
            double spread,
            boolean zoom,
            @Nullable UUID uuid
    ) {
        if (!data.canShoot(shooter)) return;

        // 开火前事件
        data.item.beforeShoot(shooter, level, shootPosition, shootDirection, data, spread, zoom);

        int projectileAmount = data.get(GunProp.PROJECTILE_AMOUNT);

        // 生成所有子弹
        for (int index0 = 0; index0 < projectileAmount; index0++) {
            if (!shootBullet(shooter, level, shootPosition, shootDirection, data, spread, zoom, uuid)) return;
        }

        // n连发模式开火数据设置
        if (data.fireMode.get() == FireMode.BURST) {
            var amount = data.burstAmount.get();
            data.burstAmount.set(amount == 0 ? data.get(GunProp.BURST_AMOUNT) - 1 : Math.max(0, amount - 1));
        }

        // 添加热量
        data.heat.set(Mth.clamp(data.heat.get() + data.get(GunProp.HEAT_PER_SHOOT), 0, 100));

        // 过热
        if (data.heat.get() >= 100 && !data.overHeat.get()) {
            data.overHeat.set(true);
            if (shooter instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.MINIGUN_OVERHEAT.get(), 2f, 1f);
            }
        }

        playFireSounds(data, shooter, zoom);

        // 开火后事件
        data.item.afterShoot(shooter, level, shootPosition, shootDirection, data, spread, zoom, uuid);

        data.save();
    }

    /**
     * 播放开火音效
     */
    public void playFireSounds(GunData data, @Nullable Entity shooter, boolean zoom) {
        if (shooter == null) return;

        ItemStack stack = data.stack;
        String origin = stack.getItem().getDescriptionId();
        String name = origin.substring(origin.lastIndexOf(".") + 1);

        float pitch = data.heat.get() <= 75 ? 1 : (float) (1 - 0.02 * Math.abs(75 - data.heat.get()));

        var perk = data.perk.get(Perk.Type.AMMO);
        if (perk == ModPerks.BEAST_BULLET.get()) {
            shooter.playSound(ModSounds.HENG.get(), 4f, pitch);
        }

        float soundRadius = data.get(GunProp.SOUND_RADIUS).floatValue();
        int barrelType = data.attachment.get(AttachmentType.BARREL);

        SoundEvent sound3p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + (barrelType == 2 ? "_fire_3p_s" : "_fire_3p")));
        if (sound3p != null) {
            shooter.playSound(sound3p, soundRadius * 0.4f, pitch);
        }

        SoundEvent soundFar = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + (barrelType == 2 ? "_far_s" : "_far")));
        if (soundFar != null) {
            shooter.playSound(soundFar, soundRadius * 0.7f, pitch);
        }

        SoundEvent soundVeryFar = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + (barrelType == 2 ? "_veryfar_s" : "_veryfar")));
        if (soundVeryFar != null) {
            shooter.playSound(soundVeryFar, soundRadius, pitch);
        }
    }

    /**
     * 服务端处理按下开火按键时的额外行为
     */
    public void onFireKeyPress(final GunData data, Player player, boolean zoom) {
        if (data.reload.prepareTimer.get() == 0 && data.reloading() && data.hasEnoughAmmoToShoot(player)) {
            data.forceStop.set(true);
        }
    }

    /**
     * 服务端处理松开开火按键时的额外行为
     */
    public void onFireKeyRelease(final GunData data, Player player, double power, boolean zoom) {
    }

    public static double perkDamage(Perk perk) {
        if (perk instanceof AmmoPerk ammoPerk) {
            return ammoPerk.damageRate;
        }
        return 1;
    }

    /**
     * 服务端发射单发子弹
     *
     * @param shooter        射击者
     * @param level          ServerLevel
     * @param shootPosition  子弹位置
     * @param shootDirection 射击方向
     * @param data           GunData
     * @param spread         子弹散布
     * @param zoom           是否开镜
     * @param uuid           已锁定实体UUID
     * @return 是否发射成功
     */
    public boolean shootBullet(
            @Nullable Entity shooter,
            @NotNull ServerLevel level,
            @NotNull Vec3 shootPosition,
            @NotNull Vec3 shootDirection,
            @NotNull GunData data,
            double spread,
            boolean zoom,
            @Nullable UUID uuid
    ) {
        var stack = data.stack;

        var projectileInfo = data.get(GunProp.PROJECTILE);
        var projectileType = projectileInfo.type;
        var projectileTypeStr = projectileType.trim().toLowerCase(Locale.ROOT);

        if (projectileTypeStr.equals("empty")) {
            return true;
        } else if (projectileTypeStr.equals("ray")) {
            return this.shootRay(shooter, level, data, shootPosition, shootDirection, zoom, uuid);
        }

        var headshot = data.get(GunProp.HEADSHOT);
        var damage = data.get(GunProp.DAMAGE);
        var velocity = data.get(GunProp.VELOCITY);
        var bypassArmorRate = data.get(GunProp.BYPASSES_ARMOR);

        if (VectorTool.isInLiquid(level, shootPosition)) {
            velocity = 2 + 0.05f * velocity;
        }

        var finalVelocity = velocity;

        AtomicReference<Entity> entityHolder = new AtomicReference<>();

        EntityType.byString(projectileType).ifPresent(entityType -> {
            var entity = entityType.create(level);
            if (entity == null) {
                Mod.LOGGER.warn("Failed to create projectile entity {}", projectileType);
                return;
            }

            if (entity instanceof Projectile projectileEntity) {
                projectileEntity.setOwner(shooter);
            }

            // SBW子弹弹射物专属属性
            if (entity instanceof ProjectileEntity projectile) {
                projectile.shooter(shooter)
                        .damage(damage.floatValue())
                        .headShot(headshot.floatValue())
                        .zoom(zoom)
                        .bypassArmorRate(bypassArmorRate.floatValue())
                        .setGunItemId(stack)
                        .velocity(finalVelocity.floatValue());
            }

            // SBW弹射物专属属性
            if (entity instanceof CustomDamageProjectile customDamageProjectile) {
                customDamageProjectile.setDamage(damage.floatValue());
            }

            if (entity instanceof CustomGravityEntity customGravityEntity && !data.get(GunProp.GRAVITY).isNaN()) {
                customGravityEntity.setGravity(data.get(GunProp.GRAVITY).floatValue());
            }

            if (entity instanceof ExplosiveProjectile explosive) {
                explosive.setExplosionDamage(data.get(GunProp.EXPLOSION_DAMAGE).floatValue());
                explosive.setExplosionRadius(data.get(GunProp.EXPLOSION_RADIUS).floatValue());
            }

            // 填充其他自定义NBT数据
            if (projectileInfo.data != null) {
                var tag = LaunchableEntityTool.getModifiedTag(projectileInfo,
                        new ShootData(shooter != null ? shooter.getUUID() : null, damage, data.get(GunProp.EXPLOSION_DAMAGE), data.get(GunProp.EXPLOSION_RADIUS), data.get(GunProp.SPREAD))
                );
                if (tag != null) {
                    entity.load(tag);
                }
            } else if (LaunchableEntityTool.launchableEntitiesData.containsKey(projectileType)) {
                var newInfo = new ProjectileInfo();
                newInfo.data = LaunchableEntityTool.launchableEntitiesData.get(projectileType).data;
                newInfo.type = projectileType;

                var tag = LaunchableEntityTool.getModifiedTag(
                        newInfo,
                        new ShootData(shooter != null ? shooter.getUUID() : null, damage, data.get(GunProp.EXPLOSION_DAMAGE), data.get(GunProp.EXPLOSION_RADIUS), data.get(GunProp.SPREAD))
                );
                if (tag != null) {
                    entity.load(tag);
                }
            }

            entityHolder.set(entity);
        });

        var entity = entityHolder.get();
        if (entity == null) {
            Mod.LOGGER.warn("Failed to create projectile entity {}", projectileType);
            return false;
        }

        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().modifyProjectile(data, instance, entity);
            }
        }

        // 发射任意实体
        entity.setPos(shootPosition.x - 0.1 * shootDirection.x, shootPosition.y - 0.1 - 0.1 * shootDirection.y, shootPosition.z + -0.1 * shootDirection.z);

        var x = shootDirection.x;
        var y = shootDirection.y + 0.001f;
        var z = shootDirection.z;

        if (uuid != null && zoom && (shooter != null && !shooter.isShiftKeyDown())) {
            Entity target = findEntity(level, String.valueOf(uuid));
            var gunData = GunData.from(stack);
            int intelligentChipLevel = gunData.perk.getLevel(ModPerks.INTELLIGENT_CHIP);
            if (intelligentChipLevel > 0 && target != null) {
                Vec3 targetVec = target.getEyePosition();
                Vec3 playerVec = shooter.getEyePosition();
                var hasGravity = gunData.perk.getLevel(ModPerks.MICRO_MISSILE) <= 0;
                Vec3 toVec = RangeTool.calculateFiringSolution(playerVec, targetVec, Vec3.ZERO, data.get(GunProp.VELOCITY), hasGravity ? 0.03 : 0);
                x = toVec.x;
                y = toVec.y;
                z = toVec.z;
            }
        }

        if (entity instanceof Projectile projectile) {
            projectile.shoot(x, y, z, velocity.floatValue(), (float) spread);
        } else {
            Vec3 vec3 = new Vec3(x, y, z)
                    .normalize()
                    .add(
                            entity.getRandom().triangle(0.0, 0.0172275 * spread),
                            entity.getRandom().triangle(0.0, 0.0172275 * spread),
                            entity.getRandom().triangle(0.0, 0.0172275 * spread)
                    )
                    .scale(velocity);

            entity.setDeltaMovement(vec3);
            entity.hasImpulse = true;
            double d0 = vec3.horizontalDistance();
            entity.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * 180.0F / (float) Math.PI));
            entity.setXRot((float) (Mth.atan2(vec3.y, d0) * 180.0F / (float) Math.PI));
            entity.yRotO = entity.getYRot();
            entity.xRotO = entity.getXRot();
        }

        level.addFreshEntity(entity);
        return true;
    }

    public boolean shootRay(@Nullable Entity shooter, ServerLevel level, @NotNull GunData data, Vec3 shootPosition, Vec3 shootDirection, boolean zoom, UUID uuid) {
        if (shooter == null) {
            return false;
        }

        // TODO 替换为prop
        double range = 3;

        Entity target = null;

        double distance = range * range;
        Vec3 eyePos = shooter.getEyePosition(1.0f);
        HitResult hitResult = shooter.pick(range, 1.0f, false);

        Vec3 viewVec = shooter.getViewVector(1.0F);
        Vec3 toVec = eyePos.add(viewVec.x * range, viewVec.y * range, viewVec.z * range);
        AABB aabb = shooter.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(shooter, eyePos, toVec, aabb, p -> !p.isSpectator() && p.isAlive(), distance);

        if (entityHitResult != null) {
            hitResult = entityHitResult;

            var hitPos = entityHitResult.getLocation();
            target = entityHitResult.getEntity();

            if (target.isAlive()) {
                var hitBoxPos = hitPos.subtract(target.position());
                boolean headshot = false;
                boolean legShot = false;
                float eyeHeight = target.getEyeHeight();
                float bodyHeight = target.getBbHeight();

                if (target instanceof LivingEntity) {
                    if (eyeHeight - 0.25 < hitBoxPos.y && hitBoxPos.y < eyeHeight + 0.3) {
                        headshot = true;
                    }
                    if (hitBoxPos.y < 0.33 * bodyHeight) {
                        legShot = true;
                    }
                }

                var res = new EntityResult(target, hitPos, headshot, legShot);
                this.onRayHitEntity(shooter, level, data, res);
            }
        }

        BlockHitResult blockHitResult = shooter.level().clip(new ClipContext(shootPosition, shootPosition.add(shootDirection.scale(3)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));

        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState state = level.getBlockState(blockPos);

        Vec3 pos = null;

        if (state.canOcclude()) {
            pos = blockHitResult.getLocation();
        }
        if (target != null) {
            pos = hitResult.getLocation();
        }

        if (pos != null) {
            this.onRayHitBlock(shooter, level, target, data, shootDirection, blockHitResult, pos);
        }

        return true;
    }

    public void onRayHitBlock(Entity shooter, ServerLevel level, @Nullable Entity target, @NotNull GunData data, Vec3 shootDirection, BlockHitResult result, @NotNull Vec3 pos) {
        BlockPos blockPos = result.getBlockPos();
        BlockState state = level.getBlockState(blockPos);

        this.summonRayHitParticle(level, state, pos, shootDirection.scale(-1).normalize());
        if (target == null) {
            BulletDecalOption bulletDecalOption = new BulletDecalOption(result.getDirection(), blockPos);
            sendParticle(level, bulletDecalOption, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0, true);
        }
        level.playSound(null, pos.x, pos.y, pos.z, this.getRayHitBlockSound(data), SoundSource.BLOCKS, 0.7F, (float) ((2 * Math.random() - 1) * 0.05f + 1.0f));
    }

    public SoundEvent getRayHitBlockSound(GunData data) {
        return SoundEvents.EMPTY;
    }

    public void onRayHitEntity(Entity shooter, ServerLevel level, @NotNull GunData data, EntityResult result) {
        var target = result.getEntity();
        if (target instanceof LivingEntity living) {
            ICustomKnockback iCustomKnockback = ICustomKnockback.getInstance(living);
            iCustomKnockback.superbWarfare$setKnockbackStrength(0);

            float damage = data.get(GunProp.DAMAGE).floatValue();
            float headshot = data.get(GunProp.HEADSHOT).floatValue();

            if (result.isHeadshot()) {
                DamageHandler.doDamage(living, ModDamageTypes.causeLaserHeadshotDamage(level.registryAccess(), null, shooter), damage * headshot);
            } else if (result.isLegShot()) {
                DamageHandler.doDamage(living, ModDamageTypes.causeLaserDamage(level.registryAccess(), null, shooter), damage * 0.5f);
            } else {
                DamageHandler.doDamage(living, ModDamageTypes.causeLaserDamage(level.registryAccess(), null, shooter), damage);
            }

            target.invulnerableTime = 0;

            iCustomKnockback.superbWarfare$resetKnockbackStrength();

            if (shooter instanceof ServerPlayer player) {
                player.level().playSound(null, player.blockPosition(), result.isHeadshot() ? ModSounds.HEADSHOT.get() : ModSounds.INDICATION.get(), SoundSource.VOICE, 0.1f, 1);
                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(result.isHeadshot() ? 1 : 0, 5));
            }
        }
    }

    public void summonRayHitParticle(ServerLevel serverLevel, BlockState state, Vec3 pos, Vec3 dir) {
        BlockParticleOption particleData = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < 1; i++) {
            Vec3 vec3 = this.randomVec(dir, 40);
            sendParticle(serverLevel, particleData, pos.x + 0.05 * i * dir.x, pos.y + 0.05 * i * dir.y, pos.z + 0.05 * i * dir.z, 0, vec3.x, vec3.y, vec3.z, 10, true);
        }
        for (int i = 0; i < 3; i++) {
            Vec3 vec3 = this.randomVec(dir, 20);
            sendParticle(serverLevel, ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0, vec3.x, vec3.y, vec3.z, 0.05, true);
        }
        for (int i = 0; i < 2; i++) {
            Vec3 vec3 = this.randomVec(dir, 80);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), pos.x, pos.y, pos.z, 0, vec3.x, vec3.y, vec3.z, 0.2 + 0.1 * Math.random(), true);
        }
    }

    protected Vec3 randomVec(Vec3 vec3, double spread) {
        return vec3.normalize().add(random.triangle(0.0D, 0.0172275D * spread), this.random.triangle(0.0D, 0.0172275D * spread), this.random.triangle(0.0D, 0.0172275D * spread));
    }

    public boolean canEditAttachments(GunData data) {
        return data.ammoConsumers.size() > 1;
    }

    /**
     * 在切枪之后触发
     *
     * @param stack  被切走的枪
     * @param player 玩家
     */
    public void onChangeSlot(ItemStack stack, Player player) {
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Screen getItemScreen(ItemStack stack, Player player, InteractionHand hand) {
        if (ClientEventHandler.canOpenEditScreen(stack, hand) && stack.getItem() instanceof GunItem && canEditAttachments(GunData.from(stack))) {
            return new WeaponEditScreen(stack);
        }
        return null;
    }
}
