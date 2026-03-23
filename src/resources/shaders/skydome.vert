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

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inColor;
layout(location = 2) in vec2 inTexCoord;

layout(location = 0) out vec3 fragWorldDir;

void main() {
    // Center the skydome on the table (table center is at 0, 0.1, 0)
    vec3 tableCenter = vec3(0.0, 0.1, 0.0);
    vec3 worldPos = inPosition * 50.0 + tableCenter;
    vec4 pos = ubo.proj * ubo.view * vec4(worldPos, 1.0);
    // Set z = w so depth is always max (furthest away)
    gl_Position = pos.xyww;
    fragWorldDir = inPosition;
}
