#version 150

#moj_import <superbwarfare:scope_thermal.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 ThermalTexelSize;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = vec4(scopeThermalColour(Sampler0, Sampler1, texCoord, ThermalTexelSize), 1.0);
}
