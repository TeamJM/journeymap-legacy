/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.fullscreen;

import net.minecraft.client.gui.GuiChat;
import org.lwjgl.opengl.GL11;


public class MapChat extends GuiChat
{
    protected boolean hidden = false;
    protected int bottomMargin = 8;
    private int cursorCounter;

    public MapChat(String defaultText, boolean hidden)
    {
        super(defaultText);
        this.hidden = hidden;
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
        hidden = true;
    }

    public void close()
    {
        onGuiClosed();
    }

    /**
     * Called from the main game loop to update the screen.
     */
    @Override
    public void updateScreen()
    {
        if (hidden)
        {
            return;
        }
        super.updateScreen();
        cursorCounter++;
    }

    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
    @Override
    public void keyTyped(char par1, int par2)
    {
        if (hidden)
        {
            return;
        }
        super.keyTyped(par1, par2);
    }

    /**
     * Handles mouse input.
     */
    @Override
    public void handleMouseInput()
    {
        if (hidden)
        {
            return;
        }
        super.handleMouseInput();
    }

    /**
     * Called when the mouse is clicked.
     */
    //@Override
    public void mouseClicked(int par1, int par2, int par3)
    {
        if (hidden)
        {
            return;
        }
        super.mouseClicked(par1, par2, par3);
    }

    @Override
    public void confirmClicked(boolean par1, int par2)
    {
        if (hidden)
        {
            return;
        }
        super.confirmClicked(par1, par2);
    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int par1, int par2, float par3)
    {
        GL11.glPushMatrix();
        GL11.glTranslatef(0, this.height - 39.5f - bottomMargin, 0.0F);
        if (this.mc != null)
        {
            if (this.mc.ingameGUI != null && this.mc.ingameGUI.getChatGUI() != null)
            {
                this.mc.ingameGUI.getChatGUI().drawChat(hidden ? this.mc.ingameGUI.getUpdateCounter() : this.cursorCounter);
            }
        }
        GL11.glPopMatrix();

        if (hidden)
        {
            return;
        }

        super.drawScreen(par1, par2, par3);
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    public void setText(String defaultText)
    {
        this.inputField.setText(defaultText);
    }
}
