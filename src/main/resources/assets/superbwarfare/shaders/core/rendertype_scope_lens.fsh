#version 150

#moj_import <superbwarfare:scope_thermal.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform int ThermalEnabled;
uniform vec2 LensCenter;
uniform float LensRadius;
uniform float LensEdgeWidth;
uniform float LensMagnification;
uniform float LensScreenScale;
uniform float LensHardEdge;
uniform vec2 LensTexelSize;
uniform vec3 LensTint;
uniform mat4 ProjMat;

in vec3 vViewPos;

out vec4 fragColor;

const float BLUR = 0.155;
const float LENS_SCALE = 0.98;
const float DISTORTION = 0.05;
const float ABERRATION = 0.007;
const float RCAS_LIMIT = 0.25;
const float RCAS_SHARPNESS = 0.2;

vec2 screenUv(vec3 viewPos) {
    vec4 clip = ProjMat * vec4(viewPos, 1.0);
    return clip.xy / clip.w * (0.5 * LensScreenScale) + 0.5;
}

vec3 easuFetch(ivec2 p, ivec2 hi) {
    return texelFetch(Sampler0, clamp(p, ivec2(0), hi), 0).rgb;
}

vec3 lensVoxel(vec2 uv) {
    vec2 size = floor(1.0 / LensTexelSize + 0.5);
    ivec2 hi = ivec2(size) - ivec2(1);
    return texelFetch(Sampler0, clamp(ivec2(floor(uv * size)), ivec2(0), hi), 0).rgb;
}

void easuTap(inout vec3 aC, inout float aW, vec2 off, vec2 dir, vec2 len, float lob, float clp, vec3 c) {
    vec2 v = vec2(dot(off, dir), dot(off, vec2(-dir.y, dir.x)));
    v *= len;
    float d2 = min(dot(v, v), clp);
    float wB = 0.4 * d2 - 1.0;
    float wA = lob * d2 - 1.0;
    wB *= wB;
    wA *= wA;
    wB = 1.5625 * wB - 0.5625;
    float w = wB * wA;
    aC += c * w;
    aW += w;
}

void easuSet(inout vec2 dir, inout float len, float w, float lA, float lB, float lC, float lD, float lE) {
    float lenX = max(abs(lD - lC), abs(lC - lB));
    float dirX = lD - lB;
    dir.x += dirX * w;
    lenX = clamp(abs(dirX) / max(lenX, 1.0e-8), 0.0, 1.0);
    len += lenX * lenX * w;

    float lenY = max(abs(lE - lC), abs(lC - lA));
    float dirY = lE - lA;
    dir.y += dirY * w;
    lenY = clamp(abs(dirY) / max(lenY, 1.0e-8), 0.0, 1.0);
    len += lenY * lenY * w;
}

