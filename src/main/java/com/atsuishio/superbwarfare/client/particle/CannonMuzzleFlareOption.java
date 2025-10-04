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

public class CannonMuzzleFlareOption implements ParticleOptions {

    public static final Codec<CannonMuzzleFlareOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.INT.fieldOf("color").forGetter(option -> option.color),
                    Codec.INT.fieldOf("life").forGetter(option -> option.life),
                    Codec.FLOAT.fieldOf("fade").forGetter(option -> option.fade),
                    Codec.INT.fieldOf("animationSpeed").forGetter(option -> option.animation_speed),
                    Codec.FLOAT.fieldOf("sizeAdd").forGetter(option -> option.sizeAdd)
            ).apply(builder, CannonMuzzleFlareOption::new));

    @SuppressWarnings("deprecation")
    public static final Deserializer<CannonMuzzleFlareOption> DESERIALIZER = new Deserializer<>() {
        @Override
        public CannonMuzzleFlareOption fromCommand(ParticleType<CannonMuzzleFlareOption> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int color = reader.readInt();
            reader.expect(' ');
            int life = reader.readInt();
            reader.expect(' ');
            float fade = reader.readFloat();
            reader.expect(' ');
            int animation_speed = reader.readInt();
            reader.expect(' ');
            float sizeAdd = reader.readFloat();
            return new CannonMuzzleFlareOption(color, life, fade, animation_speed, sizeAdd);
        }

        @Override
        public CannonMuzzleFlareOption fromNetwork(ParticleType<CannonMuzzleFlareOption> particleType, FriendlyByteBuf buffer) {
            return new CannonMuzzleFlareOption(buffer.readInt(), buffer.readInt(), buffer.readFloat(), buffer.readInt(), buffer.readFloat());
        }
    };

    private final int color;
    private final int life;
    private final float fade;
    private final int animation_speed;
    private final float sizeAdd;

    public CannonMuzzleFlareOption(float r, float g, float b, int life, float fade, int animation_speed, float sizeAdd) {
        this(Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255), life, fade, animation_speed, sizeAdd);
    }

    public CannonMuzzleFlareOption(int color, int life, float fade, int animation_speed, float sizeAdd) {
        this.color = color;
        this.life = life;
        this.fade = fade;
        this.animation_speed = animation_speed;
        this.sizeAdd = sizeAdd;
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

    public float getFade() {
        return fade;
    }

    public int getAnimationSpeed() {
        return animation_speed;
    }

    public float getSizeAdd() {
        return sizeAdd;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticleTypes.CANNON_MUZZLE_FLARE.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(this.color);
        buffer.writeInt(this.life);
        buffer.writeFloat(this.fade);
        buffer.writeInt(this.animation_speed);
        buffer.writeFloat(this.sizeAdd);
    }

    @Override
    public String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()) + " [" + this.color + ", " + this.life + ", " + this.fade + ", " + this.animation_speed + ", " + this.sizeAdd + "]";
    }
}
