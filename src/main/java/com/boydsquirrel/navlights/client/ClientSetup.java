package com.boydsquirrel.navlights.client;

import com.boydsquirrel.navlights.ColorUtil;
import com.boydsquirrel.navlights.ModBlockEntities;
import com.boydsquirrel.navlights.ModBlocks;
import com.boydsquirrel.navlights.NavLightsMod;
import com.boydsquirrel.navlights.block.NavLightBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = NavLightsMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.NAV_LIGHT.get(), NavLightRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LANDING_LIGHT.get(), LandingLightRenderer::new);
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> tintIndex == 0 ? ColorUtil.tint(state.getValue(NavLightBlock.COLOR)) : -1,
                ModBlocks.NAV_LIGHT.get());
    }
}
