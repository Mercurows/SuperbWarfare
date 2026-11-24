#version 150

uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform sampler2D WorldDepth;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
uniform mat4 Projection;
uniform mat4 View;
uniform vec2 TexelSize;
uniform int ShaderPackMode;
uniform vec3 LightPosition;
uniform vec3 LightDirection;
uniform float LightRadius;
uniform float LightStrength;
// x = cos(外半角), y = cos(内半角)；y <= x 表示不启用锥形（枪口火光保持原行为）
uniform vec2 LightCone;
uniform vec3 LightColor;
// LightShadow = 屏幕空间遮挡的深度厚度：0 = 关闭；枪口火光默认 0.65；手电筒 1000。
uniform float LightShadow;
// 手电筒"自身遮挡"半径：比光源更靠近相机、且离光源比这个更近的深度不算遮挡物，
// 用来排除发光者自己的枪械/握把/身体；枪口火光传 0 = 不启用。
uniform float LightSelfMask;
// x = 光轴被方块截断后的长度，y = 方块遮挡过渡后的灯头可见度。
uniform vec2 FlashlightEffect;
in vec2 texCoord;
out vec4 fragColor;

vec3 viewPosition(vec2 uv, float depth) {
    vec4 p = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

float shadowSample(vec2 uv, float rayZ, float thickness, vec3 lightView) {
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) return 0.0;
    float depth = texture(WorldDepth, uv).r;
    if (depth >= 0.999999) return 0.0;
    vec3 sampleView = viewPosition(uv, depth);
    // 排除发光者自己的枪械/握把/手臂/身体：
    //  - 光源后方（相机与光源之间）的深度物理上不可能挡住光源发出的光；
    //  - 光源前方 LightSelfMask 米内的深度是枪身/护木/手等紧贴光源的自体几何
    //    （看截图实测：枪管/护木在光源前一点点，仍会被当作遮挡物，形成锯齿状假阴影）。
    // 比这更远的深度才是真正的遮挡物（墙、柱子、其他实体），照常参与遮挡。
    if (LightSelfMask > 0.0 && sampleView.z > lightView.z - LightSelfMask) return 0.0;
    float gap = sampleView.z - rayZ;
    // 软边过渡（penumbra）：比原来的 0.025~0.075 更宽，遮挡边界不再是硬阶跃；
    // 大半径世界光（载具/爆炸）投影面积大时尤其明显，可有效掩盖屏幕空间步进的锯齿。
    return smoothstep(0.035, 0.16, gap) * (1.0 - smoothstep(thickness * 0.65, thickness, gap));
}

// Filter occlusion comparisons, never raw depth: interpolating foreground and
// background depth would invent an occluder between unrelated surfaces.
float filteredShadow(vec2 uv, float rayZ, float thickness, vec3 lightView) {
    vec2 footprint = TexelSize * 2.0;
    vec2 grid = uv / footprint - 0.5;
    vec2 base = (floor(grid) + 0.5) * footprint;
    vec2 weight = fract(grid);
    float a = shadowSample(base, rayZ, thickness, lightView);
    float b = shadowSample(base + vec2(footprint.x, 0.0), rayZ, thickness, lightView);
    float c = shadowSample(base + vec2(0.0, footprint.y), rayZ, thickness, lightView);
    float d = shadowSample(base + footprint, rayZ, thickness, lightView);
    return mix(mix(a, b, weight.x), mix(c, d, weight.x), weight.y);
}

// A fixed sampling pattern is stable across frames and needs no temporal history.
float visibility(vec3 surface, vec3 light, float thickness) {
    // March uniformly in screen space, with perspective-correct ray depth.
    // Uniform world-space steps can jump over a thin fence and form sawtooth shadows.
    vec3 end = light;
    if (end.z > -0.05) end = mix(surface, end, (-0.05 - surface.z) / (end.z - surface.z));
    vec4 startClip = Projection * vec4(surface, 1.0);
    vec4 endClip = Projection * vec4(end, 1.0);
    vec2 startUv = startClip.xy / startClip.w * 0.5 + 0.5;
    vec2 deltaUv = endClip.xy / endClip.w * 0.5 + 0.5 - startUv;
    float limit = 1.0;
    for (int axis = 0; axis < 2; axis++) {
        if (deltaUv[axis] > 0.000001) limit = min(limit, (1.0 - startUv[axis]) / deltaUv[axis]);
        if (deltaUv[axis] < -0.000001) limit = min(limit, -startUv[axis] / deltaUv[axis]);
    }
    limit = clamp(limit, 0.0, 1.0);
    float pixels = length(deltaUv * limit / TexelSize);
    // 每 ~1.5 像素一步，最多 64 步：载具炮口/爆炸这类大半径光投影跨度大，
    // 原来 32 步的上限会让遮挡边缘出现明显的大台阶。
    int steps = int(clamp(ceil(pixels / 1), 16.0, 128.0));
    float visible = 1.0;
    for (int i = 0; i < 128; i++) {
        if (i >= steps) break;
        float t = (float(i) + 0.5) / float(steps) * limit;
        vec2 uv = startUv + deltaUv * t;
        float reciprocalW = mix(1.0 / startClip.w, 1.0 / endClip.w, t);
        float rayZ = mix(surface.z / startClip.w, end.z / endClip.w, t) / reciprocalW;
        visible = min(visible, 1.0 - filteredShadow(uv, rayZ, thickness, light));
        if (visible <= 0.005) return 0.0;
    }
    return visible;
}

