package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.PoseTool;
import com.atsuishio.superbwarfare.client.screens.WeaponEditScreen;
import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.GunPropertyModifier;
import com.atsuishio.superbwarfare.data.gun.ProjectileInfo;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.data.launchable.LaunchableEntityTool;
import com.atsuishio.superbwarfare.data.launchable.ShootData;
import com.atsuishio.superbwarfare.entity.projectile.ExplosiveProjectile;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.CustomRendererItem;
import com.atsuishio.superbwarfare.item.ItemScreenProvider;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.RangeTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.atsuishio.superbwarfare.tools.EntityFindUtil.findEntity;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber
public abstract class GunItem extends Item implements GeoItem, CustomRendererItem, ItemScreenProvider, GunPropertyModifier {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GunItem(Properties properties) {
        super(properties);
        addReloadTimeBehavior(this.reloadTimeBehaviors);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);

        modifyProperty(GunProp.DAMAGE, (data, v) -> v + getCustomDamage(data.stack));
        modifyProperty(GunProp.HEADSHOT, (data, v) -> v + getCustomHeadshot(data.stack));
        modifyProperty(GunProp.BYPASSES_ARMOR, (data, v) -> v + getCustomBypassArmor(data.stack));
        modifyProperty(GunProp.MAGAZINE, (data, v) -> v + getCustomMagazine(data.stack));
        modifyProperty(GunProp.DEFAULT_ZOOM, (data, v) -> v + getCustomZoom(data.stack));
        modifyProperty(GunProp.RPM, (data, v) -> v + getCustomRPM(data.stack));
        modifyProperty(GunProp.WEIGHT, (data, v) -> v + getCustomWeight(data.stack));
        modifyProperty(GunProp.VELOCITY, (data, v) -> v + getCustomVelocity(data.stack));
        modifyProperty(GunProp.SOUND_RADIUS, (data, v) -> v + getCustomSoundRadius(data.stack));
        modifyProperty(GunProp.BOLT_ACTION_TIME, (data, v) -> v + getCustomBoltActionTime(data.stack));
    }

    protected final Map<GunProp<?>, BiFunction<GunData, ?, ?>> propertyModifiers = new HashMap<>();

    @Override
    public @NotNull Map<GunProp<?>, BiFunction<GunData, ?, ?>> getPropModifiers() {
        return this.propertyModifiers;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(stack.getItem() instanceof GunItem)) return;

        if (level instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
        }

        var data = GunData.from(stack);

        var inMainHand = entity instanceof LivingEntity living && living.getMainHandItem() == stack;
        data.tick(entity, inMainHand);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> map = super.getAttributeModifiers(slot, stack);
        UUID uuid = new UUID(slot.toString().hashCode(), 0);
        if (slot != EquipmentSlot.MAINHAND) return map;

        var data = GunData.from(stack);
        map = HashMultimap.create(map);

        // 移速
        map.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                uuid, Mod.ATTRIBUTE_MODIFIER,
                -0.01f - 0.005f * data.get(GunProp.WEIGHT),
                AttributeModifier.Operation.MULTIPLY_BASE
        ));

        // 近战伤害
        if (data.get(GunProp.MELEE_DAMAGE) > 0) {
            map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    BASE_ATTACK_DAMAGE_UUID, Mod.ATTRIBUTE_MODIFIER,
                    data.get(GunProp.MELEE_DAMAGE),
                    AttributeModifier.Operation.ADDITION
            ));
        }
        return map;
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new GunImageComponent(pStack));
    }

    public Set<SoundEvent> getReloadSound() {
        return Set.of();
    }

    public ResourceLocation getGunIcon(ItemStack stack) {
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
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    public int getMaxDamage(@NotNull ItemStack stack) {
        return GunData.getDefault(stack).maxDurability;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return getMaxDamage(stack) > 0;
    }

    /**
     * 开膛待击
     *
     * @param stack 武器物品
     */
    public boolean isOpenBolt(ItemStack stack) {
        return false;
    }

    /**
     * 是否允许额外往枪管里塞入一发子弹
     *
     * @param stack 武器物品
     */
    public boolean hasBulletInBarrel(ItemStack stack) {
        return false;
    }

    /**
     * 武器是否能更换枪管配件
     *
     * @param stack 武器物品
     */
    public boolean hasCustomBarrel(ItemStack stack) {
        return false;
    }

    public int[] getValidBarrels() {
        return new int[]{0, 1, 2};
    }

    /**
     * 武器是否能更换枪托配件
     *
     * @param stack 武器物品
     */
    public boolean hasCustomGrip(ItemStack stack) {
        return false;
    }

    public int[] getValidGrips() {
        return new int[]{0, 1, 2, 3};
    }

    /**
     * 武器是否能更换弹匣配件
     *
     * @param stack 武器物品
     */
    public boolean hasCustomMagazine(ItemStack stack) {
        return false;
    }

    public int[] getValidMagazines() {
        return new int[]{0, 1, 2};
    }

    /**
     * 武器是否能更换瞄具配件
     *
     * @param stack 武器物品
     */
    public boolean hasCustomScope(ItemStack stack) {
        return false;
    }

    public int[] getValidScopes() {
        return new int[]{0, 1, 2, 3};
    }

    /**
     * 武器是否能更换枪托配件
     *
     * @param stack 武器物品
     */
    public boolean hasCustomStock(ItemStack stack) {
        return false;
    }

    public int[] getValidStocks() {
        return new int[]{0, 1, 2};
    }

    /**
     * 武器是否有脚架
     *
     * @param stack 武器物品
     */
    public boolean hasBipod(ItemStack stack) {
        return false;
    }

    /**
     * 武器是否会抛壳
     *
     * @param stack 武器物品
     */
    public boolean canEjectShell(ItemStack stack) {
        return false;
    }

    /**
     * 武器是否能进行近战攻击
     *
     * @param stack 武器物品
     */
    public boolean hasMeleeAttack(ItemStack stack) {
        return GunData.from(stack).get(GunProp.MELEE_DAMAGE) > 0;
    }

    /**
     * 获取额外伤害加成
     */
    public double getCustomDamage(ItemStack stack) {
        return 0;
    }

    /**
     * 获取额外爆头伤害加成
     */
    public double getCustomHeadshot(ItemStack stack) {
        return 0;
    }

    /**
     * 获取额外护甲穿透加成
     */
    public double getCustomBypassArmor(ItemStack stack) {
        return 0;
    }

    /**
     * 获取额外弹匣容量加成
     */
    public int getCustomMagazine(ItemStack stack) {
        return 0;
    }

    /**
     * 获取额外缩放倍率加成
     */
    public double getCustomZoom(ItemStack stack) {
        return 0;
    }

    /**
     * 获取额外RPM加成
     */
    public int getCustomRPM(ItemStack stack) {
        return 0;
    }

    /**
     * 获取额外总重量加成
     */
    public double getCustomWeight(ItemStack stack) {
        CompoundTag tag = GunData.from(stack).attachment();

        double scopeWeight = switch (tag.getInt("Scope")) {
            case 1 -> 0.5;
            case 2 -> 1;
            case 3 -> 1.5;
            default -> 0;
        };

        double barrelWeight = switch (tag.getInt("Barrel")) {
            case 1 -> 0.5;
            case 2 -> 1;
            default -> 0;
        };

        double magazineWeight = switch (tag.getInt("Magazine")) {
            case 1 -> 1;
            case 2 -> 2;
            default -> 0;
        };

        double stockWeight = switch (tag.getInt("Stock")) {
            case 1 -> -2;
            case 2 -> 1.5;
            default -> 0;
        };

        double gripWeight = switch (tag.getInt("Grip")) {
            case 1, 2 -> 0.25;
            case 3 -> 1;
            default -> 0;
        };

        return scopeWeight + barrelWeight + magazineWeight + stockWeight + gripWeight;
    }

    /**
     * 获取额外弹速加成
     */
    public double getCustomVelocity(ItemStack stack) {
        return 0;
    }

    /**
     * 获取额外音效半径加成
     */
    public double getCustomSoundRadius(ItemStack stack) {
        return GunData.from(stack).attachment().getInt("Barrel") == 2 ? 0.6 : 1;
    }

    public int getCustomBoltActionTime(ItemStack stack) {
        return 0;
    }

    /**
     * 是否允许缩放
     */
    public boolean canAdjustZoom(ItemStack stack) {
        return false;
    }

    /**
     * 是否允许切换瞄具
     */
    public boolean canSwitchScope(ItemStack stack) {
        return false;
    }

    public final Map<Integer, Consumer<GunData>> reloadTimeBehaviors = new HashMap<>();

    /**
     * 添加达到指定换弹时间时的额外行为
     */
    public void addReloadTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
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
        if (data.ammo.get() == 1) {
            data.holdOpen.set(true);
        }

        // 判断是否为栓动武器（BoltActionTime > 0），并在开火后给一个需要上膛的状态
        if (data.get(GunProp.BOLT_ACTION_TIME) > 0 && data.ammo.get() > 1) {
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
            data.ammo.set(data.ammo.get() - 1);
            data.isEmpty.set(true);
        } else {
            data.consumeBackupAmmo(shooter, 1);
        }

        var stack = data.stack();
        if (this.getMaxDamage(stack) > 0) {
            if (shooter instanceof LivingEntity living) {
                stack.hurtAndBreak(1, living, p -> p.broadcastBreakEvent(living.getUsedItemHand()));
            } else {
                if (stack.hurt(1, RandomSource.create(), null)) {
                    stack.shrink(1);
                }
            }
        }
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
        var perk = data.perk.get(Perk.Type.AMMO);

        // 生成所有子弹
        for (int index0 = 0; index0 < (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 1 : projectileAmount); index0++) {
            if (!shootBullet(shooter, level, shootPosition, shootDirection, data, spread, zoom, uuid)) return;
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

        SoundEvent sound3p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + (barrelType == 2 ? "_fire_3p_s" : "_fire_3p")));
        if (sound3p != null) {
            shooter.playSound(sound3p, soundRadius * 0.4f, pitch);
        }

        SoundEvent soundFar = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + (barrelType == 2 ? "_far_s" : "_far")));
        if (soundFar != null) {
            shooter.playSound(soundFar, soundRadius * 0.7f, pitch);
        }

        SoundEvent soundVeryFar = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + (barrelType == 2 ? "_veryfar_s" : "_veryfar")));
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

        var headshot = data.get(GunProp.HEADSHOT);
        var damage = data.get(GunProp.DAMAGE);
        var velocity = data.get(GunProp.VELOCITY);
        var bypassArmorRate = data.get(GunProp.BYPASSES_ARMOR);

        if (VectorTool.isInLiquid(level, shootPosition)) {
            velocity = 2 + 0.05f * velocity;
        }

        var finalVelocity = velocity;

        var projectileInfo = data.get(GunProp.PROJECTILE);
        var projectileType = projectileInfo.type;

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

            // SBW爆炸物专属属性
            if (entity instanceof ExplosiveProjectile explosive) {
                explosive.setDamage(damage.floatValue());
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
            var random = RandomSource.create();
            Vec3 vec3 = new Vec3(x, y, z)
                    .normalize()
                    .add(
                            random.triangle(0.0, 0.0172275 * spread),
                            random.triangle(0.0, 0.0172275 * spread),
                            random.triangle(0.0, 0.0172275 * spread)
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

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(this.getClientExtensions());
    }

    @OnlyIn(Dist.CLIENT)
    public IClientItemExtensions getClientExtensions() {
        return new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = GunItem.this.getRenderer().get();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
                return PoseTool.pose(entityLiving, hand, stack);
            }
        };
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Screen getItemScreen(ItemStack stack, Player player, InteractionHand hand) {
        if (ClientEventHandler.canOpenEditScreen(stack, hand)) {
            return new WeaponEditScreen(stack);
        }
        return null;
    }
}
