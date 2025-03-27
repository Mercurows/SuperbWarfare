package com.atsuishio.superbwarfare.client.layer.gun;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.client.ModRenderTypes;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.machinegun.MinigunItem;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class MinigunHeatLayer extends GeoRenderLayer<MinigunItem> {
    private static final ResourceLocation LAYER = ModUtils.loc("textures/item/minigun_heat_e.png");

    public MinigunHeatLayer(GeoRenderer<MinigunItem> itemGeoRenderer) {
        super(itemGeoRenderer);
    }

    @Override
    public void render(PoseStack poseStack, MinigunItem animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderType glowRenderType = ModRenderTypes.ILLUMINATED.apply(LAYER);
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;
        float heat = (float) NBTTool.getOrCreateTag(stack).getDouble("heat");
        var color = FastColor.ARGB32.color(1, (int) (heat / 55 * 255), (int) (heat / 55 * 255), (int) (heat / 55 * 255));
        getRenderer().reRender(getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, glowRenderType, bufferSource.getBuffer(glowRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY, color);
    }
}

