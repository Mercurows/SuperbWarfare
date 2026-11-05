#version 150

#moj_import <superbwarfare:scope_thermal.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 ThermalTexelSize;
uniform float ThermalMagnification;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    // 没有实体镜片可走镜片着色器的热成像瞄具：在这里按屏幕中心补上镜内放大，
    // 否则世界被拆成 sqrt(zoom) 渲染、镜内却按 1:1 显示，热源遮罩就会和视野错位。
    float magnification = max(ThermalMagnification, 0.0001);
    vec2 uv = (texCoord - 0.5) / magnification + 0.5;
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        discard;
    }
    fragColor = vec4(scopeThermalColour(Sampler0, Sampler1, uv, ThermalTexelSize * magnification), 1.0);
}
