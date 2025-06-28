package com.atsuishio.superbwarfare.client.particle;

import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;

public class BulletDecalOption implements ParticleOptions {

    public static final Codec<BulletDecalOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.INT.fieldOf("dir").forGetter(option -> option.direction.ordinal()),
                    Codec.LONG.fieldOf("pos").forGetter(option -> option.pos.asLong()),
                    Codec.FLOAT.fieldOf("r").forGetter(option -> option.red),
                    Codec.FLOAT.fieldOf("g").forGetter(option -> option.green),
                    Codec.FLOAT.fieldOf("b").forGetter(option -> option.blue)
            ).apply(builder, BulletDecalOption::new));

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<BulletDecalOption> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public BulletDecalOption fromCommand(ParticleType<BulletDecalOption> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int dir = reader.readInt();
            reader.expect(' ');
            long pos = reader.readLong();
            reader.expect(' ');
            float r = reader.readFloat();
            reader.expect(' ');
            float g = reader.readFloat();
            reader.expect(' ');
            float b = reader.readFloat();
            return new BulletDecalOption(dir, pos, r, g, b);
        }

        @Override
        public BulletDecalOption fromNetwork(ParticleType<BulletDecalOption> particleType, FriendlyByteBuf buffer) {
            return new BulletDecalOption(buffer.readVarInt(), buffer.readLong(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        }
    };

    private final Direction direction;
    private final BlockPos pos;
    private final float red;
    private final float green;
    private final float blue;

    public BulletDecalOption(int dir, long pos) {
        this(Direction.values()[dir], BlockPos.of(pos), 0f, 0f, 0f);
    }

    public BulletDecalOption(int dir, long pos, float r, float g, float b) {
        this(Direction.values()[dir], BlockPos.of(pos), r, g, b);
    }

    public BulletDecalOption(Direction dir, BlockPos pos) {
        this(dir, pos, 0f, 0f, 0f);
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
    public ParticleType<?> getType() {
        return ModParticleTypes.BULLET_DECAL.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.direction);
        buffer.writeBlockPos(this.pos);
        buffer.writeFloat(this.red);
        buffer.writeFloat(this.green);
        buffer.writeFloat(this.blue);
    }

    @Override
    public String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()) + " " + this.direction.getName();
    }
}
