#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(binding = 0) uniform UniformBufferObject {
    mat4 model;
    mat4 view;
    mat4 proj;
    float time;
    float hoverU;
    float hoverV;
    float hoverRadiusU;
    float hoverRadiusV;
    float hoverColorR;
    float hoverColorG;
    float hoverColorB;
    float hoverEdgeCount;
    vec4 hoverEdge0;
    vec4 hoverEdge1;
    vec4 hoverEdge2;
    vec4 hoverEdge3;
    vec4 hoverEdge4;
    vec4 hoverEdge5;
    vec4 hoverEdge6;
    vec4 hoverEdge7;
} ubo;

layout(binding = 1) uniform sampler2D texSampler;

layout(location = 0) in vec3 fragColor;
layout(location = 1) in vec2 fragTexCoord;
layout(location = 2) in float fragTime;

layout(location = 0) out vec4 outColor;

// Compute perpendicular distance from point p to line segment a->b, and parametric t along segment
float distToSegment(vec2 p, vec2 a, vec2 b, out float t)
{
    vec2 ab = b - a;
    float lenSq = dot(ab, ab);
    if (lenSq < 1e-8)
    {
        t = 0.0;
        return length(p - a);
    }
    t = clamp(dot(p - a, ab) / lenSq, 0.0, 1.0);
    vec2 proj = a + t * ab;
    return length(p - proj);
}

vec4 getHoverEdge(int idx)
{
    if (idx == 0) return ubo.hoverEdge0;
    if (idx == 1) return ubo.hoverEdge1;
    if (idx == 2) return ubo.hoverEdge2;
    if (idx == 3) return ubo.hoverEdge3;
    if (idx == 4) return ubo.hoverEdge4;
    if (idx == 5) return ubo.hoverEdge5;
    if (idx == 6) return ubo.hoverEdge6;
    return ubo.hoverEdge7;
}

void main() {
    // Detect map panel by vertex color (map = white, wood = brown)
    bool isMapPanel = fragColor.b > 0.9;

    if(isMapPanel)
    {
        float border = 0.025;

        // Animated cross pattern moving diagonally
        float tileScale = 12.0;
        float speed = 0.04;
        float offsetX = fragTime * speed;
        float offsetY = fragTime * speed;
        float cellX = fract((fragTexCoord.x + offsetX) * tileScale);
        float cellY = fract((fragTexCoord.y + offsetY) * tileScale);
        // Center cell coords to -0.5..0.5
        float cx = cellX - 0.5;
        float cy = cellY - 0.5;
        // Cross shape: thin horizontal or vertical bar through center
        float armWidth = 0.01;
        float cross = (abs(cx) < armWidth) || (abs(cy) < armWidth) ? 1.0 : 0.0;
        vec3 dark  = vec3(0.071, 0.118, 0.204);
        vec3 light = vec3(0.149, 0.227, 0.361);
        vec3 col = mix(dark, light, cross);

        // Composite the map texture on top using its alpha
        vec4 mapTex = texture(texSampler, fragTexCoord);
        col = mix(col, mapTex.rgb, mapTex.a);

        // Draw hover circle overlay when hoverRadiusU > 0
        if(ubo.hoverRadiusU > 0.0)
        {
            float du = (fragTexCoord.x - ubo.hoverU) / ubo.hoverRadiusU;
            float dv = (fragTexCoord.y - ubo.hoverV) / ubo.hoverRadiusV;
            float dist = du * du + dv * dv;

            if(dist < 1.0)
            {
                vec3 hoverCol = vec3(ubo.hoverColorR, ubo.hoverColorG, ubo.hoverColorB);
                float alpha = smoothstep(1.0, 0.85, dist);
                col = mix(col, hoverCol, alpha * 0.9);
            }

            // Dark outline ring
            float outlineOuter = 1.05;
            float outlineInner = 0.90;
            if(dist < outlineOuter && dist > outlineInner)
            {
                float ring = smoothstep(outlineOuter, outlineOuter - 0.05, dist)
                           * smoothstep(outlineInner, outlineInner + 0.05, dist);
                col = mix(col, vec3(0.04, 0.04, 0.04), ring * 0.86);
            }

            // Animated dashed edges for hovered city
            int edgeCount = int(ubo.hoverEdgeCount);
            vec3 dashColor = vec3(0.3, 0.65, 1.0);
            vec3 dashOutlineColor = vec3(0.05, 0.15, 0.35);
            float dashSpacing = 0.025;
            float dashSpeed = 3.5;
            float fixedLineHalfWidth = 0.003;
            float fixedOutlineHalfWidth = 0.0045;

            for (int i = 0; i < 8; i++)
            {
                if (i >= edgeCount) break;
                vec4 edge = getHoverEdge(i);
                vec2 a = edge.xy;
                vec2 b = edge.zw;

                float segLen = length(b - a);
                if (segLen < 1e-6) continue;

                float t;
                float d = distToSegment(fragTexCoord, a, b, t);

                // Dash pattern based on absolute UV distance along segment
                float distAlongSeg = t * segLen;
                float dashPhase = fract(distAlongSeg / dashSpacing - ubo.time * dashSpeed);
                float dash = smoothstep(0.35, 0.45, dashPhase) * smoothstep(0.95, 0.85, dashPhase);

                // Dark outline
                float outlineAlpha = smoothstep(fixedOutlineHalfWidth, fixedOutlineHalfWidth * 0.6, d);
                col = mix(col, dashOutlineColor, outlineAlpha * dash * 0.7);

                // Bright dash line
                float lineAlpha = smoothstep(fixedLineHalfWidth, fixedLineHalfWidth * 0.5, d);
                col = mix(col, dashColor, lineAlpha * dash * 0.9);
            }
        }

        outColor = vec4(col, 1.0);
    }
    else
    {
        outColor = texture(texSampler, fragTexCoord);
    }
}