package com.atsuishio.superbwarfare.client.renderer.armor;

import com.atsuishio.superbwarfare.client.model.armor.UsChestIotvModel;
import com.atsuishio.superbwarfare.item.armor.UsChestIotv;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class UsChestIotvArmorRenderer extends GeoArmorRenderer<UsChestIotv> {
    public UsChestIotvArmorRenderer() {
        super(new UsChestIotvModel());
        this.body = new GeoBone(null, "armorBody", false, (double) 0, false, false);
        this.rightArm = new GeoBone(null, "armorRightArm", false, (double) 0, false, false);
        this.leftArm = new GeoBone(null, "armorLeftArm", false, (double) 0, false, false);
    }

    @Override
    public RenderType getRenderType(UsChestIotv animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
