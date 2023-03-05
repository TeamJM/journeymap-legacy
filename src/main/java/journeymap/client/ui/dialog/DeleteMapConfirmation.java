/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.dialog;

import journeymap.client.Constants;
import journeymap.client.task.main.DeleteMapTask;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.component.Button;
import journeymap.client.ui.component.JmUI;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Keyboard;

public class DeleteMapConfirmation extends JmUI
{

    Button buttonAll, buttonCurrent, buttonClose;

    public DeleteMapConfirmation()
    {
        super(Constants.getString("jm.common.deletemap_dialog"));
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    @Override
    public void initGui()
    {
        buttonList.clear();

        buttonAll = new Button(Constants.getString("jm.common.deletemap_dialog_all"));
        buttonCurrent = new Button(Constants.getString("jm.common.deletemap_dialog_this"));

        buttonClose = new Button(Constants.getString("jm.waypoint.cancel"));

        buttonList.add(buttonAll);
        buttonList.add(buttonCurrent);

        buttonList.add(buttonClose);
    }

    @Override
    protected void layoutButtons()
    {

        if (this.buttonList.isEmpty())
        {
            this.initGui();
        }

        final int x = this.width / 2;
        final int y = this.height / 4;
        final int vgap = 3;

        this.drawCenteredString(getFontRenderer(), Constants.getString("jm.common.deletemap_dialog_text"), x, y, 16777215);

        buttonAll.centerHorizontalOn(x).setY(y + 18);
        buttonCurrent.centerHorizontalOn(x).below(buttonAll, vgap);
        buttonClose.centerHorizontalOn(x).below(buttonCurrent, vgap * 4);
    }

    @Override
    protected void actionPerformed(GuiButton guibutton)
    {
        if (guibutton == buttonAll || guibutton == buttonCurrent)
        {
            DeleteMapTask.queue(guibutton == buttonAll);
            UIManager.getInstance().openFullscreenMap();
            return;
        }

        if (guibutton == buttonClose)
        {
            UIManager.getInstance().openMapActions();
            return;
        }
    }

    @Override
    protected void keyTyped(char c, int i)
    {
        switch (i)
        {
            case Keyboard.KEY_ESCAPE:
            {
                UIManager.getInstance().openMapActions();
                break;
            }
        }
    }
}
