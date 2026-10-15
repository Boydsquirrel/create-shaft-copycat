package com.boydsquirrel.copycatcasing.client;

import org.jetbrains.annotations.Nullable;

import com.boydsquirrel.copycatcasing.CopycatEncasedShaftBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.state.BlockState;

/** Lets grass, leaves, water-tinted blocks etc. keep their biome colour when used as a casing. */
public class CopycatShaftColor implements BlockColor {

	public static BlockColor create() {
		return new CopycatShaftColor();
	}

	@Override
	public int getColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
		if (level == null || pos == null)
			return GrassColor.get(0.5D, 1.0D);
		if (level.getBlockEntity(pos) instanceof CopycatEncasedShaftBlockEntity be && be.hasCustomMaterial())
			return Minecraft.getInstance()
				.getBlockColors()
				.getColor(be.getMaterial(), level, pos, tintIndex);
		return -1;
	}
}
