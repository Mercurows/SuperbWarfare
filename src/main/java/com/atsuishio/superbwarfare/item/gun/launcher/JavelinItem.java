package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.PoseTool;
import com.atsuishio.superbwarfare.client.renderer.gun.JavelinItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.LauncherImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.DecoyEntity;
import com.atsuishio.superbwarfare.entity.projectile.JavelinMissileEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.receive.ShootClientMessage;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class JavelinItem extends GunItem {

    public JavelinItem() {
        super(new Properties().stacksTo(1).rarity(RarityTool.LEGENDARY));
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new JavelinItemRenderer();
                }
                return renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
                return PoseTool.pose(entityLiving, hand, stack);
            }
        });
    }

    private PlayState idlePredicate(AnimationState<JavelinItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.javelin.idle"));

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
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.JAVELIN_RELOAD_EMPTY.get(), ModSounds.JAVELIN_LOCK.get(), ModSounds.JAVELIN_LOCKON.get());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (entity instanceof Player player && selected) {
            var tag = stack.getOrCreateTag();
            if (tag.getBoolean("Seeking")) {

                List<Entity> decoy = SeekTool.seekLivingEntities(player, player.level(), 512, 8);
                for (var e : decoy) {
                    if (e instanceof DecoyEntity decoyEntity) {
                        tag.putString("TargetEntity", decoyEntity.getDecoyUUID());
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
            stack.getOrCreateTag().putInt("SeekTime", 0);
        }
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
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new LauncherImageComponent(pStack));
    }

    @Override
    public String getAmmoDisplayName(GunData data) {
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
                    new Vec3(tag.getDouble("TargetPosX"), tag.getDouble("TargetPosY"), tag.getDouble("TargetPosZ")));

            for (Perk.Type type : Perk.Type.values()) {
                var instance = data.perk.getInstance(type);
                if (instance != null) {
                    instance.perk().modifyProjectile(data, instance, missileEntity);
                }
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

            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ShootClientMessage(10));
        }

        player.getCooldowns().addCooldown(stack.getItem(), 10);
        data.ammo.set(data.ammo.get() - 1);
    }

    @Override
    public void onFireKeyRelease(GunData data, Player player, double power, boolean zoom) {
        super.onFireKeyRelease(data, player, power, zoom);

        fire(player);

        var tag = data.tag();
        tag.putBoolean("Seeking", false);
        tag.putInt("SeekTime", 0);
        tag.putString("TargetEntity", "none");
        if (player instanceof ServerPlayer serverPlayer) {
            var clientboundstopsoundpacket = new ClientboundStopSoundPacket(new ResourceLocation(Mod.MODID, "javelin_lock"), SoundSource.PLAYERS);
            serverPlayer.connection.send(clientboundstopsoundpacket);
        }
    }

    @Override
    public void onShoot(GunData data, Player player, double spread, boolean zoom) {
    }

    @Override
    public void onFireKeyPress(GunData data, Player player, boolean zoom) {
        super.onFireKeyPress(data, player, zoom);

        if (!zoom || data.ammo.get() <= 0) return;

        var tag = data.tag();

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
}