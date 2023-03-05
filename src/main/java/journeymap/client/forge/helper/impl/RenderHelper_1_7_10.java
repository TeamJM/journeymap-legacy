/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.helper.impl;

import journeymap.client.cartography.RGB;
import journeymap.client.forge.helper.IRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import org.lwjgl.opengl.GL11;

import java.awt.*;

/**
 * Encapsulates setting up vertices for a Tesselator
 */
public class RenderHelper_1_7_10 implements IRenderHelper
{
    Tessellator tessellator = Tessellator.instance;

    @Override
    public void sizeDisplay(double width, double height)
    {
        // 1.7.10
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);

        // 1.8
        /*
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, width, height, 0.0D, 100.0D, 3000.0D);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);
        */
    }

    @Override
    public void startDrawingQuads(boolean useColor)
    {
        // 1.7.10
        tessellator.startDrawingQuads();

        // 1.8
        // worldrenderer.startDrawingQuads();

        // 1.8.8
        /*
        if(useColor)
        {
            // (floats) x,y,z + (floats) uv + (ints) r,g,b,a
            worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        }
        else
        {
            // (floats) x,y,z + (floats) u,v
            worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        }
        */
    }

    @Override
    public void addVertexWithUV(double x, double y, double z, double u, double v)
    {
        // 1.7.10
        tessellator.addVertexWithUV(x, y, z, u, v);

        // 1.8
        // worldrenderer.addVertexWithUV(x, y, z, u, v);

        // 1.8.8
        //worldrenderer.pos(x, y, z).tex(u, v).endVertex();
    }

    @Override
    public void addVertexWithUV(double x, double y, double z, double u, double v, int[] rgba)
    {
        // 1.7
        tessellator.setColorRGBA(rgba[0], rgba[1], rgba[2], rgba[3]);
        tessellator.addVertexWithUV(x, y, z, u, v);

        // 1.8
        // tessellator.setColorRGBA(rgba[0], rgba[1], rgba[2], rgba[3]);
        // worldrenderer.addVertexWithUV(x, y, z, u, v);

        // 1.8.8
        // worldrenderer.pos(x, y, z).tex(u, v).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
    }

    @Override
    public void draw()
    {
        tessellator.draw();
    }

    @Override
    public void glEnableBlend()
    {
        // 1.7
        GL11.glEnable(GL11.GL_BLEND);

        // 1.8
        // GlStateManager.enableBlend();
    }

    @Override
    public void glDisableBlend()
    {
        // 1.7
        GL11.glDisable(GL11.GL_BLEND);

        // 1.8
        // GlStateManager.disableBlend();
    }

    @Override
    public void glEnableTexture2D()
    {
        // 1.7
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // 1.8
        // GlStateManager.enableTexture2D();
    }

    @Override
    public void glDisableTexture2D()
    {
        // 1.7
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        // 1.8
        // GlStateManager.disableTexture2D();
    }

    @Override
    public void glEnableAlpha()
    {
        // 1.7
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        // 1.8
        // GlStateManager.enableAlpha();
    }

    @Override
    public void glDisableAlpha()
    {
        // 1.7
        GL11.glDisable(GL11.GL_ALPHA_TEST);

        // 1.8
        // GlStateManager.disableAlpha();
    }

    @Override
    public void glBlendFunc(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha)
    {
        // 1.7
        OpenGlHelper.glBlendFunc(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);

        // 1.8
        // GlStateManager.tryBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    @Override
    public void glBlendFunc(int sfactorRGB, int dfactorRGB)
    {
        // 1.7
        GL11.glBlendFunc(sfactorRGB, dfactorRGB);

        // 1.8
        // GlStateManager.blendFunc(sfactorRGB, dfactorRGB);
    }

    @Override
    public void glColor4f(float r, float g, float b, float a)
    {
        // 1.7
        GL11.glColor4f(r,g,b,a);

        // 1.8
        // GlStateManager.color(r, g, b, a);
    }

    @Override
    public void glClearColor(float r, float g, float b, float a)
    {
        // 1.7
        GL11.glClearColor(r,g,b,a);

        // 1.8
        // GlStateManager.clearColor(r, g, b, a);
    }

    @Override
    public void glColorMask(boolean r, boolean g, boolean b, boolean a)
    {
        // 1.7
        GL11.glColorMask(r,g,b,a);

        // 1.8
        // GlStateManager.colorMask(r, g, b, a);
    }

    @Override
    public void glTexParameteri(int target, int pname, int param)
    {
        GL11.glTexParameteri(target, pname, param);
    }

    @Override
    public void glScaled(double x, double y, double z)
    {
        // 1.7
        GL11.glScaled(x,y,z);

        // 1.8
        // GlStateManager.scale(x, y, z);
    }

    @Override
    public void glDepthFunc(int func)
    {
        // 1.7
        GL11.glDepthFunc(func);

        // 1.8
        // GlStateManager.depthFunc(func);
    }

    @Override
    public void glShadeModel(int model)
    {
        // 1.7
        GL11.glShadeModel(model);

        // 1.8
        // GlStateManager.shadeModel(model);
    }

    @Override
    public void glBindTexture(int glid)
    {
        // 1.7
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glid);

        // 1.8
        // GlStateManager.bindTexture(glid);
    }

    @Override
    public void glDisableDepth()
    {
        // 1.7
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        // 1.8
        // GlStateManager.disableDepth();
    }

    @Override
    public void glEnableDepth()
    {
        // 1.7
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // 1.8
        // GlStateManager.enableDepth();
    }

    @Override
    public void glDepthMask(boolean enable)
    {
        // 1.7
        GL11.glDepthMask(enable);

        // 1.8
        // GlStateManager.depthMask(enable);
    }

    @Override
    public void glEnableLighting()
    {
        // 1.7
        GL11.glEnable(GL11.GL_LIGHTING);

        // 1.8
        // GlStateManager.enableLighting();
    }

    @Override
    public void glDisableLighting()
    {
        // 1.7
        GL11.glDisable(GL11.GL_LIGHTING);

        // 1.8
        // GlStateManager.disableLighting();
    }

    @Override
    public void glEnableFog()
    {
        // 1.7
        GL11.glEnable(GL11.GL_FOG);

        // 1.8
        // GlStateManager.enableFog();
    }

    @Override
    public void glDisableFog()
    {
        // 1.7
        GL11.glDisable(GL11.GL_FOG);

        // 1.8
        // GlStateManager.disableFog();
    }

    @Override
    public void glEnableCull()
    {
        // 1.7
        GL11.glEnable(GL11.GL_CULL_FACE);

        // 1.8
        // GlStateManager.enableCull();
    }

    @Override
    public void glDisableCull()
    {
        // 1.7
        GL11.glDisable(GL11.GL_CULL_FACE);

        // 1.8
        // GlStateManager.disableCull();
    }

    @Override
    public void glDeleteTextures(int textureId)
    {
        // 1.7
        GL11.glDeleteTextures(textureId);

        // 1.8
        // GlStateManager.deleteTexture(textureId);
    }
}
