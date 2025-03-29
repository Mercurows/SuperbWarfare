package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.entity.vehicle.*;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Math;

import java.util.List;

import static com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity.PROPELLER_ROT;
import static com.atsuishio.superbwarfare.entity.vehicle.Bmp2Entity.DELTA_ROT;
import static com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity.POWER;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientSoundHandler {
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
                if (e instanceof Ah6Entity ah6Entity) {
                    distanceReduce = (float) Math.max((1 - distance / 128), 0);
                    if (player.getVehicle() == ah6Entity) {
                        player.playSound(ModSounds.HELICOPTER_ENGINE_1P.get(), 2 * (mobileVehicle.getEntityData().get(PROPELLER_ROT) - 0.012f), (float) ((2 * Math.random() - 1) * 0.1f + 1.0f));
                    } else {
                        player.level().playLocalSound(BlockPos.containing(engineSoundPos), engineSound, mobileVehicle.getSoundSource(), 5 * (mobileVehicle.getEntityData().get(PROPELLER_ROT) - 0.012f) * distanceReduce * distanceReduce, (float) ((2 * Math.random() - 1) * 0.1f + 1), false);
                    }
                }
                if (e instanceof Lav150Entity lav150) {
                    distanceReduce = (float) Math.max((1 - distance / 64), 0);
                    if (player.getVehicle() == lav150) {
                        player.playSound(ModSounds.LAV_ENGINE_1P.get(), 1 * (Mth.abs(mobileVehicle.getEntityData().get(POWER)) - 0.006f), (float) ((2 * Math.random() - 1) * 0.1f + 0.95f));
                    } else {
                        player.level().playLocalSound(BlockPos.containing(engineSoundPos), engineSound, mobileVehicle.getSoundSource(), 5 * (Mth.abs(mobileVehicle.getEntityData().get(POWER)) - 0.006f) * distanceReduce * distanceReduce, (float) ((2 * Math.random() - 1) * 0.1f + 1), false);
                    }
                }
                if (e instanceof Bmp2Entity bmp2) {
                    distanceReduce = (float) Math.max((1 - distance / 64), 0);
                    if (player.getVehicle() == bmp2) {
                        player.playSound(ModSounds.BMP_ENGINE_1P.get(), 1 * (Mth.abs(mobileVehicle.getEntityData().get(POWER)) + Mth.abs(0.08f * mobileVehicle.getEntityData().get(DELTA_ROT)) - 0.004f), (float) ((2 * Math.random() - 1) * 0.1f + 0.95f));
                    } else {
                        player.level().playLocalSound(BlockPos.containing(engineSoundPos), engineSound, mobileVehicle.getSoundSource(), 5 * (Mth.abs(mobileVehicle.getEntityData().get(POWER)) + Mth.abs(0.08f * mobileVehicle.getEntityData().get(DELTA_ROT)) - 0.004f) * distanceReduce * distanceReduce, (float) ((2 * Math.random() - 1) * 0.1f + 1), false);
                    }
                }
                if (e instanceof Yx100Entity yx100) {
                    distanceReduce = (float) Math.max((1 - distance / 64), 0);
                    if (player.getVehicle() == yx100) {
                        player.playSound(ModSounds.BMP_ENGINE_1P.get(), 1 * (Mth.abs(mobileVehicle.getEntityData().get(POWER)) + Mth.abs(0.08f * mobileVehicle.getEntityData().get(DELTA_ROT)) - 0.004f), (float) ((2 * Math.random() - 1) * 0.1f + 0.95f));
                    } else {
                        player.level().playLocalSound(BlockPos.containing(engineSoundPos), engineSound, mobileVehicle.getSoundSource(), 5 * (Mth.abs(mobileVehicle.getEntityData().get(POWER)) + Mth.abs(0.08f * mobileVehicle.getEntityData().get(DELTA_ROT)) - 0.004f) * distanceReduce * distanceReduce, (float) ((2 * Math.random() - 1) * 0.1f + 1), false);
                    }
                }
                if (e instanceof DroneEntity drone) {
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
    }
}
