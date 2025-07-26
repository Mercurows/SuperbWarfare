package com.atsuishio.superbwarfare.client.renderer.curio;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.model.curio.ParachuteModel;
import com.atsuishio.superbwarfare.item.curio.ParachuteItem;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class ParachuteRenderer implements ICurioRenderer {

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
}
