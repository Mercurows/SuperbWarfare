package com.atsuishio.superbwarfare.client.renderer.armor;

import com.atsuishio.superbwarfare.client.model.armor.GeHelmetM35Model;
import com.atsuishio.superbwarfare.item.armor.GeHelmetM35;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class GeHelmetM35ArmorRenderer extends GeoArmorRenderer<GeHelmetM35> implements ICurioRenderer {

    public GeHelmetM35ArmorRenderer() {
        super(new GeHelmetM35Model());
        this.head = new GeoBone(null, "", false, (double) 0, false, false);
    }

    @Override
    public RenderType getRenderType(GeHelmetM35 animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    // TODO 让头盔正确跟着头旋转
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack matrixStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        matrixStack.pushPose();

        var clientExtensions = IClientItemExtensions.of(stack);
        HumanoidModel<?> helmetModel = clientExtensions.getHumanoidArmorModel(slotContext.entity(), stack, EquipmentSlot.HEAD, null);

        this.prepForRender(slotContext.entity(), stack, EquipmentSlot.HEAD, helmetModel);

        applyBaseModel(this.baseModel);
        grabRelevantBones(getGeoModel().getBakedModel(getGeoModel().getModelResource(this.animatable)));
        applyBaseTransformations(this.baseModel);

        this.renderToBuffer(matrixStack, renderTypeBuffer.getBuffer(RenderType.entityTranslucent(getTextureLocation((GeHelmetM35) stack.getItem()))),
                light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        matrixStack.popPose();
    }
}
