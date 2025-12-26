#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D ThermalSampler;

in vec2 texCoord;

out vec4 fragColor;
// ...existing code...
// 简单的伪随机函数
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// 辅助函数：计算亮度 (Luminance)
float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    // 使用 textureLod 强制采样 Level 0，避免 RenderTarget 可能存在的 Mipmap 问题
    vec4 thermalColor = textureLod(ThermalSampler, texCoord, 0.0);

    // 1. 背景处理 (冷色调 + 噪点 + 晕影 + 扫描线)
    float sceneLuma = luma(sceneColor.rgb);

//    // 更深邃的冷色调背景
//    vec3 bgDeep = vec3(0.0, 0.02, 0.1);// 深黑蓝
//    vec3 bgMid  = vec3(0.05, 0.1, 0.35);// 蓝紫
//    vec3 bgHigh = vec3(0.0, 0.4, 0.5);// 青绿 (高亮部分)
//
//    vec3 bgColor = mix(bgDeep, bgMid, smoothstep(0.0, 0.4, sceneLuma));
//    bgColor = mix(bgColor, bgHigh, smoothstep(0.4, 1.0, sceneLuma));

    // 用这个就是原色背景
    vec3 bgColor = sceneColor.rgb;
//    vec3 bgColor = vec3(sceneColor.r * 0.75, sceneColor.g * 0.75, sceneColor.b * 0.75);

//    // 添加噪点 (模拟传感器噪声)
//    float noise = random(texCoord * 100.0);
//    bgColor += (noise - 0.5) * 0.08;

    // 添加晕影 (Vignette)
    vec2 uv = texCoord * (1.0 - texCoord.yx);
    float vig = uv.x * uv.y * 15.0;
    vig = pow(vig, 0.25);
    bgColor *= vig;

    // 2. 热源处理
    vec3 finalColor = bgColor;

//    // 环境热源检测 (岩浆、火、太阳等)
//    // 优化逻辑：使用平滑过渡，结合亮度和暖色调
//    // warmth: 红色分量超出绿/蓝分量的程度
//    float warmth = sceneColor.r - max(sceneColor.g, sceneColor.b);
//
//    // 1. 极亮物体 (太阳、强光源)：亮度极高时直接视为热源
//    float brightHeat = smoothstep(0.92, 1.0, sceneLuma);
//
//    // 2. 暖色高亮物体 (岩浆、火)：亮度中等偏高，且色调偏暖
//    float warmHeat = smoothstep(0.5, 0.9, sceneLuma) * smoothstep(0.05, 0.4, warmth);
//
//    float envHeat = max(brightHeat, warmHeat);
//
//    if (envHeat > 0.01) {
//        // 环境热源色谱：橙红 -> 黄白 (提高饱和度)
//        vec3 envColor = mix(vec3(1.0, 0.15, 0.0), vec3(1.0, 0.9, 0.4), envHeat);
//        // 混合强度优化：增加基础混合权重，防止低热度时被背景冷色淹没
//        // 只要是热源，至少有 40% 的暖色覆盖
//        finalColor = mix(finalColor, envColor, clamp(envHeat + 0.4, 0.0, 1.0));
//    }

    // 3. 实体热源处理 (最高优先级)
    // 兼容性修改：Oculus/光影可能会修改 Alpha 通道，所以同时检查 RGB 亮度
    bool isEntityHot = thermalColor.a > 0.01 || dot(thermalColor.rgb, vec3(1.0)) > 0.01;

    if (isEntityHot) {
        float texLuma = luma(thermalColor.rgb);

        // 核心改进：提升基础热度。
        // 即使纹理很黑 (texLuma 接近 0)，我们也给它一个基础热度，确保深色实体也会发光
        float heat = 0.2 + 0.7 * texLuma;
        heat = pow(heat, 0.8);// 增强对比度

        vec3 colCold = vec3(0.5, 0.5, 0.5);// 紫 (低温/边缘)
        vec3 colMid  = vec3(0.75, 0.75, 0.75);// 红 (中温)
        vec3 colHot  = vec3(1.0, 1.0, 1.0);// 黄白 (高温)

        vec3 objectColor;
        if (heat < 0.5) {
            objectColor = mix(colCold, colMid, heat * 2.0);
        } else {
            objectColor = mix(colMid, colHot, (heat - 0.5) * 2.0);
        }

        finalColor = objectColor;
    }

    // 整体提高对比度
    float contrast = 1.0; // 调整这个值，1.0为原始对比度，大于1提高对比度，小于1降低对比度
    finalColor = (finalColor - 0.5) * contrast + 0.5;
    finalColor = clamp(finalColor, 0.0, 1.0);

    fragColor = vec4(finalColor, 1.0);
}
