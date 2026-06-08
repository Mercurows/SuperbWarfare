#version 150

uniform sampler2D DiffuseSampler;
uniform float RainbowTime;

in vec2 texCoord;

out vec4 fragColor;

// Direct hue to full-saturation RGB (same pattern as vanilla creeper shader)
vec3 hue2rgb(float h) {
    float r = abs(h * 6.0 - 3.0) - 1.0;
    float g = 2.0 - abs(h * 6.0 - 2.0);
    float b = 2.0 - abs(h * 6.0 - 4.0);
    return clamp(vec3(r, g, b), 0.0, 1.0);
}

// Pseudo-random function for grain
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

void main() {
    // === Pixelation ===
    float pixRes = 1024.0;
    vec2 pixCoord = floor(texCoord * vec2(pixRes, pixRes * 0.5625)) / vec2(pixRes, pixRes * 0.5625);

    vec4 sceneColor = texture(DiffuseSampler, pixCoord);

    // Convert scene to grayscale to discard original colors entirely
    float gray = dot(sceneColor.rgb, vec3(0.299, 0.587, 0.114));

    // Gamma boost: brightens dark areas non-linearly without a constant floor
    float boosted = pow(gray, 0.4);

    // Rainbow hue: right-to-left gradient with leftward flow
    float flowSpeed = 0.25;  // 4 seconds per full cycle
    float hue = (1.0 - pixCoord.x) - RainbowTime * flowSpeed;
    hue = fract(hue);

    // Full saturation rainbow color
    vec3 rainbow = hue2rgb(hue);

    // Scene brightness modulates the rainbow
    vec3 finalColor = rainbow * boosted;

    // === Film grain ===
    float grain = (random(pixCoord + RainbowTime) - 0.5) * 0.12;
    finalColor += grain;

    // Subtle vignette for goggle framing
    vec2 uv = texCoord * (1.0 - texCoord.yx);
    float vig = uv.x * uv.y * 15.0;
    vig = pow(clamp(vig, 0.0, 1.0), 0.3);
    finalColor *= (0.45 + 0.55 * vig);

    fragColor = vec4(finalColor, 1.0);
}
