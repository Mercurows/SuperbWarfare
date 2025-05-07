package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.PoseTool;
import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.CustomRendererItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.value.AttachmentType;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public abstract class GunItem extends Item implements CustomRendererItem {

    public GunItem(Properties properties) {
        super(properties);
        addReloadTimeBehavior(this.reloadTimeBehaviors);
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
    @ParametersAreNonnullByDefault
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof LivingEntity living) || !(stack.getItem() instanceof GunItem gunItem)) return;

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
        handleGunPerks(data);

        var hasBulletInBarrel = gunItem.hasBulletInBarrel(stack);
        var ammoCount = data.ammo.get();
        var magazine = data.magazine();

        if ((hasBulletInBarrel && ammoCount > magazine + 1) || (!hasBulletInBarrel && ammoCount > magazine)) {
            int count = ammoCount - magazine - (hasBulletInBarrel ? 1 : 0);
            var capability = entity.getData(ModAttachments.PLAYER_VARIABLE).watch();

            var ammoType = data.ammoTypeInfo().playerAmmoType();
            if (ammoType != null) {
                ammoType.add(capability, count);
            }

            entity.setData(ModAttachments.PLAYER_VARIABLE, capability);
            capability.sync(entity);
            data.ammo.set(magazine + (hasBulletInBarrel ? 1 : 0));
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

        data.save();
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

        list.add(new ItemAttributeModifiers.Entry(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(SPEED_ID,
                        -0.01f - 0.005f * data.weight(),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        ));

        return new ItemAttributeModifiers(list, true);
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

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }


    private void handleGunPerks(GunData data) {
        var perk = data.perk;

        perk.reduceCooldown(ModPerks.FOURTH_TIMES_CHARM, "FourthTimesCharmTick");

        perk.reduceCooldown(ModPerks.HEAD_SEEKER, "HeadSeeker");

        perk.reduceCooldown(ModPerks.DESPERADO, "DesperadoTime");
        perk.reduceCooldown(ModPerks.DESPERADO, "DesperadoTimePost");

        if (perk.getLevel(ModPerks.FOURTH_TIMES_CHARM) > 0) {
            var tag = data.perk.getTag(ModPerks.FOURTH_TIMES_CHARM);
            int count = perk.getTag(ModPerks.FOURTH_TIMES_CHARM).getInt("FourthTimesCharmCount");

            if (count >= 4) {
                tag.remove("FourthTimesCharmTick");
                tag.remove("FourthTimesCharmCount");

                int mag = data.magazine();
                data.ammo.set(Math.min(mag, data.ammo.get() + 2));
            }
        }

        data.save();
    }

    public boolean canApplyPerk(Perk perk) {
        return true;
    }


    /**
     * 是否使用弹匣换弹
     *
     * @param stack 武器物品
     */
    public boolean isMagazineReload(ItemStack stack) {
        return false;
    }

    /**
     * 是否使用弹夹换弹
     *
     * @param stack 武器物品
     */
    public boolean isClipReload(ItemStack stack) {
        return false;
    }

    /**
     * 是否是单发装填换弹
     *
     * @param stack 武器物品
     */
    public boolean isIterativeReload(ItemStack stack) {
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
     * 武器是否为全自动武器
     *
     * @param stack 武器物品
     */
    public boolean isAutoWeapon(ItemStack stack) {
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
        return false;
    }

    /**
     * 获取武器可用的开火模式
     */
    public int getAvailableFireModes() {
        return 0;
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
        var attachment = GunData.from(stack).attachment;

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
    public double getCustomVelocity(ItemStack stack) {
        return 0;
    }

    /**
     * 获取额外音效半径加成
     */
    public double getCustomSoundRadius(ItemStack stack) {
        return GunData.from(stack).attachment.get(AttachmentType.BARREL) == 2 ? 0.6 : 1;
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

    public enum FireMode {
        SEMI(1),
        BURST(2),
        AUTO(4);

        public final int flag;

        FireMode(int i) {
            this.flag = i;
        }
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

        SoundEvent sound3p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + (barrelType == 2 ? "_fire_3p_s" : "_fire_3p")));
        if (sound3p != null) {
            player.playSound(sound3p, soundRadius * 0.4f, pitch);
        }

        SoundEvent soundFar = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + (barrelType == 2 ? "_far_s" : "_far")));
        if (soundFar != null) {
            player.playSound(soundFar, soundRadius * 0.7f, pitch);
        }

        SoundEvent soundVeryFar = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + (barrelType == 2 ? "_veryfar_s" : "_veryfar")));
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

    public static double perkSpeed(GunData data) {
        var perk = data.perk.get(Perk.Type.AMMO);
        if (perk instanceof AmmoPerk ammoPerk) {
            return ammoPerk.speedRate;
        }
        return 1;
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

        float headshot = (float) data.headshot();
        float damage = (float) data.damage();
        float velocity = (float) (data.velocity() * perkSpeed(data));
        int projectileAmount = data.projectileAmount();
        float bypassArmorRate = (float) data.bypassArmor();
        var perkInstance = data.perk.getInstance(Perk.Type.AMMO);
        var perk = perkInstance != null ? perkInstance.perk() : null;

        ProjectileEntity projectile = new ProjectileEntity(player.level())
                .shooter(player)
                .damage(perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? projectileAmount * damage : damage)
                .headShot(headshot)
                .zoom(zoom)
                .setGunItemId(stack);

        if (perk instanceof AmmoPerk ammoPerk) {
            int level = data.perk.getLevel(perk);

            bypassArmorRate += ammoPerk.bypassArmorRate + (perk == ModPerks.AP_BULLET.get() ? 0.05f * (level - 1) : 0);
            projectile.setRGB(ammoPerk.rgb);

            if (!ammoPerk.mobEffects.get().isEmpty()) {
                int amplifier;
                if (perk.descriptionId.equals("blade_bullet")) {
                    amplifier = level / 3;
                } else if (perk.descriptionId.equals("bread_bullet")) {
                    amplifier = 1;
                } else {
                    amplifier = level - 1;
                }

                ArrayList<MobEffectInstance> mobEffectInstances = new ArrayList<>();
                for (MobEffect effect : ammoPerk.mobEffects.get()) {
                    mobEffectInstances.add(new MobEffectInstance(Holder.direct(effect), 70 + 30 * level, amplifier));
                }
                projectile.effect(mobEffectInstances);
            }

            if (perk.descriptionId.equals("bread_bullet")) {
                projectile.knockback(level * 0.3f);
                projectile.forceKnockback();
            }
        }

        bypassArmorRate = Math.max(bypassArmorRate, 0);
        projectile.bypassArmorRate(bypassArmorRate);

        if (perk == ModPerks.SILVER_BULLET.get()) {
            int level = data.perk.getLevel(perk);
            projectile.undeadMultiple(1.0f + 0.5f * level);
        } else if (perk == ModPerks.BEAST_BULLET.get()) {
            projectile.beast();
        } else if (perk == ModPerks.JHP_BULLET.get()) {
            int level = data.perk.getLevel(perk);
            projectile.jhpBullet(level);
        } else if (perk == ModPerks.HE_BULLET.get()) {
            int level = data.perk.getLevel(perk);
            projectile.heBullet(level);
        } else if (perk == ModPerks.INCENDIARY_BULLET.get()) {
            int level = data.perk.getLevel(perk);
            projectile.fireBullet(level, stack.is(ModTags.Items.SHOTGUN));
        }

        var dmgPerk = data.perk.get(Perk.Type.DAMAGE);
        if (dmgPerk == ModPerks.MONSTER_HUNTER.get()) {
            int level = data.perk.getLevel(dmgPerk);
            projectile.monsterMultiple(0.1f + 0.1f * level);
        }

        projectile.setPos(player.getX() - 0.1 * player.getLookAngle().x, player.getEyeY() - 0.1 - 0.1 * player.getLookAngle().y, player.getZ() + -0.1 * player.getLookAngle().z);
        projectile.shoot(player, player.getLookAngle().x, player.getLookAngle().y + 0.001f, player.getLookAngle().z, stack.is(ModTags.Items.SHOTGUN) && perk == ModPerks.INCENDIARY_BULLET.get() ? 4.5f : velocity, (float) spread);
        player.level().addFreshEntity(projectile);

        return true;
    }

    @SubscribeEvent
    private static void registerGunExtensions(RegisterClientExtensionsEvent event) {
        for (var item : ModItems.GUNS.getEntries()) {
            if (item.get() instanceof GunItem gun) {
                event.registerItem(new IClientItemExtensions() {
                    private final BlockEntityWithoutLevelRenderer renderer = gun.getRenderer().get();

                    @Override
                    public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return renderer;
                    }

                    @Override
                    @ParametersAreNonnullByDefault
                    public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
                        return PoseTool.pose(entityLiving, hand, stack);
                    }
                }, item);
            }
        }
    }
}
