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
        TextureStateShard shard = new TextureStateShard(location, false, false);
        CompositeState state = CompositeState.builder().setTextureState(shard)
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER).setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setCullState(NO_CULL).setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE).createCompositeState(false);
        return RenderType.create("laser", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, state);
    });

    /**
     * 激光束的附加混合：`SRC_ALPHA / ONE`，且**不写目的 alpha**（TACZ 的 `LIGHTNING_ADDITIVE_TRANSPARENCY`）。
     *
     * ⚠ 不能用原版的 {@link RenderStateShard#ADDITIVE_TRANSPARENCY}：那是 `ONE / ONE`，
     * 混合完全忽略顶点的 alpha —— 沿光束长度的渐隐（第三人称短光束靠它淡出）会整个失效，
     * 只剩一根硬边管子。
     */
    public static final TransparencyStateShard LASER_BEAM_ADDITIVE = new TransparencyStateShard("laser_beam_additive", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    /**
     * 枪械激光瞄准器的光束（TACZ 风格的单层方管）。
     *
     * 用不 discard 低 alpha 的实体自发光着色器：顶点 alpha 才能沿光束长度平滑渐隐到 0
     * （EYES 着色器会在 alpha &lt; 0.1 处硬切，第三人称短光束的渐隐会变成截断）。
     *
     * 有三项状态是照搬 TACZ 的做法，缺一不可：
     *
     * - **{@code ITEM_ENTITY_TARGET}**（输出状态）：把几何送进"物品 / 实体"那一个渲染目标。
     *   光影（Oculus / Iris）是**按输出状态 + 着色器**给渲染类型挑 pass 的，本模组自定义的
     *   `laser_beam` 不在原版那批已知实体类型里，不显式声明就会被丢进一个光影不认识的 pass：
     *   开光影时光束会被云、地形按错误的深度关系挡住（关光影时看不出来），也不参与瞄具模板的裁切
     *   ——枪身走的就是这个目标，只有同目标才在同一个批次里被同一个模板状态覆盖。
     * - **{@code VIEW_OFFSET_Z_LAYERING}**：多边形偏移把光束朝相机方向拉一点，出光口贴着配件表面
     *   也不会被枪管 / 瞄具 / 手臂切掉最近那一小截，于是**不需要**再手动关深度测试
     *   （手动关掉的那一版既会漏到别的 pass，也把深度写关没了）。
     * - **{@link #LASER_BEAM_ADDITIVE}**：顶点 alpha 必须真的参与混合，渐隐才有意义。
     *
     * 光照贴图 / 覆盖层跟着 TACZ 一起打开（`LIGHTMAP` + `OVERLAY`）：顶点写的是全亮 0xF000F0 与
     * `NO_OVERLAY`，等价于不受光照与受伤红屏影响，但着色器采样器有对应的绑定。
     */
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
        TextureStateShard shard = new TextureStateShard(location, false, false);
        CompositeState state = RenderType.CompositeState.builder()
                // 关键：使用位置-颜色-贴图着色器
                .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                // 启用半透明混合
                .setTransparencyState(TEST_TRANSPARENCY)
                // 绑定贴图（替换为你的路径）
                .setTextureState(shard)
                // 禁用光照和覆盖颜色
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setOverlayState(RenderStateShard.NO_OVERLAY)
                // 深度测试配置
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)   // 允许颜色写入
                .createCompositeState(false);
        return RenderType.create("muzzle_flash", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true, state);
    });

    public static final Function<ResourceLocation, RenderType> TOW_CHAIN = Util.memoize((location) -> {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                .setTextureState(new RenderStateShard.TextureStateShard(location, false, false))
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .createCompositeState(false);
        return RenderType.create("tow_chain", DefaultVertexFormat.POSITION_TEX_COLOR,
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
