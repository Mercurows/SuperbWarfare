package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.client.VehicleSoundInstance;
import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.TrackEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.joml.Math;

import java.util.List;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientSoundHandler {

    @SubscribeEvent
    public static void handleJoinLevelEvent(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            if (event.getEntity() instanceof MobileVehicleEntity mobileVehicle) {
                Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.EngineSound(mobileVehicle, mobileVehicle.getEngineSound()));
            }
            if (event.getEntity() instanceof MobileVehicleEntity mobileVehicle && mobileVehicle instanceof TrackEntity) {
                Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.TrackSound(mobileVehicle));
            }
        }
    }

    @SubscribeEvent
    public static void handleClientTick(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        List<Entity> engineVehicle = SeekTool.getVehicleWithinRange(player, player.level(), 192);

        for (var e : engineVehicle) {
            if (e instanceof MobileVehicleEntity mobileVehicle) {

                Vec3 listener = player.getEyePosition();
                Vec3 engineRealPos = e.getEyePosition();
                Vec3 toVec = listener.vectorTo(engineRealPos).normalize();
                double distance = listener.distanceTo(engineRealPos);

                var engineSoundPos = new Vec3(listener.x + toVec.x, listener.y + toVec.y, listener.z + toVec.z);
                SoundEvent engineSound = mobileVehicle.getEngineSound();
                float distanceReduce;

                if (e instanceof DroneEntity) {
                    distanceReduce = (float) Math.max((1 - distance / 64), 0);
                    ItemStack stack = player.getMainHandItem();
                    final var tag = NBTTool.getTag(stack);
                    if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using")) {
                        player.playSound(engineSound, 1, (float) ((2 * Math.random() - 1) * 0.002f + 1.05));
                    } else {
                        player.level().playLocalSound(BlockPos.containing(engineSoundPos), engineSound, mobileVehicle.getSoundSource(), e.onGround() ? 0 : distanceReduce * distanceReduce, (float) ((2 * Math.random() - 1) * 0.002f + 1.05), false);
                    }
                }
            }
        }

        List<Entity> swarmDrone = SeekTool.getEntityWithinRange(player, player.level(), 64);

        for (var e : swarmDrone) {
            if (e instanceof SwarmDroneEntity swarmDroneEntity) {

                Vec3 listener = player.getEyePosition();
                Vec3 engineRealPos = e.getEyePosition();
                Vec3 toVec = listener.vectorTo(engineRealPos).normalize();
                double distance = listener.distanceTo(engineRealPos);

                var engineSoundPos = new Vec3(listener.x + toVec.x, listener.y + toVec.y, listener.z + toVec.z);
                SoundEvent engineSound = ModSounds.DRONE_SOUND.get();
                float distanceReduce;

                distanceReduce = (float) Math.max((1 - distance / 64), 0);
                if (swarmDroneEntity.tickCount > 10) {
                    player.level().playLocalSound(BlockPos.containing(engineSoundPos), engineSound, swarmDroneEntity.getSoundSource(), distanceReduce * distanceReduce, (float) ((2 * Math.random() - 1) * 0.002f + 1.15), false);
                }
            }
        }
    }
}
