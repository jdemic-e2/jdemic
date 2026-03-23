#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(binding = 1) uniform sampler2D skydomeTexture;

layout(location = 0) in vec3 fragWorldDir;

layout(location = 0) out vec4 outColor;

const float PI = 3.14159265359;

void main() {
    // Equirectangular mapping: convert direction vector to UV
    vec3 dir = normalize(fragWorldDir);
    float u = atan(dir.z, dir.x) / (2.0 * PI) + 0.5;
    float v = asin(clamp(dir.y, -1.0, 1.0)) / PI + 0.5;

    outColor = texture(skydomeTexture, vec2(u, 1.0 - v));
}
