package com.boydsquirrel.dualwheelmount.client;

import org.joml.Vector2d;

import com.boydsquirrel.dualwheelmount.DualWheelMountBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.ryanhcode.offroad.index.OffroadPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the vertical drive shaft plus one suspension arm, spring and tire per side. The arm/spring/tire drawing is
 * adapted from Offroad's WheelMountRenderer (MIT licensed, (c) The Simulated Team / The Creators of Aeronautics). It
 * references Offroad's own models at runtime; none of its art is copied into this mod.
 */
public class DualWheelMountRenderer extends KineticBlockEntityRenderer<DualWheelMountBlockEntity> {

	/**
	 * How far each wheel's centre is drawn from the centre of the mount block, in blocks (Offroad's own mount uses
	 * 22/16). The physics treats the wheel as 1.0 away, so smaller values sit closer to what actually touches the
	 * ground. Only the visuals change; the arm and spring geometry adapts automatically.
	 */
	private static final double WHEEL_DISTANCE = 18.0 / 16.0;

	public DualWheelMountRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(DualWheelMountBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// the rotating drive shaft
		BlockState state = getRenderedBlockState(be);
		RenderType type = getRenderType(be, state);
		renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(type), light);

		for (int i = 0; i < 2; i++)
			renderWheel(be, i, partialTicks, ms, buffer, light, overlay);
	}

	private void renderWheel(DualWheelMountBlockEntity be, int index, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

		// Same convention as Offroad's mount: its model is drawn "backwards" relative to the wheel direction.
		Direction direction = be.getWheelDirection(index)
			.getOpposite();
		BlockState blockState = be.getBlockState();

		SuperByteBuffer teleOuter = CachedBuffers.partial(OffroadPartialModels.TELE_OUTER, blockState);
		SuperByteBuffer teleInner = CachedBuffers.partial(OffroadPartialModels.TELE_INNER, blockState);
		SuperByteBuffer teleMount = CachedBuffers.partial(OffroadPartialModels.TELE_MOUNT, blockState);
		SuperByteBuffer springTop = CachedBuffers.partial(OffroadPartialModels.SPRING_UPPER, blockState);
		SuperByteBuffer springBottom = CachedBuffers.partial(OffroadPartialModels.SPRING_LOWER, blockState);
		SuperByteBuffer springMiddle = CachedBuffers.partial(OffroadPartialModels.SPRING_MIDDLE, blockState);

		double wheelPivotOffsetHor = 10.0 / 16.0;
		double springWheelPivotOffsetHor = 12.0 / 16.0;
		double springWheelPivotOffsetVer = -2.0 / 16.0;

		double horizontalWheelPosition = WHEEL_DISTANCE;
		double verticalWheelPosition = -be.getLerpedExtension(index, partialTicks);

		double teleMountHor = 0.0 / 16.0;
		double teleMountVer = -6.0 / 16.0;

		double springMountHor = 7.0 / 16.0;
		double springMountVer = 7.0 / 16.0;

		double teleAngle = Math.atan2(verticalWheelPosition - teleMountVer,
			horizontalWheelPosition - wheelPivotOffsetHor - teleMountHor);
		double teleDistance = new Vector2d(verticalWheelPosition - teleMountVer,
			horizontalWheelPosition - wheelPivotOffsetHor - teleMountHor).length();

		double springAngle = Math.atan2(verticalWheelPosition - springWheelPivotOffsetVer - springMountVer,
			horizontalWheelPosition - springWheelPivotOffsetHor - springMountHor);
		double springDistance = new Vector2d(verticalWheelPosition - springWheelPivotOffsetVer - springMountVer,
			horizontalWheelPosition - springWheelPivotOffsetHor - springMountHor).length();

		ms.pushPose();
		TransformStack.of(ms)
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(direction))
			.rotateXDegrees(AngleHelper.verticalAngle(direction))
			.uncenter();

		// wheel & telescope
		ms.pushPose();
		ms.pushPose();
		ms.translate(0.0, -6.0 / 16.0, 0.0);
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.XP.rotation((float) teleAngle));
		ms.translate(-0.5, -0.5, -0.5);
		teleOuter.light(light)
			.renderInto(ms, vb);
		ms.translate(0.0, 0.0, -(teleDistance - 1.0));
		teleInner.light(light)
			.renderInto(ms, vb);
		ms.popPose();

		ms.pushPose();
		ms.translate(0.0, verticalWheelPosition, 26.0 / 16.0 - horizontalWheelPosition);

		ms.translate(0.5, 0.5, 0.5);
		// Steering is shared: both wheels yaw the same way around the vertical axis.
		ms.rotateAround(Axis.YP.rotation((float) be.getLerpedYaw(partialTicks)), 0.0F,
			0.0F, (float) (-horizontalWheelPosition + 6.0 / 16.0));
		ms.translate(-0.5, -0.5, -0.5);

		teleMount.light(light)
			.renderInto(ms, vb);

		ms.translate(0.5, 0.5, 0.5);
		ms.translate(0.0, 0.0, -26.0 / 16.0f);

		double signMultiplier = -be.getLerpedAngle(index, partialTicks)
			* (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 : -1.0)
			* (direction.getAxis() == Direction.Axis.X ? 1.0 : -1.0);

		ms.mulPose(Axis.ZP.rotation((float) signMultiplier));

		ItemStack itemStack = be.getWheel(index).tire;
		TireLike tireLike = itemStack.get(OffroadDataComponents.TIRE);
		if (tireLike != null) {
			Vec3 rotation = tireLike.rotation();
			ms.mulPose(Axis.XP.rotation((float) Math.toRadians(rotation.x)));
			ms.mulPose(Axis.YP.rotation((float) Math.toRadians(rotation.y)));
			ms.mulPose(Axis.ZP.rotation((float) Math.toRadians(rotation.z)));

			if (tireLike.model()
				.isPresent()) {
				ResourceLocation model = tireLike.model()
					.get();
				ms.translate(tireLike.offset().x, tireLike.offset().y, tireLike.offset().z);
				SuperByteBuffer wheel = CachedBuffers.partial(PartialModel.of(model), blockState);
				wheel.light(light)
					.translate(-0.5f, 0.0f, -0.5f)
					.renderInto(ms, vb);
			} else {
				ms.translate(tireLike.offset().x, tireLike.offset().y, tireLike.offset().z);
				Minecraft.getInstance()
					.getItemRenderer()
					.renderStatic(itemStack, ItemDisplayContext.NONE, light, overlay, ms, buffer, be.getLevel(), 0);
			}
		}

		ms.popPose();
		ms.popPose();

		// spring
		ms.pushPose();
		ms.translate(0.5, 0.5 + springMountVer, 0.5 - springMountHor);
		ms.mulPose(Axis.XP.rotation((float) springAngle + Mth.PI / 2.0f));
		ms.translate(-0.5, -0.5 - springMountVer, -0.5 + springMountHor);

		float springExtension = (float) springDistance;
		// never let the middle section scale negative (it would draw inside out) when fully compressed
		float springSpan = Math.max(springExtension - 4.0f / 16.0f, 0.02f);

		springTop.light(light)
			.renderInto(ms, vb);
		springMiddle.light(light)
			.translate(0.0f, 13.0f / 16.0f, 0.0f)
			.scale(1.0f, springSpan / (14.0f / 16.0f), 1.0f)
			.translateBack(0.0f, 13.0f / 16.0f, 0.0f)
			.renderInto(ms, vb);
		springBottom.light(light)
			.translate(0.0, -(springSpan + -14.0 / 16.0), 0.0)
			.renderInto(ms, vb);
		ms.popPose();

		ms.popPose();
	}

	@Override
	public int getViewDistance() {
		return 512;
	}
}
