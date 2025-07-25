package com.atsuishio.superbwarfare.client.particle;

import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;

public class FastCloudOption implements ParticleOptions {

    public static final Codec<FastCloudOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.FLOAT.fieldOf("r").forGetter(option -> option.red),
                    Codec.FLOAT.fieldOf("g").forGetter(option -> option.green),
                    Codec.FLOAT.fieldOf("b").forGetter(option -> option.blue),
                    Codec.INT.fieldOf("life").forGetter(option -> option.life),
                    Codec.INT.fieldOf("size").forGetter(option -> option.size),
                    Codec.BOOL.fieldOf("cooldown").forGetter(option -> option.cooldown),
                    Codec.BOOL.fieldOf("light").forGetter(option -> option.light)
            ).apply(builder, FastCloudOption::new));

    @SuppressWarnings("deprecation")
    public static final Deserializer<FastCloudOption> DESERIALIZER = new Deserializer<>() {
        @Override
        public FastCloudOption fromCommand(ParticleType<FastCloudOption> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float r = reader.readFloat();
            reader.expect(' ');
            float g = reader.readFloat();
            reader.expect(' ');
            float b = reader.readFloat();
            reader.expect(' ');
            int life = reader.readInt();
            reader.expect(' ');
            int size = reader.readInt();
            reader.expect(' ');
            boolean cooldown = reader.readBoolean();
            reader.expect(' ');
            boolean light = reader.readBoolean();
            return new FastCloudOption(r, g, b, life, size, cooldown, light);
        }

        @Override
        public FastCloudOption fromNetwork(ParticleType<FastCloudOption> particleType, FriendlyByteBuf buffer) {
            return new FastCloudOption(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean());
        }
    };

    private final float red;
    private final float green;
    private final float blue;
    private final int life;
    private final int size;
    private final boolean cooldown;
    private final boolean light;

    public FastCloudOption(float r, float g, float b, int life, int size, boolean cooldown, boolean light) {
        this.red = r;
        this.green = g;
        this.blue = b;
        this.life = life;
        this.size = size;
        this.cooldown = cooldown;
        this.light = light;
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

    public int getSize() {
        return size;
    }

    public boolean getCooldown() {
        return cooldown;
    }

    public boolean getLight() {
        return light;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticleTypes.FAST_CLOUD.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.red);
        buffer.writeFloat(this.green);
        buffer.writeFloat(this.blue);
        buffer.writeInt(this.life);
        buffer.writeInt(this.size);
        buffer.writeBoolean(this.cooldown);
        buffer.writeBoolean(this.light);
    }

    @Override
    public String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()) + " [" + this.red + ", " + this.green + ", " + this.blue + ", " + this.life + ", " + this.size + ", " + this.cooldown + ", " + this.light + "]";
    }
}
