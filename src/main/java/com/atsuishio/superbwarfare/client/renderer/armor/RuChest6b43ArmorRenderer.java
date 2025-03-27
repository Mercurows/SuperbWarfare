package com.atsuishio.superbwarfare.client.renderer.armor;

import com.atsuishio.superbwarfare.client.model.armor.RuChest6b43Model;
import com.atsuishio.superbwarfare.item.armor.RuChest6b43;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class RuChest6b43ArmorRenderer extends GeoArmorRenderer<RuChest6b43> {
    public RuChest6b43ArmorRenderer() {
        super(new RuChest6b43Model());
        this.body = new GeoBone(null, "armorBody", false, (double) 0, false, false);
        this.rightArm = new GeoBone(null, "armorRightArm", false, (double) 0, false, false);
        this.leftArm = new GeoBone(null, "armorLeftArm", false, (double) 0, false, false);
    }

    @Override
    public RenderType getRenderType(RuChest6b43 animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
