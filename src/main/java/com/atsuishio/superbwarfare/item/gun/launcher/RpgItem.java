package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.RpgItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.LauncherImageComponent;
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.SpecialFireWeapon;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.network.message.receive.ShootClientMessage;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class RpgItem extends GunItem implements GeoItem, SpecialFireWeapon {

    @Override
    public String getAmmoDisplayName(ItemStack stack) {
        return "Yassin105 TBG";
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public RpgItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new RpgItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<RpgItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;
        var data = GunData.from(stack);

        if (data.reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.rpg.reload"));
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 4, this::idlePredicate);
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
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.RPG_RELOAD_EMPTY.get());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        var data = GunData.from(stack);
        final var tag = data.tag();
        if (tag.getBoolean("draw")) {
            tag.putBoolean("draw", false);

            if (data.ammo() == 0) {
                data.setIsEmpty(true);
            }
        }

        if (entity instanceof Player player) {
            data.setMaxAmmo(getAmmoCount(player));
        }
        data.save();

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    protected static boolean check(ItemStack stack) {
        return stack.getItem() == ModItems.ROCKET.get();
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/rpg_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "RPG-7";
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
    public void fireOnPress(Player player, final GunData data, boolean zoom) {
        Level level = player.level();
        ItemStack stack = player.getMainHandItem();

        if (data.reloading()
                || player.getCooldowns().isOnCooldown(stack.getItem())
                || data.ammo() <= 0
        ) return;

        double spread = data.spread();

        if (player.level() instanceof ServerLevel serverLevel) {
            RpgRocketEntity rocket = new RpgRocketEntity(player, level,
                    (float) data.damage(),
                    (float) data.explosionDamage(),
                    (float) data.explosionRadius()
            );

            var dmgPerk = data.perk.get(Perk.Type.DAMAGE);
            if (dmgPerk == ModPerks.MONSTER_HUNTER.get()) {
                int perkLevel = data.perk.getLevel(dmgPerk);
                rocket.setMonsterMultiplier(0.1f + 0.1f * perkLevel);
            }

            float velocity = (float) data.velocity();

            if (data.perk.get(Perk.Type.AMMO) == ModPerks.MICRO_MISSILE.get()) {
                rocket.setNoGravity(true);

                int perkLevel = data.perk.getLevel(ModPerks.MICRO_MISSILE);
                if (perkLevel > 0) {
                    rocket.setExplosionRadius(0.5f);
                    rocket.setDamage((float) data.damage() * (1.1f + perkLevel * 0.1f));
                    velocity *= 1.2f;
                }
            }

            rocket.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            rocket.shoot(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z, velocity,
                    (float) (zoom ? 0.1 : spread));
            level.addFreshEntity(rocket);

            ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, player.getX() + 1.8 * player.getLookAngle().x,
                    player.getY() + player.getBbHeight() - 0.1 + 1.8 * player.getLookAngle().y,
                    player.getZ() + 1.8 * player.getLookAngle().z,
                    30, 0.4, 0.4, 0.4, 0.005, true);

            var serverPlayer = (ServerPlayer) player;

            SoundTool.playLocalSound(serverPlayer, ModSounds.RPG_FIRE_1P.get(), 2, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.RPG_FIRE_3P.get(), SoundSource.PLAYERS, 2, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.RPG_FAR.get(), SoundSource.PLAYERS, 5, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.RPG_VERYFAR.get(), SoundSource.PLAYERS, 10, 1);

            PacketDistributor.sendToPlayer(serverPlayer, new ShootClientMessage(10));
        }

        if (data.ammo() == 1) {
            data.setIsEmpty(true);
            data.setCloseHammer(true);
        }

        player.getCooldowns().addCooldown(stack.getItem(), 10);
        data.setAmmo(data.ammo() - 1);
    }

    @Override
    public void addReloadTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
        super.addReloadTimeBehavior(behaviors);

        behaviors.put(84, data -> data.setIsEmpty(false));
        behaviors.put(9, data -> data.setCloseHammer(false));
    }

    @Override
    public Item getCustomAmmoItem() {
        return ModItems.ROCKET.get();
    }
}