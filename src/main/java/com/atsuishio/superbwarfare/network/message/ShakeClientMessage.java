package com.atsuishio.superbwarfare.network.message;

import com.atsuishio.superbwarfare.ModUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = ModUtils.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public record ShakeClientMessage(
        double time, double radius, double amplitude,
        double x, double y, double z
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShakeClientMessage> TYPE = new CustomPacketPayload.Type<>(ModUtils.loc("shake_client"));

    public static final StreamCodec<ByteBuf, ShakeClientMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::time,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::radius,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::amplitude,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::x,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::y,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::z,
            ShakeClientMessage::new
    );

    public static void handler(final ShakeClientMessage message, final IPayloadContext context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        shakeTime = message.time;
        shakeRadius = message.radius;
        shakeAmplitude = message.amplitude * Mth.DEG_TO_RAD;
        shakePos[0] = message.x;
        shakePos[1] = message.y;
        shakePos[2] = message.z;
        shakeType = 2 * (Math.random() - 0.5);
    }

    public static double shakeTime = 0;
    public static double shakeRadius = 0;
    public static double shakeAmplitude = 0;
    public static double[] shakePos = {0, 0, 0};
    public static double shakeType = 0;

    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        ClientLevel level = Minecraft.getInstance().level;
        Entity entity = event.getCamera().getEntity();

        if (!(entity instanceof LivingEntity living)) return;
        ItemStack stack = living.getMainHandItem();

//        if (level != null &&
//                (stack.is(ModItems.MONITOR.get()) && NBTTool.getOrCreateTag(stack).getBoolean("Using") && NBTTool.getOrCreateTag(stack).getBoolean("Linked"))) {
//            handleDroneCamera(event, living);
//        } else {
//            var effect = Minecraft.getInstance().gameRenderer.currentEffect();
//            if (effect != null && effect.getName().equals(ModUtils.MODID + ":shaders/post/scan_pincushion.json")) {
//                Minecraft.getInstance().gameRenderer.shutdownEffect();
//            }
//        }

        float times = Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
        LocalPlayer player = Minecraft.getInstance().player;

        float yaw = event.getYaw();
        float pitch = event.getPitch();
        float roll = event.getRoll();

        shakeTime = Mth.lerp(0.175 * times, shakeTime, 0);

        if (player != null && shakeTime > 0) {
            float shakeRadiusAmplitude = (float) Mth.clamp(1 - player.position().distanceTo(new Vec3(shakePos[0], shakePos[1], shakePos[2])) / shakeRadius, 0, 1);

            boolean onVehicle = player.getVehicle() != null;

            if (shakeType > 0) {
                event.setYaw((float) (yaw + (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType * (onVehicle ? 0.4 : 1))));
                event.setPitch((float) (pitch - (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType * (onVehicle ? 0.4 : 1))));
                event.setRoll((float) (roll - (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * (onVehicle ? 0.4 : 1))));
            } else {
                event.setYaw((float) (yaw - (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType * (onVehicle ? 0.4 : 1))));
                event.setPitch((float) (pitch + (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType * (onVehicle ? 0.4 : 1))));
                event.setRoll((float) (roll + (shakeTime * Math.sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * (onVehicle ? 0.4 : 1))));
            }
        }

        // TODO 完善事件处理
//        cameraPitch = event.getPitch();
//        cameraYaw = event.getYaw();
//        cameraRoll = event.getRoll();
//
//        if (player != null && player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player)) {
//            return;
//        }
//
//        if (level != null && stack.is(ModTags.Items.GUN)) {
//            handleWeaponSway(living);
//            handleWeaponMove(living);
//            handleWeaponZoom(living);
//            handlePlayerBreath(living);
//            handleWeaponFire(event, living);
//            handleWeaponShell();
//            handleGunRecoil();
//            handleBowPullAnimation(living);
//            handleWeaponDraw(living);
//            handlePlayerCamera(event);
//        }
//
//        handleShockCamera(event, living);
    }


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