vec3 easu(vec2 uv) {
    vec2 texSize = 1.0 / LensTexelSize;
    ivec2 hi = ivec2(round(texSize)) - ivec2(1);
    vec2 pp = uv * texSize - 0.5;
    vec2 fpF = floor(pp);
    pp -= fpF;
    ivec2 fp = ivec2(fpF);

    vec3 bC = easuFetch(fp + ivec2(0, -1), hi);
    vec3 cC = easuFetch(fp + ivec2(1, -1), hi);
    vec3 iC = easuFetch(fp + ivec2(-1, 1), hi);
    vec3 jC = easuFetch(fp + ivec2(0, 1), hi);
    vec3 fC = easuFetch(fp, hi);
    vec3 eC = easuFetch(fp + ivec2(-1, 0), hi);
    vec3 kC = easuFetch(fp + ivec2(1, 1), hi);
    vec3 lC = easuFetch(fp + ivec2(2, 1), hi);
    vec3 hC = easuFetch(fp + ivec2(2, 0), hi);
    vec3 gC = easuFetch(fp + ivec2(1, 0), hi);
    vec3 oC = easuFetch(fp + ivec2(1, 2), hi);
    vec3 nC = easuFetch(fp + ivec2(0, 2), hi);

    float bL = bC.g + 0.5 * (bC.r + bC.b);
    float cL = cC.g + 0.5 * (cC.r + cC.b);
    float iL = iC.g + 0.5 * (iC.r + iC.b);
    float jL = jC.g + 0.5 * (jC.r + jC.b);
    float fL = fC.g + 0.5 * (fC.r + fC.b);
    float eL = eC.g + 0.5 * (eC.r + eC.b);
    float kL = kC.g + 0.5 * (kC.r + kC.b);
    float lL = lC.g + 0.5 * (lC.r + lC.b);
    float hL = hC.g + 0.5 * (hC.r + hC.b);
    float gL = gC.g + 0.5 * (gC.r + gC.b);
    float oL = oC.g + 0.5 * (oC.r + oC.b);
    float nL = nC.g + 0.5 * (nC.r + nC.b);

    vec2 dir = vec2(0.0);
    float len = 0.0;
    easuSet(dir, len, (1.0 - pp.x) * (1.0 - pp.y), bL, eL, fL, gL, jL);
    easuSet(dir, len, pp.x * (1.0 - pp.y), cL, fL, gL, hL, kL);
    easuSet(dir, len, (1.0 - pp.x) * pp.y, fL, iL, jL, kL, nL);
    easuSet(dir, len, pp.x * pp.y, gL, jL, kL, lL, oL);

    vec2 dir2 = dir * dir;
    float dirR = dir2.x + dir2.y;
    bool zro = dirR < (1.0 / 32768.0);
    dirR = zro ? 1.0 : inversesqrt(dirR);
    dir.x = zro ? 1.0 : dir.x;
    dir *= dirR;

    len *= 0.5;
    len *= len;
    float stretch = dot(dir, dir) / max(max(abs(dir.x), abs(dir.y)), 1.0e-8);
    vec2 len2 = vec2(1.0 + (stretch - 1.0) * len, 1.0 - 0.5 * len);
    float lob = 0.5 - 0.29 * len;
    float clp = 1.0 / lob;

    vec3 min4 = min(min(fC, gC), min(jC, kC));
    vec3 max4 = max(max(fC, gC), max(jC, kC));

    vec3 aC = vec3(0.0);
    float aW = 0.0;
    easuTap(aC, aW, vec2(0.0, -1.0) - pp, dir, len2, lob, clp, bC);
    easuTap(aC, aW, vec2(1.0, -1.0) - pp, dir, len2, lob, clp, cC);
    easuTap(aC, aW, vec2(-1.0, 1.0) - pp, dir, len2, lob, clp, iC);
    easuTap(aC, aW, vec2(0.0, 1.0) - pp, dir, len2, lob, clp, jC);
    easuTap(aC, aW, vec2(0.0, 0.0) - pp, dir, len2, lob, clp, fC);
    easuTap(aC, aW, vec2(-1.0, 0.0) - pp, dir, len2, lob, clp, eC);
    easuTap(aC, aW, vec2(1.0, 1.0) - pp, dir, len2, lob, clp, kC);
    easuTap(aC, aW, vec2(2.0, 1.0) - pp, dir, len2, lob, clp, lC);
    easuTap(aC, aW, vec2(2.0, 0.0) - pp, dir, len2, lob, clp, hC);
    easuTap(aC, aW, vec2(1.0, 0.0) - pp, dir, len2, lob, clp, gC);
    easuTap(aC, aW, vec2(1.0, 2.0) - pp, dir, len2, lob, clp, oC);
    easuTap(aC, aW, vec2(0.0, 2.0) - pp, dir, len2, lob, clp, nC);

    return min(max4, max(min4, aC / max(aW, 1.0e-8)));
}

