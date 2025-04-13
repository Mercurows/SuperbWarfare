package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.JavelinItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.LauncherImageComponent;
import com.atsuishio.superbwarfare.entity.projectile.DecoyEntity;
import com.atsuishio.superbwarfare.entity.projectile.JavelinMissileEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModEnumExtensions;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.SpecialFireWeapon;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.network.message.receive.ShootClientMessage;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class JavelinItem extends GunItem implements GeoItem, SpecialFireWeapon {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public JavelinItem() {
        super(new Properties().stacksTo(1).rarity(ModEnumExtensions.getLegendary()));
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return JavelinItemRenderer::new;
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<JavelinItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        if (GunData.from(stack).reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.javelin.reload"));
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.javelin.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.javelin.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.javelin.idle"));
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
        return Set.of(ModSounds.JAVELIN_RELOAD_EMPTY.get(), ModSounds.JAVELIN_LOCK.get(), ModSounds.JAVELIN_LOCKON.get());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        var data = GunData.from(stack);
        final var tag = data.tag();

        if (entity instanceof Player player && selected) {
            data.maxAmmo.set(getAmmoCount(player));

            if (tag.getBoolean("Seeking")) {

                List<Entity> decoy = SeekTool.seekLivingEntities(player, player.level(), 512, 8);
                for (var e : decoy) {
                    if (e instanceof DecoyEntity decoyEntity) {
                        tag.putString("TargetEntity", decoyEntity.getStringUUID());
                        tag.putDouble("TargetPosX", decoyEntity.getPosition().x);
                        tag.putDouble("TargetPosY", decoyEntity.getPosition().y);
                        tag.putDouble("TargetPosZ", decoyEntity.getPosition().z);
                    }
                }

                Entity targetEntity = EntityFindUtil.findEntity(player.level(), tag.getString("TargetEntity"));
                Entity seekingEntity = SeekTool.seekEntity(player, player.level(), 512, 8);


                if (tag.getInt("GuideType") == 0) {
                    if (seekingEntity != null && seekingEntity == targetEntity) {
                        tag.putInt("SeekTime", tag.getInt("SeekTime") + 1);
                        if (tag.getInt("SeekTime") > 0 && (!seekingEntity.getPassengers().isEmpty() || seekingEntity instanceof VehicleEntity) && seekingEntity.tickCount % 3 == 0) {
                            seekingEntity.level().playSound(null, seekingEntity.getOnPos(), seekingEntity instanceof Pig ? SoundEvents.PIG_HURT : ModSounds.LOCKING_WARNING.get(), SoundSource.PLAYERS, 1, 1f);
                        }
                    } else {
                        tag.putInt("SeekTime", 0);
                    }

                    if (tag.getInt("SeekTime") == 1 && player instanceof ServerPlayer serverPlayer) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.JAVELIN_LOCK.get(), 1, 1);
                    }

                    if (seekingEntity != null && tag.getInt("SeekTime") > 20) {
                        if (player instanceof ServerPlayer serverPlayer) {
                            SoundTool.playLocalSound(serverPlayer, ModSounds.JAVELIN_LOCKON.get(), 1, 1);
                        }
                        if ((!seekingEntity.getPassengers().isEmpty() || seekingEntity instanceof VehicleEntity) && seekingEntity.tickCount % 2 == 0) {
                            seekingEntity.level().playSound(null, seekingEntity.getOnPos(), seekingEntity instanceof Pig ? SoundEvents.PIG_HURT : ModSounds.LOCKED_WARNING.get(), SoundSource.PLAYERS, 1, 0.95f);
                        }
                    }

                } else if (tag.getInt("GuideType") == 1) {

                    Vec3 toVec = player.getEyePosition().vectorTo(new Vec3(tag.getDouble("TargetPosX"), tag.getDouble("TargetPosY"), tag.getDouble("TargetPosZ"))).normalize();
                    if (VectorTool.calculateAngle(player.getViewVector(1), toVec) < 8) {
                        tag.putInt("SeekTime", tag.getInt("SeekTime") + 1);
                    } else {
                        tag.putInt("SeekTime", 0);
                    }

                    if (tag.getInt("SeekTime") == 1 && player instanceof ServerPlayer serverPlayer) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.JAVELIN_LOCK.get(), 1, 1);
                    }

                    if (tag.getInt("SeekTime") > 20) {
                        if (player instanceof ServerPlayer serverPlayer) {
                            SoundTool.playLocalSound(serverPlayer, ModSounds.JAVELIN_LOCKON.get(), 1, 1);
                        }
                    }
                }
            }
        } else {
            tag.putInt("SeekTime", 0);
        }
        data.save();
    }

    protected static boolean check(ItemStack stack) {
        return stack.getItem() == ModItems.JAVELIN_MISSILE.get();
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/javelin_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "FGM-148";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return PerkHelper.LAUNCHER_PERKS.test(perk);
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
    public String getAmmoDisplayName(ItemStack stack) {
        return "Javelin Missile";
    }

    private void fire(Player player) {
        Level level = player.level();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);
        CompoundTag tag = data.tag();

        if (tag.getInt("SeekTime") < 20) return;

        float yRot = player.getYRot() + 360;
        yRot = (yRot + 90) % 360;

        var firePos = new Vector3d(0, -0.2, 0.15);
        firePos.rotateZ(-player.getXRot() * Mth.DEG_TO_RAD);
        firePos.rotateY(-yRot * Mth.DEG_TO_RAD);

        if (player.level() instanceof ServerLevel serverLevel) {
            JavelinMissileEntity missileEntity = new JavelinMissileEntity(player, level,
                    (float) data.damage(),
                    (float) data.explosionDamage(),
                    (float) data.explosionRadius(),
                    tag.getInt("GuideType"),
                    new Vec3(tag.getDouble("TargetPosX"), tag.getDouble("TargetPosY"), tag.getDouble("TargetPosZ"))
            );

            var dmgPerk = data.perk.get(Perk.Type.DAMAGE);
            if (dmgPerk == ModPerks.MONSTER_HUNTER.get()) {
                int perkLevel = data.perk.getLevel(dmgPerk);
                missileEntity.setMonsterMultiplier(0.1f + 0.1f * perkLevel);
            }

            missileEntity.setPos(player.getX() + firePos.x, player.getEyeY() + firePos.y, player.getZ() + firePos.z);
            missileEntity.shoot(player.getLookAngle().x, player.getLookAngle().y + 0.3, player.getLookAngle().z, 3f, 1);
            missileEntity.setTargetUuid(tag.getString("TargetEntity"));
            missileEntity.setAttackMode(tag.getBoolean("TopMode"));

            level.addFreshEntity(missileEntity);
            ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, player.getX() + 1.8 * player.getLookAngle().x,
                    player.getY() + player.getBbHeight() - 0.1 + 1.8 * player.getLookAngle().y,
                    player.getZ() + 1.8 * player.getLookAngle().z,
                    30, 0.4, 0.4, 0.4, 0.005, true);

            var serverPlayer = (ServerPlayer) player;

            SoundTool.playLocalSound(serverPlayer, ModSounds.JAVELIN_FIRE_1P.get(), 2, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.JAVELIN_FIRE_3P.get(), SoundSource.PLAYERS, 4, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.JAVELIN_FAR.get(), SoundSource.PLAYERS, 10, 1);

            PacketDistributor.sendToPlayer(serverPlayer, new ShootClientMessage(10));
        }

        player.getCooldowns().addCooldown(stack.getItem(), 10);
        data.ammo.set(data.ammo.get() - 1);
        data.save();
    }

    @Override
    public void fireOnRelease(Player player, final GunData data, double power, boolean zoom) {
        fire(player);
        var tag = data.tag();
        tag.putBoolean("Seeking", false);
        tag.putInt("SeekTime", 0);
        tag.putString("TargetEntity", "none");
        if (player instanceof ServerPlayer serverPlayer) {
            var clientboundstopsoundpacket = new ClientboundStopSoundPacket(Mod.loc("javelin_lock"), SoundSource.PLAYERS);
            serverPlayer.connection.send(clientboundstopsoundpacket);
        }
    }

    @Override
    public void fireOnPress(Player player, final GunData data, boolean zoom) {
        var tag = data.tag();

        if (!zoom || data.ammo.get() <= 0) return;

        Entity seekingEntity = SeekTool.seekEntity(player, player.level(), 512, 8);

        if (seekingEntity != null && !player.isCrouching()) {
            tag.putInt("GuideType", 0);
            tag.putString("TargetEntity", seekingEntity.getStringUUID());
        } else {
            BlockHitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getViewVector(1).scale(512)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            Vec3 hitPos = result.getLocation();

            tag.putInt("GuideType", 1);
            tag.putDouble("TargetPosX", hitPos.x);
            tag.putDouble("TargetPosY", hitPos.y);
            tag.putDouble("TargetPosZ", hitPos.z);
        }
        tag.putBoolean("Seeking", true);
        tag.putInt("SeekTime", 0);
    }

    @Override
    public Item getCustomAmmoItem() {
        return ModItems.JAVELIN_MISSILE.get();
    }
}