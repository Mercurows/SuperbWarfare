package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ProjectileInfo;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.data.launchable.LaunchableEntityTool;
import com.atsuishio.superbwarfare.data.launchable.ShootData;
import com.atsuishio.superbwarfare.entity.projectile.ExplosiveProjectile;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.network.PlayerVariable;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber
public abstract class GunItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GunItem(Properties properties) {
        super(properties);
        addReloadTimeBehavior(this.reloadTimeBehaviors);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        var data = GunData.from(stack);
        return data.heat.get() != 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var data = GunData.from(stack);
        return Math.round((float) data.heat.get() * 13.0F / 100F);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        var data = GunData.from(stack);
        double f = 1 - data.heat.get() / 100.0F;
        return Mth.hsvToRgb((float) f / 3.0F, 1.0F, 1.0F);
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
        if (!(entity instanceof LivingEntity living) || !(stack.getItem() instanceof GunItem gunItem)) return;

        if (level instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
        }

        var data = GunData.from(stack);

        if (!data.initialized()) {
            data.initialize();
            if (level.getServer() != null && entity instanceof Player player && player.isCreative()) {
                data.ammo.set(data.magazine());
            }
        }
        data.draw.set(false);

        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().tick(data, instance, living);
            }
        }

        var hasBulletInBarrel = gunItem.hasBulletInBarrel(stack);
        var ammoCount = data.ammo.get();
        var magazine = data.magazine();

        if ((hasBulletInBarrel && ammoCount > magazine + 1) || (!hasBulletInBarrel && ammoCount > magazine)) {
            int count = ammoCount - magazine - (hasBulletInBarrel ? 1 : 0);
            PlayerVariable.modify(entity, capability -> {
                var ammoType = data.ammoTypeInfo().playerAmmoType();
                if (ammoType != null) {
                    ammoType.add(capability, count);
                }

                data.ammo.set(magazine + (hasBulletInBarrel ? 1 : 0));
            });
        }

        // 冷却
        double cooldown = 0;
        if (entity.wasInPowderSnow) {
            cooldown = 0.15;
        } else if (entity.isInWaterOrRain()) {
            cooldown = 0.04;
        } else if (entity.isOnFire() || entity.isInLava()) {
            cooldown = -0.1;
        }

        data.heat.set(Mth.clamp(data.heat.get() - 0.25 - cooldown, 0, 100));

        if (data.heat.get() < 80 && data.overHeat.get()) {
            data.overHeat.set(false);
        }
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
                -0.01f - 0.005f * data.weight(),
                AttributeModifier.Operation.MULTIPLY_BASE
        ));

        // 近战伤害
        if (data.meleeDamage() > 0) {
            map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    BASE_ATTACK_DAMAGE_UUID, Mod.ATTRIBUTE_MODIFIER,
                    data.meleeDamage(),
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

    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/default_icon.png");
    }

    public String getGunDisplayName() {
        return "";
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return false;
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        if (event.getItem().getItem().is(ModTags.Items.GUN)) {
            GunData.from(event.getItem().getItem()).draw.set(true);
        }
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
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
     * 武器是否能进行改装
     *
     * @param stack 武器物品
     */
    public boolean isCustomizable(ItemStack stack) {
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

    /**
     * 武器是否能更换枪托配件
     *
     * @param stack 武器物品
     */
    public boolean hasCustomGrip(ItemStack stack) {
        return false;
    }

    /**
     * 武器是否能更换弹匣配件
     *
     * @param stack 武器物品
     */
    public boolean hasCustomMagazine(ItemStack stack) {
        return false;
    }

    /**
     * 武器是否能更换瞄具配件
     *
     * @param stack 武器物品
     */
    public boolean hasCustomScope(ItemStack stack) {
        return false;
    }

    /**
     * 武器是否能更换枪托配件
     *
     * @param stack 武器物品
     */
    public boolean hasCustomStock(ItemStack stack) {
        return false;
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
        return GunData.from(stack).meleeDamage() > 0;
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

    /**
     * 右下角弹药显示名称
     */
    public String getAmmoDisplayName(GunData data) {
        var type = data.ammoTypeInfo().playerAmmoType();
        if (type != null) {
            return type.displayName;
        }
        return "";
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
    public boolean canShoot(GunData data) {
        return data.projectileAmount() > 0;
    }

    /**
     * 服务端在开火前的额外行为
     */
    public void beforeShoot(GunData data, Player player, double spread, boolean zoom) {
        // 空仓挂机
        if (data.ammo.get() == 1) {
            data.holdOpen.set(true);
        }


        // 判断是否为栓动武器（BoltActionTime > 0），并在开火后给一个需要上膛的状态
        if (data.defaultActionTime() > 0 && data.ammo.get() > 1) {
            data.bolt.needed.set(true);
        }
    }

    /**
     * 服务端在开火后的额外行为
     */
    public void afterShoot(GunData data, Player player) {
        if (!data.useBackpackAmmo()) {
            data.ammo.set(data.ammo.get() - 1);
            data.isEmpty.set(true);
        } else {
            data.consumeBackupAmmo(player, 1);
        }
    }

    /**
     * 服务端处理开火
     */
    public void onShoot(GunData data, Player player, double spread, boolean zoom) {
        if (!data.hasEnoughAmmoToShoot(player)) return;

        // 开火前事件
        data.item.beforeShoot(data, player, spread, zoom);

        int projectileAmount = data.projectileAmount();
        var perk = data.perk.get(Perk.Type.AMMO);

        // 生成所有子弹
        for (int index0 = 0; index0 < (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 1 : projectileAmount); index0++) {
            if (!shootBullet(player, data, spread, zoom)) return;
        }

        // 添加热量

        data.heat.set(Mth.clamp(data.heat.get() + data.heatPerShoot(), 0, 100));

        // 过热
        if (data.heat.get() >= 100 && !data.overHeat.get()) {
            data.overHeat.set(true);
            if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.MINIGUN_OVERHEAT.get(), 2f, 1f);
            }
        }

        data.item.afterShoot(data, player);
        playFireSounds(data, player, zoom);
    }

    /**
     * 播放开火音效
     */
    public void playFireSounds(GunData data, Player player, boolean zoom) {
        ItemStack stack = data.stack;
        if (!(stack.getItem() instanceof GunItem)) return;

        String origin = stack.getItem().getDescriptionId();
        String name = origin.substring(origin.lastIndexOf(".") + 1);

        float pitch = data.heat.get() <= 75 ? 1 : (float) (1 - 0.02 * Math.abs(75 - data.heat.get()));

        var perk = data.perk.get(Perk.Type.AMMO);
        if (perk == ModPerks.BEAST_BULLET.get()) {
            player.playSound(ModSounds.HENG.get(), 4f, pitch);
        }

        float soundRadius = (float) data.soundRadius();
        int barrelType = data.attachment.get(AttachmentType.BARREL);

        SoundEvent sound3p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + (barrelType == 2 ? "_fire_3p_s" : "_fire_3p")));
        if (sound3p != null) {
            player.playSound(sound3p, soundRadius * 0.4f, pitch);
        }

        SoundEvent soundFar = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + (barrelType == 2 ? "_far_s" : "_far")));
        if (soundFar != null) {
            player.playSound(soundFar, soundRadius * 0.7f, pitch);
        }

        SoundEvent soundVeryFar = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + (barrelType == 2 ? "_veryfar_s" : "_veryfar")));
        if (soundVeryFar != null) {
            player.playSound(soundVeryFar, soundRadius, pitch);
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
     * @return 是否发射成功
     */
    public boolean shootBullet(Player player, GunData data, double spread, boolean zoom) {
        var stack = data.stack;
        var level = player.level();

        float headshot = (float) data.headshot();
        float damage = (float) data.damage();
        float velocity = (float) data.velocity();
        float bypassArmorRate = (float) data.bypassArmor();

        var projectileType = data.projectileType();
        var projectileInfo = data.projectileInfo();
        AtomicReference<Entity> entityHolder = new AtomicReference<>();
        EntityType.byString(projectileType).ifPresent(entityType -> {
            var entity = entityType.create(level);
            if (entity == null) return;

            if (entity instanceof Projectile projectileEntity) {
                projectileEntity.setOwner(player);
            }

            // SBW子弹弹射物专属属性
            if (entity instanceof ProjectileEntity projectile) {
                projectile.shooter(player)
                        .damage(damage)
                        .headShot(headshot)
                        .zoom(zoom)
                        .bypassArmorRate(bypassArmorRate)
                        .setGunItemId(stack);
            }

            // SBW爆炸物专属属性
            if (entity instanceof ExplosiveProjectile explosive) {
                explosive.setDamage(damage);
                explosive.setExplosionDamage((float) data.explosionDamage());
                explosive.setExplosionRadius((float) data.explosionRadius());
            }

            // 填充其他自定义NBT数据
            if (projectileInfo.data != null) {
                var tag = LaunchableEntityTool.getModifiedTag(projectileInfo,
                        new ShootData(player.getUUID(), damage, data.explosionDamage(), data.explosionRadius(), data.spread())
                );
                if (tag != null) {
                    entity.load(tag);
                }

            } else if (LaunchableEntityTool.launchableEntitiesData.containsKey(projectileType)) {
                var newInfo = new ProjectileInfo();
                newInfo.data = LaunchableEntityTool.launchableEntitiesData.get(projectileType);
                newInfo.type = projectileType;

                var tag = LaunchableEntityTool.getModifiedTag(
                        newInfo,
                        new ShootData(player.getUUID(), damage, data.explosionDamage(), data.explosionRadius(), data.spread())
                );
                if (tag != null) {
                    entity.load(tag);
                }
            }

            entityHolder.set(entity);
        });

        var entity = entityHolder.get();
        if (entity == null) return false;

        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().modifyProjectile(data, instance, entity);
                if (instance.perk() instanceof AmmoPerk ammoPerk) {
                    velocity = (float) ammoPerk.getModifiedVelocity(data, instance);
                }
            }
        }

        // 发射任意实体
        entity.setPos(player.getX() - 0.1 * player.getLookAngle().x, player.getEyeY() - 0.1 - 0.1 * player.getLookAngle().y, player.getZ() + -0.1 * player.getLookAngle().z);

        var x = player.getLookAngle().x;
        var y = player.getLookAngle().y + 0.001f;
        var z = player.getLookAngle().z;

        if (entity instanceof Projectile projectile) {
            projectile.shoot(x, y, z, velocity, (float) spread);
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
}