vec3 rcas(vec3 b, vec3 d, vec3 e, vec3 f, vec3 h) {
    float bL = b.g + 0.5 * (b.r + b.b);
    float dL = d.g + 0.5 * (d.r + d.b);
    float eL = e.g + 0.5 * (e.r + e.b);
    float fL = f.g + 0.5 * (f.r + f.b);
    float hL = h.g + 0.5 * (h.r + h.b);

    float nz = 0.25 * (bL + dL + fL + hL) - eL;
    float range = max(
        max(max(bL, dL), max(eL, max(fL, hL))) - min(min(bL, dL), min(eL, min(fL, hL))),
        1.0e-6
    );
    nz = clamp(abs(nz) / range, 0.0, 1.0);
    nz = 1.0 - 0.5 * nz;

    vec3 mn4 = min(min(b, d), min(f, h));
    vec3 mx4 = max(max(b, d), max(f, h));
    vec3 hitMin = min(mn4, e) / (4.0 * mx4 + 1.0e-5);
    vec3 hitMax = (1.0 - max(mx4, e)) / (4.0 * mn4 - 4.0 - 1.0e-5);
    vec3 lobeRGB = max(-hitMin, hitMax);
    float lobe = max(-RCAS_LIMIT, min(max(lobeRGB.r, max(lobeRGB.g, lobeRGB.b)), 0.0));
    lobe *= exp2(-RCAS_SHARPNESS) * nz;
    return (lobe * (b + d + f + h) + e) / (4.0 * lobe + 1.0);
}

void main() {
    vec2 rel = vViewPos.xy - LensCenter;
    float dist = length(rel) / max(LensRadius, 0.0001);
    if (dist > 1.0) {
        discard;
    }

    float blur = BLUR * max(LensEdgeWidth, 0.001);
    float falloff = (1.0 - clamp(dist, 0.0, 1.0)) / blur;
    float edge = exp(-falloff * falloff);

    float ramp = smoothstep(max(1.0 - blur * 3.0, 0.0), 1.0, dist);
    float scale = LENS_SCALE * (1.0 - DISTORTION * ramp) / max(LensMagnification, 0.0001);
    vec3 center = vec3(LensCenter + rel * scale, vViewPos.z);

    float split = ABERRATION * ramp;
    vec2 uvR = screenUv(center - vec3(rel * split, 0.0));
    vec2 uvG = screenUv(center);
    vec2 uvB = screenUv(center + vec3(rel * split, 0.0));

    if (ThermalEnabled != 0) {
        vec3 thermal = scopeThermalColour(Sampler0, Sampler1, uvG, LensTexelSize * scale * LensScreenScale);
        fragColor = vec4(thermal * (1.0 - edge), 1.0);
        return;
    }

    vec3 sampled;
    if (LensMagnification > 1.0001) {
        vec3 eR = easu(uvR);
        vec3 eG = easu(uvG);
        vec3 eB = easu(uvB);
        vec3 e = vec3(eR.r, eG.g, eB.b);
        vec2 stepUv = scale * LensScreenScale * LensTexelSize;
        vec3 b = easu(uvG + vec2(0.0, -stepUv.y));
        vec3 d = easu(uvG + vec2(-stepUv.x, 0.0));
        vec3 f = easu(uvG + vec2(stepUv.x, 0.0));
        vec3 h = easu(uvG + vec2(0.0, stepUv.y));
        sampled = rcas(b, d, e, f, h);
    } else {
        sampled = vec3(texture(Sampler0, uvR).r, texture(Sampler0, uvG).g, texture(Sampler0, uvB).b);
    }

    if (LensHardEdge > 0.0) {
        vec3 voxel = vec3(lensVoxel(uvR).r, lensVoxel(uvG).g, lensVoxel(uvB).b);
        sampled = mix(sampled, voxel, clamp(LensHardEdge, 0.0, 1.0));
    }

    fragColor = vec4(sampled * LensTint * (1.0 - edge), 1.0);
}
