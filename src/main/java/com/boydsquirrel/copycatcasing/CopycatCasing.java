package com.boydsquirrel.copycatcasing;

import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(CopycatCasing.MODID)
public class CopycatCasing {
	public static final String MODID = "copycatcasing";
	public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);

	public CopycatCasing(IEventBus modBus) {
		REGISTRATE.registerEventListeners(modBus);
		CCBlocks.register();
		CCBlockEntities.register();
		modBus.addListener(CopycatCasing::addToCreativeTab);
	}

	private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
			event.accept(CCBlocks.COPYCAT_ENCASED_SHAFT.get());
			event.accept(CCBlocks.COPYCAT_ENCASED_SHAFT_SLAB.get());
		}
	}
}
