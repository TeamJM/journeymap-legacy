/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.dialog;

import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.io.MapSaver;
import journeymap.client.log.ChatLog;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.MapState;
import journeymap.client.model.MapType;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.texture.TextureCache;
import journeymap.client.render.texture.TextureImpl;
import journeymap.client.task.multi.MapRegionTask;
import journeymap.client.task.multi.SaveMapTask;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.component.BooleanPropertyButton;
import journeymap.client.ui.component.Button;
import journeymap.client.ui.component.ButtonList;
import journeymap.client.ui.component.JmUI;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.common.Journeymap;
import journeymap.common.version.VersionCheck;
import net.minecraft.client.gui.GuiButton;
import org.apache.logging.log4j.Level;
import org.lwjgl.input.Keyboard;

import java.io.IOException;


public class FullscreenActions extends JmUI
{
    protected TextureImpl patreonLogo = TextureCache.instance().getPatreonLogo();

    Button buttonAutomap, buttonSave, buttonAbout, buttonClose, buttonBrowser, buttonCheck, buttonDonate, buttonDeleteMap;
    BooleanPropertyButton buttonEnableMapping;

    public FullscreenActions()
    {
        super(Constants.getString("jm.common.actions"));
    }

    public FullscreenActions(JmUI returnDisplay)
    {
        super(Constants.getString("jm.common.actions"), returnDisplay);
    }


    public static void launchLocalhost()
    {
        String url = "http://localhost:" + JourneymapClient.getWebMapProperties().port.get();
        try
        {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        }
        catch (IOException e)
        {
            Journeymap.getLogger().log(Level.ERROR, "Could not launch browser with URL: " + url + ": " + LogFormatter.toString(e));
        }
    }

    public static void launchPatreon()
    {
        String url = "http://patreon.com/techbrew";
        try
        {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        }
        catch (IOException e)
        {
            Journeymap.getLogger().log(Level.ERROR, "Could not launch browser with URL: " + url + ": " + LogFormatter.toString(e));
        }
    }

    /**
     * Launch the JourneyMap website in the native OS.
     */
    public static void launchWebsite()
    {
        String url = Journeymap.DOWNLOAD_URL;
        try
        {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        }
        catch (Throwable e)
        {
            Journeymap.getLogger().error("Could not launch browser with URL: " + url, LogFormatter.toString(e)); //$NON-NLS-1$
        }
    }

