package com.boydsquirrel.navlights;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Builds one collision/outline shape per facing from a box that is defined for the "UP" orientation. */
public final class ShapeUtil {
    private ShapeUtil() {}

    public static Map<Direction, VoxelShape> make(double x1, double y1, double z1, double x2, double y2, double z2) {
        EnumMap<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.UP, Block.box(x1, y1, z1, x2, y2, z2));
        map.put(Direction.DOWN, Block.box(x1, 16 - y2, z1, x2, 16 - y1, z2));
        map.put(Direction.NORTH, Block.box(x1, z1, 16 - y2, x2, z2, 16 - y1));
        map.put(Direction.SOUTH, Block.box(x1, z1, y1, x2, z2, y2));
        map.put(Direction.EAST, Block.box(y1, x1, z1, y2, x2, z2));
        map.put(Direction.WEST, Block.box(16 - y2, x1, z1, 16 - y1, x2, z2));
        return map;
    }
}
