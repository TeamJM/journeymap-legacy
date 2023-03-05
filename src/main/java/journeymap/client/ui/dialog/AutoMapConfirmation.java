/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.dialog;

import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.task.main.IMainThreadTask;
import journeymap.client.task.multi.MapRegionTask;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.component.Button;
import journeymap.client.ui.component.JmUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Keyboard;

public class AutoMapConfirmation extends JmUI
{

    Button buttonAll, buttonMissing, buttonNone, buttonClose;

    public AutoMapConfirmation()
    {
        super(Constants.getString("jm.common.automap_dialog"));
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    @Override
    public void initGui()
    {
        buttonList.clear();

        buttonAll = new Button(Constants.getString("jm.common.automap_dialog_all"));
        buttonMissing = new Button(Constants.getString("jm.common.automap_dialog_missing"));
        buttonNone = new Button(Constants.getString("jm.common.automap_dialog_none"));
        buttonClose = new Button(Constants.getString("jm.common.close"));

        boolean enable = !JourneymapClient.getInstance().isTaskManagerEnabled(MapRegionTask.Manager.class);
        buttonAll.setEnabled(enable);
        buttonMissing.setEnabled(enable);
        buttonNone.setEnabled(!enable);

        buttonList.add(buttonAll);
        buttonList.add(buttonMissing);
        buttonList.add(buttonNone);
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

        this.drawCenteredString(getFontRenderer(), Constants.getString("jm.common.automap_dialog_text"), x, y, 16777215);

        buttonAll.centerHorizontalOn(x).setY(y + 18);
        buttonMissing.centerHorizontalOn(x).below(buttonAll, vgap);
        buttonNone.centerHorizontalOn(x).below(buttonMissing, vgap);
        buttonClose.centerHorizontalOn(x).below(buttonNone, vgap * 4);
    }

    @Override
    protected void actionPerformed(GuiButton guibutton)
    {
        if (guibutton != buttonClose)
        {
            final boolean enable;
            final Object arg;
            if (guibutton == buttonAll)
            {
                enable = true;
                arg = Boolean.TRUE;
            }
            else if (guibutton == buttonMissing)
            {
                enable = true;
                arg = Boolean.FALSE;
            }
            else
            {
                enable = false;
                arg = null;
            }

            JourneymapClient.getInstance().queueMainThreadTask(new IMainThreadTask()
            {
                @Override
                public IMainThreadTask perform(Minecraft mc, JourneymapClient jm)
                {
                    JourneymapClient.getInstance().toggleTask(MapRegionTask.Manager.class, enable, arg);
                    return null;
                }

                @Override
                public String getName()
                {
                    return "Automap";
                }
            });
        }

        UIManager.getInstance().openFullscreenMap();
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
