package com.atsuishio.superbwarfare.item.gun.special;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.client.renderer.item.BocekItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.BocekImageComponent;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.SpecialFireWeapon;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

public class BocekItem extends GunItem implements GeoItem, SpecialFireWeapon {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public BocekItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new BocekItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<BocekItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (player.isSprinting() && player.onGround() && player.getPersistentData().getDouble("noRun") == 0 && ClientEventHandler.drawTime < 0.01) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
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
        if (entity instanceof Player player) {
            GunsTool.setGunIntTag(stack, "MaxAmmo", getAmmoCount(player));
        }

        if (GunsTool.getGunIntTag(stack, "ArrowEmpty") > 0) {
            GunsTool.setGunIntTag(stack, "ArrowEmpty", GunsTool.getGunIntTag(stack, "ArrowEmpty") - 1);
        }
    }

    protected static boolean check(ItemStack stack) {
        return stack.getItem() == Items.ARROW;
    }

    public static ItemStack getGunInstance() {
        ItemStack stack = new ItemStack(ModItems.BOCEK.get());
        GunsTool.initCreativeGun(stack, ModItems.BOCEK.getId().getPath());
        return stack;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/bocek_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "Bocek";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return switch (perk.type) {
            case AMMO -> !perk.descriptionId.equals("butterfly_bullet") && perk != ModPerks.MICRO_MISSILE.get();
            case FUNCTIONAL -> perk == ModPerks.FIELD_DOCTOR.get() || perk == ModPerks.INTELLIGENT_CHIP.get();
            case DAMAGE -> perk == ModPerks.MONSTER_HUNTER.get() || perk == ModPerks.KILLING_TALLY.get();
        };
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new BocekImageComponent(pStack));
    }

    @Override
    public boolean useBackpackAmmo(ItemStack stack) {
        return true;
    }


    @Override
    public void fireOnRelease(Player player) {
        if (player.level().isClientSide()) return;

        ItemStack stack = player.getMainHandItem();

        var perk = PerkHelper.getPerkByType(stack, Perk.Type.AMMO);

        if (player instanceof ServerPlayer serverPlayer) {
            SoundTool.stopSound(serverPlayer, ModSounds.BOCEK_PULL_1P.getId(), SoundSource.PLAYERS);
            SoundTool.stopSound(serverPlayer, ModSounds.BOCEK_PULL_3P.getId(), SoundSource.PLAYERS);

            // TODO shoot client msg
//            ModUtils.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ShootClientMessage(10));
        }

        if (GunsTool.getGunDoubleTag(stack, "Power") >= 6) {
            var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
            if (cap != null && cap.zoom) {
                // TODO FireMessage.spawnBullet
//                spawnBullet(player);

                SoundTool.playLocalSound(player, ModSounds.BOCEK_ZOOM_FIRE_1P.get(), 10, 1);
                player.playSound(ModSounds.BOCEK_ZOOM_FIRE_3P.get(), 2, 1);
            } else {
//                for (int i = 0; i < (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 1 : 10); i++) {
//                    spawnBullet(player);
//                }

                SoundTool.playLocalSound(player, ModSounds.BOCEK_SHATTER_CAP_FIRE_1P.get(), 10, 1);
                player.playSound(ModSounds.BOCEK_SHATTER_CAP_FIRE_3P.get(), 2, 1);
            }

            if (perk == ModPerks.BEAST_BULLET.get()) {
                player.playSound(ModSounds.HENG.get(), 4f, 1f);

                if (player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.HENG.get(), 4f, 1f);
                }
            }

            player.getCooldowns().addCooldown(stack.getItem(), 7);
            GunsTool.setGunIntTag(stack, "ArrowEmpty", 7);
            GunsTool.setGunDoubleTag(stack, "Power", 0);

            if (!InventoryTool.hasCreativeAmmoBox(player) && !player.isCreative()) {
                player.getInventory().clearOrCountMatchingItems(p -> Items.ARROW == p.getItem(), 1, player.inventoryMenu.getCraftSlots());
            }
        }
    }

    @Override
    public void fireOnPress(Player player) {
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap != null) {
            cap.bowPullHold = true;
            cap.syncPlayerVariables(player);
        }
    }
}