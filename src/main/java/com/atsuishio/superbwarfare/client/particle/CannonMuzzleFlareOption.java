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

public record CannonMuzzleFlareOption(
        int color,
        int life,
        float fade,
        int animationSpeed,
        float sizeAdd
) implements ParticleOptions {

    public static final MapCodec<CannonMuzzleFlareOption> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    Codec.INT.fieldOf("color").forGetter(option -> option.color),
                    Codec.INT.fieldOf("life").forGetter(option -> option.life),
                    Codec.FLOAT.fieldOf("fade").forGetter(option -> option.fade),
                    Codec.INT.fieldOf("animationSpeed").forGetter(option -> option.animationSpeed),
                    Codec.FLOAT.fieldOf("sizeAdd").forGetter(option -> option.sizeAdd)
            ).apply(builder, CannonMuzzleFlareOption::new));

    public static final StreamCodec<ByteBuf, CannonMuzzleFlareOption> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            CannonMuzzleFlareOption::color,
            ByteBufCodecs.VAR_INT,
            CannonMuzzleFlareOption::life,
            ByteBufCodecs.FLOAT,
            CannonMuzzleFlareOption::fade,
            ByteBufCodecs.VAR_INT,
            CannonMuzzleFlareOption::animationSpeed,
            ByteBufCodecs.FLOAT,
            CannonMuzzleFlareOption::sizeAdd,
            CannonMuzzleFlareOption::new
    );

    public CannonMuzzleFlareOption(float r, float g, float b, int life, float fade, int animation_speed, float sizeAdd) {
        this(Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255), life, fade, animation_speed, sizeAdd);
    }

    public float getRed() {
        return (this.color >> 16 & 255) / 255f;
    }

    public float getGreen() {
        return (this.color >> 8 & 255) / 255f;
    }

    public float getBlue() {
        return (this.color & 255) / 255f;
    }

    public int getAnimationSpeed() {
        return animationSpeed;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticleTypes.CANNON_MUZZLE_FLARE.get();
    }
}
