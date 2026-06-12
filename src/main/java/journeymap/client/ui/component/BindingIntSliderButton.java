package journeymap.client.ui.component;

import cpw.mods.fml.client.config.GuiUtils;
import journeymap.client.api.settings.IntSettingBinding;
import journeymap.client.cartography.RGB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

public class BindingIntSliderButton extends Button implements IPropertyHolder<IntSettingBinding, Integer>
{
    protected String prefix = "";
    protected boolean dragging;
    protected IntSettingBinding binding;

    public BindingIntSliderButton(String prefix, IntSettingBinding binding)
    {
        super(prefix);
        this.prefix = prefix;
        this.binding = binding;
        this.disabledLabelColor = RGB.DARK_GRAY_RGB;
        refresh();
    }

    protected int getMinValue()
    {
        return binding == null ? 0 : binding.getMinValue();
    }

    protected int getMaxValue()
    {
        return binding == null ? 0 : binding.getMaxValue();
    }

    protected int getStep()
    {
        return binding == null ? 1 : Math.max(1, binding.getStep());
    }

    protected String getSuffix()
    {
        return binding == null || binding.getSuffix() == null ? "" : binding.getSuffix();
    }

    protected double getSliderValue()
    {
        return (getPropertyValue() - getMinValue()) * 1D / Math.max(1, getMaxValue() - getMinValue());
    }

    protected int clampToStep(double sliderValue)
    {
        int min = getMinValue();
        int max = getMaxValue();
        if (sliderValue < 0D)
        {
            sliderValue = 0D;
        }
        if (sliderValue > 1D)
        {
            sliderValue = 1D;
        }

        int raw = (int) Math.round(sliderValue * (max - min) + min);
        int step = getStep();
        int snapped = min + (int) Math.round((raw - min) * 1D / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    protected void setSliderValue(double sliderValue)
    {
        setPropertyValue(clampToStep(sliderValue));
    }

    protected void updateLabel()
    {
        this.displayString = prefix + getPropertyValue() + getSuffix();
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY)
    {
        super.drawButton(minecraft, mouseX, mouseY);
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
        int max = fr.getStringWidth(prefix + getMinValue() + getSuffix());
        max = Math.max(max, fr.getStringWidth(prefix + getMaxValue() + getSuffix()));
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
    public IntSettingBinding getProperty()
    {
        return binding;
    }

    @Override
    public Integer getPropertyValue()
    {
        return binding == null ? 0 : binding.get();
    }

    @Override
    public void setPropertyValue(Integer value)
    {
        if (binding == null || value == null)
        {
            return;
        }

        int min = getMinValue();
        int max = getMaxValue();
        int step = getStep();
        int clamped = Math.max(min, Math.min(max, value));
        int snapped = min + (int) Math.round((clamped - min) * 1D / step) * step;
        binding.set(Math.max(min, Math.min(max, snapped)));
        updateLabel();
    }
}
