package com.boydsquirrel.dualwheelmount;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A mount that holds two tires, one on each side (along FACING and the opposite direction). Power comes in through
 * a vertical shaft (top and/or bottom). Each tire behaves like an Offroad wheel mount: suspension, steering from the
 * side redstone signals, and free rolling when the vehicle moves without power.
 */
public class DualWheelMountBlock extends KineticBlock implements IBE<DualWheelMountBlockEntity> {

	/** The direction of the first wheel. The second wheel is on the opposite side. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public DualWheelMountBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// Wheels end up on the player's left and right.
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection()
			.getClockWise());
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == Direction.Axis.Y;
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return Direction.Axis.Y;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hit) {
		Direction face = hit.getDirection();
		Direction facing = state.getValue(FACING);
		boolean onWheelSide = face == facing || face == facing.getOpposite();
		boolean tireOrEmpty = held.isEmpty() || held.has(OffroadDataComponents.TIRE);

		if (!onWheelSide || !tireOrEmpty)
			return super.useItemOn(held, state, level, pos, player, hand, hit);

		boolean first = face == facing;
		return onBlockEntityUseItemOn(level, pos, be -> {
			ItemStack current = be.getTire(first);
			boolean canSwap = held.isEmpty() ? !current.isEmpty() : true;
			if (!canSwap)
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (level.isClientSide())
				return ItemInteractionResult.SUCCESS;
			be.swapTire(first, player, hand);
			return ItemInteractionResult.CONSUME;
		});
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!isMoving && state.getBlock() != newState.getBlock()
			&& level.getBlockEntity(pos) instanceof DualWheelMountBlockEntity be) {
			Direction facing = state.getValue(FACING);
			// Like the Offroad mount: tires drop where they were attached.
			dropTire(level, pos.relative(facing), be.getTire(true));
			dropTire(level, pos.relative(facing.getOpposite()), be.getTire(false));
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	private static void dropTire(Level level, BlockPos at, ItemStack stack) {
		if (!stack.isEmpty())
			Containers.dropItemStack(level, at.getX(), at.getY(), at.getZ(), stack.copy());
	}

	@Override
	public Class<DualWheelMountBlockEntity> getBlockEntityClass() {
		return DualWheelMountBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DualWheelMountBlockEntity> getBlockEntityType() {
		return DWMBlockEntities.DUAL_WHEEL_MOUNT.get();
	}
}
