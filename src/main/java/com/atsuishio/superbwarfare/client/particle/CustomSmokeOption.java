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

public class CustomSmokeOption implements ParticleOptions {

    public static final Codec<CustomSmokeOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.FLOAT.fieldOf("r").forGetter(option -> option.red),
                    Codec.FLOAT.fieldOf("g").forGetter(option -> option.green),
                    Codec.FLOAT.fieldOf("b").forGetter(option -> option.blue)
            ).apply(builder, CustomSmokeOption::new));

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<CustomSmokeOption> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public CustomSmokeOption fromCommand(ParticleType<CustomSmokeOption> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float r = reader.readFloat();
            reader.expect(' ');
            float g = reader.readFloat();
            reader.expect(' ');
            float b = reader.readFloat();
            return new CustomSmokeOption(r, g, b);
        }

        @Override
        public CustomSmokeOption fromNetwork(ParticleType<CustomSmokeOption> particleType, FriendlyByteBuf buffer) {
            return new CustomSmokeOption(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        }
    };

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
    public ParticleType<?> getType() {
        return ModParticleTypes.CUSTOM_SMOKE.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.red);
        buffer.writeFloat(this.green);
        buffer.writeFloat(this.blue);
    }

    @Override
    public String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()) + " [" + this.red + ", " + this.green + ", " + this.blue + "]";
    }
}
