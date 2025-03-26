package com.atsuishio.superbwarfare.network.message;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ShootMessage(double spread) implements CustomPacketPayload {
    public static final Type<ShootMessage> TYPE = new Type<>(ModUtils.loc("shoot"));

    public static final StreamCodec<ByteBuf, ShootMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ShootMessage::spread,
            ShootMessage::new
    );

    public static void handler(final ShootMessage message, final IPayloadContext context) {
        pressAction(context.player(), message.spread);
    }

    public static void pressAction(Player player, double spared) {
        ItemStack stack = player.getMainHandItem();
        if (stack.is(ModTags.Items.NORMAL_GUN)) {
            int projectileAmount = GunsTool.getGunIntTag(stack, "ProjectileAmount", 1);

            if (GunsTool.getGunIntTag(stack, "Ammo", 0) > 0) {
                // 空仓挂机
                if (GunsTool.getGunIntTag(stack, "Ammo", 0) == 1) {
                    GunsTool.setGunBooleanTag(stack, "HoldOpen", true);
                }

                if (stack.is(ModTags.Items.REVOLVER)) {
                    NBTTool.getOrCreateTag(stack).putBoolean("canImmediatelyShoot", false);
                }

                // 判断是否为栓动武器（BoltActionTime > 0），并在开火后给一个需要上膛的状态
                if (GunsTool.getGunIntTag(stack, "BoltActionTime", 0) > 0 && GunsTool.getGunIntTag(stack, "Ammo", 0) > (stack.is(ModTags.Items.REVOLVER) ? 0 : 1)) {
                    GunsTool.setGunBooleanTag(stack, "NeedBoltAction", true);
                }

                GunsTool.setGunIntTag(stack, "Ammo", GunsTool.getGunIntTag(stack, "Ammo", 0) - 1);

                NBTTool.getOrCreateTag(stack).putDouble("empty", 1);

//                if (stack.getItem() == ModItems.M_60.get() && GunsTool.getGunIntTag(stack, "Ammo", 0) <= 5) {
//                    GunsTool.setGunBooleanTag(stack, "HideBulletChain", true);
//                }
//
//                if (stack.getItem() == ModItems.HOMEMADE_SHOTGUN.get()) {
//                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
//                    if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {
//                        ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, player.getX() + 1.8 * player.getLookAngle().x, player.getY() + player.getBbHeight() - 0.1 + 1.8 * player.getLookAngle().y,
//                                player.getZ() + 1.8 * player.getLookAngle().z, 30, 0.4, 0.4, 0.4, 0.005, true, serverPlayer);
//                    }
//                }
//
//                if (stack.getItem() == ModItems.SENTINEL.get()) {
//                    stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
//                            iEnergyStorage -> iEnergyStorage.extractEnergy(3000, false)
//                    );
//                }
//
//                var perk = PerkHelper.getPerkByType(stack, Perk.Type.AMMO);

                for (int index0 = 0; index0 < (
                        // todo perk
//                        perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 1 : projectileAmount
                        projectileAmount
                ); index0++) {
                    GunEventHandler.gunShoot(player, spared);
                }

                GunEventHandler.playGunSounds(player);
            }
        }
        // todo minigun
//        else if (stack.is(ModItems.MINIGUN.get())) {
//            var tag = GunNBTTool.getOrCreateTag(stack);
//
//            if ((player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables())).rifleAmmo > 0
//                    || InventoryTool.hasCreativeAmmoBox(player)) {
//                tag.putDouble("heat", (tag.getDouble("heat") + 0.1));
//                if (tag.getDouble("heat") >= 50.5) {
//                    tag.putDouble("overheat", 40);
//                    player.getCooldowns().addCooldown(stack.getItem(), 40);
//                    if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
//                        SoundTool.playLocalSound(serverPlayer, ModSounds.MINIGUN_OVERHEAT.get(), 2f, 1f);
//                    }
//                }
//                var perk = PerkHelper.getPerkByType(stack, Perk.Type.AMMO);
//                float pitch = tag.getDouble("heat") <= 40 ? 1 : (float) (1 - 0.025 * Math.abs(40 - tag.getDouble("heat")));
//
//                if (!player.level().isClientSide() && player instanceof ServerPlayer) {
//                    float soundRadius = (float) GunsTool.getGunDoubleTag(stack, "SoundRadius");
//
//                    player.playSound(ModSounds.MINIGUN_FIRE_3P.get(), soundRadius * 0.2f, pitch);
//                    player.playSound(ModSounds.MINIGUN_FAR.get(), soundRadius * 0.5f, pitch);
//                    player.playSound(ModSounds.MINIGUN_VERYFAR.get(), soundRadius, pitch);
//
//                    if (perk == ModPerks.BEAST_BULLET.get()) {
//                        player.playSound(ModSounds.HENG.get(), 4f, pitch);
//                    }
//                }
//
//                GunEventHandler.gunShoot(player, spared);
//                if (!InventoryTool.hasCreativeAmmoBox(player)) {
//                    player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
//                        capability.rifleAmmo = player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables()).rifleAmmo - 1;
//                        capability.syncPlayerVariables(player);
//                    });
//                }
//            }
//        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
