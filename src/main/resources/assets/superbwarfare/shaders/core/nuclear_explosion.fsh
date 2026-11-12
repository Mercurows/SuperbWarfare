#version 150

uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform sampler2D WorldDepth;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
uniform vec3 ExplosionPosition;
uniform float Age;
uniform float FlowTime;
uniform vec3 EffectScale;
uniform float Seed;
uniform float FlashVisibility;
uniform int ShaderPackMode;

in vec2 texCoord;
out vec4 fragColor;

// World-space, continuously advected density. No sprites, cubes or polygon shells.
float hash(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + vec3(1,0,0)), f.x),
                   mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
               mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
                   mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y), f.z);
}

// Broad billows plus a smaller folded surface, all continuous in world space.
float fbm(vec3 p) {
    return 0.72 * noise(p) + 0.28 * noise(p * 1.93 + vec3(7.1, 19.3, 3.7));
}

// Value and analytic gradient from the same eight corners. The gradient shades
// small folds without tracing a second complete cloud for every occupied sample.
vec4 noiseGradient(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f), du = 6.0 * f * (1.0 - f);
    float a = hash(i), b = hash(i + vec3(1,0,0));
    float c = hash(i + vec3(0,1,0)), d = hash(i + vec3(1,1,0));
    float e = hash(i + vec3(0,0,1)), f1 = hash(i + vec3(1,0,1));
    float g = hash(i + vec3(0,1,1)), h = hash(i + vec3(1,1,1));
    float k1 = b-a, k2 = c-a, k3 = e-a;
    float k4 = a-b-c+d, k5 = a-c-e+g, k6 = a-b-e+f1;
    float k7 = -a+b+c-d+e-f1-g+h;
    float value = a + k1*u.x + k2*u.y + k3*u.z + k4*u.x*u.y +
        k5*u.y*u.z + k6*u.z*u.x + k7*u.x*u.y*u.z;
    vec3 gradient = du * vec3(k1 + k4*u.y + k6*u.z + k7*u.y*u.z,
        k2 + k5*u.z + k4*u.x + k7*u.z*u.x, k3 + k6*u.x + k5*u.y + k7*u.x*u.y);
    return vec4(value, gradient);
}

float ellipsoid(vec3 p, vec3 radius) {
    return (length(p / radius) - 1.0) * min(min(radius.x, radius.y), radius.z);
}

