package dev.ryanhcode.offroad.content.components;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * COMPILE-TIME STUB. Mirrors the record components of Offroad's real TireLike so this mod can compile without the
 * Offroad artifact. It is excluded from the jar; the real class is used at runtime.
 */
public record TireLike(float radius, Vec3 rotation, Vec3 offset, Optional<ResourceLocation> model, float minimumFriction) {
}
