package com.atsuishio.superbwarfare.item.gun.special;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.client.renderer.item.TaserItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.EnergyImageComponent;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.SpecialFireWeapon;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class TaserItem extends GunItem implements GeoItem, SpecialFireWeapon {

    public static final int MAX_ENERGY = 6000;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;
    private final Supplier<Integer> energyCapacity;

    public TaserItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.COMMON));
        this.energyCapacity = () -> MAX_ENERGY;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.getEnergyStored() > 0 && cap.getEnergyStored() < MAX_ENERGY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return Math.round((float) (cap != null ? cap.getEnergyStored() : 0) * 13.0F / MAX_ENERGY);
    }

    // TODO register cap
//    @Override
//    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag tag) {
//        return new ItemEnergyProvider(stack, energyCapacity.get());
//    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        return 0xFFFF00;
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.TASER_RELOAD_EMPTY.get());
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new TaserItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<TaserItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (NBTTool.getTag(stack).getBoolean("is_empty_reloading")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.taser.reload"));
        }

        if (player.isSprinting() && player.onGround() && player.getPersistentData().getDouble("noRun") == 0 && ClientEventHandler.drawTime < 0.01) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.taser.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.taser.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.taser.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        AnimationController<TaserItem> idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static int getAmmoCount(Player player) {
        int count = 0;
        for (var inv : player.getInventory().items) {
            if (inv.is(ModItems.CREATIVE_AMMO_BOX.get())) {
                count++;
            }
        }

        if (count == 0) {
            int sum = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                ItemStack itemstack = player.getInventory().getItem(i);
                if (check(itemstack)) {
                    sum += itemstack.getCount();
                }
            }
            return sum;
        }
        return (int) Double.POSITIVE_INFINITY;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (entity instanceof Player player) {
            GunsTool.setGunIntTag(stack, "MaxAmmo", getAmmoCount(player));
        }

        // TODO perk
//        int perkLevel = PerkHelper.getItemPerkLevel(ModPerks.REGENERATION.get(), stack);

        var stackStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
//        if (stackStorage != null) {
//            stackStorage.receiveEnergy(perkLevel, false);
//        }

        if (entity instanceof Player player) {
            for (var cell : player.getInventory().items) {
                if (cell.is(ModItems.CELL.get())) {
                    if (stackStorage == null) continue;
                    int stackMaxEnergy = stackStorage.getMaxEnergyStored();
                    int stackEnergy = stackStorage.getEnergyStored();

                    var cellStorage = cell.getCapability(Capabilities.EnergyStorage.ITEM);
                    if (cellStorage == null) continue;
                    int cellEnergy = cellStorage.getEnergyStored();

                    int stackEnergyNeed = Math.min(cellEnergy, stackMaxEnergy - stackEnergy);

                    if (cellEnergy > 0) {
                        stackStorage.receiveEnergy(stackEnergyNeed, false);
                    }
                    cellStorage.extractEnergy(stackEnergyNeed, false);
                }
            }
        }
    }

    protected static boolean check(ItemStack stack) {
        return stack.getItem() == ModItems.TASER_ELECTRODE.get();
    }

    public static ItemStack getGunInstance() {
        ItemStack stack = new ItemStack(ModItems.TASER.get());
        GunsTool.initCreativeGun(stack, ModItems.TASER.getId().getPath());
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap != null) {
            cap.receiveEnergy(MAX_ENERGY, false);
        }
        return stack;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return ModUtils.loc("textures/gun_icon/taser_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "TASER";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return switch (perk.type) {
            case AMMO -> perk == ModPerks.LONGER_WIRE.get();
            case FUNCTIONAL ->
                    perk == ModPerks.REGENERATION.get() || perk == ModPerks.POWERFUL_ATTRACTION.get() || perk == ModPerks.INTELLIGENT_CHIP.get();
            case DAMAGE -> perk == ModPerks.VOLT_OVERLOAD.get();
        };
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new EnergyImageComponent(pStack));
    }

    @Override
    public boolean isMagazineReload(ItemStack stack) {
        return true;
    }

    @Override
    public void fireOnPress(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (GunsTool.getGunBooleanTag(stack, "Reloading")) return;

        // TODO perk
//        int perkLevel = PerkHelper.getItemPerkLevel(ModPerks.VOLT_OVERLOAD.get(), stack);
        int perkLevel = 0;
        var energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        var hasEnoughEnergy = energyStorage != null && energyStorage.getEnergyStored() >= 400 + 100 * perkLevel;

        if (player.getCooldowns().isOnCooldown(stack.getItem())
                || GunsTool.getGunIntTag(stack, "Ammo", 0) <= 0
                || !hasEnoughEnergy
        ) return;

        player.getCooldowns().addCooldown(stack.getItem(), 5);

        if (player instanceof ServerPlayer serverPlayer) {
            var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
            boolean zoom = cap != null && cap.zoom;
            double spread = GunsTool.getGunDoubleTag(stack, "Spread");

            // TODO perk
//            int volt = PerkHelper.getItemPerkLevel(ModPerks.VOLT_OVERLOAD.get(), stack);
//            int wireLength = PerkHelper.getItemPerkLevel(ModPerks.LONGER_WIRE.get(), stack);
            int volt = 0;
            int wireLength = 0;

            SoundTool.playLocalSound(serverPlayer, ModSounds.TASER_FIRE_1P.get(), 1, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.TASER_FIRE_3P.get(), SoundSource.PLAYERS, 1, 1);

            var level = serverPlayer.level();

            // TODO taser bullet
//            TaserBulletEntity taserBulletProjectile = new TaserBulletEntity(player, level,
//                    (float) GunsTool.getGunDoubleTag(stack, "Damage", 0), volt, wireLength);
//
//            taserBulletProjectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
//            taserBulletProjectile.shoot(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z, (float) GunsTool.getGunDoubleTag(stack, "Velocity", 0),
//                    (float) (zoom ? 0.1 : spread));
//            level.addFreshEntity(taserBulletProjectile);
//
//            ModUtils.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ShootClientMessage(10));
        }

        GunsTool.setGunIntTag(stack, "Ammo", GunsTool.getGunIntTag(stack, "Ammo", 0) - 1);
        energyStorage.extractEnergy(400 + 100 * perkLevel, false);
        NBTTool.getTag(stack).putBoolean("shoot", true);
    }
}
