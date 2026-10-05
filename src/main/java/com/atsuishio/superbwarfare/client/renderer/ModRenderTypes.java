package com.atsuishio.superbwarfare.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
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

    /** 激光束的附加混合 */
    public static final TransparencyStateShard LASER_BEAM_ADDITIVE = new TransparencyStateShard("laser_beam_additive", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    /** 枪械激光瞄准器的光束 */
    public static final Function<ResourceLocation, RenderType> LASER_BEAM = Util.memoize((location) -> {
        TextureStateShard shard = new RenderStateShard.TextureStateShard(location, false, false);
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(shard)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .setTransparencyState(LASER_BEAM_ADDITIVE)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("laser_beam", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, state);
    });

    public static RenderType laserBeam(ResourceLocation location) {
        return LASER_BEAM.apply(location);
    }

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

    // 支持半透明和自发光的多边形网格渲染类型，用于载具特效（如枪口火焰）
    public static final Function<ResourceLocation, RenderType> POLY_MESH_TRANSLUCENT_EMISSIVE = Util.memoize((location) -> {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_EYES_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(location, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create("poly_mesh_translucent_emissive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 256, false, true, state);
    });

    // 自发光多边形网格：与 net.minecraft.client.renderer.RenderType.eyes 同一套状态
    // （rendertype_eyes 着色器、加法混合、无光照、只写颜色），只是换成 TRIANGLES + NO_CULL，
    // 好和 polyMeshCutout 那一遍的剪影对齐（那一遍就是 NO_CULL）。
    // 枪械的自发光层（_e 贴图）就是靠它 + RenderType.eyes 把模型再画一遍，见 GunEmissiveTextures。
    public static final Function<ResourceLocation, RenderType> POLY_MESH_EYES = Util.memoize((location) -> {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_EYES_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(location, false, false))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create("poly_mesh_eyes", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 256, false, true, state);
    });

    public static RenderType polyMeshEyes(ResourceLocation location) {
        return POLY_MESH_EYES.apply(location);
    }

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

    public static final Function<ResourceLocation, RenderType> TOW_CHAIN = Util.memoize((location) -> {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.POSITION_COLOR_TEX_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(location, false, false))
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .createCompositeState(false);
        return RenderType.create("tow_chain", DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.TRIANGLE_STRIP, 256, false, true, state);
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
}
