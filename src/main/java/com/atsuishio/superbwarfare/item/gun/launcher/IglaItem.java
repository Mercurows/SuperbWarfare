package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.IglaItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.entity.projectile.IglaMissileEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.receive.ShootClientMessage;
import com.atsuishio.superbwarfare.perk.Perk;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class IglaItem extends GunGeoItem {

    public IglaItem() {
        super(new Properties().rarity(Rarity.EPIC));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return IglaItemRenderer::new;
    }

    private PlayState idlePredicate(AnimationState<IglaItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.igla_9k38.idle"));

        if (GunData.from(stack).reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.igla_9k38.reload"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.igla_9k38.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 0, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.IGLA_RELOAD_EMPTY.get(), ModSounds.IGLA_LOCK.get(), ModSounds.IGLA_LOCKON.get());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (entity instanceof Player player && selected) {
            var tag = stack.getOrCreateTag();
            if (tag.getBoolean("Seeking")) {
                List<Entity> decoy = SeekTool.seekLivingEntities(player, 512, 20);
                for (var e : decoy) {
                    if (e.getType().is(ModTags.EntityTypes.DECOY)) {
                        tag.putString("TargetEntity", e.getStringUUID());
                        tag.putDouble("TargetPosX", e.position().x);
                        tag.putDouble("TargetPosY", e.position().y);
                        tag.putDouble("TargetPosZ", e.position().z);
                    }
                }

                Entity targetEntity = EntityFindUtil.findEntity(player.level(), tag.getString("TargetEntity"));

                if (targetEntity != null && VectorTool.calculateAngle(player.getViewVector(1), player.getEyePosition().vectorTo(targetEntity.getBoundingBox().getCenter())) < 20) {
                    tag.putInt("SeekTime", tag.getInt("SeekTime") + 1);
                    if (tag.getInt("SeekTime") > 0 && (!targetEntity.getPassengers().isEmpty() || targetEntity instanceof VehicleEntity) && targetEntity.tickCount % 3 == 0) {
                        targetEntity.level().playSound(null, targetEntity.getOnPos(), targetEntity instanceof Pig ? SoundEvents.PIG_HURT : ModSounds.LOCKING_WARNING.get(), SoundSource.PLAYERS, 1, 1f);
                    }
                } else {
                    tag.putInt("SeekTime", 0);
                }

                if (tag.getInt("SeekTime") == 1 && player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.IGLA_LOCK.get(), 1, 1);
                }

                if (targetEntity != null && tag.getInt("SeekTime") > 30) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.IGLA_LOCKON.get(), 1, 1);
                    }
                    if ((!targetEntity.getPassengers().isEmpty() || targetEntity instanceof VehicleEntity) && targetEntity.tickCount % 2 == 0) {
                        targetEntity.level().playSound(null, targetEntity.getOnPos(), targetEntity instanceof Pig ? SoundEvents.PIG_HURT : ModSounds.LOCKED_WARNING.get(), SoundSource.PLAYERS, 1, 0.95f);
                    }
                }

                Entity seekingEntity = SeekTool.seekEntity(player,512, 20);

                if (seekingEntity != null && seekingEntity.getType().is(ModTags.EntityTypes.DECOY)) {
                    tag.putInt("SeekTime", 0);
                }
            }

            GunData data = GunData.from(stack);

            if (data.reloading()) {
                tag.putBoolean("Seeking", false);
                tag.putInt("SeekTime", 0);
                tag.putString("TargetEntity", "none");
            }
        } else {
            stack.getOrCreateTag().putInt("SeekTime", 0);
        }
    }

    @Override
    public boolean useSpecialFireProcedure(GunData data) {
        return true;
    }

    @Override
    public ResourceLocation getGunIcon(GunData data) {
        return Mod.loc("textures/gun_icon/igla_9k38_icon.png");
    }

    private void fire(Player player) {
        Level level = player.level();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);
        CompoundTag tag = data.tag();

        if (tag.getInt("SeekTime") < 30) return;

        float yRot = player.getYRot() + 360;
        yRot = (yRot + 90) % 360;

        var firePos = new Vector3d(0, -0.2, 0.15);
        firePos.rotateZ(-player.getXRot() * Mth.DEG_TO_RAD);
        firePos.rotateY(-yRot * Mth.DEG_TO_RAD);

        if (player.level() instanceof ServerLevel serverLevel) {
            IglaMissileEntity missileEntity = new IglaMissileEntity(player, level,
                    data.get(GunProp.DAMAGE).floatValue(),
                    data.get(GunProp.EXPLOSION_DAMAGE).floatValue(),
                    data.get(GunProp.EXPLOSION_RADIUS).floatValue()
            );

            for (Perk.Type type : Perk.Type.values()) {
                var instance = data.perk.getInstance(type);
                if (instance != null) {
                    instance.perk().modifyProjectile(data, instance, missileEntity);
                }
            }

            missileEntity.setPos(player.getX() + firePos.x, player.getEyeY() + firePos.y, player.getZ() + firePos.z);
            missileEntity.shoot(player.getLookAngle().x, player.getLookAngle().y + 0.12, player.getLookAngle().z, 3f, 1);
            missileEntity.setTargetUuid(tag.getString("TargetEntity"));

            level.addFreshEntity(missileEntity);
            ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, player.getX() + 1.8 * player.getLookAngle().x,
                    player.getY() + player.getBbHeight() - 0.1 + 1.8 * player.getLookAngle().y,
                    player.getZ() + 1.8 * player.getLookAngle().z,
                    30, 0.4, 0.4, 0.4, 0.005, true);

            var serverPlayer = (ServerPlayer) player;

            SoundTool.playLocalSound(serverPlayer, ModSounds.IGLA_FIRE_1P.get(), 2, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.IGLA_FIRE_3P.get(), SoundSource.PLAYERS, 4, 1);
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.IGLA_FAR.get(), SoundSource.PLAYERS, 10, 1);

            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ShootClientMessage(10));
        }

        data.ammo.set(data.ammo.get() - data.get(GunProp.AMMO_COST_PER_SHOOT));
    }

    @Override
    public void shoot(@NotNull ShootParameters parameters) {
    }

    @Override
    public void onFireKeyPress(GunData data, Player player, boolean zoom) {
        super.onFireKeyPress(data, player, zoom);
        if (!zoom || !data.hasEnoughAmmoToShoot(player)) return;
        fire(player);
    }

    @Override
    public void onChangeSlot(ItemStack stack, Player player) {
        super.onChangeSlot(stack, player);
        GunData data = GunData.from(stack);
        var tag = data.tag();
        tag.remove("Seeking");
        tag.remove("SeekTime");
        tag.putString("TargetEntity", "none");

        if (player instanceof ServerPlayer serverPlayer) {
            var clientboundstopsoundpacket = new ClientboundStopSoundPacket(Mod.loc("igla_9k38_lock"), SoundSource.PLAYERS);
            serverPlayer.connection.send(clientboundstopsoundpacket);
        }
    }
}