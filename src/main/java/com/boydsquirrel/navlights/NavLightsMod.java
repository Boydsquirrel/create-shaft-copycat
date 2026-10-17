package com.boydsquirrel.navlights;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(NavLightsMod.MODID)
public class NavLightsMod {
    public static final String MODID = "navlights";

    public NavLightsMod(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModTabs.TABS.register(modBus);
    }
}
