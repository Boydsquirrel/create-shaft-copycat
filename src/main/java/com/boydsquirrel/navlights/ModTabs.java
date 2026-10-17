package com.boydsquirrel.navlights;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NavLightsMod.MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.navlights"))
                    .icon(() -> new ItemStack(ModBlocks.NAV_LIGHT_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModBlocks.NAV_LIGHT_ITEM.get());
                        output.accept(ModBlocks.LANDING_LIGHT_ITEM.get());
                    })
                    .build());
}
