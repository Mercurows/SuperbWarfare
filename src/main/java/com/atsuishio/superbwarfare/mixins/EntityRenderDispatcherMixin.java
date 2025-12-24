package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes;
import com.atsuishio.superbwarfare.client.renderer.special.OBBRenderer;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Shadow
    public abstract <T extends Entity> EntityRenderer<? super T> getRenderer(T pEntity);

    @Shadow
    protected abstract void renderFlame(PoseStack poseStack, MultiBufferSource buffer, Entity entity, Quaternionf quaternion);

    @Shadow
    @Final
    public Options options;

    @Shadow
    private boolean shouldRenderShadow;

    @Shadow
    public abstract double distanceToSqr(double pX, double pY, double pZ);

    @Shadow
    private static void renderShadow(PoseStack pMatrixStack, MultiBufferSource pBuffer, Entity pEntity, float pWeight, float pPartialTicks, LevelReader pLevel, float pSize) {
    }

    @Shadow
    private Level level;

    @Shadow
    private boolean renderHitBoxes;

    @Shadow
    private static void renderHitbox(PoseStack poseStack, VertexConsumer buffer, Entity p_entity, float red, float green, float blue, float alpha) {
    }

    @Inject(method = "renderHitbox", at = @At("RETURN"))
    private static void onRenderHitbox(PoseStack poseStack, VertexConsumer buffer, Entity p_entity, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (p_entity instanceof VehicleEntity vehicle && !vehicle.enableAABB()) {
            OBBRenderer.INSTANCE.render(vehicle, vehicle.getOBBs(), poseStack, buffer, 0, 1, 0, 1, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        }
    }

    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void onPreRenderHitbox(PoseStack poseStack, VertexConsumer buffer, Entity p_entity, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (p_entity.getType().is(ModTags.EntityTypes.MINE) && MiscConfig.MINE_HITBOX_INVISIBLE.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    public <E extends Entity> void render(E pEntity, double pX, double pY, double pZ, float pRotationYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, CallbackInfo ci) {
        if (!ClientEventHandler.activeThermalImaging) return;
        ci.cancel();

        EntityRenderer<? super E> entityrenderer = this.getRenderer(pEntity);

        try {
            Vec3 vec3 = entityrenderer.getRenderOffset(pEntity, pPartialTicks);
            double d2 = pX + vec3.x();
            double d3 = pY + vec3.y();
            double d0 = pZ + vec3.z();
            pMatrixStack.pushPose();
            pMatrixStack.translate(d2, d3, d0);

            entityrenderer.render(pEntity, pRotationYaw, pPartialTicks, pMatrixStack, renderType -> pBuffer.getBuffer(ModRenderTypes.WHITE_SOLID), pPackedLight);

            if (pEntity.displayFireAnimation()) {
                this.renderFlame(pMatrixStack, pBuffer, pEntity);
            }

            var accessor = (EntityRendererAccessor<?>) entityrenderer;

            pMatrixStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
            if (this.options.entityShadows().get() && this.shouldRenderShadow && accessor.getShadowRadius() > 0.0F && !pEntity.isInvisible()) {
                double d1 = this.distanceToSqr(pEntity.getX(), pEntity.getY(), pEntity.getZ());
                float f = (float) ((1.0D - d1 / 256.0D) * (double) accessor.getShadowStrength());
                if (f > 0.0F) {
                    renderShadow(pMatrixStack, pBuffer, pEntity, f, pPartialTicks, this.level, Math.min(accessor.getShadowRadius(), 32.0F));
                }
            }

            if (this.renderHitBoxes && !pEntity.isInvisible() && !Minecraft.getInstance().showOnlyReducedInfo()) {
                renderHitbox(pMatrixStack, pBuffer.getBuffer(RenderType.lines()), pEntity, 1, 1, 1, 1);
            }

            pMatrixStack.popPose();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering entity in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being rendered");
            pEntity.fillCrashReportCategory(crashreportcategory);
            CrashReportCategory rendererDetails = crashreport.addCategory("Renderer details");
            rendererDetails.setDetail("Assigned renderer", entityrenderer);
            rendererDetails.setDetail("Location", CrashReportCategory.formatLocation(this.level, pX, pY, pZ));
            rendererDetails.setDetail("Rotation", pRotationYaw);
            rendererDetails.setDetail("Delta", pPartialTicks);
            throw new ReportedException(crashreport);
        }
    }
}
