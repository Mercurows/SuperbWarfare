package com.atsuishio.superbwarfare.item.common.ammo.box;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AmmoBoxInfo(String type, boolean isDrop) {
    public static final Codec<AmmoBoxInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("type").forGetter(AmmoBoxInfo::type),
            Codec.BOOL.fieldOf("is_drop").forGetter(AmmoBoxInfo::isDrop)
    ).apply(instance, AmmoBoxInfo::new));
}