float roundUnion(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float capRadius;
float capHeight;
float mushroom;
float dustRadius;
float shockRadius;
vec3 drift;

float pressure(vec3 p) {
    if (Age >= 1.25 || abs(p.y - 3.0) > 5.0) return 0.0;
    float shell = length(vec2((length(p.xz) - shockRadius) * 0.55, (p.y - 3.0) * 1.25)) - 3.2;
    return (1.0 - smoothstep(-1.0, 2.0, shell)) * smoothstep(0.01, 0.05, Age) *
        (1.0 - smoothstep(0.25, 1.25, Age)) * smoothstep(-1.0, 0.5, p.y);
}

// Inverse advection around a toroidal convection cell: up the middle, out over the
// crown, down its outer edge and back underneath. The stem feeds the same volume.
vec3 convection(vec3 p) {
    float radial = length(p.xz);
    vec2 direction = p.xz / max(radial, 0.01);
    float major = capRadius * 0.58;
    vec2 crossSection = vec2(radial - major, (p.y - capHeight) * 1.15);
    // GLSL constructs this matrix column-first. Negative time samples upward at
    // the outer rim, so the visible material rolls DOWN there and over the top.
    float angle = -FlowTime * 0.66;
    mat2 rotation = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    crossSection = rotation * crossSection;
    vec3 rolling = vec3(direction.x * (major + crossSection.x), crossSection.y / 1.15,
                        direction.y * (major + crossSection.x));
    return rolling;
}

// Untwist curved streamlines before sampling the advected field. Visible wisps
// follow a quarter-ellipse vertically and also bend sideways into the stem.
vec2 entrainmentDirection(vec3 p) {
    float outer = max(capRadius * 0.82, 7.0);
    float fraction = clamp((length(p.xz) - 6.0) / (outer - 6.0), 0.0, 1.0);
    float angle = atan(p.z, p.x) - 0.85 * (1.0 - fraction) * (1.0 - fraction);
    return vec2(cos(angle), sin(angle));
}

vec3 entrainment(vec3 p, float height) {
    float radial = length(p.xz);
    vec2 direction = entrainmentDirection(p);
    float upstream = radial + FlowTime * 4.2;
    return vec3(direction.x * upstream, (p.y - height) * 1.4, direction.y * upstream);
}

// Each curved smoke lane has its own seeded join height, drop and thickness.
// Fade the lane edges so height changes make separate wisps, never vertical seams.
vec4 entrainmentProfile(vec3 p) {
    vec2 direction = entrainmentDirection(p);
    float lanePosition = (atan(direction.y, direction.x) + 3.1415927) * 9.0 / 6.2831853;
    float lane = floor(lanePosition);
    float heightRandom = hash(vec3(lane, Seed * 53.0, 1.7));
    float dropRandom = hash(vec3(lane, Seed * 53.0, 7.3));
    float widthRandom = hash(vec3(lane, Seed * 53.0, 13.1));
    float edge = abs(fract(lanePosition) - 0.5);
    float width = mix(0.30, 0.47, widthRandom);
    float mask = 1.0 - smoothstep(width * 0.55, width, edge);
    float height = (heightRandom - 0.5) * capRadius * 0.32 +
        sin(FlowTime * 0.14 + lane * 2.37 + Seed * 17.0) * 1.4;
    return vec4(height, capRadius * mix(0.28, 0.53, dropRandom),
        mix(1.1, 2.4, widthRandom), mask);
}

float entrainmentHeight(vec3 p) {
    float outer = max(capRadius * 0.82, 7.0);
    float fraction = clamp((length(p.xz) - 6.0) / (outer - 6.0), 0.0, 1.0);
    vec4 profile = entrainmentProfile(p);
    return max(capHeight - capRadius * 0.48, 8.0) + 2.0 -
        profile.y * sqrt(fraction * (2.0 - fraction)) + profile.x;
}

// Smoke and fire are temperature fractions of ONE evolving buoyant volume.
vec4 medium(vec3 point, out vec4 surface) {
    surface = vec4(0.0, 1.0, 0.0, 0.0);
    vec3 p = point - drift * smoothstep(0.0, capHeight, point.y);
    float radial = length(p.xz);
    vec3 head = p - vec3(0.0, capHeight, 0.0);
    float roundness = mix(1.0, 0.58, mushroom);
    float dome = ellipsoid(head, vec3(capRadius, capRadius * roundness, capRadius));
    float crown = 10000.0;
    // Reject empty space before sector trigonometry, billow noise or lighting.
    if (dome < capRadius * 0.18 + 8.0) {
        float angle = atan(p.z, p.x);
        float sectorWidth = 6.2831853 / 10.0;
        float rotation = FlowTime * 0.10;
        float sector = floor((angle - rotation) / sectorWidth);
        float lobes = 10000.0;
        for (int j = 0; j < 2; ++j) {
            float theta = (sector + float(j)) * sectorWidth + rotation;
            vec3 center = vec3(cos(theta) * capRadius * 0.68,
                capRadius * 0.05 + sin(theta * 3.0 - FlowTime * 0.48) * 2.8,
                sin(theta) * capRadius * 0.68);
            float lobe = ellipsoid(head - center, vec3(capRadius * 0.34, capRadius * 0.45, capRadius * 0.34));
            lobes = roundUnion(lobes, lobe, 2.5);
        }
        float top = ellipsoid(head - vec3(0.0,capRadius * 0.08,0.0),
            vec3(capRadius * 0.80,capRadius * 0.56,capRadius * 0.80));
        crown = mix(dome, roundUnion(top, lobes, 3.0), mushroom);
    }
    float visibleStemHeight = max(capHeight - capRadius * 0.48, 8.0);
    float stemFraction = clamp(p.y / visibleStemHeight, 0.0, 1.0);
    float stemRadius = mix(19.0, 7.5, smoothstep(0.0, 1.0, stemFraction));
    // Only the final billows flare into the cap; the long exposed stem stays tapered.
    float neck = smoothstep(visibleStemHeight - capRadius * 0.18,
        visibleStemHeight + capRadius * 0.14, p.y) * mushroom;
    stemRadius += capRadius * 0.18 * neck;
    vec2 stemOffset = vec2(0.0);
    if (p.y > -8.0 && p.y < capHeight + 8.0 && radial < 30.0) {
        float bend = sin(stemFraction * 3.1415927);
        stemOffset = vec2(sin(p.y * 0.093 - FlowTime * 0.23 + Seed * 11.0),
            sin(p.y * 0.057 + FlowTime * 0.17 + Seed * 19.0)) * bend * 4.8;
        stemRadius *= 1.0 + sin(p.y * 0.19 + Seed * 23.0 - FlowTime * 0.31) * 0.12;
    }
    vec2 stemPosition = p.xz - stemOffset;
    float stem = max(length(stemPosition) - stemRadius, max(-p.y + 0.3, p.y - capHeight));
    // The stem emerges as the original fireball lifts; no separately spawned smoke cap.
    stem += (1.0 - smoothstep(0.7, 3.0, Age)) * 80.0;
    float joinSoftness = mix(2.0, 11.0, mushroom);
    float crownWeight = clamp(0.5 + 0.5 * (stem - crown) / joinSoftness, 0.0, 1.0);
    float bodyShape = roundUnion(crown, stem, joinSoftness);
    float dustHeight = (2.5 + min(Age, 7.0)) * 0.3;
    float dustBand = 10000.0;
    if (Age < 16.0 && point.y < 7.0) {
        dustBand = length(vec2((radial - dustRadius) * 0.52,
            (point.y - dustHeight) * 4.5)) - (3.0 + min(Age, 12.0) * 0.35);
    }
    float shock = pressure(point);
    bool nearBody = bodyShape < 8.0;
    bool nearDust = Age < 16.0 && dustBand < 7.0;
    float inflowInner = 6.0;
    float inflowOuter = max(capRadius * 0.82, inflowInner + 1.0);
    float inflowFraction = 0.0, inflowHeight = 0.0, inflowShape = 10000.0, inflowLane = 0.0;
    if (Age > 4.0 && radial > inflowInner && radial < inflowOuter &&
        p.y > visibleStemHeight - capRadius * 0.70 - 8.0 && p.y < visibleStemHeight + capRadius * 0.20 + 8.0) {
        inflowFraction = clamp((radial - inflowInner) / (inflowOuter - inflowInner), 0.0, 1.0);
        vec4 profile = entrainmentProfile(p);
        float joinHeight = visibleStemHeight + 2.0 + profile.x;
        inflowHeight = joinHeight - profile.y * sqrt(inflowFraction * (2.0 - inflowFraction));
        inflowLane = profile.w;
        // Distance to the curved path, rather than a sloping plane. The outer
        // tangent is horizontal; the inner tangent turns upward into the stem.
        float drop = profile.y;
        vec2 arc = vec2((radial - inflowOuter) / (inflowOuter - inflowInner),
            (p.y - joinHeight) / drop);
        inflowShape = abs(length(arc) - 1.0) * drop - profile.z * mix(1.0, 0.5, inflowFraction);
    }
    bool nearInflow = inflowShape < 2.0 && inflowLane > 0.001;
    // Compress the previous bank vertically to 30%, then taper it down toward its edge.
    // Radius still follows the crown at 1.18x; this changes height, not its broad footprint.
    float pileRadius = capRadius * 1.18;
    float pileHeight = 3.0 + 11.0 * (1.0 - exp(-Age / 4.0));
    float pileThickness = 3.0 + 10.0 * (1.0 - exp(-Age / 5.0));
    float taper = mix(1.0, 0.18, smoothstep(0.15, 1.0, radial / pileRadius));
    vec3 footPoint = vec3(point.x, point.y / (0.3 * taper), point.z);
    float pileShape = 10000.0;
    if (radial < pileRadius + 8.0 && abs(footPoint.y - pileHeight) < pileThickness * 1.5 + 8.0) {
        pileShape = ellipsoid(footPoint - vec3(0.0, pileHeight, 0.0),
            vec3(pileRadius, pileThickness, pileRadius));
        float pileRim = length(vec2(radial - pileRadius * 0.63,
            (footPoint.y - pileHeight) * 1.2)) - pileThickness * 1.1;
        pileShape = roundUnion(pileShape, pileRim, 3.0);
        float pileSector = floor(atan(point.z, point.x) * 7.0 / 6.2831853);
        for (int j = 0; j < 2; ++j) {
            float theta = (pileSector + float(j)) * 6.2831853 / 7.0;
            vec3 center = vec3(cos(theta) * pileRadius * 0.60,
                pileHeight + sin(theta * 2.0 + 0.7) * pileThickness * 0.24,
                sin(theta) * pileRadius * 0.60);
            float heap = ellipsoid(footPoint - center,
                vec3(pileRadius * 0.42, pileThickness * 1.25, pileRadius * 0.42));
            pileShape = roundUnion(pileShape, heap, 2.5);
        }
    }
    bool nearFoot = pileShape < 8.0;
    if (!(nearBody || nearDust || nearFoot || nearInflow || shock > 0.0)) return vec4(0.0);

    float n = 0.5, body = 0.0, temperature = 0.0;
    if (nearBody) {
        float crownBlend = smoothstep(visibleStemHeight - capRadius * 0.22,
            visibleStemHeight + capRadius * 0.20, p.y);
        vec3 rising = p - vec3(0.0, FlowTime * 5.0, 0.0);
        vec3 rolling = convection(p);
        float broad = 0.0;
        vec4 folds = vec4(0.0);
        if (crownBlend > 0.0) {
            broad = fbm(rolling * 0.14 + Seed * 73.0);
            folds = noiseGradient(rolling * 0.34 + Seed * 107.0);
        }
        if (crownBlend < 1.0) {
            broad = mix(fbm(rising * 0.14 + Seed * 73.0), broad, crownBlend);
            folds = mix(noiseGradient(rising * 0.34 + Seed * 107.0), folds, crownBlend);
        }
        n = broad * 0.60 + folds.x * 0.40;
        float billow = (broad - 0.5) * 9.0 + (folds.x - 0.5) * 3.2;
        body = 1.0 - smoothstep(-0.9, 1.1, bodyShape + billow);
        body *= mix(0.55, 1.25, smoothstep(0.24, 0.75, n));
        vec3 crownNormal = normalize(head / vec3(1.0, roundness * roundness, 1.0) + vec3(0.0001));
        vec3 stemNormal = normalize(vec3(stemPosition.x, stemRadius * 11.5 / visibleStemHeight,
            stemPosition.y) + vec3(0.0001));
        vec3 normal = normalize(mix(stemNormal, crownNormal, crownWeight) + vec3(0.0001));
        surface = vec4(normalize(normal + folds.yzw * 0.65), bodyShape);

        // Persistent orange-red heat lives in the crown's folded smoke clumps.
        float cooling = smoothstep(4.0, 38.0, Age);
        float thermalCell = smoothstep(0.29, 0.70, broad);
        temperature = clamp(1.0 - cooling * (1.1 - thermalCell * 0.7), 0.0, 1.0);
        temperature *= mix(0.35, 1.0, smoothstep(capHeight * 0.3, capHeight * 0.75, p.y));
        temperature = mix(1.0, temperature, smoothstep(1.8, 4.5, Age));
        float hotFold = smoothstep(0.28, 0.68, folds.x + broad * 0.12);
        temperature *= mix(1.0, mix(0.24, 1.0, hotFold), smoothstep(3.5, 9.0, Age));
        temperature *= 1.0 - smoothstep(36.0, 48.0, Age);
        // Spread the thermal transition across interlocking neck billows while
        // preserving a cool stem, instead of cutting a hot/cold seam.
        float neckHeat = smoothstep(visibleStemHeight - capRadius * 0.03,
            visibleStemHeight + capRadius * 0.20, p.y);
        float nearNeck = 1.0 - smoothstep(capRadius * 0.22, capRadius * 0.50, radial);
        float crownHeat = smoothstep(-joinSoftness * 0.35, joinSoftness * 0.8, stem - crown);
        temperature *= crownHeat * mix(1.0, neckHeat, nearNeck * mushroom);
        // Faint, broken embers bridge the upper neck into the burning crown.
        // Keep them above the long grey stem rather than warming the whole column.
        float upperNeck = smoothstep(visibleStemHeight - capRadius * 0.10,
            visibleStemHeight + capRadius * 0.10, p.y) *
            (1.0 - smoothstep(visibleStemHeight + capRadius * 0.12,
                visibleStemHeight + capRadius * 0.35, p.y));
        float neckEmbers = upperNeck * nearNeck * mushroom * (0.06 + hotFold * 0.18) *
            (1.0 - smoothstep(25.0, 45.0, Age));
        temperature = max(temperature, neckEmbers);
    }
    float dust = 0.0;
    if (nearDust) {
        float frontNoise = noise((p - vec3(0.0, FlowTime, 0.0)) * 0.2 + Seed * 73.0);
        dust = (1.0 - smoothstep(-1.0, 1.6, dustBand + (frontNoise - 0.5) * 7.0)) * 0.7;
        dust *= smoothstep(0.06, 0.35, Age) * (1.0 - smoothstep(5.5, 16.0, Age));
    }
    if (nearFoot) {
        vec3 pileFlow = footPoint + vec3(sin(footPoint.y * 0.3 + FlowTime * 0.8) * 3.0,
            -FlowTime * 1.1, cos(point.y * 0.3 - FlowTime * 0.7) * 3.0);
        float pileNoise = fbm(pileFlow * 0.13 + Seed * 73.0);
        float pile = 1.0 - smoothstep(-0.9, 1.4, pileShape + (pileNoise - 0.5) * 8.0);
        dust += pile * smoothstep(0.3, 3.0, Age) * 0.95;
    }
    float inflow = 0.0;
    if (nearInflow) {
        vec2 direction = entrainmentDirection(p);
        float streaks = noise(vec3(direction * 6.5, Seed * 37.0 + FlowTime * 0.06));
        float wisps = fbm(entrainment(p, inflowHeight) * 0.24 + Seed * 83.0);
        inflow = (1.0 - smoothstep(-0.7, 1.0, inflowShape + (wisps - 0.5) * 3.0)) *
            smoothstep(0.25, 0.60, streaks) * smoothstep(0.20, 0.62, wisps);
        inflow *= smoothstep(4.0, 9.0, Age) * (1.0 - smoothstep(0.85, 1.0, inflowFraction)) * inflowLane * 0.75;
    }
    float ground = smoothstep(-0.25, 0.18, point.y);
    float disperse = 1.0 - smoothstep(30.0, 50.0, Age);
    return vec4((body * (1.0 - temperature) + inflow) * ground * disperse,
        dust * ground * disperse + shock * 0.65, body * temperature * ground * disperse, n);
}

vec4 medium(vec3 point) {
    vec4 surface;
    return medium(point, surface);
}

vec3 viewPosition(float depth) {
    vec4 p = InverseProjection * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

vec2 intersectBounds(vec3 origin, vec3 ray, vec3 low, vec3 high) {
    vec3 safeRay = mix(vec3(0.000001), ray, greaterThan(abs(ray), vec3(0.000001)));
    vec3 a = (low - origin) / safeRay, b = (high - origin) / safeRay;
    vec3 entry = min(a, b), exitPoint = max(a, b);
    return vec2(max(max(entry.x, entry.y), entry.z), min(min(exitPoint.x, exitPoint.y), exitPoint.z));
}

void main() {
    float depth = texture(SceneDepth, texCoord).r;
    float worldDepth = texture(WorldDepth, texCoord).r;
    if (ShaderPackMode != 0 && depth < 0.6 && depth + 0.00001 < worldDepth) {
        fragColor = vec4(0.0);
        return;
    }
    vec3 nearView = viewPosition(0.0);
    vec3 nearWorld = (InverseView * vec4(nearView, 1.0)).xyz;
    // Trace in the authored cloud's local space. Horizontal radius and total
    // height scale independently, including its ground bank and shock front.
    vec3 originWorld = nearWorld - ExplosionPosition;
    vec3 worldRay = normalize(mat3(InverseView) * (viewPosition(1.0) - nearView));
    vec3 origin = originWorld / EffectScale;
    vec3 localDirection = worldRay / EffectScale;
    float rayScale = length(localDirection);
    vec3 ray = localDirection / rayScale;
    vec3 surface = ((InverseView * vec4(viewPosition(worldDepth), 1.0)).xyz - ExplosionPosition) / EffectScale;
    float surfaceDistance = worldDepth > 0.999999 ? 100000.0 : length(surface - origin);

    float lift = max(Age - 0.7, 0.0);
    capRadius = 2.0 + 26.0 * (1.0 - exp(-Age * 2.4)) +
        32.0 * (1.0 - exp(-max(Age - 1.0, 0.0) / 8.0));
    capHeight = 5.0 + 95.0 * (1.0 - exp(-lift / 8.0));
    mushroom = smoothstep(2.5, 11.0, Age);
    dustRadius = 5.0 + 51.0 * (1.0 - exp(-Age / 0.65));
    shockRadius = 10.0 + 280.0 * (1.0 - exp(-Age / 0.24));
    drift = vec3(Age * 0.24, 0.0, Age * 0.11);

    float radius = max(capRadius * 1.18 + 14.0, Age < 16.0 ? dustRadius + 34.0 : 32.0);
    radius = max(radius, Age < 1.25 ? shockRadius + 12.0 : 0.0);
    vec2 bounds = intersectBounds(origin, ray, vec3(-radius,-3.0,-radius) + drift,
        vec3(radius, capHeight + capRadius + 14.0, radius) + drift);
    float entry = max(bounds.x, 0.0), exitPoint = min(bounds.y, surfaceDistance);
    vec3 radiance = vec3(0.0);
    float transmission = 1.0;
    if (exitPoint > entry) {
        // A small stratification breaks march bands; broad billows avoid a gritty noise fringe.
        float jitter = 0.43 + 0.14 * fract(sin(dot(gl_FragCoord.xy,vec2(12.9898,78.233))) * 43758.5453);
        float stepSize = (exitPoint - entry) / 80.0;
        float distanceAlong = entry + jitter * stepSize;
        for (int i = 0; i < 80; ++i) {
            if (distanceAlong > exitPoint || transmission < 0.012) break;
            vec3 p = origin + ray * distanceAlong;
            vec4 surfaceInfo;
            vec4 m = medium(p, surfaceInfo);
            float density = m.x * 0.15 + m.y * 0.13 + m.z * 0.16;
            if (density > 0.001) {
                float opacity = 1.0 - exp(-density * stepSize);
                float directional = max(dot(normalize(surfaceInfo.xyz / EffectScale), vec3(-0.52,0.81,0.27)), 0.0);
                float burial = exp(-max(-surfaceInfo.w, 0.0) * 0.045);
                float selfShadow = 0.22 + directional * mix(0.38,0.78,burial);
                float sun = 0.18 + selfShadow * 0.82;
                // A compact, smoothly blended palette reads as sculpted MC-style billows.
                vec3 smokeColor = mix(vec3(0.16,0.145,0.13), vec3(0.39,0.37,0.34), smoothstep(0.2,0.58,sun));
                smokeColor = mix(smokeColor, vec3(0.62,0.59,0.53), smoothstep(0.62,0.9,sun) * 0.75);
                // Entrained soil gives the stem a warm ochre/brown palette, with
                // darker folds and tan highlights rather than neutral grey.
                float soilSmoke = 1.0 - smoothstep(capHeight - capRadius * 0.48,
                    capHeight + capRadius * 0.08, p.y);
                vec3 columnColor = mix(vec3(0.22,0.14,0.075), vec3(0.48,0.35,0.22), smoothstep(0.2,0.58,sun));
                columnColor = mix(columnColor, vec3(0.68,0.53,0.35), smoothstep(0.62,0.9,sun) * 0.75);
                smokeColor = mix(smokeColor, columnColor, soilSmoke * 0.9);
                float dustFraction = clamp(m.y / max(m.x + m.y + m.z, 0.01), 0.0, 1.0);
                float piled = (1.0 - smoothstep(28.0, 68.0, length(p.xz))) * smoothstep(2.0, 6.0, Age);
                vec3 earthColor = mix(vec3(0.40,0.30,0.20), vec3(0.36,0.32,0.27), piled);
                earthColor *= 0.65 + selfShadow * 0.5;
                vec3 dustColor = mix(earthColor, vec3(0.91,0.94,0.96),
                    clamp(pressure(p) * 0.65 / max(m.y, 0.01), 0.0, 1.0));
                smokeColor = mix(smokeColor, dustColor, dustFraction);
                float heatPatch = smoothstep(0.28,0.72,m.w);
                vec3 fireColor = mix(vec3(0.85,0.16,0.025), vec3(1.45,0.58,0.08), heatPatch);
                fireColor = mix(fireColor, vec3(1.9,0.78,0.14), smoothstep(0.78,0.95,m.w) * 0.45);
                float whiteHeat = (1.0 - smoothstep(0.8,5.0,Age)) * (0.35 + heatPatch * 0.65);
                vec3 whiteFire = mix(vec3(1.3,0.55,0.13), vec3(1.7,1.3,0.65), heatPatch);
                fireColor = mix(fireColor, whiteFire, whiteHeat);
                float fireMix = clamp(m.z * 0.16 / density, 0.0, 1.0);
                vec3 color = mix(smokeColor, fireColor, fireMix);
                color += vec3(0.28,0.09,0.015) * fireMix * (1.0 - selfShadow) * 0.35;
                // Atmospheric perspective gives the large cloud a place in the world.
                float haze = 1.0 - exp(-distanceAlong / rayScale * 0.0007);
                color = mix(color, vec3(0.45,0.49,0.53), haze * 0.45);
                radiance += transmission * opacity * color;
                transmission *= 1.0 - opacity;
            }
            distanceAlong += stepSize;
        }
    }

    // Brief primary flash and weaker, warmer secondary pulse; exposure recovers rapidly.
    float flash = exp(-Age * 7.0) + 0.38 * exp(-pow((Age - 0.32) / 0.20, 2.0));
    vec3 source = vec3(0.0, 4.0 * EffectScale.y, 0.0) - originWorld;
    float distanceToSource = max(length(source), 1.0);
    float facing = max(dot(worldRay, source / distanceToSource), 0.0);
    float attenuation = 1.0 / (1.0 + distanceToSource * distanceToSource / (90000.0 * EffectScale.x * EffectScale.x));
    float wash = 1.0 - exp(-flash * attenuation * FlashVisibility * (0.35 + pow(facing, 6.0) * 3.8));
    float bloom = pow(facing, 90.0) * flash * attenuation * FlashVisibility;
    radiance = radiance * (1.0 - wash) + vec3(1.0,0.94,0.78) * wash + vec3(1.0,0.65,0.27) * bloom;
    transmission *= 1.0 - wash;

    // Short-lived reflected light on the actual reconstructed terrain, without changing blocks.
    if (worldDepth < 0.999999) {
        float illumination = exp(-length(surface * EffectScale) / (95.0 * EffectScale.x)) *
            (flash * 1.6 + exp(-Age * 0.7) * 0.55) * FlashVisibility;
        radiance += texture(SceneColor, texCoord).rgb * vec3(1.0,0.65,0.32) * illumination * transmission;
    }
    fragColor = vec4(radiance, 1.0 - transmission);
}
