package com.boydsquirrel.dualwheelmount;

import com.boydsquirrel.dualwheelmount.client.DualWheelMountRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class DWMBlockEntities {
	private static final CreateRegistrate REGISTRATE = DualWheelMount.REGISTRATE;

	public static final BlockEntityEntry<DualWheelMountBlockEntity> DUAL_WHEEL_MOUNT = REGISTRATE
		.blockEntity("dual_wheel_mount", DualWheelMountBlockEntity::new)
		.validBlocks(DWMBlocks.DUAL_WHEEL_MOUNT)
		.renderer(() -> DualWheelMountRenderer::new)
		.register();

	public static void register() {
	}
}
