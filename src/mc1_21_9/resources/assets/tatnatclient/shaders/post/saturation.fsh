#version 330

uniform sampler2D InSampler;

layout(std140) uniform SaturationConfig {
    float Saturation;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 c = texture(InSampler, texCoord);
    float grey = dot(c.rgb, vec3(0.299, 0.587, 0.114));
    fragColor = vec4(clamp(mix(vec3(grey), c.rgb, Saturation), 0.0, 1.0), 1.0);
}
