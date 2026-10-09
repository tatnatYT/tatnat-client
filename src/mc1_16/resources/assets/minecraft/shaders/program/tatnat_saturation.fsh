#version 110

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;
uniform float Saturation;


void main() {
    vec4 c = texture2D(DiffuseSampler, texCoord);
    float grey = dot(c.rgb, vec3(0.299, 0.587, 0.114));
    gl_FragColor = vec4(clamp(mix(vec3(grey), c.rgb, Saturation), 0.0, 1.0), 1.0);
}
