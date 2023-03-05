/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.helper;

import java.awt.*;

/**
 * Interface used to encapsulate compile-time differences between Minecraft/Forge versions
 * with respect to OpenGL operations.
 */
public interface IRenderHelper
{
    public void sizeDisplay(double width, double height);

    public void startDrawingQuads(boolean useColor);

    public void addVertexWithUV(double x, double y, double z, double u, double v);

    public void addVertexWithUV(double x, double y, double z, double u, double v, int[] rgba);

    public void draw();

    public void glEnableBlend();

    public void glDisableBlend();

    public void glEnableTexture2D();

    public void glDisableTexture2D();

    public void glEnableAlpha();

    public void glDisableAlpha();

    public void glBlendFunc(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha);

    public void glBlendFunc(int sfactorRGB, int dfactorRGB);

    public void glColor4f(float r, float g, float b, float a);

    public void glClearColor(float r, float g, float b, float a);

    public void glColorMask(boolean r, boolean g, boolean b, boolean a);

    public void glTexParameteri(int target, int pname, int param);

    public void glScaled(double x, double y, double z);

    public void glDepthFunc(int func);

    public void glShadeModel(int model);

    public void glBindTexture(int glid);

    public void glDisableDepth();

    public void glEnableDepth();

    public void glDepthMask(boolean enable);

    public void glEnableLighting();

    public void glDisableLighting();

    public void glEnableFog();

    public void glDisableFog();

    public void glEnableCull();

    public void glDisableCull();

    public void glDeleteTextures(int textureId);
}
