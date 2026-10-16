package com.boydsquirrel.dualwheelmount;

import com.simibubi.create.foundation.data.CreateRegistrate;

import dev.ryanhcode.sable.platform.SableEventPlatform;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(DualWheelMount.MODID)
public class DualWheelMount {
	public static final String MODID = "dualwheelmount";
	public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);

	public DualWheelMount(IEventBus modBus) {
		REGISTRATE.registerEventListeners(modBus);
		DWMBlocks.register();
		DWMBlockEntities.register();
		modBus.addListener(DualWheelMount::addToCreativeTab);

		// Forces computed during the physics step are applied in one batch afterwards (same pattern Offroad uses).
		SableEventPlatform.INSTANCE.onPhysicsTick((physicsSystem, timeStep) ->
			DualWheelMountBlockEntity.applyAllBatchedForces(physicsSystem.getLevel(), timeStep));
	}

	private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
			event.accept(DWMBlocks.DUAL_WHEEL_MOUNT.get());
	}
}
