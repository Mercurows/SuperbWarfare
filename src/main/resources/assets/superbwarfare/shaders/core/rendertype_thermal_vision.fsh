#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 ThermalTexelSize;
uniform float PixelSize;
uniform float NoiseStrength;
uniform float VignetteStrength;
uniform int NoiseFrame;
in vec2 texCoord;
out vec4 fragColor;

float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

// Integer avalanche avoids the repeating diagonal patterns of a sine/dot hash.
float noiseAt(uvec2 p, uint seed) {
    uint h = p.x * 1597334677u ^ p.y * 3812015801u ^ seed * 2798796415u;
    h = (h ^ (h >> 16u)) * 2246822519u;
    h = (h ^ (h >> 13u)) * 3266489917u;
    h ^= h >> 16u;
    return float(h & 16777215u) / 16777216.0;
}

void main() {
    vec2 block = max(PixelSize, 1.0) * ThermalTexelSize;
    vec2 uv = (floor(texCoord / block) + 0.5) * block;
    // Blend in native-resolution luminance, retaining small terrain/texture details.
    float scene = mix(luma(texture(Sampler0, uv).rgb), luma(texture(Sampler0, texCoord).rgb), 0.35);
    float heat = clamp(texture(Sampler1, uv).r, 0.0, 1.0);
    float cold = 0.035 + pow(clamp(scene, 0.0, 1.0), 1.15) * 0.45;
    float hot = mix(0.68, 1.0, clamp(scene, 0.0, 1.0));
    float signal = mix(cold, hot, heat);

    uvec2 pixel = uvec2(gl_FragCoord.xy);
    uint frame = uint(NoiseFrame);
    // 16 display levels; stochastic rounding preserves sub-step detail without a Bayer grid.
    float dither = noiseAt(pixel, frame + 71u) - 0.5;
    float grey = floor(clamp(signal + dither * (0.65 / 15.0), 0.0, 1.0) * 15.0 + 0.5) / 15.0;

    // Independent fine grain, small clumps and weak line interference, refreshed at 30 Hz.
    float grain = (noiseAt(pixel, frame) - 0.5) * 0.65
                + (noiseAt(pixel / 3u, frame + 19u) - 0.5) * 0.25
                + (noiseAt(uvec2(0u, pixel.y / 2u), frame + 43u) - 0.5) * 0.10;
    grey += grain * NoiseStrength * mix(1.0, 0.45, heat);

    // Six-pixel CRT scan lines: double the previous line width and spacing.
    float scanline = 0.5 + 0.5 * cos(gl_FragCoord.y * 1.0471976);
    grey *= 1.0 - 0.14 * scanline;

    // Elliptical falloff: keep the central sight picture clear and darken edges/corners.
    vec2 radial = (texCoord - 0.5) * 2.0;
    float vignette = 1.0 - 0.72 * smoothstep(0.15, 1.65, dot(radial, radial));
    vignette = mix(1.0, vignette, clamp(VignetteStrength, 0.0, 1.0));
    grey *= mix(vignette, 1.0, heat * 0.30);

    fragColor = vec4(vec3(clamp(grey, 0.0, 1.0)), 1.0);
}
