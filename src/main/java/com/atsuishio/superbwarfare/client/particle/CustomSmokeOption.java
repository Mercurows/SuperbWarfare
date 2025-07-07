package com.atsuishio.superbwarfare.client.particle;

import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public class CustomSmokeOption implements ParticleOptions {

    public static final MapCodec<CustomSmokeOption> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    Codec.FLOAT.fieldOf("r").forGetter(option -> option.red),
                    Codec.FLOAT.fieldOf("g").forGetter(option -> option.green),
                    Codec.FLOAT.fieldOf("b").forGetter(option -> option.blue)
            ).apply(builder, CustomSmokeOption::new));

    public static final StreamCodec<ByteBuf, CustomSmokeOption> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            CustomSmokeOption::getRed,
            ByteBufCodecs.FLOAT,
            CustomSmokeOption::getGreen,
            ByteBufCodecs.FLOAT,
            CustomSmokeOption::getBlue,
            CustomSmokeOption::new
    );

    private final float red;
    private final float green;
    private final float blue;

    public CustomSmokeOption(float r, float g, float b) {
        this.red = r;
        this.green = g;
        this.blue = b;
    }

    public float getRed() {
        return red;
    }

    public float getGreen() {
        return green;
    }

    public float getBlue() {
        return blue;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticleTypes.CUSTOM_SMOKE.get();
    }
}
