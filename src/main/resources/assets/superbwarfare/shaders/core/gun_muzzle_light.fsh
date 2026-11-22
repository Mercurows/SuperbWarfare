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
    // Feather both ends of the finite depth thickness to avoid binary depth bands.
    // 火光用 0.65 的薄层（只看贴近光线的遮挡物）；手电筒允许更厚的遮挡体（见 main）。
    return smoothstep(0.025, 0.075, gap) * (1.0 - smoothstep(thickness * 0.7, thickness, gap));
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
    int steps = int(clamp(ceil(pixels / 2.0), 12.0, 32.0));
    float visible = 1.0;
    for (int i = 0; i < 32; i++) {
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

void main() {
    float depth = texture(WorldDepth, texCoord).r;
    if (depth >= 0.999999 || LightStrength <= 0.0) discard;
    // Iris hands use a different projection and are lit through their own material shader.
    if (ShaderPackMode != 0 && texture(SceneDepth, texCoord).r + 0.000001 < depth) discard;
    vec3 p = viewPosition(texCoord, depth);
    vec3 world = (InverseView * vec4(p, 1.0)).xyz;
    vec3 delta = world - LightPosition;
    float distanceToLight = length(delta);
    if (distanceToLight >= LightRadius) discard;

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
    vec3 lightView = (View * vec4(LightPosition, 1.0)).xyz;
    vec3 toLight = lightView - p;
    float diffuse = max(dot(n, toLight / max(length(toLight), 0.0001)), 0.0);
    // Keep the near field strong; roll off smoothly only towards the radius boundary.
    float normalizedDistance = distanceToLight / LightRadius;
    float falloff = 1.0 - normalizedDistance * normalizedDistance;
    float forward = dot(delta / max(distanceToLight, 0.0001), LightDirection);
    // 锥形只对手电筒启用；枪口火光保持原有点光行为（LightCone = (-2,-2)）。
    bool spot = LightCone.y > LightCone.x;
    float cone = 1.0;
    if (spot) {
        // 外沿处平滑归零：光斑边界不再是一道硬"截止面"。
        cone = smoothstep(LightCone.x, LightCone.y, forward);
        if (cone <= 0.0) discard;
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
    if (!(energy > 0.0) || isinf(energy)) discard;
    vec3 scene = texture(SceneColor, texCoord).rgb;
    // Scene colour already contains ambient darkness. Lift that attenuation before
    // applying a new light, retaining texture contrast instead of multiplying darkness twice.
    vec3 surface = max(scene, vec3(0.0));
    vec3 material = vec3(0.08) + 0.77 * surface / (surface + vec3(0.12));
    // Preserve highlight headroom in daylight without reducing the dark-room flash.
    vec3 headroom = vec3(1.0) - clamp(scene, vec3(0.0), vec3(1.0));
    // A bounded response keeps brighter texture texels brighter even at high light strength.
    float response = 1.0 - exp(-energy);
    fragColor = vec4(material * LightColor * response * headroom, 0.0);
}

