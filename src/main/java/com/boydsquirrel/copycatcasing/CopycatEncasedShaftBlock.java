package com.boydsquirrel.copycatcasing;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A shaft encased in any full block, chosen by right-clicking with that block (like a copycat).
 * Sneak + wrench turns it back into a plain shaft and returns the block.
 */
public class CopycatEncasedShaftBlock extends AbstractEncasedShaftBlock
	implements IBE<CopycatEncasedShaftBlockEntity> {

	public CopycatEncasedShaftBlock(Properties properties) {
		super(properties);
	}

	/** Which blocks may be used as the "casing": full cubes with no block entity. */
	@Nullable
	private static BlockState getAcceptedMaterial(Level level, BlockPos pos, ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem))
			return null;
		Block block = blockItem.getBlock();
		if (block instanceof EntityBlock || block instanceof CopycatEncasedShaftBlock)
			return null;
		BlockState state = block.defaultBlockState();
		if (!state.isCollisionShapeFullBlock(level, pos))
			return null;
		return state;
	}

	/** The block this casing is currently wearing, or null if none has been applied yet. */
	@Nullable
	private static BlockState getMaterialOrNull(BlockGetter level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof CopycatEncasedShaftBlockEntity be && be.hasCustomMaterial())
			return be.getMaterial();
		return null;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hitResult) {
		BlockState material = getAcceptedMaterial(level, pos, stack);
		if (material == null)
			return super.useItemOn(stack, state, level, pos, player, hand, hitResult);

		return onBlockEntityUseItemOn(level, pos, be -> {
			if (be.hasCustomMaterial())
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (level.isClientSide())
				return ItemInteractionResult.SUCCESS;

			// Remember the item first so the client sync that setMaterial triggers already contains it.
			be.setConsumedItem(stack);
			be.setMaterial(material);
			level.playSound(null, pos, material.getSoundType()
				.getPlaceSound(), SoundSource.BLOCKS, 1, .75f);

			if (!player.isCreative())
				stack.shrink(1);
			return ItemInteractionResult.SUCCESS;
		});
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		if (level.getBlockEntity(pos) instanceof CopycatEncasedShaftBlockEntity be && be.hasCustomMaterial()) {
			ItemStack item = be.getConsumedItem()
				.copy();
			// Clear it so onRemove (which fires when the block is swapped) doesn't drop it a second time.
			be.setConsumedItem(ItemStack.EMPTY);

			Player player = context.getPlayer();
			if (player == null)
				Block.popResource(level, pos, item);
			else if (!player.isCreative())
				player.getInventory()
					.placeItemBackInInventory(item);
		}

		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
		KineticBlockEntity.switchToBlockState(level, pos, AllBlocks.SHAFT.getDefaultState()
			.setValue(AXIS, state.getValue(AXIS)));
		return InteractionResult.SUCCESS;
	}

	// --- Giving the encasing block back -------------------------------------------------------------

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		// Creative players don't get items back (and shouldn't dupe blocks).
		if (player.isCreative() && level.getBlockEntity(pos) instanceof CopycatEncasedShaftBlockEntity be)
			be.setConsumedItem(ItemStack.EMPTY);
		return super.playerWillDestroy(level, pos, state, player);
	}

	/**
	 * Drops the encasing block however this block goes away (player, explosion, machine, ...), the same
	 * way Create's own copycats do. Not done when a piston or contraption is just moving the block.
	 */
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!isMoving && state.getBlock() != newState.getBlock()
			&& level.getBlockEntity(pos) instanceof CopycatEncasedShaftBlockEntity be)
			Block.popResource(level, pos, be.getConsumedItem()
				.copy());
		super.onRemove(state, level, pos, newState, isMoving);
	}

	// --- Behave like the block we're wearing ----------------------------------------------------------

	@Override
	public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
		BlockState material = getMaterialOrNull(level, pos);
		return material != null ? material.getSoundType() : super.getSoundType(state, level, pos, entity);
	}

	@Override
	public float getFriction(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
		BlockState material = getMaterialOrNull(level, pos);
		return material != null ? material.getFriction(level, pos, entity)
			: super.getFriction(state, level, pos, entity);
	}

	@Override
	public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
		BlockState material = getMaterialOrNull(level, pos);
		return material != null ? material.getExplosionResistance(level, pos, explosion)
			: super.getExplosionResistance(state, level, pos, explosion);
	}

	@Override
	public Class<CopycatEncasedShaftBlockEntity> getBlockEntityClass() {
		return CopycatEncasedShaftBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CopycatEncasedShaftBlockEntity> getBlockEntityType() {
		return CCBlockEntities.COPYCAT_ENCASED_SHAFT.get();
	}
}
