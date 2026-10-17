package com.boydsquirrel.navlights;

import com.boydsquirrel.navlights.block.LandingLightBlock;
import com.boydsquirrel.navlights.block.NavLightBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NavLightsMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NavLightsMod.MODID);

    public static final DeferredBlock<Block> NAV_LIGHT = BLOCKS.registerBlock("nav_light",
            NavLightBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(BlockStateProperties.POWERED) ? 10 : 0));

    public static final DeferredBlock<Block> LANDING_LIGHT = BLOCKS.registerBlock("landing_light",
            LandingLightBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(BlockStateProperties.POWERED) ? 15 : 0));

    public static final DeferredItem<BlockItem> NAV_LIGHT_ITEM = ITEMS.registerSimpleBlockItem("nav_light", NAV_LIGHT);
    public static final DeferredItem<BlockItem> LANDING_LIGHT_ITEM = ITEMS.registerSimpleBlockItem("landing_light", LANDING_LIGHT);
}
