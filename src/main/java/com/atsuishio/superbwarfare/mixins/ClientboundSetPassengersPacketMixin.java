package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientboundSetPassengersPacket.class)
public class ClientboundSetPassengersPacketMixin {

    @Mutable
    @Shadow
    @Final
    private int[] passengers;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private void init(Entity entity, CallbackInfo ci) {
        if (entity instanceof VehicleEntity vehicle) {
            // 使用顺序乘客信息代替原乘客信息
            List<Entity> list = vehicle.getOrderedPassengers();
            passengers = new int[list.size()];

            for (int i = 0; i < list.size(); ++i) {
                var passenger = list.get(i);
                passengers[i] = passenger == null ? -1 : passenger.getId();
            }
        }
    }
}