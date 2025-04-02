package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.client.renderer.item.M79ItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.LauncherImageComponent;
import com.atsuishio.superbwarfare.entity.projectile.GunGrenadeEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.SpecialFireWeapon;
import com.atsuishio.superbwarfare.network.message.receive.ShootClientMessage;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

public class M79Item extends GunItem implements GeoItem, SpecialFireWeapon {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.M_79_RELOAD_EMPTY.get());
    }

    public M79Item() {
        super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE));
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new M79ItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<M79Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (NBTTool.getTag(stack).getBoolean("is_empty_reloading")) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m79.reload"));
        }

        if (player.isSprinting() && player.onGround() && player.getPersistentData().getDouble("noRun") == 0 && ClientEventHandler.drawTime < 0.01) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m79.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m79.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m79.idle"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
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
            final var tag = NBTTool.getTag(stack);
            GunsTool.setGunIntTag(tag, "MaxAmmo", getAmmoCount(player));
            NBTTool.saveTag(stack, tag);
        }
    }

    protected static boolean check(ItemStack stack) {
        return stack.getItem() == ModItems.GRENADE_40MM.get();
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/m79_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "M79 LAUNCHER";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return PerkHelper.LAUNCHER_PERKS.test(perk) || perk == ModPerks.MICRO_MISSILE.get();
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new LauncherImageComponent(pStack));
    }

    @Override
    public boolean isMagazineReload(ItemStack stack) {
        return true;
    }

    @Override
    public void fireOnPress(Player player, final CompoundTag tag) {
        ItemStack stack = player.getMainHandItem();

        if (GunsTool.getGunBooleanTag(tag, "Reloading")) return;
        if (player.getCooldowns().isOnCooldown(stack.getItem()) || GunsTool.getGunIntTag(tag, "Ammo") <= 0) return;

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        boolean zooming = cap != null && cap.zoom;
        double spread = GunsTool.getGunDoubleTag(tag, "Spread");

        if (player.level() instanceof ServerLevel serverLevel) {
            GunGrenadeEntity gunGrenadeEntity = new GunGrenadeEntity(player, serverLevel,
                    (float) GunsTool.getGunDoubleTag(tag, "Damage"),
                    (float) GunsTool.getGunDoubleTag(tag, "ExplosionDamage"),
                    (float) GunsTool.getGunDoubleTag(tag, "ExplosionRadius"));

            var dmgPerk = PerkHelper.getPerkByType(tag, Perk.Type.DAMAGE);
            if (dmgPerk == ModPerks.MONSTER_HUNTER.get()) {
                int perkLevel = PerkHelper.getItemPerkLevel(dmgPerk, tag);
                gunGrenadeEntity.setMonsterMultiplier(0.1f + 0.1f * perkLevel);
            }

            gunGrenadeEntity.setNoGravity(PerkHelper.getPerkByType(tag, Perk.Type.AMMO) == ModPerks.MICRO_MISSILE.get());

            float velocity = (float) GunsTool.getGunDoubleTag(tag, "Velocity");
            int perkLevel = PerkHelper.getItemPerkLevel(ModPerks.MICRO_MISSILE.get(), tag);
            if (perkLevel > 0) {
                gunGrenadeEntity.setExplosionRadius((float) GunsTool.getGunDoubleTag(tag, "ExplosionRadius") * 0.5f);
                gunGrenadeEntity.setDamage((float) GunsTool.getGunDoubleTag(tag, "Damage") * (1.1f + perkLevel * 0.1f));
                velocity *= 1.2f;
            }

            gunGrenadeEntity.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            gunGrenadeEntity.shoot(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z, velocity,
                    (float) (zooming ? 0.1 : spread));
            serverLevel.addFreshEntity(gunGrenadeEntity);

            ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, player.getX() + 1.8 * player.getLookAngle().x,
                    player.getY() + player.getBbHeight() - 0.1 + 1.8 * player.getLookAngle().y,
                    player.getZ() + 1.8 * player.getLookAngle().z,
                    4, 0.1, 0.1, 0.1, 0.002, true);

            var serverPlayer = (ServerPlayer) player;

            SoundTool.playLocalSound(serverPlayer, ModSounds.M_79_FIRE_1P.get(), 2, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.M_79_FIRE_3P.get(), SoundSource.PLAYERS, 2, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.M_79_FAR.get(), SoundSource.PLAYERS, 5, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.M_79_VERYFAR.get(), SoundSource.PLAYERS, 10, 1);

            PacketDistributor.sendToPlayer(serverPlayer, new ShootClientMessage(10));
        }

        player.getCooldowns().addCooldown(stack.getItem(), 2);
        GunsTool.setGunIntTag(tag, "Ammo", GunsTool.getGunIntTag(tag, "Ammo") - 1);
    }
}