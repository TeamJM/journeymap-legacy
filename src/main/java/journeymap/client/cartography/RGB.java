/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.cartography;

import java.awt.*;

/**
 * Color operations utility class.
 */
public final class RGB
{
    public static final int ALPHA_OPAQUE = 0xff000000;
    public static final int BLACK_ARGB = 0xFF000000; // -16777216
    public static final int BLACK_RGB = 0x000000; // 0
    public static final int WHITE_ARGB = 0xFFFFFFFF; // 4294967295
    public static final int WHITE_RGB = 0xFFFFFF; // 16777215
    public static final int GREEN_RGB = 0x00FF00;
    public static final int RED_RGB = 0xFF0000;
    public static final int BLUE_RGB = 0x0000FF;
    public static final int CYAN_RGB = 0x00FFFF;
    public static final int GRAY_RGB = 0x808080;
    public static final int DARK_GRAY_RGB = 0x404040;
    public static final int LIGHT_GRAY_RGB = 0xC0C0C0;

    transient private static final PixelPaint PIXEL_PAINT = new PixelPaint();

    /**
     * Don't instantiate.
     */
    private RGB()
    {
    }

    /**
     * Whether a color is black, with our without the alpha channel.
     */
    public static boolean isBlack(int rgb)
    {
        return rgb == BLACK_ARGB || rgb == BLACK_RGB;
    }

    /**
     * Whether a color is white, with our without the alpha channel.
     */
    public static boolean isWhite(int rgb)
    {
        return rgb == WHITE_ARGB || rgb == WHITE_RGB;
    }

    /**
     * Gets the PixelPaint singleton and sets the rgb color.
     */
    public static Paint paintOf(int rgb)
    {
        return PIXEL_PAINT.setColor(ALPHA_OPAQUE | rgb);
    }

    public static Integer max(Integer... colors)
    {
        int[] out = {0, 0, 0};

        int used = 0;
        for (Integer color : colors)
        {
            if (color == null)
            {
                continue;
            }
            int[] cInts = ints(color);
            out[0] = Math.max(out[0], cInts[0]);
            out[1] = Math.max(out[1], cInts[1]);
            out[2] = Math.max(out[2], cInts[2]);

            used++;
        }

        if (used == 0)
        {
            return null;
        }

        return toInteger(out);
    }

    public static int toInteger(float r, float g, float b)
    {
        return ((0xFF) << 24) |
                (((int) (r * 255 + 0.5) & 0xFF) << 16) |
                (((int) (g * 255 + 0.5) & 0xFF) << 8) |
                (((int) (b * 255 + 0.5) & 0xFF));
    }

    public static int toInteger(float[] rgb)
    {
        return ((0xFF) << 24) |
                (((int) (rgb[0] * 255 + 0.5) & 0xFF) << 16) |
                (((int) (rgb[1] * 255 + 0.5) & 0xFF) << 8) |
                (((int) (rgb[2] * 255 + 0.5) & 0xFF));
    }

