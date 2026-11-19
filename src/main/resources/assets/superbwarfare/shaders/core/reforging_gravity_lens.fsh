#version 150

uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform sampler2D WorldDepth;
uniform int ShaderPackMode;
uniform mat4 LensProjection;
uniform mat4 InverseProjection;
uniform int LensCount;
uniform float LensTime;
uniform vec4 Lens0;
uniform vec4 Lens1;
uniform vec4 Lens2;
uniform vec4 Lens3;
uniform vec4 Lens4;
uniform vec4 Lens5;
uniform vec4 Lens6;
uniform vec4 Lens7;
uniform vec4 LensStrengths0;
uniform vec4 LensStrengths1;
uniform sampler2D EntitiesBefore;
uniform sampler2D EntitiesAfter;
uniform int EntityImageMask;

in vec2 texCoord;
out vec4 fragColor;

// Keep the spherical fallback in the renderer at the same radius.
const float HORIZON_RADIUS = 0.21;
const float INNER_LENS_RADIUS = 0.14;
// Preserve the existing outer-ring twist while extending refraction toward the smaller horizon.
const float LENS_SPIN_RADIUS = 0.28;
const float EINSTEIN_RADIUS_SQUARED = 0.16;

vec3 viewPosition(vec2 uv, float depth) {
    vec4 position = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return position.xyz / position.w;
}

bool handPixel(vec2 uv) {
    if (ShaderPackMode == 0) return false;
    float depth = texture(SceneDepth, uv).r;
    float beforeHand = texture(WorldDepth, uv).r;
    return depth < 0.6 && depth + 0.00001 < beforeHand;
}

bool changedEntityPixel(vec2 uv) {
    vec3 difference = abs(texture(EntitiesAfter, uv).rgb - texture(EntitiesBefore, uv).rgb);
    return max(max(difference.r, difference.g), difference.b) > 0.001;
}

bool entityPixel(vec2 uv) {
    if (EntityImageMask == 0) return false;
    // Dilate the actual silhouette to cover bilinear filtering and shader-pack subpixel jitter.
    vec2 texel = 1.0 / vec2(textureSize(EntitiesBefore, 0));
    return changedEntityPixel(uv) ||
        changedEntityPixel(uv + vec2(texel.x, 0.0)) ||
        changedEntityPixel(uv - vec2(texel.x, 0.0)) ||
        changedEntityPixel(uv + vec2(0.0, texel.y)) ||
        changedEntityPixel(uv - vec2(0.0, texel.y)) ||
        changedEntityPixel(uv + texel) || changedEntityPixel(uv - texel) ||
        changedEntityPixel(uv + vec2(-texel.x, texel.y)) ||
        changedEntityPixel(uv + vec2(texel.x, -texel.y));
}
vec4 lensAt(int index) {
    if (index == 0) return Lens0;
    if (index == 1) return Lens1;
    if (index == 2) return Lens2;
    if (index == 3) return Lens3;
    if (index == 4) return Lens4;
    if (index == 5) return Lens5;
    if (index == 6) return Lens6;
    return Lens7;
}