    /**
     * Launch the download website in the native OS.
     */
    public static void launchDownloadWebsite()
    {
        String url = VersionCheck.getDownloadUrl();
        try
        {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        }
        catch (Throwable e)
        {
            Journeymap.getLogger().error("Could not launch browser with URL: " + url, LogFormatter.toString(e)); //$NON-NLS-1$
        }
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    @Override
    public void initGui()
    {
        this.buttonList.clear();

        buttonAbout = new Button(Constants.getString("jm.common.splash_about"));
        buttonSave = new Button(Constants.getString("jm.common.save_map"));
        buttonClose = new Button(Constants.getString("jm.common.close"));
        buttonBrowser = new Button(Constants.getString("jm.common.use_browser"));
        buttonBrowser.setEnabled(JourneymapClient.getWebMapProperties().enabled.get());

        buttonAutomap = new Button(Constants.getString("jm.common.automap_title"));
        buttonAutomap.setTooltip(Constants.getString("jm.common.automap_text"));
        buttonAutomap.setEnabled(ForgeHelper.INSTANCE.getClient().isSingleplayer() && JourneymapClient.getCoreProperties().mappingEnabled.get());

        buttonDeleteMap = new Button(Constants.getString("jm.common.deletemap_title"));
        buttonDeleteMap.setTooltip(Constants.getString("jm.common.deletemap_text"));

        buttonDonate = new Button(Constants.getString("jm.webmap.donate_text"));
        buttonDonate.setDefaultStyle(false);
        buttonDonate.setDrawBackground(false);
        buttonDonate.setDrawFrame(false);

        buttonCheck = new Button(Constants.getString("jm.common.update_check"));

        buttonEnableMapping = new BooleanPropertyButton(Constants.getString("jm.common.enable_mapping_false"),
                Constants.getString("jm.common.enable_mapping_true"),
                JourneymapClient.getCoreProperties(),
                JourneymapClient.getCoreProperties().mappingEnabled);

        buttonList.add(buttonAbout);
        buttonList.add(buttonAutomap);
        buttonList.add(buttonSave);
        buttonList.add(buttonCheck);
        buttonList.add(buttonDonate);
        buttonList.add(buttonBrowser);
        buttonList.add(buttonDeleteMap);
        buttonList.add(buttonEnableMapping);

        new ButtonList(buttonList).equalizeWidths(getFontRenderer());

        buttonList.add(buttonClose);

    }

    /**
     * Center buttons in UI.
     */
    @Override
    protected void layoutButtons()
    {
        // Buttons

        if (buttonList.isEmpty())
        {
            initGui();
        }

        buttonSave.setEnabled(!JourneymapClient.getInstance().isTaskManagerEnabled(MapRegionTask.Manager.class));

        final int hgap = 4;
        final int vgap = 3;
        final int bx = (this.width) / 2;
        int by = this.height / 4;

        buttonAbout.centerHorizontalOn(bx).setY(by);
        by = buttonAbout.getBottomY() + vgap;

        ButtonList row1 = new ButtonList(buttonAutomap, buttonEnableMapping);
        ButtonList row2 = new ButtonList(buttonSave, buttonDeleteMap);
        ButtonList row3 = new ButtonList(buttonBrowser, buttonCheck);

        row1.layoutCenteredHorizontal(bx, by, true, hgap);
        row2.layoutCenteredHorizontal(bx, row1.getBottomY() + vgap, true, hgap);
        row3.layoutCenteredHorizontal(bx, row2.getBottomY() + vgap, true, hgap);

        int patreonX = bx - 8;
        int patreonY = row2.getBottomY() + 32;
        DrawUtil.drawImage(patreonLogo, patreonX, patreonY, false, .5f, 0);

        buttonDonate.centerHorizontalOn(bx).setY(patreonY + 16);
        buttonClose.below(buttonDonate, vgap * 4).centerHorizontalOn(bx);
    }

    @Override
    protected void actionPerformed(GuiButton guibutton)
    { // actionPerformed

        if (guibutton == buttonSave)
        {
            save();
            UIManager.getInstance().openFullscreenMap();
            return;
        }
        if (guibutton == buttonClose)
        {
            UIManager.getInstance().openFullscreenMap();
            return;
        }
        if (guibutton == buttonBrowser)
        {
            launchLocalhost();
            UIManager.getInstance().openFullscreenMap();
            return;
        }
        if (guibutton == buttonDonate)
        {
            launchPatreon();
            UIManager.getInstance().openFullscreenMap();
            return;
        }
        if (guibutton == buttonAutomap)
        {
            UIManager.getInstance().open(AutoMapConfirmation.class);
            return;
        }
        if (guibutton == buttonDeleteMap)
        {
            UIManager.getInstance().open(DeleteMapConfirmation.class);
            return;
        }
        if (guibutton == buttonCheck)
        {
            launchWebsite();
            UIManager.getInstance().openFullscreenMap();
            return;
        }
        if (guibutton == buttonAbout)
        {
            UIManager.getInstance().openSplash(this);
            return;
        }
        if (guibutton == buttonEnableMapping)
        {
            buttonEnableMapping.toggle();
            if (JourneymapClient.getCoreProperties().mappingEnabled.get())
            {
                UIManager.getInstance().openFullscreenMap();
                ChatLog.announceI18N("jm.common.enable_mapping_true_text");
                return;
            }
            else
            {
                JourneymapClient.getInstance().stopMapping();
                ChatLog.announceI18N("jm.common.enable_mapping_false_text");
                UIManager.getInstance().openFullscreenMap();
                return;
            }
        }

    }

    void save()
    {
        final MapState state = Fullscreen.state();
        boolean showCaves = JourneymapClient.getFullMapProperties().showCaves.get();
        final MapType mapType = state.getMapType(showCaves);
        final MapSaver mapSaver = new MapSaver(state.getWorldDir(), mapType);
        if (mapSaver.isValid())
        {
            JourneymapClient.getInstance().toggleTask(SaveMapTask.Manager.class, true, mapSaver);
            ChatLog.announceI18N("jm.common.save_filename", mapSaver.getSaveFileName());
        }
        closeAndReturn();
    }


    @Override
    protected void keyTyped(char c, int i)
    {
        switch (i)
        {
            case Keyboard.KEY_ESCAPE:
            {
                closeAndReturn();
                break;
            }
        }
    }
}
