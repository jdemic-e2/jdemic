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
} ubo;

layout(binding = 1) uniform sampler2D texSampler;

layout(location = 0) in vec3 fragColor;
layout(location = 1) in vec2 fragTexCoord;
layout(location = 2) in float fragTime;

layout(location = 0) out vec4 outColor;

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
        }

        outColor = vec4(col, 1.0);
    }
    else
    {
        outColor = texture(texSampler, fragTexCoord);
    }
}