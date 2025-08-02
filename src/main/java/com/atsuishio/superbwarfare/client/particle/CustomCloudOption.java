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
import org.joml.Vector3f;

public class CustomCloudOption implements ParticleOptions {

    public static final MapCodec<CustomCloudOption> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    Codec.FLOAT.fieldOf("r").forGetter(option -> option.red),
                    Codec.FLOAT.fieldOf("g").forGetter(option -> option.green),
                    Codec.FLOAT.fieldOf("b").forGetter(option -> option.blue),
                    Codec.INT.fieldOf("life").forGetter(option -> option.life),
                    Codec.FLOAT.fieldOf("size").forGetter(option -> option.size),
                    Codec.BOOL.fieldOf("cooldown").forGetter(option -> option.cooldown),
                    Codec.BOOL.fieldOf("light").forGetter(option -> option.light)
            ).apply(builder, CustomCloudOption::new));

    public static final StreamCodec<ByteBuf, CustomCloudOption> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VECTOR3F, CustomCloudOption::getColor,
            ByteBufCodecs.INT, CustomCloudOption::getLife,
            ByteBufCodecs.FLOAT, CustomCloudOption::getSize,
            ByteBufCodecs.BOOL, CustomCloudOption::getCooldown,
            ByteBufCodecs.BOOL, CustomCloudOption::getLight,
            CustomCloudOption::new
    );

    private final float red;
    private final float green;
    private final float blue;
    private final int life;
    private final float size;
    private final boolean cooldown;
    private final boolean light;


    public CustomCloudOption(Vector3f color, int life, float size, boolean cooldown, boolean light) {
        this(color.x, color.y, color.z, life, size, cooldown, light);
    }

    public CustomCloudOption(float r, float g, float b, int life, float size, boolean cooldown, boolean light) {
        this.red = r;
        this.green = g;
        this.blue = b;
        this.life = life;
        this.size = size;
        this.cooldown = cooldown;
        this.light = light;
    }

    public Vector3f getColor() {
        return new Vector3f(red, green, blue);
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

    public int getLife() {
        return life;
    }

    public float getSize() {
        return size;
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
