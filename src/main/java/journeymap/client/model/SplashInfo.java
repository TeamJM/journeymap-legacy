/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model;

import journeymap.client.log.LogFormatter;
import journeymap.client.properties.config.Config;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.component.JmUI;
import journeymap.client.ui.dialog.OptionsManager;
import journeymap.common.Journeymap;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Simple model for GSON binding to assets/journeymap/lang/message/splash*.json
 */
public class SplashInfo
{
    public ArrayList<Line> lines = new ArrayList<Line>();

    public SplashInfo()
    {
    }

    public static class Line
    {
        public String label;
        public String action;

        public Line()
        {
        }

        public boolean hasAction()
        {
            return action != null && action.trim().length() > 0;
        }

        public void invokeAction(JmUI returnUi)
        {
            if (!hasAction())
            {
                return;
            }
            String[] parts = this.action.split("#");
            String className = parts[0];
            String action = null;
            if (parts.length > 0)
            {
                action = parts[1];
            }

            try
            {
                Class<? extends JmUI> uiClass = (Class<? extends JmUI>) Class.forName("journeymap.client.ui." + className);

                if (uiClass.equals(OptionsManager.class) && action != null)
                {
                    Config.Category category = Config.Category.valueOf(action);
                    UIManager.getInstance().openOptionsManager(returnUi, category);
                    return;
                }
                else if (action != null)
                {
                    String arg = parts.length == 3 ? parts[2] : null;
                    Method actionMethod;
                    try
                    {
                        JmUI ui = UIManager.getInstance().open(uiClass, returnUi);

                        if (arg == null)
                        {
                            actionMethod = uiClass.getMethod(action);
                            actionMethod.invoke(ui);
                        }
                        else
                        {
                            actionMethod = uiClass.getMethod(action, String.class);
                            actionMethod.invoke(ui, arg);
                        }
                        return;
                    }
                    catch (Exception e)
                    {
                        Journeymap.getLogger().warn("Couldn't perform action " + action + " on " + uiClass + ": " + e.getMessage());
                    }
                }

                UIManager.getInstance().open(uiClass, returnUi);
            }
            catch (Throwable t)
            {
                Journeymap.getLogger().error("Couldn't invoke action: " + action + ": " + LogFormatter.toString(t));
            }
        }
    }
}