void main() {
    if (handPixel(texCoord)) discard;
    vec2 halfTexel = 0.5 / vec2(textureSize(SceneColor, 0));
    vec3 nearPoint = viewPosition(texCoord, 0.0);
    vec3 ray = normalize(viewPosition(texCoord, 1.0) - nearPoint);
    // The projection can also contain camera-bob translations; recover its perspective origin.
    vec4 eye = InverseProjection * vec4(0.0, 0.0, 1.0, 0.0);
    vec3 eyeOrigin = eye.xyz / eye.w;
    vec3 surface = viewPosition(texCoord, texture(SceneDepth, texCoord).r);
    float surfaceDistance = dot(surface - nearPoint, ray);
    vec2 displacement = vec2(0.0);
    float coreOpacity = 0.0;
    float nearestCore = 1.0e20;
    vec3 haloColor = vec3(0.0);
    bool entityChecked = false;
    bool entityFragment = false;

    for (int index = 0; index < 8; ++index) {
        if (index >= LensCount) break;
        vec4 lens = lensAt(index);
        float strength = index < 4 ? LensStrengths0[index] : LensStrengths1[index - 4];
        float distanceAlongRay = dot(lens.xyz - nearPoint, ray);
        if (distanceAlongRay <= 0.0 || lens.w <= 0.0 || strength <= 0.0) continue;
        vec3 separation = nearPoint + ray * distanceAlongRay - lens.xyz;
        float impact = length(separation) / lens.w;
        if (impact >= 1.0) continue;

        float coreRadius = lens.w * HORIZON_RADIUS;
        float coreFront = distanceAlongRay - sqrt(max(coreRadius * coreRadius - dot(separation, separation), 0.0));
        float visibility = smoothstep(coreFront - lens.w * 0.005, coreFront, surfaceDistance);
        float aa = max(fwidth(impact) * 1.25, 0.001);
        float capture = 1.0 - smoothstep(HORIZON_RADIUS - aa, HORIZON_RADIUS + aa, impact);
        float opacity = capture * visibility;
        if (opacity > 0.0 && coreFront < nearestCore) {
            // Sphere geometry and lensing share the same ray/center, preventing displaced crescents.
            // Lens-strength fading must never make the event horizon transparent.
            coreOpacity = opacity;
            nearestCore = coreFront;
        }
        float outside = max(impact - HORIZON_RADIUS, 0.0);
        // Match blackHoleCore's fallback: hot white inside, broad gold outside, then a soft fade.
        float whiteBand = exp(-pow(outside / 0.032, 2.0)) * 1.65;
        float goldDistance = (outside - 0.052) / 0.05;
        float goldBand = exp(-goldDistance * goldDistance) * 0.85;
        float softHalo = exp(-pow(outside / 0.15, 2.0)) * 0.12;
        haloColor += (vec3(1.0, 0.98, 0.93) * whiteBand +
            vec3(1.0, 0.75, 0.2) * (goldBand + softHalo)) * visibility;
        if (opacity >= 1.0) continue;

        // Test against the black-hole core, not the front of its much larger influence sphere.
        float fade = clamp(strength / 0.2, 0.0, 1.0) * visibility;
        if (fade <= 0.0) continue;
        if (impact < INNER_LENS_RADIUS) continue;
        if (!entityChecked) {
            entityFragment = entityPixel(texCoord);
            entityChecked = true;
        }
        // A single final scene cannot reconstruct the background hidden by an entity's old silhouette.
        // Keep that silhouette stable rather than displaying both the original and a stretched copy.
        if (entityFragment) continue;

        // Stretch the original inner image annulus all the way down to the smaller horizon.
        // A negative thin-lens branch sampled foreground ground across the center; rejecting that
        // sample exposed an undistorted "window" between the black sphere and the lens wall.
        float innerExtension = 1.0 - smoothstep(HORIZON_RADIUS, 0.65, impact);
        float wallRadius = impact + (sqrt(EINSTEIN_RADIUS_SQUARED) - HORIZON_RADIUS) * innerExtension;
        float outerFade = 1.0 - smoothstep(0.72, 1.0, wallRadius);
        float sourceRadius = max(wallRadius - EINSTEIN_RADIUS_SQUARED * outerFade / wallRadius, 0.0);
        float spin = (0.07 + 0.01 * sin(LensTime * 0.18 + index)) * outerFade
            * pow(LENS_SPIN_RADIUS / max(wallRadius, LENS_SPIN_RADIUS), 2.0);
        // Use the same angular ray geometry as the spherical horizon. Screen-space radius ratios
        // drift away from it at close range and when the ball is above/below the screen center.
        vec3 toCenter = lens.xyz - eyeOrigin;
        float centerDistance = length(toCenter);
        vec3 axis = toCenter / centerDistance;
        vec3 tangent = normalize(ray - axis * dot(ray, axis));
        tangent = tangent * cos(spin) + cross(axis, tangent) * sin(spin);
        float sourceSine = clamp(sourceRadius * lens.w / centerDistance, 0.0, 0.9999);
        vec3 sourceRay = axis * sqrt(1.0 - sourceSine * sourceSine) + tangent * sourceSine;
        vec4 sourceClip = LensProjection * vec4(eyeOrigin + sourceRay * centerDistance, 1.0);
        if (sourceClip.w <= 0.0) continue;
        vec2 sourceUv = sourceClip.xy / sourceClip.w * 0.5 + 0.5;
        float screenEdge = min(min(sourceUv.x, sourceUv.y), min(1.0 - sourceUv.x, 1.0 - sourceUv.y));
        float screenVisibility = smoothstep(0.0, 0.015, screenEdge);
        if (screenVisibility <= 0.0) continue;
        sourceUv = clamp(sourceUv, halfTexel, vec2(1.0) - halfTexel);
        if (handPixel(sourceUv)) continue;
        if (entityPixel(sourceUv)) {
            // Find a nearby background sample instead of pulling entity colors into the lens wall.
            vec2 blockedUv = sourceUv;
            float blocked = 0.0;
            float safe = 1.0;
            for (int step = 0; step < 6; ++step) {
                float middle = (blocked + safe) * 0.5;
                if (entityPixel(mix(blockedUv, texCoord, middle))) blocked = middle;
                else safe = middle;
            }
            float margin = 2.0 * length(halfTexel) / max(length(texCoord - blockedUv), 0.000001);
            sourceUv = mix(blockedUv, texCoord, min(safe + margin, 1.0));
            if (entityPixel(sourceUv) || handPixel(sourceUv)) sourceUv = texCoord;
        }
        vec3 source = viewPosition(sourceUv, texture(SceneDepth, sourceUv).r);
        float sourceVisibility = smoothstep(coreFront - coreRadius * 0.05, coreFront, dot(source - nearPoint, ray));
        float contribution = fade * screenVisibility * sourceVisibility;
        displacement += (sourceUv - texCoord) * contribution;
    }

    if (coreOpacity == 0.0 && dot(displacement, displacement) == 0.0 && dot(haloColor, haloColor) == 0.0) discard;
    vec2 uv = clamp(texCoord + displacement, halfTexel, vec2(1.0) - halfTexel);
    if (dot(displacement, displacement) > 0.0 && (handPixel(uv) || entityPixel(uv))) uv = texCoord;
    vec3 color = texture(SceneColor, uv).rgb;
    color = mix(color, vec3(0.0), coreOpacity) + haloColor * (1.0 - coreOpacity);
    fragColor = vec4(color, texture(SceneColor, texCoord).a);
}
