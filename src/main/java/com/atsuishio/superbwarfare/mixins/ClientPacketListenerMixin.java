package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow
    private ClientLevel level;

    /**
     * 正确处理VehicleEntity的带顺序乘客
     */
    @Inject(method = "handleSetEntityPassengersPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPassengersPacket;)V", at = @At("HEAD"), cancellable = true)
    public void vehicleEntityUpdate(ClientboundSetPassengersPacket pPacket, CallbackInfo ci) {
        var minecraft = Minecraft.getInstance();
        PacketUtils.ensureRunningOnSameThread(pPacket, (ClientPacketListener) (Object) this, minecraft);

        // 只处理VehicleEntity
        Entity entity = this.level.getEntity(pPacket.getVehicle());
        if (!(entity instanceof VehicleEntity vehicle)) return;
        ci.cancel();

        var player = minecraft.player;
        assert player != null;
        boolean hasIndirectPassenger = entity.hasIndirectPassenger(player);

        entity.ejectPassengers();

        // 获取排序后的Passengers
        var passengers = pPacket.getPassengers();
        vehicle.entityIndexOverride = (e) -> {
            for (int i = 0; i < passengers.length; i++) {
                if (passengers[i] == e.getId()) {
                    return i;
                }
            }
            return -1;
        };

        for (int i : passengers) {
            if (i == -1) continue;

            Entity passenger = this.level.getEntity(i);
            if (passenger != null) {
                passenger.startRiding(entity, true);

                if (passenger == player || hasIndirectPassenger) {
                    Component component = Component.translatable("mount.onboard", ModKeyMappings.DISMOUNT.getTranslatedKeyMessage());
                    minecraft.gui.setOverlayMessage(component, false);
                    minecraft.getNarrator().sayNow(component);
                }
            }
        }

        vehicle.entityIndexOverride = null;
    }

    // TODO what is this
//    @Inject(method = "postAddEntitySoundInstance(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
//    private void postAddEntitySoundInstance(Entity pEntity, CallbackInfo ci) {
//        if (pEntity instanceof LoudlyEntity) {
//            Minecraft.getInstance().getSoundManager().play(new LoudlyEntitySoundInstance.EntitySound(pEntity));
//            Minecraft.getInstance().getSoundManager().play(new LoudlyEntitySoundInstance.EntitySoundClose(pEntity));
//        } else {
//            Mod.queueClientWork(30, () -> {
//                if (pEntity instanceof MobileVehicleEntity mobileVehicle) {
//                    if (mobileVehicle instanceof TrackEntity) {
//                        Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.TrackSound(mobileVehicle));
//                    }
//                    if (mobileVehicle instanceof A10Entity) {
//                        Minecraft.getInstance().getSoundManager().play(new VehicleFireSoundInstance.A10FireSound(mobileVehicle));
//                    }
//                    if (mobileVehicle instanceof Hpj11Entity) {
//                        Minecraft.getInstance().getSoundManager().play(new VehicleFireSoundInstance.HPJ11CloseFireSound(mobileVehicle));
//                    }
//
//                    Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.EngineSound(mobileVehicle, mobileVehicle.getEngineSound()));
//                    Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.SwimSound(mobileVehicle));
//                }
//            });
//        }
//    }
}
