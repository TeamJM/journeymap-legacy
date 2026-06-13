package journeymap.client.ui.component;

import cpw.mods.fml.client.config.GuiUtils;
import journeymap.client.api.settings.ExternalSettingEntry.DoubleBinding;
import journeymap.client.cartography.RGB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

import java.util.Locale;

public class BindingDoubleSliderButton extends Button implements IPropertyHolder<DoubleBinding, Double>
{
    protected String prefix = "";
    protected boolean dragging;
    protected DoubleBinding binding;

    public BindingDoubleSliderButton(String prefix, DoubleBinding binding)
    {
        super(prefix);
        this.prefix = prefix;
        this.binding = binding;
        this.disabledLabelColor = RGB.DARK_GRAY_RGB;
        refresh();
    }

    protected double getMinValue()
    {
        return binding == null ? 0D : binding.getMinValue();
    }

    protected double getMaxValue()
    {
        return binding == null ? 0D : binding.getMaxValue();
    }

    protected double getStep()
    {
        return binding == null ? 0.1D : Math.max(0.0001D, binding.getStep());
    }

    protected int getPrecision()
    {
        return binding == null ? 1 : Math.max(0, binding.getPrecision());
    }

    protected String getSuffix()
    {
        return binding == null || binding.getSuffix() == null ? "" : binding.getSuffix();
    }

    protected double getSliderValue()
    {
        return (getPropertyValue() - getMinValue()) / Math.max(0.0001D, getMaxValue() - getMinValue());
    }

    protected void setSliderValue(double sliderValue)
    {
        if (sliderValue < 0D)
        {
            sliderValue = 0D;
        }
        if (sliderValue > 1D)
        {
            sliderValue = 1D;
        }
        double value = sliderValue * (getMaxValue() - getMinValue()) + getMinValue();
        setPropertyValue(value);
    }

    protected double roundToStep(double value)
    {
        double min = getMinValue();
        double max = getMaxValue();
        double step = getStep();
        double snapped = min + Math.round((value - min) / step) * step;
        double scale = Math.pow(10D, getPrecision());
        snapped = Math.round(snapped * scale) / scale;
        return Math.max(min, Math.min(max, snapped));
    }

    protected String formatValue(double value)
    {
        return String.format(Locale.ROOT, "%1$." + getPrecision() + "f", value);
    }

    protected void updateLabel()
    {
        this.displayString = prefix + formatValue(getPropertyValue()) + getSuffix();
    }

    @Override
    protected void mouseDragged(Minecraft minecraft, int mouseX, int mouseY)
    {
        if (this.visible && this.isEnabled())
        {
            if (this.dragging)
            {
                setSliderValue((mouseX - (this.xPosition + 4)) / (float) (this.width - 8));
            }

            if (this.isEnabled() || this.dragging)
            {
                renderHelper.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                double sliderValue = getSliderValue();
                GuiUtils.drawContinuousTexturedBox(buttonTextures,
                        this.xPosition + 1 + (int) (sliderValue * (float) (this.width - 10)), this.yPosition + 1, 0, 66, 8,
                        height - 2, 200, 20, 2, 3, 2, 2, this.zLevel);
            }
        }
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY)
    {
        if (super.mousePressed(minecraft, mouseX, mouseY))
        {
            setSliderValue((mouseX - (this.xPosition + 4)) / (float) (this.width - 8));
            this.dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY)
    {
        this.dragging = false;
    }

    @Override
    public int getFitWidth(FontRenderer fr)
    {
        int max = fr.getStringWidth(prefix + formatValue(getMinValue()) + getSuffix());
        max = Math.max(max, fr.getStringWidth(prefix + formatValue(getMaxValue()) + getSuffix()));
        return max + WIDTH_PAD;
    }

    @Override
    public boolean keyTyped(char c, int i)
    {
        if (this.isEnabled() && this.isMouseOver())
        {
            if (i == Keyboard.KEY_LEFT || i == Keyboard.KEY_DOWN || i == Keyboard.KEY_SUBTRACT)
            {
                setPropertyValue(getPropertyValue() - getStep());
                return true;
            }
            if (i == Keyboard.KEY_RIGHT || i == Keyboard.KEY_UP || i == Keyboard.KEY_ADD)
            {
                setPropertyValue(getPropertyValue() + getStep());
                return true;
            }
        }
        return false;
    }

    @Override
    public void refresh()
    {
        updateLabel();
    }

    @Override
    public DoubleBinding getProperty()
    {
        return binding;
    }

    @Override
    public Double getPropertyValue()
    {
        return binding == null ? 0D : binding.get();
    }

    @Override
    public void setPropertyValue(Double value)
    {
        if (binding == null || value == null)
        {
            return;
        }

        binding.set(roundToStep(value));
        updateLabel();
    }
}
