#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform vec2 DepthTexelSize;
uniform float HandDepthEnabled;
uniform float SelfHandPass;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    if (texture(Sampler0, texCoord).a < 0.1) discard;
    vec2 uv = gl_FragCoord.xy * DepthTexelSize;
    // Arms use their own projection/depth range. Strict final-depth testing keeps guns cold.
    if (SelfHandPass > 0.5) {
        if (HandDepthEnabled < 0.5 || gl_FragCoord.z > texture(Sampler2, uv).r + 0.000002) discard;
        fragColor = vec4(1.0);
        return;
    }
    float sceneDepth = texture(Sampler1, uv).r;
    // Accommodate the subpixel jitter applied by temporal shader packs at silhouette edges.
    for (int y = -1; y <= 1; ++y)
        for (int x = -1; x <= 1; ++x)
            sceneDepth = max(sceneDepth, texture(Sampler1, uv + vec2(x, y) * DepthTexelSize).r);
    if (gl_FragCoord.z > sceneDepth + 0.000002) discard;
    // 第一人称的手与手持物不在世界深度里，单独用一张深度图把它们挡掉，避免热源"穿透"手
    if (HandDepthEnabled > 0.5) {
        float handDepth = texture(Sampler2, uv).r;
        if (gl_FragCoord.z > handDepth + 0.000002) discard;
    }
    fragColor = vec4(1.0);
}
