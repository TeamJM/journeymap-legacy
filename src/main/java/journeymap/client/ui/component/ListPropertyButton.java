/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.component;

import journeymap.client.cartography.RGB;
import journeymap.client.properties.PropertiesBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for a property that has a list of values.
 */
public class ListPropertyButton<T> extends Button implements IPropertyHolder<AtomicReference<T>, T>
{
    protected final PropertiesBase properties;
    protected final AtomicReference<T> valueHolder;
    protected final List<T> values;
    protected final String baseLabel;
    protected final String glyph = "\u21D5";
    protected final String labelPattern = "%1$s : %2$s %3$s %2$s";

    public ListPropertyButton(Collection<T> values, String label, PropertiesBase properties, AtomicReference<T> valueHolder)
    {
        super("");
        this.valueHolder = valueHolder;
        this.properties = properties;
        this.values = new ArrayList<T>(values);
        this.baseLabel = label;
        setValue(valueHolder.get());
        disabledLabelColor = RGB.DARK_GRAY_RGB;
    }

    public void setValue(T value)
    {
        if (!valueHolder.get().equals(value))
        {
            valueHolder.set(value);
            if (properties != null)
            {
                properties.save();
            }
        }
        displayString = getFormattedLabel(value.toString());
    }

    public AtomicReference<T> getValueHolder()
    {
        return valueHolder;
    }

    public void nextOption()
    {
        int index = values.indexOf(valueHolder.get()) + 1;
        if (index == values.size())
        {
            index = 0;
        }
        setValue(values.get(index));
    }

    public void prevOption()
    {
        int index = values.indexOf(valueHolder.get()) - 1;
        if (index == -1)
        {
            index = values.size() - 1;
        }
        setValue(values.get(index));
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, int i, int j)
    {
        if (super.mousePressed(minecraft, i, j))
        {
            nextOption();
            return true;
        }
        return false;
    }

    protected String getFormattedLabel(String value)
    {
        return String.format(labelPattern, baseLabel, glyph, value);
    }

    @Override
    public int getFitWidth(FontRenderer fr)
    {
        int max = fr.getStringWidth(displayString);
        for (T value : values)
        {
            max = Math.max(max, fr.getStringWidth(getFormattedLabel(value.toString())));
        }
        return max + WIDTH_PAD;
    }

    public boolean keyTyped(char c, int i)
    {
        if (this.isMouseOver())
        {
            if (i == Keyboard.KEY_LEFT || i == Keyboard.KEY_DOWN || i == Keyboard.KEY_SUBTRACT)
            {
                prevOption();
                return true;
            }
            if (i == Keyboard.KEY_RIGHT || i == Keyboard.KEY_UP || i == Keyboard.KEY_ADD)
            {
                nextOption();
                return true;
            }
        }
        return false;
    }

    @Override
    public void refresh()
    {
        setValue(valueHolder.get());
    }

    @Override
    public AtomicReference<T> getProperty()
    {
        return valueHolder;
    }

    @Override
    public T getPropertyValue()
    {
        return valueHolder.get();
    }

    @Override
    public void setPropertyValue(T value)
    {
        if (valueHolder == null)
        {
            return;
        }
        valueHolder.set(value);
        if (properties != null)
        {
            properties.save();
        }
    }
}
