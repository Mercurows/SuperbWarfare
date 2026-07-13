#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D ThermalSampler;
uniform vec2 OutSize;
uniform float ThermalTime;
uniform float ThermalArtifactStrength;
uniform float ThermalInterference; // PJM: 0..1.5 динамическая интенсивность помех (пол + выстрел + низкое HP)

in vec2 texCoord;

out vec4 fragColor;
// ...existing code...
// 简单的伪随机函数
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float smoothNoise(float value) {
    float cell = floor(value);
    float fraction = fract(value);
    fraction = fraction * fraction * (3.0 - 2.0 * fraction);
    return mix(random(vec2(cell, 7.0)), random(vec2(cell + 1.0, 7.0)), fraction);
}

// 辅助函数：计算亮度 (Luminance)
float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

float thermalMask(vec2 coord) {
    vec4 thermal = textureLod(ThermalSampler, clamp(coord, 0.001, 0.999), 0.0);
    return max(thermal.a, luma(thermal.rgb));
}

vec2 sensorCoord(vec2 coord) {
    // Same sensor grid as WRBDrones' thermal_blk_wht shader: one source pixel occupies
    // five display pixels, producing the low-resolution image of a real thermal sight.
    const float sensorPixelSize = 5.0;
    vec2 sensorPixels = max(floor(OutSize / sensorPixelSize), vec2(1.0));
    vec2 quantized = (floor(coord * sensorPixels) + 0.5) / sensorPixels;
    return mix(coord, quantized, ThermalArtifactStrength);
}

