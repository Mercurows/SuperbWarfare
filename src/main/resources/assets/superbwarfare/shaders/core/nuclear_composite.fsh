#version 150

uniform sampler2D VolumeColor;
uniform sampler2D SceneDepth;
uniform sampler2D WorldDepth;
uniform sampler2D SceneColor;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
uniform int ShaderPackMode;
uniform int HeatCount;
uniform vec4 Heat0;
uniform vec4 Heat1;
uniform vec4 Heat2;
uniform vec4 Heat3;
uniform vec2 HeatScale0;
uniform vec2 HeatScale1;
uniform vec2 HeatScale2;
uniform vec2 HeatScale3;
in vec2 texCoord;
out vec4 fragColor;

float viewDepth(vec2 uv) {
    float depth = texture(WorldDepth, uv).r;
    vec4 p = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return abs(p.z / p.w);
}

vec2 heatOffset(vec2 uv) {
    vec4 farPoint = InverseProjection * vec4(uv * 2.0 - 1.0, 1.0, 1.0);
    vec3 ray = normalize(mat3(InverseView) * (farPoint.xyz / farPoint.w));
    vec4 surface = InverseProjection * vec4(uv * 2.0 - 1.0, texture(WorldDepth, uv).r * 2.0 - 1.0, 1.0);
    float surfaceDistance = length(surface.xyz / surface.w);
    vec2 offset = vec2(0.0);
    for (int i = 0; i < 4; ++i) {
        if (i >= HeatCount) break;
        vec4 h = i == 0 ? Heat0 : i == 1 ? Heat1 : i == 2 ? Heat2 : Heat3;
        vec2 scaleXY = i == 0 ? HeatScale0 : i == 1 ? HeatScale1 : i == 2 ? HeatScale2 : HeatScale3;
        vec3 scale = vec3(scaleXY.x, scaleXY.y, scaleXY.x);
        float age = h.w;
        if (age < 0.15 || age >= 40.0) continue;
        float lift = 5.0 + 95.0 * (1.0 - exp(-max(age - 0.7, 0.0) / 8.0));
        float radius = 8.0 + 40.0 * (1.0 - exp(-age / 3.0));
        vec3 center = h.xyz + vec3(0.0,lift * scale.y,0.0);
        vec3 localRay = ray / scale, localCenter = center / scale;
        float a = dot(localRay, localRay), halfB = -dot(localCenter, localRay);
        float c = dot(localCenter, localCenter) - radius * radius * 1.69;
        float discriminant = halfB * halfB - a * c;
        if (discriminant < 0.0) continue;
        float entry = (-halfB - sqrt(discriminant)) / a;
        float along = -halfB / a;
        if (along < 0.0 || surfaceDistance < max(entry,0.0)) continue;
        vec3 p = (ray * along - center) / scale;
        float envelope = (1.0 - smoothstep(radius * 0.3, radius * 1.3, length(p))) *
            smoothstep(0.15,0.8,age) * (1.0 - smoothstep(18.0,40.0,age));
        vec2 wave = vec2(sin(p.y * 0.32 - age * 5.5 + sin(p.x * 0.18 + age * 2.0)),
                         cos(p.x * 0.26 + age * 3.8 + sin(p.y * 0.21 - age * 2.4)));
        offset += wave * envelope * 0.0022 / (1.0 + length(center) / 400.0);
    }
    return offset;
}

void main() {
    vec4 original = texture(SceneColor, texCoord);
    float depth = texture(SceneDepth, texCoord).r;
    if (ShaderPackMode != 0 && depth < 0.6 && depth + 0.00001 < texture(WorldDepth, texCoord).r) {
        fragColor = original;
        return;
    }
    vec2 size = vec2(textureSize(VolumeColor, 0));
    vec2 offset = heatOffset(texCoord);
    vec2 warped = clamp(texCoord + offset, vec2(0.0), vec2(1.0));
    float centerDepth = viewDepth(texCoord);
    // Never refract foreground silhouettes into the background.
    if (abs(viewDepth(warped) - centerDepth) > max(0.75, centerDepth * 0.008)) warped = texCoord;
    vec2 pixel = warped * size - 0.5;
    vec2 base = floor(pixel), f = fract(pixel);
    vec4 color = vec4(0.0);
    float total = 0.0;
    for (int y = 0; y < 2; ++y) for (int x = 0; x < 2; ++x) {
        vec2 tap = clamp((base + vec2(x,y) + 0.5) / size, 0.5 / size, 1.0 - 0.5 / size);
        vec2 weightXY = mix(1.0 - f, f, vec2(x,y));
        float weight = weightXY.x * weightXY.y;
        // Use linear distance: non-linear depth differences fail at far terrain silhouettes.
        weight *= exp(-abs(viewDepth(tap) - centerDepth) / max(0.75, centerDepth * 0.008));
        color += texture(VolumeColor, tap) * weight;
        total += weight;
    }
    color = total > 0.00001 ? color / total : vec4(0.0);
    fragColor = vec4(color.rgb + texture(SceneColor, warped).rgb * (1.0 - color.a), original.a);
}
