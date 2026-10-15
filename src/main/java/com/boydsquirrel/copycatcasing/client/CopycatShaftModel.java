package com.boydsquirrel.copycatcasing.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the stored material around the shaft.
 * <ul>
 * <li>Full block: the material's cube, with a 4x4px opening on the two faces the shaft passes through.</li>
 * <li>Slab (state has a HALF property): the same, cropped to the bottom or top half.</li>
 * </ul>
 * The shaft itself is drawn separately by the kinetic block entity visual/renderer, exactly like Create's
 * own encased shafts.
 */
public class CopycatShaftModel extends CopycatModel {

	private static final AABB FULL = new AABB(0, 0, 0, 1, 1, 1);
	private static final AABB BOTTOM_HALF = new AABB(0, 0, 0, 1, .5, 1);
	private static final AABB TOP_HALF = new AABB(0, .5, 0, 1, 1, 1);

	/** The shaft is 4x4 pixels wide, centred: from 6px to 10px. */
	private static final double HOLE_MIN = 6 / 16d;
	private static final double HOLE_MAX = 10 / 16d;

	public CopycatShaftModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	protected List<BakedQuad> getCroppedQuads(BlockState state, Direction side, RandomSource rand,
		BlockState material, ModelData wrappedData, RenderType renderType) {
		BakedModel model = getModelOf(material);

		Direction.Axis shaftAxis = state.getOptionalValue(BlockStateProperties.AXIS)
			.orElse(null);
		Half half = state.getOptionalValue(BlockStateProperties.HALF)
			.orElse(null);

		AABB limit = FULL;
		Direction openFace = null;
		if (half != null) {
			boolean top = half == Half.TOP;
			limit = top ? TOP_HALF : BOTTOM_HALF;
			openFace = top ? Direction.DOWN : Direction.UP;
		}

		// A slab's open face sits in the middle of the block space, so a solid neighbour above/below must
		// not cull it. It is never returned for the culled side; it is added to the unculled list instead.
		if (side != null && side == openFace)
			return Collections.emptyList();

		List<BakedQuad> quads = new ArrayList<>();
		addCropped(quads, model.getQuads(material, side, rand, wrappedData, renderType), limit, shaftAxis);
		if (side == null && openFace != null)
			addCropped(quads, model.getQuads(material, openFace, rand, wrappedData, renderType), limit, shaftAxis);
		return quads;
	}

	private static void addCropped(List<BakedQuad> out, List<BakedQuad> templates, AABB limit,
		Direction.Axis shaftAxis) {
		for (BakedQuad quad : templates) {
			boolean isEndFace = shaftAxis != null && quad.getDirection()
				.getAxis() == shaftAxis;

			if (isEndFace) {
				// Cut the opening for the shaft: four strips around it.
				for (AABB strip : endFaceStrips(shaftAxis, limit))
					out.add(crop(quad, strip));
			} else if (limit != FULL) {
				out.add(crop(quad, limit));
			} else {
				out.add(quad);
			}
		}
	}

	private static BakedQuad crop(BakedQuad quad, AABB box) {
		return BakedQuadHelper.cloneWithCustomGeometry(quad,
			BakedModelHelper.cropAndMove(quad.getVertices(), quad.getSprite(), box, Vec3.ZERO));
	}

	/**
	 * The four boxes that tile a face perpendicular to the shaft axis, leaving the shaft's 4x4 opening empty,
	 * each clipped to {@code limit}. Boxes that end up empty (e.g. cut away by a slab) are dropped.
	 */
	private static List<AABB> endFaceStrips(Direction.Axis axis, AABB limit) {
		int a = axis.choose(0, 1, 2);
		int u = (a + 1) % 3;
		int v = (a + 2) % 3;

		// {u min, u max, v min, v max}
		double[][] regions = {
			{0, HOLE_MIN, 0, 1},
			{HOLE_MAX, 1, 0, 1},
			{HOLE_MIN, HOLE_MAX, 0, HOLE_MIN},
			{HOLE_MIN, HOLE_MAX, HOLE_MAX, 1}
		};

		List<AABB> strips = new ArrayList<>(4);
		for (double[] r : regions) {
			double[] min = {0, 0, 0};
			double[] max = {1, 1, 1};
			min[u] = r[0];
			max[u] = r[1];
			min[v] = r[2];
			max[v] = r[3];

			AABB box = new AABB(min[0], min[1], min[2], max[0], max[1], max[2]).intersect(limit);
			if (box.getXsize() > 1e-6 && box.getYsize() > 1e-6 && box.getZsize() > 1e-6)
				strips.add(box);
		}
		return strips;
	}
}
