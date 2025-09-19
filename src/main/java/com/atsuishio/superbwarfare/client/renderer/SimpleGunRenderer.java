package com.atsuishio.superbwarfare.client.renderer;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class SimpleGunRenderer<T extends GunGeoItem & GeoAnimatable> extends CustomGunRenderer<T> {

    private final double x;
    private final double y;
    private final double z;
    private final double size;
    private final boolean useOldHandRenderer;

    public SimpleGunRenderer(GeoModel<T> model, double x, double y, double z, double size, boolean useOldHandRenderer) {
        super(model);

        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
        this.useOldHandRenderer = useOldHandRenderer;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        Minecraft mc = Minecraft.getInstance();
        String name = bone.getName();
        boolean renderingArms = false;
        if (name.equals("Lefthand") || name.equals("Righthand")) {
            bone.setHidden(true);
            renderingArms = true;
        } else {
            bone.setHidden(false);
        }

        var player = mc.player;
        if (player == null) return;
        ItemStack itemStack = player.getMainHandItem();

        if (itemStack.getItem() instanceof GunItem && GeoItem.getId(itemStack) == this.getInstanceId(animatable)) {
            if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || this.renderPerspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                AnimationHelper.handleShootFlare(name, poseStack, itemStack, bone, bufferSource, packedLight, this.x, this.y, this.z, this.size);
            }
        }

        if (renderingArms) {
            AnimationHelper.renderArms(player, this.renderPerspective, poseStack, name, bone, bufferSource, renderType, packedLight, this.useOldHandRenderer);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
