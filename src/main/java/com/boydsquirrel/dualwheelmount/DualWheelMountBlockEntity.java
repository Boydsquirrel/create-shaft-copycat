package com.boydsquirrel.dualwheelmount;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Holds two tires and runs the suspension / steering / free-rolling logic once per wheel.
 *
 * <p>The per-wheel physics is adapted from Offroad's WheelMountBlockEntity (MIT licensed, (c) The Simulated Team /
 * The Creators of Aeronautics), generalised so one block entity drives a wheel on each side.</p>
 *
 * <p>Wheel 0 sits in the neighbouring block along FACING, wheel 1 in the neighbouring block opposite to it. Steering
 * is shared so both wheels turn together like a steering axle.</p>
 */
public class DualWheelMountBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor, Clearable {

	private static final double MAX_ALLOWED_EXTENSION = 0.65;
	private static final double NO_WHEEL_EXTENSION = 0.5;
	private static final int MAX_SUSPENSION_STRENGTH = 180;

	private static final Collection<DualWheelMountBlockEntity> queuedMounts = new ObjectOpenHashSet<>();

	/** State of one wheel. */
	public static class Wheel {
		public ItemStack tire = ItemStack.EMPTY;
		public double extension = NO_WHEEL_EXTENSION;
		public double lastExtension = NO_WHEEL_EXTENSION;
		public double angle;
		public double lastAngle;
		public double angularVelocity;
		public double touchingFriction = 1.0;
		public boolean liftedUp;
	}

	private final Wheel[] wheels = {new Wheel(), new Wheel()};
	private ScrollValueBehaviour strength;

	// Steering is shared by both wheels.
	private double chasingYaw, lastChasingYaw;
	private int clientSteeringSignal;
	private int lastServerSteeringSignal;
	private int lastServerSteeringSignalLeft;
	private int lastServerSteeringSignalRight;

	private final ForceTotal forceTotal = new ForceTotal();

