#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 DepthTexelSize;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    if (texture(Sampler0, texCoord).a < 0.1) discard;
    vec2 uv = gl_FragCoord.xy * DepthTexelSize;
    float sceneDepth = texture(Sampler1, uv).r;
    // Accommodate the subpixel jitter applied by temporal shader packs at silhouette edges.
    for (int y = -1; y <= 1; ++y)
        for (int x = -1; x <= 1; ++x)
            sceneDepth = max(sceneDepth, texture(Sampler1, uv + vec2(x, y) * DepthTexelSize).r);
    if (gl_FragCoord.z > sceneDepth + 0.000002) discard;
    fragColor = vec4(1.0);
}
