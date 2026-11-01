#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

uniform float Time;

void main() {
    float phase = Time * 6.2831853;
    vec2 shift = vec2(sin(texCoord.y * 22.0 + phase), cos(texCoord.x * 18.0 + phase)) * 0.006;
    fragColor = vec4(texture(DiffuseSampler, texCoord + shift).rgb, 1.0);
}
