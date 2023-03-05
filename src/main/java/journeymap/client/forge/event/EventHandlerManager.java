/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.event;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventBus;
import journeymap.client.cartography.ColorManager;
import journeymap.client.log.LogFormatter;
import journeymap.client.network.WorldInfoHandler;
import journeymap.common.Journeymap;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

/**
 * @author techbrew 1/29/14.
 */
public class EventHandlerManager
{
    static WorldInfoHandler worldInfoHandler;
    private static HashMap<Class<? extends EventHandler>, EventHandler> handlers = new HashMap<Class<? extends EventHandler>, EventHandler>();

    public static void registerGeneralHandlers()
    {
        register(new ChatEventHandler());
        register(new StateTickHandler());
        register(new WorldEventHandler());
        register(new ChunkUpdateHandler());
        register(new WaypointBeaconHandler());
        register(new TextureAtlasHandler());
        worldInfoHandler = new WorldInfoHandler();
        ColorManager.instance();
    }

    public static void registerGuiHandlers()
    {
        register(new MiniMapOverlayHandler());
        KeyEventHandler.initKeyBindings();
        register(new KeyEventHandler());
    }

    public static void unregisterAll()
    {
        ArrayList<Class<? extends EventHandler>> list = new ArrayList<Class<? extends EventHandler>>(handlers.keySet());
        for (Class<? extends EventHandler> handlerClass : list)
        {
            unregister(handlerClass);
        }
    }

    private static void register(EventHandler handler)
    {
        if (handlers.containsKey(handler.getClass()))
        {
            Journeymap.getLogger().warn("Handler already registered: " + handler.getClass().getName());
            return;
        }

        boolean registered = false;
        for (BusType busType : handler.getBus())
        {
            String name = handler.getClass().getName();
            try
            {
                busType.eventBus.register(handler);
                registered = true;
                Journeymap.getLogger().debug(name + " registered in " + busType);
            }
            catch (Throwable t)
            {
                Journeymap.getLogger().error(name + " registration FAILED in " + busType + ": " + LogFormatter.toString(t));
            }
        }

        if (registered)
        {
            handlers.put(handler.getClass(), handler);
        }
        else
        {
            Journeymap.getLogger().warn("Handler was not registered at all: " + handler.getClass().getName());
        }
    }

    public static void unregister(Class<? extends EventHandler> handlerClass)
    {
        EventHandler handler = handlers.remove(handlerClass);
        if (handler != null)
        {
            EnumSet<BusType> buses = handler.getBus();
            for (BusType busType : handler.getBus())
            {
                String name = handler.getClass().getName();
                try
                {
                    boolean unregistered = false;
                    switch (busType)
                    {
                        case MinecraftForgeBus:
                            MinecraftForge.EVENT_BUS.unregister(handler);
                            unregistered = true;
                            break;
                    }
                    if (unregistered)
                    {
                        Journeymap.getLogger().debug(name + " unregistered from " + busType);
                    }
                }
                catch (Throwable t)
                {
                    Journeymap.getLogger().error(name + " unregistration FAILED from " + busType + ": " + LogFormatter.toString(t));
                }
            }
        }
    }

    public enum BusType
    {
        FMLCommonHandlerBus(FMLCommonHandler.instance().bus()),
        MinecraftForgeBus(MinecraftForge.EVENT_BUS);

        protected final EventBus eventBus;

        private BusType(EventBus eventBus)
        {
            this.eventBus = eventBus;
        }
    }

    public static interface EventHandler
    {
        EnumSet<BusType> getBus();
    }
}
