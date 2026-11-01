#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    vec2 split = oneTexel * vec2(6.0, 2.0);
    float red = texture(DiffuseSampler, texCoord + split).r;
    float green = texture(DiffuseSampler, texCoord).g;
    float blue = texture(DiffuseSampler, texCoord - split).b;
    fragColor = vec4(red, green, blue, 1.0);
}
