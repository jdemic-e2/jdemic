#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(binding = 0) uniform UniformBufferObject {
    mat4 model;
    mat4 view;
    mat4 proj;
    float time;
} ubo;

// Screen-space overlay (card deck): local positions are already in clip space when screenSpace != 0.
layout(push_constant) uniform Push {
    int screenSpace;
    mat4 clipFromLocal;
} pc;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inColor;
layout(location = 2) in vec2 inTexCoord;

layout(location = 0) out vec3 fragColor;
layout(location = 1) out vec2 fragTexCoord;
layout(location = 2) out float fragTime;

void main() {
    if (pc.screenSpace != 0) {
        gl_Position = pc.clipFromLocal * vec4(inPosition, 1.0);
    } else {
        gl_Position = ubo.proj * ubo.view * ubo.model * vec4(inPosition, 1.0);
    }
    fragColor = inColor;
    fragTexCoord = inTexCoord;
    fragTime = ubo.time;
}
