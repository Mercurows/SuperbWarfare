package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.entity.LoudlyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientSoundHandler {

    public static void playClientSoundInstance(Entity entity) {
        if (entity instanceof LoudlyEntity) {
            Minecraft.getInstance().getSoundManager().play(new LoudlyEntitySoundInstance.EntitySound(entity));
            Minecraft.getInstance().getSoundManager().play(new LoudlyEntitySoundInstance.EntitySoundClose(entity));
        }
    }
}
