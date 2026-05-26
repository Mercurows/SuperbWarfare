package com.atsuishio.superbwarfare.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public abstract class ModBedrockModelRenderTypes extends RenderType {
    private static final Function<ResourceLocation, RenderType> POLY_MESH_TRANSLUCENT = Util.memoize(ModBedrockModelRenderTypes::createPolyMeshTranslucent);

    private ModBedrockModelRenderTypes() {
        super("dummy", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {});
    }

    public static RenderType polyMeshTranslucent(ResourceLocation texture) {
        return POLY_MESH_TRANSLUCENT.apply(texture);
    }

    private static RenderType createPolyMeshTranslucent(ResourceLocation texture) {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
        return create(
                "bedrock_poly_mesh_translucent",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.TRIANGLES,
                256,
                true,
                false,
                state
        );
    }
}