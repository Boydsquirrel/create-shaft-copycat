package com.boydsquirrel.copycatcasing;

import com.boydsquirrel.copycatcasing.client.CopycatShaftColor;
import com.boydsquirrel.copycatcasing.client.CopycatShaftModel;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.MapColor;

public class CCBlocks {
	private static final CreateRegistrate REGISTRATE = CopycatCasing.REGISTRATE;

	public static final BlockEntry<CopycatEncasedShaftBlock> COPYCAT_ENCASED_SHAFT =
		REGISTRATE.block("copycat_encased_shaft", CopycatEncasedShaftBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.noOcclusion()
				.mapColor(MapColor.NONE)
				.isValidSpawn((state, level, pos, type) -> false))
			.addLayer(() -> RenderType::solid)
			.addLayer(() -> RenderType::cutout)
			.addLayer(() -> RenderType::cutoutMipped)
			.addLayer(() -> RenderType::translucent)
			.color(() -> CopycatShaftColor::create)
			.transform(TagGen.axeOrPickaxe())
			.onRegister(CreateRegistrate.blockModel(() -> CopycatShaftModel::new))
			.item()
			.build()
			.register();

	public static final BlockEntry<CopycatEncasedShaftSlabBlock> COPYCAT_ENCASED_SHAFT_SLAB =
		REGISTRATE.block("copycat_encased_shaft_slab", CopycatEncasedShaftSlabBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.noOcclusion()
				.mapColor(MapColor.NONE)
				.isValidSpawn((state, level, pos, type) -> false))
			.addLayer(() -> RenderType::solid)
			.addLayer(() -> RenderType::cutout)
			.addLayer(() -> RenderType::cutoutMipped)
			.addLayer(() -> RenderType::translucent)
			.color(() -> CopycatShaftColor::create)
			.transform(TagGen.axeOrPickaxe())
			.onRegister(CreateRegistrate.blockModel(() -> CopycatShaftModel::new))
			.item()
			.build()
			.register();

	public static void register() {
	}
}
