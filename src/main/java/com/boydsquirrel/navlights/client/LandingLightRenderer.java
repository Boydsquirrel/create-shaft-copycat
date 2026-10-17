package com.boydsquirrel.navlights.client;

import com.boydsquirrel.navlights.block.LandingLightBlock;
import com.boydsquirrel.navlights.blockentity.LandingLightBlockEntity;
import com.boydsquirrel.navlights.client.RenderUtil.P;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Draws the lens flare, a soft volumetric cone and a light pool where the beam hits something. */
public class LandingLightRenderer implements BlockEntityRenderer<LandingLightBlockEntity> {
    private static final float LENGTH = 35.0F;
    private static final float R_START = 0.3F;
    private static final float R_END = 6.5F;   // 13 blocks wide at full length
    private static final int SEGMENTS = 14;
    private static final float LENS_OFFSET = -0.0625F; // lens face relative to block center, along facing

    public LandingLightRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 160;
    }

    @Override
    public void render(LandingLightBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!state.getValue(LandingLightBlock.POWERED)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Direction dir = state.getValue(LandingLightBlock.FACING);
        float sx = dir.getStepX();
        float sy = dir.getStepY();
        float sz = dir.getStepZ();

        long now = mc.level.getGameTime();
        if (now - be.lastCast >= 3L || be.lastCast > now) {
            be.lastCast = now;
            castBeam(be, dir, mc.player);
        }
        float len = be.beamLength;

        // beam origin (lens face), local coordinates
        float ox = 0.5F + sx * LENS_OFFSET;
        float oy = 0.5F + sy * LENS_OFFSET;
        float oz = 0.5F + sz * LENS_OFFSET;

        // how directly are we looking into the lamp?
        Vector3f lens = RenderUtil.toCameraSpace(ps, ox, oy, oz);
        float dist = lens.length();
        float facing = 0.0F;
        if (dist > 0.001F) {
            Vector3f f = new Vector3f(sx, sy, sz);
            ps.last().normal().transform(f);
            f.normalize();
            facing = Math.max(0.0F, f.dot(-lens.x / dist, -lens.y / dist, -lens.z / dist));
        }

        // ---- volumetric cone ----
        VertexConsumer beam = buffers.getBuffer(RenderType.eyes(RenderUtil.BEAM));
        float px, py, pz, qx, qy, qz;
        if (dir.getAxis() == Direction.Axis.Y) {
            px = 1; py = 0; pz = 0; qx = 0; qy = 0; qz = 1;
        } else if (dir.getAxis() == Direction.Axis.X) {
            px = 0; py = 1; pz = 0; qx = 0; qy = 0; qz = 1;
        } else {
            px = 1; py = 0; pz = 0; qx = 0; qy = 1; qz = 0;
        }
        float rEnd = R_START + (R_END - R_START) * (len / LENGTH);
        float vEnd = len / LENGTH;
        float t0 = 0.05F;
        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = Math.PI * 2.0 * i / SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / SEGMENTS;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float d0x = px * c0 + qx * s0, d0y = py * c0 + qy * s0, d0z = pz * c0 + qz * s0;
            float d1x = px * c1 + qx * s1, d1y = py * c1 + qy * s1, d1z = pz * c1 + qz * s1;
            float u0 = (float) i / SEGMENTS;
            float u1 = (float) (i + 1) / SEGMENTS;

            P n0 = new P(ox + sx * t0 + d0x * R_START, oy + sy * t0 + d0y * R_START, oz + sz * t0 + d0z * R_START, u0, 0.0F);
            P n1 = new P(ox + sx * t0 + d1x * R_START, oy + sy * t0 + d1y * R_START, oz + sz * t0 + d1z * R_START, u1, 0.0F);
            P f1 = new P(ox + sx * len + d1x * rEnd, oy + sy * len + d1y * rEnd, oz + sz * len + d1z * rEnd, u1, vEnd);
            P f0 = new P(ox + sx * len + d0x * rEnd, oy + sy * len + d0y * rEnd, oz + sz * len + d0z * rEnd, u0, vEnd);
            RenderUtil.quad2(beam, ps.last(), n0, n1, f1, f0, 255, 244, 214, 62);
        }

        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(RenderUtil.GLOW));

        // ---- light pool where the beam lands ----
        Direction hf = be.hitFace;
        if (hf != null) {
            float nx = hf.getStepX();
            float ny = hf.getStepY();
            float nz = hf.getStepZ();
            float ux, uy, uz, vx, vy, vz;
            if (hf.getAxis() == Direction.Axis.Y) {
                ux = 1; uy = 0; uz = 0; vx = 0; vy = 0; vz = 1;
            } else if (hf.getAxis() == Direction.Axis.X) {
                ux = 0; uy = 0; uz = 1; vx = 0; vy = 1; vz = 0;
            } else {
                ux = 1; uy = 0; uz = 0; vx = 0; vy = 1; vz = 0;
            }
            float half = Math.min((R_START + (R_END - R_START) * (len / LENGTH)) * 1.3F, 9.0F);
            float cx = be.hx + nx * 0.03F;
            float cy = be.hy + ny * 0.03F;
            float cz = be.hz + nz * 0.03F;
            int alpha = (int) (170.0F * (1.0F - 0.5F * (len / LENGTH)));
            RenderUtil.quad2(glow, ps.last(),
                    new P(cx - ux * half - vx * half, cy - uy * half - vy * half, cz - uz * half - vz * half, 0, 1),
                    new P(cx + ux * half - vx * half, cy + uy * half - vy * half, cz + uz * half - vz * half, 1, 1),
                    new P(cx + ux * half + vx * half, cy + uy * half + vy * half, cz + uz * half + vz * half, 1, 0),
                    new P(cx - ux * half + vx * half, cy - uy * half + vy * half, cz - uz * half + vz * half, 0, 0),
                    255, 240, 205, alpha);
        }

        // ---- lens flare (brightest when you look straight into it) ----
        float f2 = facing * facing;
        float flare = Math.min((1.0F + dist * 0.04F) * (0.6F + 0.4F * facing), 14.0F);
        RenderUtil.billboard(ps, glow, ox, oy, oz, flare, 0.45F, 255, 244, 214, (int) (70.0F + 185.0F * f2));
        float core = Math.min((0.6F + dist * 0.015F) * (0.5F + 0.5F * facing), 5.0F);
        RenderUtil.billboard(ps, glow, ox, oy, oz, core, 0.46F, 255, 255, 255, (int) (90.0F + 165.0F * f2));
    }

    private static void castBeam(LandingLightBlockEntity be, Direction dir, Player player) {
        be.beamLength = LENGTH;
        be.hitFace = null;
        Level level = be.getLevel();
        if (level == null || player == null) {
            return;
        }
        BlockPos pos = be.getBlockPos();
        double start = LENS_OFFSET + 0.03;
        Vec3 from = new Vec3(
                pos.getX() + 0.5 + dir.getStepX() * start,
                pos.getY() + 0.5 + dir.getStepY() * start,
                pos.getZ() + 0.5 + dir.getStepZ() * start);
        Vec3 to = from.add(dir.getStepX() * (double) LENGTH, dir.getStepY() * (double) LENGTH, dir.getStepZ() * (double) LENGTH);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 loc = hit.getLocation();
            be.beamLength = (float) Math.max(0.5, loc.distanceTo(from));
            be.hitFace = hit.getDirection();
            be.hx = (float) (loc.x - pos.getX());
            be.hy = (float) (loc.y - pos.getY());
            be.hz = (float) (loc.z - pos.getZ());
        }
    }
}
