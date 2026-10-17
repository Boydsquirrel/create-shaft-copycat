package com.boydsquirrel.navlights.blockentity;

import com.boydsquirrel.navlights.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Holds no data; exists so the client renderer (glow + strobe) can attach to the block. */
public class NavLightBlockEntity extends BlockEntity {
    public NavLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NAV_LIGHT.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(12.0);
    }
}
