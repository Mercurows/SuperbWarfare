package com.atsuishio.superbwarfare.client.renderer;

import com.atsuishio.superbwarfare.Mod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class ModRenderTypes extends RenderType {

    public ModRenderTypes(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }

    public static final Function<ResourceLocation, RenderType> LASER = Util.memoize((location) -> {
        TextureStateShard shard = new RenderStateShard.TextureStateShard(location, false, false);
        RenderType.CompositeState state = RenderType.CompositeState.builder().setTextureState(shard)
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER).setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setCullState(NO_CULL).setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE).createCompositeState(false);
        return RenderType.create("laser", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, state);
    });


    public static final Function<ResourceLocation, RenderType> ILLUMINATED = Util.memoize((location) -> {
        TextureStateShard shard = new RenderStateShard.TextureStateShard(location, false, false);
        RenderType.CompositeState state = RenderType.CompositeState.builder().setTextureState(shard)
                .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER).setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                .setLightmapState(RenderStateShard.NO_LIGHTMAP).setCullState(NO_CULL).setOverlayState(NO_OVERLAY).setWriteMaskState(COLOR_WRITE).createCompositeState(false);
        return RenderType.create("illuminated", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, state);
    });

    // DickSheep的恩情还不完
    public static final TransparencyStateShard TEST_TRANSPARENCY = new TransparencyStateShard("test_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    public static final Function<ResourceLocation, RenderType> MUZZLE_FLASH_TYPE = Util.memoize((location) -> {
        TextureStateShard shard = new RenderStateShard.TextureStateShard(location, false, false);
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                // 关键修复：使用内置的 POSITION_COLOR_TEX_SHADER（直接调用 ShaderStateShard）
                .setShaderState(RENDERTYPE_EYES_SHADER)
                // 启用半透明（确保正确排序）
                .setTransparencyState(TEST_TRANSPARENCY)
                // 绑定贴图
                .setTextureState(shard)
                // 禁用光照和覆盖颜色
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setOverlayState(RenderStateShard.NO_OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .createCompositeState(false);

        return RenderType.create("muzzle_flash", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true, state);
    });

    public static final RenderType BLOCK_OVERLAY = create("block_overlay",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
            CompositeState.builder()
                    .setShaderState(ShaderStateShard.POSITION_COLOR_SHADER)
                    .setLayeringState(NO_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(NO_TEXTURE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    // 方法1：使用白色纹理贴图
    public static final RenderType WHITE_SOLID = RenderType.create(
            "white_entity_solid",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            createWhiteTextureLocation(), // 白色纹理
                            false,
                            false
                    ))
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setCullState(RenderStateShard.CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(false)
    );

    // 获取白色纹理位置
    private static net.minecraft.resources.ResourceLocation createWhiteTextureLocation() {
        return Mod.loc("textures/entity/white.png");
    }
}
