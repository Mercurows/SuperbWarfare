package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.client.renderer.item.SecondaryCataclysmRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.SecondaryCataclysmImageComponent;
import com.atsuishio.superbwarfare.entity.projectile.GunGrenadeEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.*;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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
import java.util.function.Supplier;

public class SecondaryCataclysm extends GunItem implements GeoItem, SpecialFireWeapon {
    private final Supplier<Integer> energyCapacity = () -> 24000;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public SecondaryCataclysm() {
        super(new Properties().stacksTo(1).fireResistant().rarity(ModRarity.getLegendary()));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.getEnergyStored() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return Math.round((float) (cap != null ? cap.getEnergyStored() : 0) * 13.0F / 24000F);
    }

    // TODO register cap
//    @Override
//    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag tag) {
//        return new ItemEnergyProvider(stack, energyCapacity.get());
//    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        return 0x95E9FF;
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new SecondaryCataclysmRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState reloadAnimPredicate(AnimationState<SecondaryCataclysm> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;
        final var tag = NBTTool.getTag(stack);

        if (tag.getInt("reload_stage") == 1 && tag.getDouble("prepare_load") > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sc.prepare"));
        }

        if (tag.getDouble("load_index") == 0 && tag.getInt("reload_stage") == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sc.iterativeload"));
        }

        if (tag.getDouble("load_index") == 1 && tag.getInt("reload_stage") == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sc.iterativeload2"));
        }

        if (ClientEventHandler.gunMelee > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sc.hit"));
        }

        if (tag.getInt("reload_stage") == 3) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.sc.finish"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sc.idle"));
    }

    private PlayState idlePredicate(AnimationState<SecondaryCataclysm> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;
        final var tag = NBTTool.getTag(stack);

        if (player.isSprinting() && player.onGround()
                && player.getPersistentData().getDouble("noRun") == 0
                && !(tag.getBoolean("is_empty_reloading"))
                && tag.getInt("reload_stage") != 1
                && tag.getInt("reload_stage") != 2
                && tag.getInt("reload_stage") != 3
                && ClientEventHandler.drawTime < 0.01
                && ClientEventHandler.gunMelee == 0
                && !GunsTool.getGunBooleanTag(tag, "Reloading")) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sc.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sc.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sc.idle"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var reloadAnimController = new AnimationController<>(this, "reloadAnimController", 1, this::reloadAnimPredicate);
        data.add(reloadAnimController);
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

        if (entity instanceof Player player) {
            for (var cell : player.getInventory().items) {
                if (cell.is(ModItems.CELL.get())) {
                    var stackStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
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
        return stack.getItem() == ModItems.GRENADE_40MM.get();
    }

    public static ItemStack getGunInstance() {
        ItemStack stack = new ItemStack(ModItems.SECONDARY_CATACLYSM.get());
        GunsTool.initCreativeGun(stack, ModItems.SECONDARY_CATACLYSM.getId().getPath());
        return stack;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/secondary_cataclysm_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "SECONDARY CATACLYSM";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return PerkHelper.LAUNCHER_PERKS.test(perk) || perk == ModPerks.MICRO_MISSILE.get();
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new SecondaryCataclysmImageComponent(pStack));
    }

    // TODO attribute
//    @Override
//    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
//        Multimap<Attribute, AttributeModifier> map = super.getAttributeModifiers(slot, stack);
//        UUID uuid = new UUID(slot.toString().hashCode(), 0);
//        if (slot == EquipmentSlot.MAINHAND) {
//            map = HashMultimap.create(map);
//            map.put(Attributes.ATTACK_DAMAGE,
//                    new AttributeModifier(uuid, ModUtils.ATTRIBUTE_MODIFIER, 19, AttributeModifier.Operation.ADDITION));
//        }
//        return map;
//    }

    @Override
    public boolean isIterativeReload(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasMeleeAttack(ItemStack stack) {
        return true;
    }

    @Override
    public int getAvailableFireModes() {
        return FireMode.SEMI.flag;
    }

    @Override
    public void fireOnPress(Player player, final CompoundTag tag) {
        ItemStack stack = player.getMainHandItem();
        if (GunsTool.getGunBooleanTag(tag, "Reloading")) return;
        if (player.getCooldowns().isOnCooldown(stack.getItem()) || GunsTool.getGunIntTag(tag, "Ammo", 0) <= 0) return;

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        boolean zooming = cap != null && cap.zoom;
        double spread = GunsTool.getGunDoubleTag(tag, "Spread");

        var stackCap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        var hasEnoughEnergy = stackCap != null && stackCap.getEnergyStored() >= 3000;

        boolean isChargedFire = zooming && hasEnoughEnergy;

        if (player.level() instanceof ServerLevel serverLevel) {
            GunGrenadeEntity gunGrenadeEntity = new GunGrenadeEntity(player, serverLevel,
                    (float) GunsTool.getGunDoubleTag(tag, "Damage", 0),
                    (float) GunsTool.getGunDoubleTag(tag, "ExplosionDamage", 0),
                    (float) GunsTool.getGunDoubleTag(tag, "ExplosionRadius", 0));

            var dmgPerk = PerkHelper.getPerkByType(tag, Perk.Type.DAMAGE);
            if (dmgPerk == ModPerks.MONSTER_HUNTER.get()) {
                int perkLevel = PerkHelper.getItemPerkLevel(dmgPerk, tag);
                gunGrenadeEntity.setMonsterMultiplier(0.1f + 0.1f * perkLevel);
            }

            gunGrenadeEntity.setNoGravity(PerkHelper.getPerkByType(tag, Perk.Type.AMMO) == ModPerks.MICRO_MISSILE.get());
            gunGrenadeEntity.charged(isChargedFire);

            float velocity = (float) GunsTool.getGunDoubleTag(tag, "Velocity", 0);
            int perkLevel = PerkHelper.getItemPerkLevel(ModPerks.MICRO_MISSILE.get(), tag);
            if (perkLevel > 0) {
                gunGrenadeEntity.setExplosionRadius((float) GunsTool.getGunDoubleTag(tag, "ExplosionRadius", 0) * 0.5f);
                gunGrenadeEntity.setDamage((float) GunsTool.getGunDoubleTag(tag, "Damage", 0) * (1.1f + perkLevel * 0.1f));
                velocity *= 1.2f;
            }

            gunGrenadeEntity.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            gunGrenadeEntity.shoot(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z, (isChargedFire ? 4 : 1) * velocity,
                    (float) (zooming ? 0.1 : spread));
            serverLevel.addFreshEntity(gunGrenadeEntity);

            ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, player.getX() + 1.8 * player.getLookAngle().x,
                    player.getEyeY() - 0.35 + 1.8 * player.getLookAngle().y,
                    player.getZ() + 1.8 * player.getLookAngle().z,
                    4, 0.1, 0.1, 0.1, 0.002, true);


            var serverPlayer = (ServerPlayer) player;

            if (isChargedFire) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.SECONDARY_CATACLYSM_FIRE_1P_CHARGE.get(), 1, 1);
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.SECONDARY_CATACLYSM_FIRE_3P_CHARGE.get(), SoundSource.PLAYERS, 3, 1);
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.SECONDARY_CATACLYSM_FAR_CHARGE.get(), SoundSource.PLAYERS, 5, 1);
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.SECONDARY_CATACLYSM_VERYFAR_CHARGE.get(), SoundSource.PLAYERS, 10, 1);

                var itemCap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
                if (itemCap != null) {
                    itemCap.extractEnergy(3000, false);
                }
            } else {
                SoundTool.playLocalSound(serverPlayer, ModSounds.SECONDARY_CATACLYSM_FIRE_1P.get(), 1, 1);
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.SECONDARY_CATACLYSM_FIRE_3P.get(), SoundSource.PLAYERS, 3, 1);
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.SECONDARY_CATACLYSM_FAR.get(), SoundSource.PLAYERS, 5, 1);
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.SECONDARY_CATACLYSM_VERYFAR.get(), SoundSource.PLAYERS, 10, 1);
            }

            PacketDistributor.sendToPlayer(serverPlayer, new ShootClientMessage(10));
        }

        GunsTool.setGunIntTag(tag, "Ammo", GunsTool.getGunIntTag(tag, "Ammo", 0) - 1);
        player.getCooldowns().addCooldown(stack.getItem(), 6);
    }
}