	public DualWheelMountBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Setup
	// ------------------------------------------------------------------------------------------------------------

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		strength = new ScrollValueBehaviour(Component.translatable("dualwheelmount.scroll_option.suspension_strength"),
			this, new CenteredSideValueBoxTransform((state, direction) -> direction.getAxis()
				.isHorizontal()
				&& direction.getAxis() != state.getValue(DualWheelMountBlock.FACING)
					.getAxis()));
		strength.between(5, MAX_SUSPENSION_STRENGTH);
		strength.value = 10;
		behaviours.add(strength);
	}

	public Direction getWheelDirection(int index) {
		Direction facing = getBlockState().getValue(DualWheelMountBlock.FACING);
		return index == 0 ? facing : facing.getOpposite();
	}

	public Wheel getWheel(int index) {
		return wheels[index];
	}

	/** @param first true for the wheel on the FACING side, false for the opposite one. */
	public ItemStack getTire(boolean first) {
		return wheels[first ? 0 : 1].tire;
	}

	/** Swaps the held item (a tire, or nothing) with the tire in the given slot, like the Offroad mount does. */
	public void swapTire(boolean first, Player player, InteractionHand hand) {
		Wheel wheel = wheels[first ? 0 : 1];
		ItemStack held = player.getItemInHand(hand);
		ItemStack old = wheel.tire.copy();

		wheel.tire = held.copyWithCount(1);
		if (!player.hasInfiniteMaterials())
			held.shrink(1);
		player.getInventory()
			.placeItemBackInInventory(old);

		setChanged();
		sendData();

		if (level != null) {
			float pitch = 0.8f + level.random.nextFloat() * 0.4f;
			if (old.isEmpty() && !wheel.tire.isEmpty() || !old.isEmpty() && !wheel.tire.isEmpty())
				level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, .75f, pitch);
			else if (!old.isEmpty())
				level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .75f, pitch);
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Server physics
	// ------------------------------------------------------------------------------------------------------------

	public static void applyAllBatchedForces(ServerLevel level, double timeStep) {
		for (DualWheelMountBlockEntity be : queuedMounts) {
			if (be.isRemoved())
				continue;
			be.applyBatchedForces();
		}
		queuedMounts.clear();
	}

	@Override
	public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
		boolean any = false;
		for (int i = 0; i < 2; i++)
			any |= tickWheelPhysics(wheels[i], getWheelDirection(i), subLevel, timeStep);
		if (any)
			queuedMounts.add(this);
	}

	private boolean tickWheelPhysics(Wheel wheel, Direction wheelDir, ServerSubLevel subLevel, double timeStep) {
		TireLike tire = wheel.tire.get(OffroadDataComponents.TIRE);
		if (tire == null)
			return false;

		float radius = tire.radius();
		double suspensionRestDistance = MAX_ALLOWED_EXTENSION;
		BlockPos blockPos = getBlockPos();

		MassData massData = subLevel.getMassTracker();

		Vec3 localPos = blockPos.relative(wheelDir)
			.getCenter();
		Vector3d forcePos = new Vector3d(localPos.x, localPos.y, localPos.z);
		double normalMass = 1.0 / massData.getInverseNormalMass(forcePos, OrientedBoundingBox3d.UP);

		double effectiveStrength = strength.getValue();
		double normalMassScaling = Math.min(normalMass / effectiveStrength, 1.0) * 10.0;

		double strengthMul = effectiveStrength * normalMassScaling * 2;
		double springStrength = effectiveStrength * normalMassScaling * 40;
		double dampingStrength = effectiveStrength * normalMassScaling;

		Pose3d pose = subLevel.logicalPose();

		Direction.Axis axis = wheelDir.getAxis();
		Vec3i normal = Direction.get(Direction.AxisDirection.POSITIVE, axis)
			.getNormal();
		Vector3dc sideD = getRotatedWheelAxis(normal);
		normal = new Vec3i(normal.getZ(), 0, normal.getX());
		Vector3dc normalD = getRotatedWheelAxis(normal);

		TerrainCastResult extensionToTerrain = computeMaxExtensionToTerrain(wheelDir, normalD, pose);
		double maxExtension = extensionToTerrain.maxExtension();

		wheel.extension = Mth.lerp(1.0, wheel.extension, maxExtension);

		if (maxExtension > suspensionRestDistance + radius + 0.25) {
			wheel.extension = suspensionRestDistance;
			return false;
		}

		double distance = (suspensionRestDistance / 6.0) + wheel.extension;
		double springLength = Mth.clamp(distance - radius, 0.0, suspensionRestDistance);

		Vector3d velocity = Sable.HELPER.getVelocity(level, JOMLConversion.toJOML(localPos));
		Vector3d localVelocity = pose.transformNormalInverse(velocity);

		double dampingForce = -localVelocity.y * dampingStrength;
		double springForce = ((suspensionRestDistance - springLength) * springStrength + dampingForce) * timeStep;

		Vec3i rayHitNormal = extensionToTerrain.normal()
			.getNormal();

		Vec3 localForce = new Vec3(springForce * rayHitNormal.getX(), springForce * rayHitNormal.getY(),
			springForce * rayHitNormal.getZ());
		if (extensionToTerrain.subLevel() != null)
			localForce = extensionToTerrain.subLevel()
				.logicalPose()
				.transformNormal(localForce);
		localForce = pose.transformNormalInverse(localForce);

		Vector3d force = new Vector3d(localForce.x, localForce.y, localForce.z);

		// friction / driving / braking
		if (extensionToTerrain.minInteractingBlock() != null)
			wheel.touchingFriction = fudgeFriction(
				PhysicsBlockPropertyHelper.getFriction(level.getBlockState(extensionToTerrain.minInteractingBlock())));
		else
			wheel.touchingFriction = 1.0;

		wheel.touchingFriction = Math.max(wheel.touchingFriction, tire.minimumFriction());

		double brakeStrength = level.getSignal(blockPos.above(), Direction.UP) / 15.0;
		double surfaceBraking = Math.min(wheel.touchingFriction, 1.0);
		double brakingFrictionStrength = (0.075 + brakeStrength * 0.3) * surfaceBraking;

		// Rolling resistance is tiny, so an unpowered vehicle that is pushed (e.g. a plane under jet thrust) rolls freely.
		float kineticSpeed = axis == Direction.Axis.X ? getSpeed() : -getSpeed();
		force.fma((localVelocity.dot(normalD) * -brakingFrictionStrength * strengthMul * timeStep)
			+ (kineticSpeed * (1.0 - brakeStrength) * surfaceBraking * 1.75 * timeStep), normalD);
		force.fma(localVelocity.dot(sideD) * -0.6 * wheel.touchingFriction * strengthMul * timeStep, sideD);

		forceTotal.applyImpulseAtPoint(subLevel, forcePos, force);
		return true;
	}

	private void applyBatchedForces() {
		SubLevel subLevel = Sable.HELPER.getContaining(this);
		if (subLevel == null)
			return;

		RigidBodyHandle handle = RigidBodyHandle.of((ServerSubLevel) subLevel);
		handle.applyForcesAndReset(forceTotal);
	}

	// give a slight amount of friction even when at 0
	public static double fudgeFriction(double realValue) {
		if (realValue < 1)
			return 0.1 + 0.9 * realValue;
		return realValue;
	}

	private record TerrainCastResult(double maxExtension, @NotNull Direction normal, @Nullable SubLevel subLevel,
		@Nullable BlockPos minInteractingBlock) {
	}

	private TerrainCastResult computeMaxExtensionToTerrain(Direction wheelDir, Vector3dc normalD, Pose3dc pose) {
		Vec3 wheelPosCenter = getBlockPos().relative(wheelDir)
			.getCenter();
		double minExtension = 5.0;
		Direction minNormal = Direction.UP;
		SubLevel minHitSubLevel = null;
		BlockPos minInteractingBlock = null;

		for (int i = -1; i <= 1; i++) {
			Vec3 localPosO = wheelPosCenter.add(JOMLConversion.toMojang(normalD)
				.scale(i));

			ClipContext clipContext = new ClipContext(localPosO, localPosO.subtract(0.0, 5.0, 0.0),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
			((ClipContextExtension) clipContext).sable$setIgnoredSubLevel(Sable.HELPER.getContaining(this));
			BlockHitResult clipResult = level.clip(clipContext);

			if (clipResult.getType() == HitResult.Type.MISS)
				continue;

			SubLevel hitSubLevel = Sable.HELPER.getContaining(level, clipResult.getLocation());
			Vec3 localHitPos = pose.transformPositionInverse(hitSubLevel == null ? clipResult.getLocation()
				: hitSubLevel.logicalPose()
					.transformPosition(clipResult.getLocation()));

			if (localHitPos.y > wheelPosCenter.y)
				continue;

			if (localPosO.distanceTo(localHitPos) < 0.05)
				continue;

			double dist = wheelPosCenter.y - localHitPos.y;
			if (dist <= 1e-5)
				continue;

			Direction dir = clipResult.getDirection();
			Vector3d hitNormal = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());

			if (hitSubLevel != null)
				hitSubLevel.logicalPose()
					.transformNormal(hitNormal);
			pose.transformNormalInverse(hitNormal);

			if (hitNormal.dot(0.0, 1.0, 0.0) < 0.5)
				continue;

			minExtension = Math.min(minExtension, dist);
			minNormal = clipResult.getDirection();
			minHitSubLevel = hitSubLevel;
			minInteractingBlock = clipResult.getBlockPos();
		}

		return new TerrainCastResult(minExtension, minNormal, minHitSubLevel, minInteractingBlock);
	}

	private @NotNull Vector3dc getRotatedWheelAxis(Vec3i normal) {
		Vector3d normalD = new Vector3d(normal.getX(), normal.getY(), normal.getZ());
		normalD.rotateY(chasingYaw);
		return normalD;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Ticking (client: visual wheel rotation, driven by actual ground speed so unpowered wheels roll freely)
	// ------------------------------------------------------------------------------------------------------------

	@Override
	public void tick() {
		super.tick();

		lastChasingYaw = chasingYaw;
		chasingYaw = Mth.lerp(0.4, chasingYaw, computeYaw());

		if (!level.isClientSide)
			return;

		for (int i = 0; i < 2; i++)
			tickWheelClient(wheels[i], getWheelDirection(i));
	}

	private void tickWheelClient(Wheel wheel, Direction wheelDir) {
		TireLike tire = wheel.tire.get(OffroadDataComponents.TIRE);

		if (tire == null) {
			wheel.angle = 0.0;
			wheel.lastAngle = 0.0;
			wheel.lastExtension = wheel.extension;
			wheel.extension = Mth.lerp(0.6, wheel.extension, NO_WHEEL_EXTENSION);
			return;
		}

		float radius = tire.radius();
		SubLevel subLevel = Sable.HELPER.getContaining(this);

		wheel.lastExtension = wheel.extension;
		wheel.extension = Mth.lerp(0.7, wheel.extension, computeMaxExtension(wheel, wheelDir, radius));

		float speed = wheelDir.getAxis() == Direction.Axis.X ? -getSpeed() : getSpeed();
		double rpt = speed * Math.PI * 2.0 / 60.0 / 20.0 * (15 - level.getSignal(getBlockPos().above(), Direction.UP)) / 15.0;
		double attemptedAngularVelocity = Mth.lerp(0.2, wheel.angularVelocity, rpt);

		// In the air (or not on a vehicle) the wheel just follows the drive speed.
		if (subLevel == null || wheel.liftedUp) {
			wheel.angularVelocity = attemptedAngularVelocity;
			wheel.lastAngle = wheel.angle;
			wheel.angle += wheel.angularVelocity;
			return;
		}

		// On the ground the wheel rotates by how far the vehicle actually travelled, powered or not.
		Vector3d velocity = Sable.HELPER.getVelocity(level, JOMLConversion.atCenterOf(getBlockPos().relative(wheelDir)));
		Vector3d localVelocity = subLevel.logicalPose()
			.transformNormalInverse(velocity)
			.div(20.0);
		Direction.Axis axis = wheelDir.getAxis();

		Vec3i normal = Direction.get(Direction.AxisDirection.POSITIVE, axis)
			.getNormal();
		normal = new Vec3i(normal.getZ(), 0, normal.getX());
		Vector3dc normalD = getRotatedWheelAxis(normal);

		double translation = localVelocity.dot(normalD);

		double circumference = Math.PI * radius * 2.0;
		double angularDelta = -translation / circumference * Math.PI * 2.0;

		// visually slip if trying to drive on slippery surface
		if (wheel.touchingFriction < 1.0)
			angularDelta = Mth.lerp(wheel.touchingFriction, attemptedAngularVelocity, angularDelta);

		wheel.lastAngle = wheel.angle;
		wheel.angle += angularDelta;
		wheel.angularVelocity = angularDelta;
	}

	private double computeMaxExtension(Wheel wheel, Direction wheelDir, float radius) {
		SubLevel subLevel = Sable.HELPER.getContaining(this);
		if (subLevel == null)
			return MAX_ALLOWED_EXTENSION;

		Pose3dc pose = subLevel.logicalPose();

		Direction.Axis axis = wheelDir.getAxis();
		Vec3i normal = Direction.get(Direction.AxisDirection.POSITIVE, axis)
			.getNormal();
		normal = new Vec3i(normal.getZ(), 0, normal.getX());
		Vector3dc rotatedAxis = getRotatedWheelAxis(normal);

		TerrainCastResult extensionToTerrain = computeMaxExtensionToTerrain(wheelDir, rotatedAxis, pose);
		double unclampedExtension = extensionToTerrain.maxExtension() - radius;

		wheel.liftedUp = unclampedExtension > MAX_ALLOWED_EXTENSION;
		if (extensionToTerrain.minInteractingBlock() == null)
			wheel.touchingFriction = 1.0;
		else
			wheel.touchingFriction = fudgeFriction(
				PhysicsBlockPropertyHelper.getFriction(level.getBlockState(extensionToTerrain.minInteractingBlock())));

		return Mth.clamp(unclampedExtension, -0.45, MAX_ALLOWED_EXTENSION);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Steering (shared by both wheels so they turn together)
	// ------------------------------------------------------------------------------------------------------------

	protected double computeYaw() {
		int signal = getSteeringSignal();
		if (signal == 0)
			return 0.0;
		return (-signal / 15.0 * Math.PI / 4.0 * (30.0 / 45.0));
	}

	protected int getSteeringSignal() {
		if (level.isClientSide)
			return clientSteeringSignal;

		Direction facing = getBlockState().getValue(DualWheelMountBlock.FACING);
		Direction d1 = facing.getClockWise();
		Direction d2 = facing.getCounterClockWise();
		BlockPos pos = getBlockPos();

		int signalLeft = level.getSignal(pos.relative(d1), d1);
		int signalRight = level.getSignal(pos.relative(d2), d2);
		int signal = signalLeft - signalRight;

		boolean sendData = signal != lastServerSteeringSignal || signalLeft != lastServerSteeringSignalLeft
			|| signalRight != lastServerSteeringSignalRight;

		lastServerSteeringSignal = signal;
		lastServerSteeringSignalLeft = signalLeft;
		lastServerSteeringSignalRight = signalRight;

		if (sendData)
			sendData();

		return signal;
	}

	public double getLerpedYaw(double partialTick) {
		return Mth.lerp(partialTick, lastChasingYaw, chasingYaw);
	}

	public float getLerpedAngle(int wheel, float partialTicks) {
		return (float) Mth.lerp(partialTicks, wheels[wheel].lastAngle, wheels[wheel].angle);
	}

	public double getLerpedExtension(int wheel, float partialTick) {
		return Mth.lerp(partialTick, wheels[wheel].lastExtension, wheels[wheel].extension);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Persistence / sync
	// ------------------------------------------------------------------------------------------------------------

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		tag.put("TireA", wheels[0].tire.saveOptional(registries));
		tag.put("TireB", wheels[1].tire.saveOptional(registries));

		if (clientPacket)
			tag.putInt("SteeringSignalStrength", lastServerSteeringSignal);

		super.write(tag, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		wheels[0].tire = ItemStack.parseOptional(registries, tag.getCompound("TireA"));
		wheels[1].tire = ItemStack.parseOptional(registries, tag.getCompound("TireB"));

		if (clientPacket) {
			if (tag.contains("SteeringSignalStrength"))
				clientSteeringSignal = tag.getInt("SteeringSignalStrength");
			invalidateRenderBoundingBox();
		}

		super.read(tag, registries, clientPacket);
	}

	@Override
	public void clearContent() {
		wheels[0].tire = ItemStack.EMPTY;
		wheels[1].tire = ItemStack.EMPTY;
	}

	@Override
	protected AABB createRenderBoundingBox() {
		double largest = 0;
		for (Wheel wheel : wheels) {
			TireLike tire = wheel.tire.get(OffroadDataComponents.TIRE);
			if (tire != null)
				largest = Math.max(largest, tire.radius());
		}
		return new AABB(getBlockPos()).inflate(largest + 1);
	}
}