// 手电筒距离衰减：对需求曲线（逻辑斯蒂）拟合后再归一化 —— 0m 处为 1，射程极限处为 0。
// 曲线中段平滑下降、末端已趋近于 0，因此半径边界不会出现可见的球形"截面"。
float spotRangeFalloff(float u) {
    const float center = 0.325;   // 拐点位置（占射程比例），对应图上强度掉到约一半处
    const float steepness = 10.6; // 中段下降陡度（对 digitized 曲线的最小二乘拟合）
    float l0 = 1.0 / (1.0 + exp(-steepness * center));
    float l1 = 1.0 / (1.0 + exp(steepness * (1.0 - center)));
    float l = 1.0 / (1.0 + exp(steepness * (u - center)));
    return clamp((l - l1) / (l0 - l1), 0.0, 1.0);
}

// 先求视线在有限光锥内的区间，再在有效区间内采样。远处较窄的光束也不会漏采。
vec2 beamInterval(vec3 ray, vec3 lightView, vec3 lightAxis, float sceneDistance) {
    vec3 offset = -lightView;
    float axisAtCamera = dot(offset, lightAxis);
    float axisSlope = dot(ray, lightAxis);
    vec2 interval = vec2(0.0, sceneDistance);
    if (abs(axisSlope) < 0.00001) {
        if (axisAtCamera < 0.0 || axisAtCamera > FlashlightEffect.x) return vec2(0.0);
    } else {
        float apex = -axisAtCamera / axisSlope;
        float cap = (FlashlightEffect.x - axisAtCamera) / axisSlope;
        interval.x = max(interval.x, min(apex, cap));
        interval.y = min(interval.y, max(apex, cap));
    }
    if (interval.y <= interval.x) return vec2(0.0);

    float coneCosSquared = LightCone.x * LightCone.x;
    // 光锥内满足 a*t*t + 2*b*t + c >= 0，背向的锥已被上面的轴向范围排除。
    float a = axisSlope * axisSlope - coneCosSquared;
    float b = axisAtCamera * axisSlope - coneCosSquared * dot(offset, ray);
    float c = axisAtCamera * axisAtCamera - coneCosSquared * dot(offset, offset);
    if (abs(a) < 0.00001) {
        if (abs(b) < 0.00001) return c >= 0.0 ? interval : vec2(0.0);
        float edge = -c / (2.0 * b);
        if (b > 0.0) interval.x = max(interval.x, edge);
        else interval.y = min(interval.y, edge);
        return interval;
    }

    float discriminant = b * b - a * c;
    if (discriminant <= 0.0) return a > 0.0 ? interval : vec2(0.0);
    float root = sqrt(discriminant);
    float first = (-b - root) / a;
    float second = (-b + root) / a;
    float nearEdge = min(first, second);
    float farEdge = max(first, second);
    if (a < 0.0) {
        interval.x = max(interval.x, nearEdge);
        interval.y = min(interval.y, farEdge);
    } else if (interval.x < nearEdge) {
        interval.y = min(interval.y, nearEdge);
    } else {
        interval.x = max(interval.x, farEdge);
    }
    return interval;
}

float flashlightBeam(vec3 ray, vec3 lightView, vec3 lightAxis, float sceneDistance) {
    if (FlashlightEffect.x <= 0.01) return 0.0;
    vec2 interval = beamInterval(ray, lightView, lightAxis, sceneDistance);
    float span = interval.y - interval.x;
    if (span <= 0.0) return 0.0;

    const int samples = 8;
    float stepSize = span / float(samples);
    float density = 0.0;
    for (int i = 0; i < samples; i++) {
        vec3 fromLight = ray * (interval.x + (float(i) + 0.5) * stepSize) - lightView;
        float distance = length(fromLight);
        float forward = dot(fromLight, lightAxis) / max(distance, 0.0001);
        float cone = smoothstep(LightCone.x, LightCone.y, forward);
        float axial = dot(fromLight, lightAxis) / FlashlightEffect.x;
        float range = distance / LightRadius;
        float fade = smoothstep(0.04, 0.30, distance) * (1.0 - smoothstep(0.82, 1.0, axial));
        float falloff = (1.0 - smoothstep(0.72, 1.0, range)) / (1.0 + range * range * 3.0);
        density += cone * fade * falloff;
    }
    const float beamDensity = 0.085;
    return 1.0 - exp(-density * stepSize * LightStrength * beamDensity);
}

