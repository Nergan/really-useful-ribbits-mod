#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    fragColor = vec4(texture(DiffuseSampler, vec2(texCoord.x, 1.0 - texCoord.y)).rgb, 1.0);
}
