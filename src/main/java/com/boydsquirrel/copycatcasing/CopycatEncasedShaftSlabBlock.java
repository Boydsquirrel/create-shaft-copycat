package com.boydsquirrel.copycatcasing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Half-height version of the Copycat Encased Shaft. Same block entity and same interactions as the
 * full block; only the placement (bottom/top half) and the shape differ. The shaft still runs through
 * the centre of the block space, so it sticks out of the open half.
 */
public class CopycatEncasedShaftSlabBlock extends CopycatEncasedShaftBlock {

	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

	private static final VoxelShape BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
	private static final VoxelShape TOP_SHAPE = Block.box(0, 8, 0, 16, 16, 16);

	public CopycatEncasedShaftSlabBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(HALF, Half.BOTTOM));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HALF);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;

		// Same rule as vanilla slabs: click the top face -> bottom half, bottom face -> top half,
		// side faces -> whichever half of the block you clicked.
		Direction face = context.getClickedFace();
		BlockPos pos = context.getClickedPos();
		boolean top;
		if (face == Direction.DOWN)
			top = true;
		else if (face == Direction.UP)
			top = false;
		else
			top = context.getClickLocation().y - pos.getY() > 0.5;

		return state.setValue(HALF, top ? Half.TOP : Half.BOTTOM);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HALF) == Half.TOP ? TOP_SHAPE : BOTTOM_SHAPE;
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}
}
