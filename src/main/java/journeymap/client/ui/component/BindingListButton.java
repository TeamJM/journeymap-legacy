package journeymap.client.ui.component;

import journeymap.client.api.settings.ExternalSettingEntry.ListBinding;
import journeymap.client.cartography.RGB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class BindingListButton<T> extends Button implements IPropertyHolder<ListBinding<T>, T>
{
    protected String glyph = "\u21D5";
    protected String labelPattern = "%1$s : %2$s %3$s %2$s";
    protected String baseLabel;
    protected ListBinding<T> binding;
    protected List<T> values;

    public BindingListButton(String label, ListBinding<T> binding)
    {
        super("");
        this.baseLabel = label;
        this.binding = binding;
        this.values = binding == null ? new ArrayList<T>() : new ArrayList<T>(binding.getOptions());
        this.disabledLabelColor = RGB.DARK_GRAY_RGB;
        refresh();
    }

    protected String getFormattedLabel(T value)
    {
        return String.format(labelPattern, baseLabel, glyph, binding.getLabel(value));
    }

    protected void nextOption()
    {
        if (binding == null || values.isEmpty())
        {
            return;
        }
        int index = values.indexOf(binding.get()) + 1;
        if (index >= values.size())
        {
            index = 0;
        }
        setPropertyValue(values.get(index));
    }

    protected void prevOption()
    {
        if (binding == null || values.isEmpty())
        {
            return;
        }
        int index = values.indexOf(binding.get()) - 1;
        if (index < 0)
        {
            index = values.size() - 1;
        }
        setPropertyValue(values.get(index));
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY)
    {
        if (super.mousePressed(minecraft, mouseX, mouseY))
        {
            nextOption();
            return true;
        }
        return false;
    }

    @Override
    public int getFitWidth(FontRenderer fr)
    {
        int max = fr.getStringWidth(displayString);
        for (T value : values)
        {
            max = Math.max(max, fr.getStringWidth(getFormattedLabel(value)));
        }
        return max + WIDTH_PAD;
    }

    @Override
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
        if (binding != null && binding.get() != null)
        {
            displayString = getFormattedLabel(binding.get());
        }
    }

    @Override
    public ListBinding<T> getProperty()
    {
        return binding;
    }

    @Override
    public T getPropertyValue()
    {
        return binding == null ? null : binding.get();
    }

    @Override
    public void setPropertyValue(T value)
    {
        if (binding == null)
        {
            return;
        }
        binding.set(value);
        refresh();
    }
}
