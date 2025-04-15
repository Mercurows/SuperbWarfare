package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.PoseTool;
import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.CustomRendererItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.value.AttachmentType;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.Ammo;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
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
    @ParametersAreNonnullByDefault
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof LivingEntity) || !(stack.getItem() instanceof GunItem gunItem)) return;

        var data = GunData.from(stack);

        if (!data.initialized()) {
            data.initialize();
            if (level.getServer() != null && entity instanceof Player player && player.isCreative()) {
                data.ammo.set(data.magazine());
            }
        }
        data.draw.set(false);
        handleGunPerks(data);

        var hasBulletInBarrel = gunItem.hasBulletInBarrel(stack);
        var ammoCount = data.ammo.get();
        var magazine = data.magazine();

        if ((hasBulletInBarrel && ammoCount > magazine + 1) || (!hasBulletInBarrel && ammoCount > magazine)) {
            int count = ammoCount - magazine - (hasBulletInBarrel ? 1 : 0);
            var capability = entity.getData(ModAttachments.PLAYER_VARIABLE).watch();

            var ammoTypeInfo = data.ammoTypeInfo();
            if (ammoTypeInfo.type() == GunData.AmmoConsumeType.PLAYER_AMMO) {
                var type = Ammo.getType(ammoTypeInfo.value());
                assert type != null;

                type.add(capability, count);
            }

            entity.setData(ModAttachments.PLAYER_VARIABLE, capability);
            capability.sync(entity);
            data.ammo.set(magazine + (hasBulletInBarrel ? 1 : 0));
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

        perk.reduceCooldown(ModPerks.HEAL_CLIP, "HealClipTime");

        perk.reduceCooldown(ModPerks.KILL_CLIP, "KillClipReloadTime");
        perk.reduceCooldown(ModPerks.KILL_CLIP, "KillClipTime");

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
     * 武器是否直接使用背包内的弹药物品进行发射，而不是使用玩家存储的弹药
     *
     * @param stack 武器物品
     */
    public boolean useBackpackAmmo(ItemStack stack) {
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
        var ammoTypeInfo = data.ammoTypeInfo();
        if (ammoTypeInfo.type() == GunData.AmmoConsumeType.PLAYER_AMMO) {
            var type = Ammo.getType(ammoTypeInfo.value());
            assert type != null;

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
