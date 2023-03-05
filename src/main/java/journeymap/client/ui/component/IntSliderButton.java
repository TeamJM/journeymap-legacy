/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.component;

import cpw.mods.fml.client.config.GuiUtils;
import journeymap.client.cartography.RGB;
import journeymap.client.properties.PropertiesBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Mark on 9/29/2014.
 */
public class IntSliderButton extends Button implements IPropertyHolder<AtomicInteger, Integer>
{
    public String prefix = "";
    /**
     * Is this slider control being dragged.
     */
    public boolean dragging = false;
    public int minValue = 0;
    public int maxValue = 0;
    public String suffix = "";
    public boolean drawString = true;
    PropertiesBase properties;
    AtomicInteger property;

    public IntSliderButton(PropertiesBase properties, AtomicInteger property, String prefix, String suf, int minVal, int maxVal, boolean drawStr)
    {
        super(prefix);
        minValue = minVal;
        maxValue = maxVal;
        this.prefix = prefix;
        suffix = suf;
        this.property = property;
        this.properties = properties;
        setValue(property.get());
        super.disabledLabelColor = RGB.DARK_GRAY_RGB;
    }

    /**
     * Returns 0 if the button is disabled, 1 if the mouse is NOT hovering over this button and 2 if it IS hovering over
     * this button.
     */
    /**
     * Returns 0 if the button is disabled, 1 if the mouse is NOT hovering over this button and 2 if it IS hovering over
     * this button.
     */
    public int getHoverState(boolean par1)
    {
        return 0;
    }

    /**
     * Fired when the mouse button is dragged. Equivalent of MouseListener.mouseDragged(MouseEvent e).
     */
    /**
     * Fired when the mouse button is dragged. Equivalent of MouseListener.mouseDragged(MouseEvent e).
     */
    protected void mouseDragged(Minecraft par1Minecraft, int par2, int par3)
    {
        if (this.visible && this.isEnabled())
        {
            if (this.dragging)
            {
                setSliderValue((par2 - (this.xPosition + 4)) / (float) (this.width - 8));
            }

            int k = this.getHoverState(this.isEnabled());

            if (this.isEnabled() || this.dragging)
            {
                renderHelper.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

                double sliderValue = getSliderValue();
                GuiUtils.drawContinuousTexturedBox(buttonTextures, this.xPosition + 1 + (int) (sliderValue * (float) (this.width - 10)), this.yPosition + 1, 0, 66, 8, height - 2, 200, 20, 2, 3, 2, 2, this.zLevel);
                //this.drawTexturedModalRect(this.xPosition + (int)(this.sliderValue * (float)(this.width - 8)), this.yPosition, 0, 66, 4, height);
                //this.drawTexturedModalRect(this.xPosition + (int)(this.sliderValue * (float)(this.width - 8)) + 4, this.yPosition, 196, 66, 4, height);

            }
        }
    }

    /**
     * Returns true if the mouse has been pressed on this control. Equivalent of MouseListener.mousePressed(MouseEvent
     * e).
     */
    /**
     * Returns true if the mouse has been pressed on this control. Equivalent of MouseListener.mousePressed(MouseEvent
     * e).
     */
    public boolean mousePressed(Minecraft par1Minecraft, int par2, int par3)
    {
        if (super.mousePressed(par1Minecraft, par2, par3))
        {
            setSliderValue((float) (par2 - (this.xPosition + 4)) / (float) (this.width - 8));
            this.dragging = true;
            return true;
        }
        else
        {
            return false;
        }
    }

    public double getSliderValue()
    {
        return (property.get() - minValue * 1d) / (maxValue - minValue);
    }

    public void setSliderValue(double sliderValue)
    {
        if (sliderValue < 0.0F)
        {
            sliderValue = 0.0F;
        }

        if (sliderValue > 1.0F)
        {
            sliderValue = 1.0F;
        }

        int intVal = (int) Math.round(sliderValue * (maxValue - minValue) + minValue);
        setValue(intVal);
    }

    public void updateLabel()
    {
        if (drawString)
        {
            displayString = prefix + property.get() + suffix;
        }
    }

    /**
     * Fired when the mouse button is released. Equivalent of MouseListener.mouseReleased(MouseEvent e).
     */
    /**
     * Fired when the mouse button is released. Equivalent of MouseListener.mouseReleased(MouseEvent e).
     */
    public void mouseReleased(int par1, int par2)
    {
        if (this.dragging)
        {
            this.dragging = false;
            if (properties != null)
            {
                properties.save();
            }
        }
    }

    @Override
    public int getFitWidth(FontRenderer fr)
    {
        int max = fr.getStringWidth(prefix + minValue + suffix);
        max = Math.max(max, fr.getStringWidth(prefix + maxValue + suffix));
        return max + WIDTH_PAD;
    }

    public boolean keyTyped(char c, int i)
    {
        if (this.isEnabled())
        {
            if (i == Keyboard.KEY_LEFT || i == Keyboard.KEY_DOWN || i == Keyboard.KEY_SUBTRACT)
            {
                setValue(Math.max(minValue, getValue() - 1));
                return true;
            }
            if (i == Keyboard.KEY_RIGHT || i == Keyboard.KEY_UP || i == Keyboard.KEY_ADD)
            {
                setValue(Math.min(maxValue, getValue() + 1));
                return true;
            }
        }
        return false;
    }

    public int getValue()
    {
        return this.property.get();
    }

    public void setValue(int value)
    {
        value = Math.min(value, maxValue);
        value = Math.max(value, minValue);
        if (property.get() != value)
        {
            property.set(value);
            if (!dragging && properties != null)
            {
                properties.save();
            }
        }
        updateLabel();
    }

    @Override
    public AtomicInteger getProperty()
    {
        return property;
    }

    @Override
    public Integer getPropertyValue()
    {
        return property.get();
    }

    @Override
    public void setPropertyValue(Integer value)
    {
        if (property == null)
        {
            return;
        }
        setValue(value);
    }
}
