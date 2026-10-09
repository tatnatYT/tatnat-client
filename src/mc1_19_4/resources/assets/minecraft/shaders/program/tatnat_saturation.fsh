#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;
uniform float Saturation;

out vec4 fragColor;

void main() {
    vec4 c = texture(DiffuseSampler, texCoord);
    float grey = dot(c.rgb, vec3(0.299, 0.587, 0.114));
    fragColor = vec4(clamp(mix(vec3(grey), c.rgb, Saturation), 0.0, 1.0), 1.0);
}
