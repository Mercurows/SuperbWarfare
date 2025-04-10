package com.atsuishio.superbwarfare.item.gun.special;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.TaserItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.EnergyImageComponent;
import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.EnergyStorageItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.SpecialFireWeapon;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.network.message.receive.ShootClientMessage;
import com.atsuishio.superbwarfare.perk.Perk;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.Set;

public class TaserItem extends GunItem implements GeoItem, SpecialFireWeapon, EnergyStorageItem {

    public static final int MAX_ENERGY = 6000;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;


    public TaserItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.COMMON));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.getEnergyStored() != 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return Math.round((float) (cap != null ? cap.getEnergyStored() : 0) * 13.0F / MAX_ENERGY);
    }

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

        if (GunData.from(stack).reload.empty()) {
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
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        var data = GunData.from(stack);

        if (entity instanceof Player player) {
            data.setMaxAmmo(getAmmoCount(player));
            data.save();
        }

        int perkLevel = data.perk.getLevel(ModPerks.REGENERATION);

        var stackStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (stackStorage != null) {
            stackStorage.receiveEnergy(perkLevel, false);
        }

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

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/taser_icon.png");
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
    public String getAmmoDisplayName(ItemStack stack) {
        return "Electrode Rod";
    }

    @Override
    public void fireOnPress(Player player, final GunData data, boolean zoom) {
        if (data.reloading()) return;
        ItemStack stack = data.stack();

        int perkLevel = data.perk.getLevel(ModPerks.VOLT_OVERLOAD);
        var energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        var hasEnoughEnergy = energyStorage != null && energyStorage.getEnergyStored() >= 400 + 100 * perkLevel;

        if (player.getCooldowns().isOnCooldown(stack.getItem())
                || data.ammo() <= 0
                || !hasEnoughEnergy
        ) return;

        player.getCooldowns().addCooldown(stack.getItem(), 5);

        if (player instanceof ServerPlayer serverPlayer) {
            double spread = data.spread();

            int volt = data.perk.getLevel(ModPerks.VOLT_OVERLOAD);
            int wireLength = data.perk.getLevel(ModPerks.LONGER_WIRE);

            SoundTool.playLocalSound(serverPlayer, ModSounds.TASER_FIRE_1P.get(), 1, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.TASER_FIRE_3P.get(), SoundSource.PLAYERS, 1, 1);

            var level = serverPlayer.level();

            TaserBulletEntity taserBulletProjectile = new TaserBulletEntity(player, level,
                    (float) data.damage(), volt, wireLength);

            taserBulletProjectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            taserBulletProjectile.shoot(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z, (float) data.velocity(),
                    (float) (zoom ? 0.1 : spread));
            level.addFreshEntity(taserBulletProjectile);

            PacketDistributor.sendToPlayer(serverPlayer, new ShootClientMessage(10));
        }

        data.setAmmo(data.ammo() - 1);
        data.tag().putBoolean("shoot", true);
        energyStorage.extractEnergy(400 + 100 * perkLevel, false);
    }

    @Override
    public int getMaxEnergy() {
        return MAX_ENERGY;
    }

    @Override
    public Item getCustomAmmoItem() {
        return ModItems.TASER_ELECTRODE.get();
    }
}
