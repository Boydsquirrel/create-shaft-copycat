package com.boydsquirrel.dualwheelmount;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.material.MapColor;

public class DWMBlocks {
	private static final CreateRegistrate REGISTRATE = DualWheelMount.REGISTRATE;

	public static final BlockEntry<DualWheelMountBlock> DUAL_WHEEL_MOUNT =
		REGISTRATE.block("dual_wheel_mount", DualWheelMountBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.mapColor(MapColor.METAL)
				.noOcclusion()) // otherwise the light at the block's own position is 0 and everything it draws is black
			.transform(TagGen.axeOrPickaxe())
			.item()
			.build()
			.register();

	public static void register() {
	}
}
