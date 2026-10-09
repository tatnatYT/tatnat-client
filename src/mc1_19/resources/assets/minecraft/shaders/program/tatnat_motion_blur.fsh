#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;
uniform float BlendFactor;

out vec4 fragColor;

void main() {
    vec4 curr = texture(DiffuseSampler, texCoord);
    vec4 prev = texture(PrevSampler, texCoord);
    fragColor = vec4(mix(curr.rgb, prev.rgb, BlendFactor), 1.0);
}
