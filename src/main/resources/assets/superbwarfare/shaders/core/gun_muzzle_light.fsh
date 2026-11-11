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

// Screen-space contact shadows only use actual scene depth. A wall hit on one
// CPU ray cannot define an infinite clipping plane across the adjacent open space.
float visibility(vec3 surface, vec3 light) {
    for (int i = 1; i <= 6; i++) {
        vec3 p = mix(surface, light, float(i) / 7.0);
        vec4 clip = Projection * vec4(p, 1.0);
        if (clip.w <= 0.0) continue;
        vec2 uv = clip.xy / clip.w * 0.5 + 0.5;
        if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) continue;
        float depth = texture(WorldDepth, uv).r;
        if (depth >= 0.999999) continue;
        float gap = viewPosition(uv, depth).z - p.z;
        if (gap > 0.07 && gap < 0.65) return 0.0;
    }
    return 1.0;
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
