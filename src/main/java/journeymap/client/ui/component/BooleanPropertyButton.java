/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.component;

import journeymap.client.properties.PropertiesBase;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Button that wraps and syncs with an AtomicBoolean value owned by a config instance.
 */
public class BooleanPropertyButton extends OnOffButton implements IPropertyHolder<AtomicBoolean, Boolean>
{
    final PropertiesBase properties;
    final AtomicBoolean valueHolder;

    public BooleanPropertyButton(String labelOn, String labelOff, PropertiesBase properties, AtomicBoolean valueHolderParam)
    {
        super(labelOn, labelOff, (valueHolderParam != null) && valueHolderParam.get());
        this.valueHolder = valueHolderParam;
        this.properties = properties;
    }

    public AtomicBoolean getValueHolder()
    {
        return valueHolder;
    }

    @Override
    public void toggle()
    {
        if (isEnabled())
        {
            if (properties != null)
            {
                setToggled(properties.toggle(valueHolder));
            }
            else
            {
                setToggled(!toggled);
            }
        }
    }

    @Override
    public void refresh()
    {
        if (valueHolder != null)
        {
            setToggled(valueHolder.get());
        }
    }

    @Override
    public Boolean getPropertyValue()
    {
        return valueHolder.get();
    }

    @Override
    public void setPropertyValue(Boolean value)
    {
        if (valueHolder == null)
        {
            return;
        }
        valueHolder.set(value);
        properties.save();
    }

    @Override
    public AtomicBoolean getProperty()
    {
        return valueHolder;
    }
}
