package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Mk42Entity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Mk42Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.MK_42.get(), world);
    }

    public Mk42Entity(EntityType<Mk42Entity> type, Level world) {
        super(type, world);
    }


    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(8 + ClientMouseHandler.custom3pDistanceLerp, 1, 0);
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        var gunData = getGunData(0);
        if (gunData == null) return InteractionResult.SUCCESS;
        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModTags.Items.TOOLS_CROWBAR) && !player.isShiftKeyDown()) {
            if (gunData.ammo.get() > 0) {
                if (player.level() instanceof ServerLevel) {
                    vehicleShoot(player, 0);
                }

            }
            return InteractionResult.SUCCESS;
        }

        // 手动添加弹药

        if (!gunData.selectedAmmoConsumer().isAmmoItem(stack)) {
            return super.interact(player, hand);
        } else {
            var inStack = this.items.get(0);
            int count = inStack.getCount();

            if (count < this.getMaxStackSize()) {
                this.setItem(0, stack.copyWithCount(count + 1));
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.MISSILE_RELOAD.get(), 1, 1);
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 1f) * damage);
    }

    @Override
    public void baseTick() {
        super.baseTick();

        var gunData = getGunData(0);
        if (gunData != null && level() instanceof ServerLevel && getNthEntity(getTurretControllerIndex()) instanceof Player player) {
            var ammoCount = InventoryTool.countItem(player, gunData.selectedAmmoConsumer().stack().getItem());
            if (ammoCount > 0) {
                var inStack = this.items.get(0);
                int count = inStack.getCount();

                if (count < Math.min(this.getMaxStackSize(), inStack.getMaxStackSize())) {
                    this.setItem(0, gunData.selectedAmmoConsumer().stack().copyWithCount(count + 1));
                    InventoryTool.consumeItem(player, gunData.selectedAmmoConsumer().stack().getItem(), 1);
                    if (player instanceof ServerPlayer serverPlayer) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.MISSILE_RELOAD.get(), 1, 1);
                    }
                }
            }
        }

        lowHealthWarning();
    }

    private PlayState shootPredicate(AnimationState<Mk42Entity> event) {
        if (getShootAnimationTimer(0, 0) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mk_42.fire"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mk_42.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "shoot", 0, this::shootPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }


    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        var gunData = getGunData(0);
        if (gunData != null) {
            return super.canPlaceItem(slot, stack) && gunData.selectedAmmoConsumer().isAmmoItem(stack);
        } else {
            return false;
        }
    }
}
