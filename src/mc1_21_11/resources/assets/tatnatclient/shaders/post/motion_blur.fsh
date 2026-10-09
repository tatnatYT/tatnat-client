#version 330

uniform sampler2D InSampler;
uniform sampler2D PrevSampler;

layout(std140) uniform MotionBlurConfig {
    float BlendFactor;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 curr = texture(InSampler, texCoord);
    vec4 prev = texture(PrevSampler, texCoord);
    fragColor = vec4(mix(curr.rgb, prev.rgb, BlendFactor), 1.0);
}