float flareDepthVisibility(vec2 center, float lightZ) {
    vec2 pixel = center / TexelSize - 0.5;
    vec2 nearest = floor(pixel + 0.5);
    vec2 subpixel = pixel - nearest;
    float visible = 0.0;
    float totalWeight = 0.0;
    // 先比较深度，再按灯头投影对相邻像素加权。轮廓跨过中心时，光晕按可见面积渐变。
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offset = vec2(x, y);
            vec2 weights = 1.0 - abs(offset - subpixel) / 1.5;
            float weight = weights.x * weights.y;
            vec2 uv = (nearest + offset + 0.5) * TexelSize;
            if (all(greaterThanEqual(uv, vec2(0.0))) && all(lessThanEqual(uv, vec2(1.0)))) {
                float depth = texture(WorldDepth, uv).r;
                float gap = depth < 0.999999 ? viewPosition(uv, depth).z - lightZ : 0.0;
                visible += weight * (1.0 - smoothstep(0.25, 0.55, gap));
            } else {
                visible += weight;
            }
            totalWeight += weight;
        }
    }
    return visible / totalWeight;
}

vec3 flashlightFlare(vec3 lightView, float face) {
    if (FlashlightEffect.y <= 0.0 || lightView.z >= -0.05 || face <= 0.0) return vec3(0.0);
    vec4 clip = Projection * vec4(lightView, 1.0);
    vec2 center = clip.xy / clip.w * 0.5 + 0.5;
    float distance = length(lightView);
    float range = distance / LightRadius;
    float headOn = smoothstep(LightCone.x, LightCone.y, face);
    float nearFade = smoothstep(0.05, 0.22, -lightView.z);
    float rangeFade = 1.0 - smoothstep(0.42, 1.0, range);
    vec2 pixel = (texCoord - center) * vec2(TexelSize.y / TexelSize.x, 1.0);

    // 白色灯芯、冷色光晕和轻微星芒。进入光锥时逐渐扩散成远光灯般的白幕。
    float spread = mix(0.046, 0.29, headOn) * mix(1.25, 0.72, smoothstep(2.0, 35.0, distance));
    float radius = length(pixel) / spread;
    float visibility = FlashlightEffect.y * flareDepthVisibility(center, lightView.z);
    float core = exp(-radius * radius * mix(52.0, 13.0, headOn));
    float halo = exp(-radius * radius * 2.5);
    float veil = exp(-radius * radius * 0.45) * headOn;
    float angle = atan(pixel.y, pixel.x + 0.00001);
    float streak = pow(abs(cos(angle * 3.0 + 0.38)), 18.0) * exp(-radius * 5.0);
    float gain = LightStrength * nearFade * rangeFade;
    float white = 1.0 - exp(-gain * core * mix(2.3, 8.0, headOn));
    float glare = 1.0 - exp(-gain * (halo * mix(0.20, 2.7, headOn) + veil * 0.55 + streak * 0.65));
    vec3 tint = mix(LightColor, vec3(0.67, 0.84, 1.0), 0.48);
    return mix(tint * glare, vec3(1.0), white) * visibility * smoothstep(0.0, 0.35, face);
}

