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

public class CustomCloudOption implements ParticleOptions {

    // TODO 为啥在服务端生成这个粒子会踢人
    public static final Codec<CustomCloudOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.INT.fieldOf("color").forGetter(option -> option.color),
                    Codec.INT.fieldOf("life").forGetter(option -> option.life),
                    Codec.FLOAT.fieldOf("size").forGetter(option -> option.size),
                    Codec.BOOL.fieldOf("cooldown").forGetter(option -> option.cooldown),
                    Codec.BOOL.fieldOf("light").forGetter(option -> option.light)
            ).apply(builder, CustomCloudOption::new));

    @SuppressWarnings("deprecation")
    public static final Deserializer<CustomCloudOption> DESERIALIZER = new Deserializer<>() {
        @Override
        public CustomCloudOption fromCommand(ParticleType<CustomCloudOption> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int color = reader.readInt();
            reader.expect(' ');
            int life = reader.readInt();
            reader.expect(' ');
            int size = reader.readInt();
            reader.expect(' ');
            boolean cooldown = reader.readBoolean();
            reader.expect(' ');
            boolean light = reader.readBoolean();
            return new CustomCloudOption(color, life, size, cooldown, light);
        }

        @Override
        public CustomCloudOption fromNetwork(ParticleType<CustomCloudOption> particleType, FriendlyByteBuf buffer) {
            return new CustomCloudOption(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean());
        }
    };

    private final int color;
    private final int life;
    private final float size;
    private final boolean cooldown;
    private final boolean light;

    public CustomCloudOption(float r, float g, float b, int life, float size, boolean cooldown, boolean light) {
        this(Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255), life, size, cooldown, light);
    }

    public CustomCloudOption(int color, int life, float size, boolean cooldown, boolean light) {
        this.color = color;
        this.life = life;
        this.size = size;
        this.cooldown = cooldown;
        this.light = light;
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

    public boolean getCooldown() {
        return cooldown;
    }

    public boolean getLight() {
        return light;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticleTypes.CUSTOM_CLOUD.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(this.color);
        buffer.writeInt(this.life);
        buffer.writeDouble(this.size);
        buffer.writeBoolean(this.cooldown);
        buffer.writeBoolean(this.light);
    }

    @Override
    public String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()) + " [" + this.color + ", " + this.life + ", " + this.size + ", " + this.cooldown + ", " + this.light + "]";
    }
}
