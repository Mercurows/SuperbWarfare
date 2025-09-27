package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.WheelChairModel;
import com.atsuishio.superbwarfare.entity.vehicle.WheelChairEntity;
import com.atsuishio.superbwarfare.entity.vehicle.quatern.QuaternionHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WheelChairRenderer extends GeoEntityRenderer<WheelChairEntity> {

    public WheelChairRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WheelChairModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public RenderType getRenderType(WheelChairEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void render(WheelChairEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        // 应用四元数旋转
        applyVehicleRotation(entityIn, poseStack, partialTicks);

        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    /**
     * 应用四元数旋转到PoseStack
     */
    protected void applyVehicleRotation(WheelChairEntity entity, PoseStack poseStack, float partialTicks) {
        QuaternionHelper rotation = entity.getRenderRotation(partialTicks);

        // 转换为Minecraft的四元数
        Quaternionf quaternionf = new Quaternionf(rotation.getX(), rotation.getY(), rotation.getZ(), rotation.getW());

        // 应用旋转
//        poseStack.mulPose(quaternionf);
        Vec3 root = new Vec3(0, entity.rotateYOffset(), 0);
        poseStack.rotateAround(quaternionf, (float) root.x, (float) root.y, (float) root.z);

//        // 调试：显示旋转角度
//        if (Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) {
//            Vector3f euler = entity.getEulerAnglesDeg();
//            // 可以在这里添加调试信息渲染
//        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, WheelChairEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        String name = bone.getName();
        if (name.equals("w_rb")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.rightWheelRotO, animatable.getRightWheelRot()));
        }
        if (name.equals("w_lb")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.leftWheelRotO, animatable.getLeftWheelRot()));
        }
        if (name.equals("w_rr")) {
            bone.setRotX(4 * Mth.lerp(partialTick, animatable.rightWheelRotO, animatable.getRightWheelRot()));
        }
        if (name.equals("w_lr")) {
            bone.setRotX(4 * Mth.lerp(partialTick, animatable.leftWheelRotO, animatable.getLeftWheelRot()));
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
