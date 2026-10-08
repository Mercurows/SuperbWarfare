#version 150

uniform sampler2D DiffuseSampler;
uniform float RadiationStrength;
uniform float RadiationTime;

in vec2 texCoord;
out vec4 fragColor;

vec2 clampUv(vec2 uv) {
    vec2 halfTexel = 0.5 / vec2(textureSize(DiffuseSampler, 0));
    return clamp(uv, halfTexel, vec2(1.0) - halfTexel);
}

void main() {
    vec2 texel = 1.0 / vec2(textureSize(DiffuseSampler, 0));
    float strength = clamp(RadiationStrength, 0.0, 1.0);
    vec2 centered = texCoord * 2.0 - 1.0;
    float time = RadiationTime;

    // Slow, continuous lens wobble. Its maximum displacement stays below a few pixels.
    float waveX = sin(centered.y * 4.0 + time * 0.72) + 0.45 * sin(centered.y * 8.0 - time * 0.38);
    float waveY = sin(centered.x * 3.2 - time * 0.58) + 0.4 * sin(centered.x * 6.0 + time * 0.31);
    vec2 distortion = vec2(waveX, waveY) * (0.00022 + 0.00125 * strength) * strength;
    vec2 uv = clampUv(texCoord + distortion);

    vec3 center = texture(DiffuseSampler, uv).rgb;

    // A compact multi-tap blur grows smoothly with radiation intensity.
    float radius = 0.65 + 3.4 * strength;
    vec2 stepSize = texel * radius;
    vec3 blur = center * 0.24;
    blur += texture(DiffuseSampler, clampUv(uv + vec2(stepSize.x, 0.0))).rgb * 0.12;
    blur += texture(DiffuseSampler, clampUv(uv - vec2(stepSize.x, 0.0))).rgb * 0.12;
    blur += texture(DiffuseSampler, clampUv(uv + vec2(0.0, stepSize.y))).rgb * 0.12;
    blur += texture(DiffuseSampler, clampUv(uv - vec2(0.0, stepSize.y))).rgb * 0.12;
    blur += texture(DiffuseSampler, clampUv(uv + stepSize * 0.707)).rgb * 0.07;
    blur += texture(DiffuseSampler, clampUv(uv - stepSize * 0.707)).rgb * 0.07;
    blur += texture(DiffuseSampler, clampUv(uv + vec2(stepSize.x, -stepSize.y) * 0.707)).rgb * 0.07;
    blur += texture(DiffuseSampler, clampUv(uv + vec2(-stepSize.x, stepSize.y) * 0.707)).rgb * 0.07;
    float blurAmount = 0.16 + 0.58 * strength;
    vec3 color = mix(center, blur, blurAmount);

    // Directional offset samples suggest visual persistence without storing old frames.
    vec2 trailDirection = normalize(vec2(0.9 + 0.25 * sin(time * 0.3), 0.28 + 0.12 * cos(time * 0.24)));
    vec2 trailOffset = trailDirection * texel * (1.0 + 5.0 * strength);
    vec3 trail = texture(DiffuseSampler, clampUv(uv - trailOffset)).rgb * 0.62;
    trail += texture(DiffuseSampler, clampUv(uv - trailOffset * 2.0)).rgb * 0.38;
    color = mix(color, trail, 0.035 + 0.27 * strength);

    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    float desaturation = 0.12 + 0.78 * strength;
    color = mix(color, vec3(luminance), desaturation);

    // Slight edge darkening reinforces the narrowing, unfocused peripheral vision.
    float vignette = smoothstep(0.35, 1.45, length(centered));
    color *= 1.0 - vignette * (0.04 + 0.16 * strength);

    fragColor = vec4(color, 1.0);
}
