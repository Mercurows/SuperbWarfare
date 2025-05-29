package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData;
import com.atsuishio.superbwarfare.data.vehicle.VehicleDataTool;
import com.atsuishio.superbwarfare.tools.BufferSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record VehiclesDataMessage(List<DefaultVehicleData> data) implements CustomPacketPayload {
    public static final Type<VehiclesDataMessage> TYPE = new Type<>(Mod.loc("set_vehicles_data"));


    public static final StreamCodec<FriendlyByteBuf, VehiclesDataMessage> STREAM_CODEC = StreamCodec.ofMember(
            (obj, buf) -> {
                buf.writeVarInt(obj.data.size());
                for (var data : obj.data) {
                    buf.writeBytes(BufferSerializer.serialize(data).copy());
                }
            },
            (buf) -> {
                var size = buf.readVarInt();
                var list = new ArrayList<DefaultVehicleData>();
                for (var i = 0; i < size; i++) {
                    list.add(BufferSerializer.deserialize(buf, new DefaultVehicleData()));
                }
                return new VehiclesDataMessage(list);
            }
    );

    public static VehiclesDataMessage create() {
        return new VehiclesDataMessage(VehicleDataTool.vehicleData.values().stream().toList());
    }

    public static void handler(final VehiclesDataMessage message) {
        VehicleDataTool.vehicleData.clear();

        for (var entry : message.data) {
            if (VehicleDataTool.vehicleData.containsKey(entry.id)) continue;
            VehicleDataTool.vehicleData.put(entry.id, entry);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
