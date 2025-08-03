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

public class CustomCloudOption implements ParticleOptions {

    public static final MapCodec<CustomCloudOption> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    Codec.INT.fieldOf("color").forGetter(option -> option.color),
                    Codec.INT.fieldOf("life").forGetter(option -> option.life),
                    Codec.FLOAT.fieldOf("size").forGetter(option -> option.size),
                    Codec.FLOAT.fieldOf("gravity").forGetter(option -> option.gravity),
                    Codec.BOOL.fieldOf("cooldown").forGetter(option -> option.cooldown),
                    Codec.BOOL.fieldOf("light").forGetter(option -> option.light)
            ).apply(builder, CustomCloudOption::new));

    public static final StreamCodec<ByteBuf, CustomCloudOption> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, CustomCloudOption::getColor,
            ByteBufCodecs.INT, CustomCloudOption::getLife,
            ByteBufCodecs.FLOAT, CustomCloudOption::getSize,
            ByteBufCodecs.FLOAT, CustomCloudOption::getGravity,
            ByteBufCodecs.BOOL, CustomCloudOption::getCooldown,
            ByteBufCodecs.BOOL, CustomCloudOption::getLight,
            CustomCloudOption::new
    );

    private final int color;
    private final int life;
    private final float size;
    private final float gravity;
    private final boolean cooldown;
    private final boolean light;


    public CustomCloudOption(float r, float g, float b, int life, float size, float gravity, boolean cooldown, boolean light) {
        this(Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255), life, size, gravity, cooldown, light);
    }

    public CustomCloudOption(int color, int life, float size, float gravity, boolean cooldown, boolean light) {
        this.color = color;
        this.life = life;
        this.size = size;
        this.gravity = gravity;
        this.cooldown = cooldown;
        this.light = light;
    }

    public int getColor() {
        return color;
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

    public int getLife() {
        return life;
    }

    public float getSize() {
        return size;
    }

    public float getGravity() {
        return gravity;
    }

    public boolean getCooldown() {
        return cooldown;
    }

    public boolean getLight() {
        return light;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticleTypes.CUSTOM_CLOUD.get();
    }
}