    public static int toInteger(int r, int g, int b)
    {
        return ((0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                ((b & 0xFF));
    }

    public static int toInteger(int[] rgb)
    {
        return ((0xFF) << 24) |
                ((rgb[0] & 0xFF) << 16) |
                ((rgb[1] & 0xFF) << 8) |
                ((rgb[2] & 0xFF));
    }

    public static Color toColor(Integer rgb)
    {
        return rgb == null ? null : new Color(rgb);
    }

    public static String toString(Integer rgb)
    {
        if (rgb == null)
        {
            return "null";
        }
        int[] ints = ints(rgb);
        return String.format("r=%s,g=%s,b=%s", ints[0], ints[1], ints[2]);
    }

    public static String toHexString(Integer rgb)
    {
        int[] ints = ints(rgb);
        return String.format("#%02x%02x%02x", ints[0], ints[1], ints[2]);
    }

    /**
     * Darken/Lighten a color by a factor.
     */
    public static int adjustBrightness(int rgb, float factor)
    {
        if (factor == 1F)
        {
            return rgb;
        }
        return toInteger(clampFloats(floats(rgb), factor));
    }

    /**
     * Desaturate a color.  Not perfect, it'll do.
     */
    public static int greyScale(int rgb)
    {
        int[] ints = ints(rgb);
        int avg = clampInt((ints[0] + ints[1] + ints[2]) / 3);
        return toInteger(avg, avg, avg);
    }

    /**
     * Darken or lighten a color by a factor.
     * If adjustBrightness, add a blue tint to simulate shadow.
     */
    public static int bevelSlope(int rgb, float factor)
    {
        final float bluer = (factor < 1) ? .85f : 1f;
        float[] floats = floats(rgb);
        floats[0] = floats[0] * bluer * factor;
        floats[1] = floats[1] * bluer * factor;
        floats[2] = floats[2] * factor;
        return toInteger(clampFloats(floats, 1f));
    }

    /**
     * Darken a color by a factor, add a fog tint.
     */
    public static int darkenAmbient(int rgb, float factor, float[] ambient)
    {
        float[] floats = floats(rgb);
        floats[0] = floats[0] * (factor + ambient[0]);
        floats[1] = floats[1] * (factor + ambient[1]);
        floats[2] = floats[2] * (factor + ambient[2]);
        return toInteger(clampFloats(floats, 1f));
    }

    /**
     * Creates an array with three elements [r,g,b]
     * @param rgb color integer
     * @return array
     */
    public static int[] ints(int rgb)
    {
        return new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, (rgb) & 0xFF};
    }

    /**
     * Creates an array with four elements [r,g,b,a]
     * @param rgb color integer
     * @param alpha alpha (0-255)
     * @return array
     */
    public static int[] ints(int rgb, int alpha)
    {
        return new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, (rgb) & 0xFF, alpha & 0xFF};
    }

    public static float[] floats(int rgb)
    {
        return new float[]{((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, ((rgb) & 0xFF) / 255f};
    }

    /**
     * Blends otherRgb into rgb using alpha as a percentage.
     */
    public static int blendWith(int rgb, int otherRgb, float otherAlpha)
    {
        if (otherAlpha == 1f)
        {
            return otherRgb;
        }
        if (otherAlpha == 0f)
        {
            return rgb;
        }

        float[] floats = floats(rgb);
        float[] otherFloats = floats(otherRgb);

        floats[0] = otherFloats[0] * otherAlpha / 1f + floats[0] * (1 - otherAlpha);
        floats[1] = otherFloats[1] * otherAlpha / 1f + floats[1] * (1 - otherAlpha);
        floats[2] = otherFloats[2] * otherAlpha / 1f + floats[2] * (1 - otherAlpha);

        return toInteger(floats);
    }

    /**
     * Adjust color rgb using a multiplier
     *
     * @param rgb
     * @param multiplier
     * @return
     */
    public static int multiply(int rgb, int multiplier)
    {
        float[] rgbFloats = floats(rgb);
        float[] multFloats = floats(multiplier);

        rgbFloats[0] = rgbFloats[0] * multFloats[0];
        rgbFloats[1] = rgbFloats[1] * multFloats[1];
        rgbFloats[2] = rgbFloats[2] * multFloats[2];

        return toInteger(rgbFloats);
    }

    /**
     * Returns a float guaranteed to be between 0 and 1, inclusive.
     *
     * @param value
     * @return
     */
    public static float clampFloat(float value)
    {
        return value < 0f ? 0f : (value > 1f ? 1f : value);
    }

    /**
     * Returns an rgb array of floats clamped between 0 and 1 after a factor is applied.
     */
    public static float[] clampFloats(float[] rgbFloats, float factor)
    {
        float r = rgbFloats[0] * factor;
        float g = rgbFloats[1] * factor;
        float b = rgbFloats[2] * factor;
        rgbFloats[0] = r < 0f ? 0f : (r > 1f ? 1f : r);
        rgbFloats[1] = g < 0f ? 0f : (g > 1f ? 1f : g);
        rgbFloats[2] = b < 0f ? 0f : (b > 1f ? 1f : b);

        return rgbFloats;
    }

    /**
     * Returns an int guaranteed to be between 0 and 1, inclusive.
     *
     * @param value
     * @return
     */
    public static int clampInt(int value)
    {
        return value < 0 ? 0 : (value > 255 ? 255 : value);
    }

}
