package com.boydsquirrel.navlights.blockentity;

import com.boydsquirrel.navlights.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Client-side cache for the beam raycast (not saved, not synced). */
public class LandingLightBlockEntity extends BlockEntity {
    public float beamLength = 35.0F;
    public Direction hitFace = null;
    public float hx, hy, hz;
    public long lastCast = -1000L;

    public LandingLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANDING_LIGHT.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(40.0);
    }
}
