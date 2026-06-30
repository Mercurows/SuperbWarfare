#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    // Removed alpha discard: if (color.a < 0.1) { discard; }
    // This enables smooth soft-edged transparency for high-resolution particles
    // Custom fog: starts at vanilla FogStart, ends at 2048 blocks (max beyond-visual-range).
    // This gives the same gradual fog attenuation as entities rendered at any distance,
    // rather than the vanilla particle fog that saturates to white near the render edge.
    // Only RGB is fogged; alpha is preserved so the particle texture still works.
    float maxFogDistance = 2048.0;
    float fogValue = clamp((vertexDistance - FogStart) / (maxFogDistance - FogStart), 0.0, 1.0);
    fragColor = vec4(mix(color.rgb, FogColor.rgb, fogValue * FogColor.a), color.a);
}
