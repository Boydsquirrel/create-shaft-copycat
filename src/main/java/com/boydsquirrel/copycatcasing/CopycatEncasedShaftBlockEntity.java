package com.boydsquirrel.copycatcasing;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** A kinetic (shaft) block entity that also remembers which block it is "wearing". */
public class CopycatEncasedShaftBlockEntity extends KineticBlockEntity {

	private BlockState material = AllBlocks.COPYCAT_BASE.getDefaultState();
	private ItemStack consumedItem = ItemStack.EMPTY;

	public CopycatEncasedShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public BlockState getMaterial() {
		return material;
	}

	public boolean hasCustomMaterial() {
		return !AllBlocks.COPYCAT_BASE.has(material);
	}

	public void setMaterial(BlockState newMaterial) {
		material = newMaterial;
		if (level != null && !level.isClientSide()) {
			notifyUpdate();
			return;
		}
		redraw();
	}

	public ItemStack getConsumedItem() {
		return consumedItem;
	}

	public void setConsumedItem(ItemStack stack) {
		consumedItem = stack.copyWithCount(1);
		setChanged();
	}

	private void redraw() {
		requestModelDataUpdate();
		if (level != null)
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("Item", consumedItem.saveOptional(registries));
		tag.put("Material", NbtUtils.writeBlockState(material));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		BlockState previous = material;

		consumedItem = ItemStack.parseOptional(registries, tag.getCompound("Item"));

		BlockState loaded = null;
		if (tag.contains("Material"))
			loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Material"));

		if (loaded == null) {
			material = AllBlocks.COPYCAT_BASE.getDefaultState();
			consumedItem = ItemStack.EMPTY;
		} else if (loaded.isAir()) {
			// The block this was encased in no longer exists (e.g. its mod was removed).
			material = AllBlocks.COPYCAT_BASE.getDefaultState();
		} else {
			material = loaded;
		}

		if (clientPacket && previous != material)
			redraw();
	}

	@Override
	public ModelData getModelData() {
		return ModelData.builder()
			.with(CopycatModel.MATERIAL_PROPERTY, material)
			.build();
	}
}
