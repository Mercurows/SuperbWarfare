package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.client.PoseTool;
import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.CustomRendererItem;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.AmmoType;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public abstract class GunItem extends Item implements CustomRendererItem {

    public GunItem(Properties properties) {
        super(properties);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof LivingEntity)
                || !stack.is(ModTags.Items.GUN)
                || !(stack.getItem() instanceof GunItem gunItem)
        ) return;

        var tag = NBTTool.getTag(stack);

        if (tag.getString("id").isEmpty()) {
            var id = stack.getDescriptionId();
            tag.putString("id", id.substring(id.lastIndexOf(".") + 1));
        }

        if (!tag.getBoolean("init")) {
            var name = this.getDescriptionId().substring(this.getDescriptionId().lastIndexOf('.') + 1);

            if (level.getServer() != null && entity instanceof Player player && player.isCreative()) {
                GunsTool.initCreativeGun(tag, name);
            } else {
                GunsTool.initGun(tag, name);
            }

            GunsTool.generateAndSetUUID(tag);
            tag.putBoolean("init", true);
        }
        tag.putBoolean("draw", false);

        handleGunPerks(tag);
        handleGunAttachment(tag);

        var hasBulletInBarrel = gunItem.hasBulletInBarrel(stack);
        var ammoCount = GunsTool.getGunIntTag(tag, "Ammo");
        var magazine = GunsTool.getGunIntTag(tag, "Magazine");
        var customMagazine = GunsTool.getGunIntTag(tag, "CustomMagazine");

        if ((hasBulletInBarrel && ammoCount > magazine + customMagazine + 1)
                || (!hasBulletInBarrel && ammoCount > magazine + customMagazine)
        ) {
            int count = ammoCount - magazine + customMagazine - (hasBulletInBarrel ? 1 : 0);

            var capability = entity.getCapability(ModCapabilities.PLAYER_VARIABLE);
            if (capability != null) {
                if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO)) {
                    AmmoType.SHOTGUN.add(capability, count);
                } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO)) {
                    AmmoType.SNIPER.add(capability, count);
                } else if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
                    AmmoType.HANDGUN.add(capability, count);
                } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO)) {
                    AmmoType.RIFLE.add(capability, count);
                } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO)) {
                    AmmoType.HEAVY.add(capability, count);
                }
                capability.syncPlayerVariables(entity);
            }
            GunsTool.setGunIntTag(tag, "Ammo", magazine + customMagazine + (hasBulletInBarrel ? 1 : 0));
        }
        NBTTool.saveTag(stack, tag);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    // TODO attribute modifier
//    @Override
//    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
//        ItemAttributeModifiers map = super.getDefaultAttributeModifiers(stack);
//        map.builder().add(
//                Attribute.BASE,
//                new AttributeModifier(uuid, ModUtils.ATTRIBUTE_MODIFIER,
//                        -0.01f - 0.005f * (GunsTool.getGunDoubleTag(tag, "Weight") + GunsTool.getGunDoubleTag(tag, "CustomWeight")),
//                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
//        )
//
//        map.put(Attributes.MOVEMENT_SPEED,
//                new AttributeModifier(uuid, ModUtils.ATTRIBUTE_MODIFIER,
//                        -0.01f - 0.005f * (GunsTool.getGunDoubleTag(tag, "Weight") + GunsTool.getGunDoubleTag(tag, "CustomWeight")),
//                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
//        return map;
//    }

//    @Override
//    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
//        Multimap<Attribute, AttributeModifier> map = super.getAttributeModifiers(slot, stack);
//        UUID uuid = new UUID(slot.toString().hashCode(), 0);
//        if (slot == EquipmentSlot.MAINHAND) {
//            map = HashMultimap.create(map);
//            map.put(Attributes.MOVEMENT_SPEED,
//                    new AttributeModifier(uuid, ModUtils.ATTRIBUTE_MODIFIER,
//                            -0.01f - 0.005f * (GunsTool.getGunDoubleTag(tag, "Weight") + GunsTool.getGunDoubleTag(tag, "CustomWeight")),
//                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
//        }
//        return map;
//    }

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


    private void handleGunPerks(final CompoundTag tag) {
        reducePerkTagCoolDown(tag, "HealClipTime", "KillClipReloadTime", "KillClipTime", "FourthTimesCharmTick", "HeadSeeker",
                "DesperadoTime", "DesperadoTimePost");

        if (PerkHelper.getItemPerkLevel(ModPerks.FOURTH_TIMES_CHARM.get(), tag) > 0) {
            int count = GunsTool.getPerkIntTag(tag, "FourthTimesCharmCount");
            if (count >= 4) {
                GunsTool.setPerkIntTag(tag, "FourthTimesCharmTick", 0);
                GunsTool.setPerkIntTag(tag, "FourthTimesCharmCount", 0);

                int mag = GunsTool.getGunIntTag(tag, "Magazine") + GunsTool.getGunIntTag(tag, "CustomMagazine");
                GunsTool.setGunIntTag(tag, "Ammo", Math.min(mag, GunsTool.getGunIntTag(tag, "Ammo") + 2));
            }
        }
    }

    private void handleGunAttachment(final CompoundTag rootTag) {
        CompoundTag tag = rootTag.getCompound("Attachments");

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

        double soundRadius = tag.getInt("Barrel") == 2 ? 0.6 : 1;

        GunsTool.setGunDoubleTag(tag, "CustomWeight", scopeWeight + barrelWeight + magazineWeight + stockWeight + gripWeight);
        GunsTool.setGunDoubleTag(tag, "CustomSoundRadius", soundRadius);
    }

    public boolean canApplyPerk(Perk perk) {
        return true;
    }

    private void reducePerkTagCoolDown(final CompoundTag tag, String... tags) {
        var compound = tag.getCompound("PerkData");

        for (String t : tags) {
            if (!compound.contains(t)) {
                continue;
            }

            if (compound.getInt(t) > 0) {
                compound.putInt(t, Math.max(0, compound.getInt(t) - 1));
            }
        }
        tag.put("PerkData", compound);
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
     * 右下角弹药显示名称
     */
    public String getAmmoDisplayName(ItemStack stack) {
        if (stack.is(ModTags.Items.USE_RIFLE_AMMO)) {
            return "Rifle Ammo";
        }
        if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
            return "Handgun Ammo";
        }
        if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO)) {
            return "Shotgun Ammo";
        }
        if (stack.is(ModTags.Items.USE_SNIPER_AMMO)) {
            return "Sniper Ammo";
        }
        if (stack.is(ModTags.Items.USE_HEAVY_AMMO)) {
            return "Heavy Ammo";
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

    @SubscribeEvent
    private static void registerGunExtensions(RegisterClientExtensionsEvent event) {
        for (var item : ModItems.GUNS.getEntries()) {
            if (item.get() instanceof GunItem gun) {
                event.registerItem(new IClientItemExtensions() {
                    private final BlockEntityWithoutLevelRenderer renderer = gun.getRenderer();

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