void main() {
    vec2 sampleCoord = sensorCoord(texCoord);
    // Scrolling horizontal sync-loss bands.  They shift the sampled image rather than just
    // tinting it, so the interference remains noticeable on a monochrome thermal image.
    float bandPosition = texCoord.y * 18.0 + ThermalTime * 2.4;
    float bandNoise = smoothNoise(bandPosition);
    // PJM: чем выше ThermalInterference, тем ниже порог -> полосы срыва появляются чаще и шире.
    // Маленький пол не даёт помехам полностью исчезнуть (просьба «чтобы не пропадали сразу»).
    float interferenceLevel = clamp(ThermalInterference, 0.0, 1.5);
    // PJM: полосы срыва и тиринг теперь слабые и ПОСТОЯННЫЕ — динамика (выстрел/низкое HP) ушла
    // целиком в зерно (см. grainAmp ниже). Порог выше -> полосы реже и уже; сдвиг меньше.
    float interferenceBand = smoothstep(0.82, 0.95, bandNoise) * ThermalArtifactStrength;
    float bandShift = (random(vec2(floor(ThermalTime * 24.0), floor(texCoord.y * 90.0))) - 0.5)
        * 0.015 * interferenceBand;
    sampleCoord.x = clamp(sampleCoord.x + bandShift, 0.001, 0.999);
    // A low-cost version of the soft sensor blur used by WRBDrones.  It is applied before
    // the thermal palette, which keeps pixel blocks from looking artificially sharp.
    vec2 texel = 1.0 / max(OutSize, vec2(1.0));
    vec4 sceneColor = texture(DiffuseSampler, sampleCoord) * 0.40;
    sceneColor += texture(DiffuseSampler, sampleCoord + vec2(texel.x * 2.0, 0.0)) * 0.15;
    sceneColor += texture(DiffuseSampler, sampleCoord - vec2(texel.x * 2.0, 0.0)) * 0.15;
    sceneColor += texture(DiffuseSampler, sampleCoord + vec2(0.0, texel.y * 2.0)) * 0.15;
    sceneColor += texture(DiffuseSampler, sampleCoord - vec2(0.0, texel.y * 2.0)) * 0.15;
    // 使用 textureLod 强制采样 Level 0，避免 RenderTarget 可能存在的 Mipmap 问题
    vec4 thermalColor = textureLod(ThermalSampler, sampleCoord, 0.0);

    // 1. 背景处理 (冷色调 + 噪点 + 晕影 + 扫描线)
    float sceneLuma = luma(sceneColor.rgb);

//    // 更深邃的冷色调背景
//    vec3 bgDeep = vec3(0.0, 0.02, 0.1);// 深黑蓝
//    vec3 bgMid  = vec3(0.05, 0.1, 0.35);// 蓝紫
//    vec3 bgHigh = vec3(0.0, 0.4, 0.5);// 青绿 (高亮部分)
//
//    vec3 bgColor = mix(bgDeep, bgMid, smoothstep(0.0, 0.4, sceneLuma));
//    bgColor = mix(bgColor, bgHigh, smoothstep(0.4, 1.0, sceneLuma));

    // PJM: тепловизор видит излучаемое тепло, а не отражённый видимый свет. Фон делаем ровным
    // холодным, чтобы бликующие/светлые поверхности (снег, лампы) не выглядели «тёплыми».
    // Силуэт рельефа берём из ГРАДИЕНТА яркости (контуры), а не из её абсолютного значения —
    // однородно-светлая поверхность не светится, но границы блоков остаются видны.
    float lumaR = luma(texture(DiffuseSampler, sampleCoord + vec2(texel.x * 2.0, 0.0)).rgb);
    float lumaU = luma(texture(DiffuseSampler, sampleCoord + vec2(0.0, texel.y * 2.0)).rgb);
    float edge = clamp((abs(sceneLuma - lumaR) + abs(sceneLuma - lumaU)) * 4.0, 0.0, 1.0);
    vec3 bgColor = vec3(0.015 + edge * 0.06);

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
        // PJM: тональная кривая тепла портирована из WRBDrones thermal_blk_wht (pow 0.65 + smoothstep)
        // — более сочный, «щелчковый» градиент вместо линейного pow 0.8.
        heat = smoothstep(0.05, 0.95, pow(heat, 0.65));

        vec3 colCold = vec3(0.5, 0.5, 0.5);// 紫 (低温/边缘)
        vec3 colMid  = vec3(0.75, 0.75, 0.75);// 红 (中温)
        vec3 colHot  = vec3(1.0, 1.0, 1.0);// 黄白 (高温)

        vec3 objectColor;
        if (heat < 0.5) {
            objectColor = mix(colCold, colMid, heat * 2.0);
        } else {
            objectColor = mix(colMid, colHot, (heat - 0.5) * 2.0);
        }

        // Thermal energy is strongest in the centre of a silhouette and falls off at its
        // contour.  Sampling the mask around the current pixel gives an inexpensive soft,
        // dark edge even for fully opaque entity textures.
        vec2 edgeTexel = 2.5 / max(OutSize, vec2(1.0));
        float neighbourMask = min(
            min(thermalMask(sampleCoord + vec2(edgeTexel.x, 0.0)), thermalMask(sampleCoord - vec2(edgeTexel.x, 0.0))),
            min(thermalMask(sampleCoord + vec2(0.0, edgeTexel.y)), thermalMask(sampleCoord - vec2(0.0, edgeTexel.y)))
        );
        float centerMask = max(thermalColor.a, luma(thermalColor.rgb));
        float hotInterior = smoothstep(0.02, 0.80, min(centerMask, neighbourMask));
        float edgeFade = mix(1.0, mix(0.42, 1.0, hotInterior), ThermalArtifactStrength);
        // PJM: цель «рассеивается» в фон у контура. Анимированный шумовой порог разбивает край
        // в крапинки, поэтому мелкие/дальние цели (почти сплошной край) мягко тают, а близкие
        // (большое сплошное тело) остаются читаемыми. Буфер глубины не нужен — размер силуэта
        // служит косвенной мерой дистанции. В режиме очков (ThermalArtifactStrength=0) край чистый.
        float dissolveNoise = random(sampleCoord * OutSize * 0.4 + floor(ThermalTime * 12.0));
        float presence = smoothstep(0.0, 0.55, min(centerMask, neighbourMask));
        float edgeDissolve = clamp(presence + (dissolveNoise - 0.5) * 0.6 * ThermalArtifactStrength, 0.0, 1.0);
        finalColor = mix(finalColor, objectColor * edgeFade, edgeDissolve);
    }

    // 整体提高对比度
    float contrast = 1.0; // 调整这个值，1.0为原始对比度，大于1提高对比度，小于1降低对比度
    finalColor = (finalColor - 0.5) * contrast + 0.5;
    finalColor = clamp(finalColor, 0.0, 1.0);

    // Sensor grain ported from WRBDrones thermal_blk_wht.  Bounded noiseTime + normalized-UV
    // seeds keep random()'s arguments small, so the grain keeps animating forever instead of
    // freezing and fading out as ThermalTime grows (the old pixel*bigTime seed overflowed
    // sin()'s precision and decayed into a static pattern). // PJM
    float resScale = 1440.0 / max(OutSize.y, 1.0);
    float noiseTime = mod(ThermalTime * 0.15, 10.0);
    // ±grain (centred at 0 for real contrast, not just a brightening) + sparse bright specks.
    // No darkness gating: it must stay visible on the near-black background too. // PJM
    float grain = random(texCoord / resScale + noiseTime) - 0.5;
    // PJM: точки мельче — выше частота сетки (было 0.04).
    float noisePixels = 0.16 * resScale;
    vec2 noiseUv = floor(texCoord * OutSize * noisePixels) / max(OutSize, vec2(1.0)) / noisePixels;
    float speck = pow(random(noiseUv + noiseTime), 1000.0);
    float signalTone = luma(finalColor);
    // PJM: ЗЕРНО — главный динамический эффект. Больше базового зерна + сильный рост с
    // interferenceLevel (выстрел/низкое HP усиливают именно его, а не полосы/тиринг).
    float grainAmp = 0.18 + interferenceLevel * interferenceLevel * 0.85;   // покой ~0.18 -> выстрел ~2.1
    float speckAmp = 0.28 + interferenceLevel * 0.70;
    // Аналоговый roll-bar: слабая ПОСТОЯННАЯ полоса развёртки (динамика ушла в зерно).
    float roll = fract(texCoord.y - ThermalTime * 0.35);
    float rollBar = smoothstep(0.93, 1.0, roll) * 0.4;
    float exposurePulse = 1.0 + sin(ThermalTime * 3.0) * 0.05 * signalTone;

    finalColor *= mix(1.0, exposurePulse, ThermalArtifactStrength);
    finalColor = mix(finalColor, finalColor + vec3(0.12), interferenceBand * 0.35);
    finalColor += vec3(grain * grainAmp + speck * speckAmp + rollBar * 0.30) * ThermalArtifactStrength;

    // A sight optic darkens the complete image, including hot entities, near the edges.
    vec2 vignetteCoord = texCoord * 2.0 - 1.0;
    float vignette = clamp(1.0 - dot(vignetteCoord, vignetteCoord) * 0.62, 0.0, 1.0);
    vignette = pow(vignette, 0.75);
    finalColor *= mix(1.0, vignette, ThermalArtifactStrength);
    finalColor = clamp(finalColor, 0.0, 1.0);

    fragColor = vec4(finalColor, 1.0);
}
