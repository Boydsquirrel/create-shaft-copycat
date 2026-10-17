package com.boydsquirrel.navlights.client;

import com.boydsquirrel.navlights.NavLightsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class RenderUtil {
    private RenderUtil() {}

    public static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(NavLightsMod.MODID, "textures/effect/glow.png");
    public static final ResourceLocation BEAM = ResourceLocation.fromNamespaceAndPath(NavLightsMod.MODID, "textures/effect/beam.png");

    public record P(float x, float y, float z, float u, float v) {}

    private static void emit(VertexConsumer vc, PoseStack.Pose pose, P a, P b, P c, P d, int r, int g, int bl, int al) {
        vertex(vc, pose, a, r, g, bl, al);
        vertex(vc, pose, b, r, g, bl, al);
        vertex(vc, pose, c, r, g, bl, al);
        vertex(vc, pose, d, r, g, bl, al);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, P p, int r, int g, int b, int a) {
        vc.addVertex(pose, p.x(), p.y(), p.z())
                .setColor(r, g, b, a)
                .setUv(p.u(), p.v())
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /** Draws a quad visible from both sides (the render type culls back faces, so we emit both windings). */
    public static void quad2(VertexConsumer vc, PoseStack.Pose pose, P a, P b, P c, P d, int r, int g, int bl, int al) {
        emit(vc, pose, a, b, c, d, r, g, bl, al);
        emit(vc, pose, d, c, b, a, r, g, bl, al);
    }

    /** Camera-facing sprite, nudged toward the camera so the lamp model does not hide it. */
    public static void billboard(PoseStack ps, VertexConsumer vc, float cx, float cy, float cz, float size, float towardCamera, int r, int g, int b, int a) {
        ps.pushPose();
        ps.translate(cx, cy, cz);
        ps.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        ps.translate(0.0F, 0.0F, towardCamera);
        PoseStack.Pose pose = ps.last();
        float h = size * 0.5F;
        quad2(vc, pose,
                new P(-h, -h, 0, 0, 1),
                new P(h, -h, 0, 1, 1),
                new P(h, h, 0, 1, 0),
                new P(-h, h, 0, 0, 0),
                r, g, b, a);
        ps.popPose();
    }

    /** Camera-relative position of a local point (the pose stack origin is the block corner, relative to the camera). */
    public static Vector3f toCameraSpace(PoseStack ps, float x, float y, float z) {
        Matrix4f m = ps.last().pose();
        Vector3f v = new Vector3f(x, y, z);
        m.transformPosition(v);
        return v;
    }
}
