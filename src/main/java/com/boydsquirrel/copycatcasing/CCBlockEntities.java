package com.boydsquirrel.copycatcasing;

import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class CCBlockEntities {
	private static final com.simibubi.create.foundation.data.CreateRegistrate REGISTRATE = CopycatCasing.REGISTRATE;

	public static final BlockEntityEntry<CopycatEncasedShaftBlockEntity> COPYCAT_ENCASED_SHAFT = REGISTRATE
		.blockEntity("copycat_encased_shaft", CopycatEncasedShaftBlockEntity::new)
		.visual(() -> SingleAxisRotatingVisual::shaft, false)
		.validBlocks(CCBlocks.COPYCAT_ENCASED_SHAFT, CCBlocks.COPYCAT_ENCASED_SHAFT_SLAB)
		.renderer(() -> ShaftRenderer::new)
		.register();

	public static void register() {
	}
}
