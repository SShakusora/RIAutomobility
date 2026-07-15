#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec2 UV0;
in vec3 Normal;
in vec4 InstanceModel0;
in vec4 InstanceModel1;
in vec4 InstanceModel2;
in vec4 InstanceModel3;
in vec4 InstanceNormal0;
in vec4 InstanceNormal1;
in vec4 InstanceNormal2;
in vec4 InstanceColor;
in vec4 InstanceOverlayLight;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main() {
    mat4 instanceModel = mat4(InstanceModel0, InstanceModel1, InstanceModel2, InstanceModel3);
    mat3 instanceNormal = mat3(InstanceNormal0.xyz, InstanceNormal1.xyz, InstanceNormal2.xyz);
    vec4 transformedPosition = instanceModel * vec4(Position, 1.0);
    vec3 transformedNormal = normalize(instanceNormal * Normal);
    ivec2 overlayUv = ivec2(InstanceOverlayLight.xy);
    ivec2 lightUv = ivec2(InstanceOverlayLight.zw);

    gl_Position = ProjMat * ModelViewMat * transformedPosition;
    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * transformedPosition.xyz, FogShape);
    vertexColor = minecraft_mix_light(
            Light0_Direction, Light1_Direction, transformedNormal, InstanceColor);
    lightMapColor = texelFetch(Sampler2, lightUv / 16, 0);
    overlayColor = texelFetch(Sampler1, overlayUv, 0);
    texCoord0 = UV0;
}
