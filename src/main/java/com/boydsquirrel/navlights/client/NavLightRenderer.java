package com.boydsquirrel.navlights.client;

import com.boydsquirrel.navlights.ColorUtil;
import com.boydsquirrel.navlights.block.NavLightBlock;
import com.boydsquirrel.navlights.blockentity.NavLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class NavLightRenderer implements BlockEntityRenderer<NavLightBlockEntity> {
    public NavLightRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void render(NavLightBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!state.getValue(NavLightBlock.POWERED)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        if (state.getValue(NavLightBlock.STROBE)) {
            // 40 tick cycle: two quick flashes, like an aircraft anti-collision strobe
            float t = (float) (mc.level.getGameTime() % 40L) + partialTick;
            boolean on = t < 2.0F || (t >= 5.0F && t < 7.0F);
            if (!on) {
                return;
            }
        }

        Direction dir = state.getValue(NavLightBlock.FACING);
        float cx = 0.5F - dir.getStepX() * 0.21875F;
        float cy = 0.5F - dir.getStepY() * 0.21875F;
        float cz = 0.5F - dir.getStepZ() * 0.21875F;

        int[] rgb = ColorUtil.glow(state.getValue(NavLightBlock.COLOR));
        Vector3f p = RenderUtil.toCameraSpace(ps, cx, cy, cz);
        float dist = p.length();

        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(RenderUtil.GLOW));

        // big soft halo that grows with distance so it stays visible from far away
        float halo = Math.min(1.4F + dist * 0.035F, 16.0F);
        RenderUtil.billboard(ps, vc, cx, cy, cz, halo, 0.45F, rgb[0], rgb[1], rgb[2], 235);

        // small hot core
        float core = Math.min(0.55F + dist * 0.012F, 4.0F);
        RenderUtil.billboard(ps, vc, cx, cy, cz, core, 0.46F, 255, 255, 255, 255);
    }
}
