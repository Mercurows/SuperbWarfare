package com.atsuishio.superbwarfare.client.renderer.curio;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.model.curio.ParachuteModel;
import com.atsuishio.superbwarfare.item.curio.ParachuteItem;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ParachuteRenderer implements ICurioRenderer {

    private static ParachuteModel firstPersonModel;
    private static final ResourceLocation TEXTURE = Mod.loc("textures/curio/parachute.png");

    private final ParachuteModel model;

    public ParachuteRenderer() {
        model = new ParachuteModel(Minecraft.getInstance().getEntityModels().bakeLayer(ParachuteModel.LAYER_LOCATION));
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack matrixStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        matrixStack.pushPose();

        matrixStack.scale(0.5f, 0.5f, 0.5f);
        matrixStack.translate(0, 1.25, 0);

        if (NBTTool.getTag(stack).getBoolean(ParachuteItem.TAG_OPEN)) {
            LivingEntity entity = slotContext.entity();
            this.model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
            this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            VertexConsumer vertexconsumer = ItemRenderer.getArmorFoilBuffer(renderTypeBuffer, RenderType.armorCutoutNoCull(TEXTURE), stack.hasFoil());

            model.renderToBuffer(matrixStack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }

        matrixStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        RenderBuffers buffers = Minecraft.getInstance().renderBuffers();
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!ParachuteItem.isParachuteOpen(player)) return;
        if (!ParachuteItem.isParachuteVisible(player)) return;
        PoseStack stack = event.getPoseStack();

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
            stack.pushPose();

            if (firstPersonModel == null) {
                firstPersonModel = new ParachuteModel(Minecraft.getInstance().getEntityModels().bakeLayer(ParachuteModel.LAYER_LOCATION));
            }

            stack.mulPose(Axis.XP.rotationDegrees(180));
            stack.mulPose(Axis.YP.rotationDegrees(player.getViewYRot(1f)));
            stack.translate(0, 1.5, 0);

            firstPersonModel.prepareMobModel(player, 0, 0, event.getPartialTick().getGameTimeDeltaPartialTick(true));
            firstPersonModel.setupAnim(player, 0, 0, player.tickCount, 0, 0);
            firstPersonModel.renderToBuffer(stack, buffers.bufferSource().getBuffer(RenderType.armorCutoutNoCull(TEXTURE)), 0xFFFFFF, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

            stack.popPose();
        }
    }
}
