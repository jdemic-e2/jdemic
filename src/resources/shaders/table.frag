#version 450
#extension GL_ARB_separate_shader_objects : enable

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

        outColor = vec4(col, 1.0);
    }
    else
    {
        outColor = texture(texSampler, fragTexCoord);
    }
}