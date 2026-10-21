// Alpha distinguishes an omitted data colour from an explicitly configured black colour.
uniform vec4 ThermalBackgroundColor;
uniform vec4 ThermalEntityColor;

vec3 scopeThermalColour(sampler2D scene, sampler2D heat, vec2 uv, vec2 texel) {
    vec3 source = texture(scene, uv).rgb;
    float luminance = dot(source, vec3(0.2126, 0.7152, 0.0722));
    vec3 cold = mix(vec3(0.006, 0.014, 0.055), vec3(0.035, 0.11, 0.38), luminance);
    if (ThermalBackgroundColor.a > 0.5) {
        cold = ThermalBackgroundColor.rgb * mix(0.15, 1.0, luminance);
    }
    float center = texture(heat, uv).r;
    float outer = center;
    float inner = center;
    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            vec2 offset = vec2(x, y) * texel * 1.5;
            float value = texture(heat, clamp(uv + offset, texel * 0.5, vec2(1.0) - texel * 0.5)).r;
            outer = max(outer, value);
            inner = min(inner, value);
        }
    }
    vec3 hot = mix(vec3(0.6, 0.012, 0.005), vec3(1.0, 0.23, 0.1), luminance);
    if (ThermalEntityColor.a > 0.5) {
        hot = ThermalEntityColor.rgb * mix(0.6, 1.0, luminance);
    }
    vec3 colour = mix(cold, hot, center);
    return mix(colour, vec3(1.0), clamp(outer - inner, 0.0, 1.0));
}