vec3 surfaceLighting(vec3 p, vec3 lightView, vec3 scene, bool spot) {
    vec3 world = (InverseView * vec4(p, 1.0)).xyz;
    vec3 delta = world - LightPosition;
    float distanceToLight = length(delta);
    if (distanceToLight >= LightRadius) return vec3(0.0);

    // Prefer the neighbour on the same surface at silhouettes and block edges.
    vec2 tx = vec2(TexelSize.x, 0.0);
    vec2 ty = vec2(0.0, TexelSize.y);
    vec3 left = p - viewPosition(texCoord - tx, texture(WorldDepth, texCoord - tx).r);
    vec3 right = viewPosition(texCoord + tx, texture(WorldDepth, texCoord + tx).r) - p;
    vec3 down = p - viewPosition(texCoord - ty, texture(WorldDepth, texCoord - ty).r);
    vec3 up = viewPosition(texCoord + ty, texture(WorldDepth, texCoord + ty).r) - p;
    vec3 dx = abs(left.z) < abs(right.z) ? left : right;
    vec3 dy = abs(down.z) < abs(up.z) ? down : up;
    vec3 n = cross(dx, dy);
    n /= max(length(n), 0.00001);
    if (dot(n, -p) < 0.0) n = -n;
    vec3 toLight = lightView - p;
    float diffuse = max(dot(n, toLight / max(length(toLight), 0.0001)), 0.0);
    // Keep the near field strong; roll off smoothly only towards the radius boundary.
    float normalizedDistance = distanceToLight / LightRadius;
    float falloff = 1.0 - normalizedDistance * normalizedDistance;
    float forward = dot(delta / max(distanceToLight, 0.0001), LightDirection);
    // 锥形只对手电筒启用；枪口火光保持原有点光行为（LightCone = (-2,-2)）。
    float cone = 1.0;
    if (spot) {
        // 外沿处平滑归零：光斑边界不再是一道硬"截止面"。
        cone = smoothstep(LightCone.x, LightCone.y, forward);
        if (cone <= 0.0) return vec3(0.0);
    }
    // 手电筒：用 LightSelfMask 排除发光者自己的枪械/握把/身体（第三人称下它们挡在相机与光源之间，
    // 不排除会把整片光斑误判成阴影）；墙体、地形和其他实体仍然照常遮挡，与枪口火光一致。
    // 手电筒另加"最小距离"：紧贴光源（<0.1m）的表面不参与遮挡、0.1~0.6m 淡入，
    // 否则枪抵墙时整片光斑会被误判成阴影而直接消失。
    float visible = 1.0;
    if (LightShadow > 0.0) {
        float shadowFade = spot ? smoothstep(0.10, 0.60, distanceToLight) : 1.0;
        visible = mix(1.0, visibility(p + n * 0.035, lightView, LightShadow), shadowFade);
    }

    float energy;
    if (spot) {
        // 手电筒：距离强度按需求曲线衰减（0m 最大，中段平滑下降，射程极限前已接近 0）；
        // Lambert 压平以避免平地近处过亮、远处过暗。
        const float SPOT_LAMBERT_FLOOR = 0.35;
        float lighting = mix(SPOT_LAMBERT_FLOOR, 1.0, diffuse);
        float rangeFade = spotRangeFalloff(normalizedDistance);
        energy = 2.2 * LightStrength * rangeFade * lighting * cone * visible;
    } else {
        float angular = mix(0.65, 1.0, smoothstep(-0.3, 0.8, forward));
        energy = 2.2 * LightStrength * falloff * falloff * diffuse * angular * visible;
    }
    if (!(energy > 0.0) || isinf(energy)) return vec3(0.0);
    // Scene colour already contains ambient darkness. Lift that attenuation before
    // applying a new light, retaining texture contrast instead of multiplying darkness twice.
    vec3 surface = max(scene, vec3(0.0));
    vec3 material = vec3(0.08) + 0.77 * surface / (surface + vec3(0.12));
    // A bounded response keeps brighter texture texels brighter even at high light strength.
    float response = 1.0 - exp(-energy);
    return material * LightColor * response;
}

void main() {
    if (LightStrength <= 0.0) discard;
    float depth = texture(WorldDepth, texCoord).r;
    bool hasSurface = depth < 0.999999;
    bool flashlight = FlashlightEffect.x > 0.01 || FlashlightEffect.y > 0.0;
    if (!hasSurface && !flashlight) discard;
    // Iris hands use a different projection and are lit through their own material shader.
    if (ShaderPackMode != 0 && texture(SceneDepth, texCoord).r + 0.000001 < depth) discard;

    vec3 lightView = (View * vec4(LightPosition, 1.0)).xyz;
    vec3 scene = texture(SceneColor, texCoord).rgb;
    vec3 p = hasSurface ? viewPosition(texCoord, depth) : vec3(0.0);
    bool spot = LightCone.y > LightCone.x;
    vec3 light = hasSurface ? surfaceLighting(p, lightView, scene, spot) : vec3(0.0);
    if (flashlight) {
        vec3 ray = normalize(viewPosition(texCoord, 0.5));
        vec3 lightAxis = normalize(mat3(View) * LightDirection);
        float face = dot(-lightView / max(length(lightView), 0.0001), lightAxis);
        // 从灯后沿光轴看时，整段光束会投影成圆斑。侧面才逐渐呈现可见的散射光。
        float beamFacing = smoothstep(-0.45, 0.15, face);
        float sceneDistance = hasSurface ? length(p) : length(lightView) + LightRadius;
        float beam = beamFacing > 0.0 ? flashlightBeam(ray, lightView, lightAxis, sceneDistance) * beamFacing : 0.0;
        vec3 scatter = mix(LightColor, vec3(0.72, 0.86, 1.0), 0.42) * beam;
        vec3 glare = flashlightFlare(lightView, face);
        light += (vec3(1.0) - light) * (scatter + (vec3(1.0) - scatter) * glare);
    }
    if (max(light.r, max(light.g, light.b)) <= 0.0) discard;
    vec3 headroom = vec3(1.0) - clamp(scene, vec3(0.0), vec3(1.0));
    fragColor = vec4(light * headroom, 0.0);
}

