package com.boydsquirrel.navlights;

import net.minecraft.world.item.DyeColor;

public final class ColorUtil {
    private ColorUtil() {}

    private static final int[][] TABLE = new int[DyeColor.values().length][];

    static {
        for (DyeColor c : DyeColor.values()) {
            TABLE[c.ordinal()] = compute(c);
        }
    }

    private static int[] compute(DyeColor c) {
        return switch (c) {
            case WHITE -> new int[]{255, 255, 255};
            case ORANGE -> new int[]{255, 150, 30};
            case MAGENTA -> new int[]{255, 60, 220};
            case LIGHT_BLUE -> new int[]{90, 190, 255};
            case YELLOW -> new int[]{255, 235, 40};
            case LIME -> new int[]{120, 255, 40};
            case PINK -> new int[]{255, 140, 190};
            case GRAY -> new int[]{150, 150, 160};
            case LIGHT_GRAY -> new int[]{210, 210, 220};
            case CYAN -> new int[]{30, 230, 230};
            case PURPLE -> new int[]{170, 60, 255};
            case BLUE -> new int[]{50, 80, 255};
            case BROWN -> new int[]{200, 120, 50};
            case GREEN -> new int[]{20, 230, 60};
            case RED -> new int[]{255, 25, 25};
            case BLACK -> new int[]{90, 90, 110};
        };
    }

    /** Bright RGB (0-255 each) used for glow sprites. */
    public static int[] glow(DyeColor c) {
        return TABLE[c.ordinal()];
    }

    /** Packed RGB used to tint the lens in the block model. */
    public static int tint(DyeColor c) {
        int[] rgb = glow(c);
        return (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }
}
