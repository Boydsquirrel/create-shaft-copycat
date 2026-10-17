package com.boydsquirrel.navlights;

import com.boydsquirrel.navlights.blockentity.LandingLightBlockEntity;
import com.boydsquirrel.navlights.blockentity.NavLightBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, NavLightsMod.MODID);

    public static final Supplier<BlockEntityType<NavLightBlockEntity>> NAV_LIGHT =
            BLOCK_ENTITIES.register("nav_light",
                    () -> BlockEntityType.Builder.of(NavLightBlockEntity::new, ModBlocks.NAV_LIGHT.get()).build(null));

    public static final Supplier<BlockEntityType<LandingLightBlockEntity>> LANDING_LIGHT =
            BLOCK_ENTITIES.register("landing_light",
                    () -> BlockEntityType.Builder.of(LandingLightBlockEntity::new, ModBlocks.LANDING_LIGHT.get()).build(null));
}
