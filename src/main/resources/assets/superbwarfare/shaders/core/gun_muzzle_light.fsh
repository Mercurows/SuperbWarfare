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
in vec2 texCoord;
out vec4 fragColor;

vec3 viewPosition(vec2 uv, float depth) {
    vec4 p = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

float shadowSample(vec2 uv, float rayZ) {
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) return 0.0;
    float depth = texture(WorldDepth, uv).r;
    if (depth >= 0.999999) return 0.0;
    float gap = viewPosition(uv, depth).z - rayZ;
    // Feather both ends of the finite depth thickness to avoid binary depth bands.
    return smoothstep(0.025, 0.075, gap) * (1.0 - smoothstep(0.45, 0.65, gap));
}

// Filter occlusion comparisons, never raw depth: interpolating foreground and
// background depth would invent an occluder between unrelated surfaces.
float filteredShadow(vec2 uv, float rayZ) {
    vec2 footprint = TexelSize * 2.0;
    vec2 grid = uv / footprint - 0.5;
    vec2 base = (floor(grid) + 0.5) * footprint;
    vec2 weight = fract(grid);
    float a = shadowSample(base, rayZ);
    float b = shadowSample(base + vec2(footprint.x, 0.0), rayZ);
    float c = shadowSample(base + vec2(0.0, footprint.y), rayZ);
    float d = shadowSample(base + footprint, rayZ);
    return mix(mix(a, b, weight.x), mix(c, d, weight.x), weight.y);
}

// A fixed sampling pattern is stable across frames and needs no temporal history.
float visibility(vec3 surface, vec3 light) {
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
        visible = min(visible, 1.0 - filteredShadow(uv, rayZ));
        if (visible <= 0.005) return 0.0;
    }
    return visible;
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
    float angular = mix(0.65, 1.0, smoothstep(-0.3, 0.8, forward));
    float energy = 2.2 * LightStrength * falloff * falloff * diffuse * angular * visibility(p + n * 0.035, lightView);
    vec3 scene = texture(SceneColor, texCoord).rgb;
    // Scene colour already contains ambient darkness. Lift that attenuation before
    // applying a new light, retaining texture contrast instead of multiplying darkness twice.
    vec3 surface = max(scene, vec3(0.0));
    vec3 material = vec3(0.08) + 0.77 * surface / (surface + vec3(0.12));
    // Preserve highlight headroom in daylight without reducing the dark-room flash.
    vec3 headroom = vec3(1.0) - clamp(scene, vec3(0.0), vec3(1.0));
    // A bounded response keeps brighter texture texels brighter even at high light strength.
    float response = 1.0 - exp(-energy);
    fragColor = vec4(material * vec3(1.0, 0.64, 0.28) * response * headroom, 0.0);
}
