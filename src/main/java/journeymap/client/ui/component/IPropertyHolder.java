/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.component;

/**
 * Created by Mark on 10/10/2014.
 */
public interface IPropertyHolder<H, T>
{
    public H getProperty();

    public T getPropertyValue();

    public void setPropertyValue(T value);
}
