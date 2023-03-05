/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.reference;

/**
 * Created by Mysticdrew on 3/18/2015.
 */

public enum Controller
{
    FORGE,
    BUKKIT;

    private static Controller controller;

    public static Controller getController()
    {
        return controller;
    }

    public static void setController(Controller controller)
    {
        Controller.controller = controller;
    }
}


