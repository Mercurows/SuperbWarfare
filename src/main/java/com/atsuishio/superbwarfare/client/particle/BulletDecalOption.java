package com.atsuishio.superbwarfare.client.particle;

import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public class BulletDecalOption implements ParticleOptions {

    public static final MapCodec<BulletDecalOption> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    Codec.INT.fieldOf("dir").forGetter(option -> option.direction.ordinal()),
                    Codec.LONG.fieldOf("pos").forGetter(option -> option.pos.asLong()),
                    Codec.FLOAT.fieldOf("r").forGetter(option -> option.red),
                    Codec.FLOAT.fieldOf("g").forGetter(option -> option.green),
                    Codec.FLOAT.fieldOf("b").forGetter(option -> option.blue)
            ).apply(builder, BulletDecalOption::new));

    public static final StreamCodec<ByteBuf, BulletDecalOption> STREAM_CODEC = StreamCodec.composite(
            Direction.STREAM_CODEC,
            BulletDecalOption::getDirection,
            BlockPos.STREAM_CODEC,
            BulletDecalOption::getPos,
            ByteBufCodecs.FLOAT,
            BulletDecalOption::getRed,
            ByteBufCodecs.FLOAT,
            BulletDecalOption::getGreen,
            ByteBufCodecs.FLOAT,
            BulletDecalOption::getBlue,
            BulletDecalOption::new
    );

    private final Direction direction;
    private final BlockPos pos;
    private final float red;
    private final float green;
    private final float blue;

    public BulletDecalOption(int dir, long pos) {
        this(Direction.values()[dir], BlockPos.of(pos), 0.9f, 0f, 0f);
    }

    public BulletDecalOption(int dir, long pos, float r, float g, float b) {
        this(Direction.values()[dir], BlockPos.of(pos), r, g, b);
    }

    public BulletDecalOption(Direction dir, BlockPos pos) {
        this(dir, pos, 0.9f, 0f, 0f);
    }

    public BulletDecalOption(Direction dir, BlockPos pos, float r, float g, float b) {
        this.direction = dir;
        this.pos = pos;
        this.red = r;
        this.green = g;
        this.blue = b;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public BlockPos getPos() {
        return this.pos;
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
        return ModParticleTypes.BULLET_DECAL.get();
    }
}
