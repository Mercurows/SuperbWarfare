package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.capability.laser.LaserHandler;
import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.entity.projectile.LaserEntity;
import com.atsuishio.superbwarfare.init.ModCapabilities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.network.message.send.LaserShootMessage;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

public class BeamTest extends Item {

    public BeamTest() {
        super(new Properties());
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {

        if (player.level().isClientSide) {
            player.playSound(ModSounds.CHARGE_RIFLE_FIRE_1P.get(), 1, 1);
        } else {
            player.playSound(ModSounds.CHARGE_RIFLE_FIRE_3P.get(), 2, 1);
        }

        var capability = player.getCapability(ModCapabilities.LASER_CAPABILITY);
        if (capability != null) {
            player.startUsingItem(hand);
            if (!level.isClientSide) {
                double px = player.getX();
                double py = player.getY() + player.getBbHeight() * 0.6F;
                double pz = player.getZ();
                float yHeadRotAngle = (float) Math.toRadians(player.yHeadRot + 90);
                float xHeadRotAngle = (float) (float) -Math.toRadians(player.getXRot());
                LaserEntity laserEntity = new LaserEntity(player.level(), player, px, py, pz, yHeadRotAngle, xHeadRotAngle, 6000);
                capability.init(new LaserHandler(player, laserEntity));
                capability.start();
            }
        }

        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (livingEntity instanceof Player player) {
            var cap = player.getCapability(ModCapabilities.LASER_CAPABILITY);
            if (cap != null) {
                cap.stop();
            }
        }
        if (livingEntity instanceof ServerPlayer serverPlayer && stack.getItem() instanceof BeamTest beamTest) {
            stopGunChargeSound(serverPlayer, beamTest);
        }


        super.releaseUsing(stack, level, livingEntity, timeCharged);
    }

    private static void stopGunChargeSound(ServerPlayer player, BeamTest beamTest) {
        beamTest.getChargeSound().forEach(sound -> {
            var clientboundstopsoundpacket = new ClientboundStopSoundPacket(sound.getLocation(), SoundSource.PLAYERS);
            final Vec3 center = new Vec3(player.getX(), player.getY(), player.getZ());
            for (ServerPlayer serverPlayer : player.level().getEntitiesOfClass(ServerPlayer.class, new AABB(center, center).inflate(48), e -> true)) {
                serverPlayer.connection.send(clientboundstopsoundpacket);
            }
        });
    }

    public Set<SoundEvent> getChargeSound() {
        return Set.of(ModSounds.CHARGE_RIFLE_FIRE_1P.get(), ModSounds.CHARGE_RIFLE_FIRE_3P.get());
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        if (pLivingEntity instanceof Player player) {
            var cap = player.getCapability(ModCapabilities.LASER_CAPABILITY);
            if (cap != null) {
                cap.stop();
            }
            player.getCooldowns().addCooldown(pStack.getItem(), 20);

            if (player.level().isClientSide()) {
                beamShoot(player);
                player.playSound(ModSounds.CHARGE_RIFLE_FIRE_BOOM_1P.get(), 1, 1);
            }
            if (!player.level().isClientSide) {
                player.playSound(ModSounds.CHARGE_RIFLE_FIRE_BOOM_3P.get(), 4, 1);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new ShakeClientMessage(10, 10, 30, serverPlayer.getX(), serverPlayer.getEyeY(), serverPlayer.getZ()));
            }
        }
        return super.finishUsingItem(pStack, pLevel, pLivingEntity);
    }

    public static void beamShoot(Player player) {
        Entity lookingEntity = TraceTool.laserfindLookingEntity(player, 512);

        if (lookingEntity == null) {
            return;
        }

        boolean canAttack = lookingEntity != player && !(lookingEntity instanceof Player player_ && (player_.isCreative() || player_.isSpectator()))
                && (!player.isAlliedTo(lookingEntity) || lookingEntity.getTeam() == null || lookingEntity.getTeam().getName().equals("TDM"));

        if (canAttack) {
            PacketDistributor.sendToServer(new LaserShootMessage(45, lookingEntity.getUUID(), TraceTool.laserHeadshot));
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 11;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        TooltipTool.addDevelopingText(tooltipComponents);
    }

}
