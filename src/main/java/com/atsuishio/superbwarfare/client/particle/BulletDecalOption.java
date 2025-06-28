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
                    Codec.LONG.fieldOf("pos").forGetter(option -> option.pos.asLong())
            ).apply(builder, BulletDecalOption::new));

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<BulletDecalOption> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public BulletDecalOption fromCommand(ParticleType<BulletDecalOption> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int dir = reader.readInt();
            reader.expect(' ');
            long pos = reader.readLong();
            return new BulletDecalOption(dir, pos);
        }

        @Override
        public BulletDecalOption fromNetwork(ParticleType<BulletDecalOption> particleType, FriendlyByteBuf buffer) {
            return new BulletDecalOption(buffer.readVarInt(), buffer.readLong());
        }
    };

    private final Direction direction;
    private final BlockPos pos;

    public BulletDecalOption(int dir, long pos) {
        this.direction = Direction.values()[dir];
        this.pos = BlockPos.of(pos);
    }

    public BulletDecalOption(Direction dir, BlockPos pos) {
        this.direction = dir;
        this.pos = pos;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticleTypes.BULLET_DECAL.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.direction);
        buffer.writeBlockPos(this.pos);
    }

    @Override
    public String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()) + " " + this.direction.getName();
    }
}
