package journeymap.client.ui.component;

import journeymap.client.api.settings.ExternalSettingEntry.StringBinding;
import journeymap.client.cartography.RGB;
import journeymap.client.render.draw.DrawUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

public class BindingStringInputButton extends Button implements IPropertyHolder<StringBinding, String>
{
    protected StringBinding binding;
    protected String label;
    protected TextField textField;

    public BindingStringInputButton(String label, StringBinding binding)
    {
        super(label);
        this.label = label;
        this.binding = binding;
        this.height = fontRenderer.FONT_HEIGHT + 6;
        this.defaultStyle = false;
        this.drawBackground = false;
        this.drawFrame = false;
        this.textField = new TextField(binding == null ? "" : binding.get(), fontRenderer, 120, this.height - 2);
        this.textField.setMaxStringLength(binding == null ? 64 : Math.max(1, binding.getMaxLength()));
    }

    @Override
    public int getFitWidth(FontRenderer fr)
    {
        int labelWidth = fr.getStringWidth(label);
        int fieldWidth = Math.max(120, Math.min(220, (binding == null ? 64 : binding.getMaxLength()) * 6));
        return labelWidth + fieldWidth + WIDTH_PAD + 12;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY)
    {
        if (!visible)
        {
            return;
        }

        refresh();
        setHovered(mouseOver(mouseX, mouseY));
        int color = !isEnabled() ? RGB.DARK_GRAY_RGB : (isHovered() ? hoverLabelColor : labelColor);
        DrawUtil.drawLabel(label, getX(), getMiddleY(), DrawUtil.HAlign.Left, DrawUtil.VAlign.Middle, null, 0, color, 255, 1, drawLabelShadow);

        int labelWidth = fontRenderer.getStringWidth(label);
        int fieldX = getX() + Math.min(labelWidth + 12, Math.max(90, width / 2));
        int fieldWidth = Math.max(90, getRightX() - fieldX);
        textField.setX(fieldX);
        textField.setY(getY() + 1);
        textField.setWidth(fieldWidth);
        textField.height = Math.max(12, getHeight() - 2);
        textField.setEnabled(isEnabled());
        textField.drawTextBox();
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY)
    {
        if (!mouseOver(mouseX, mouseY))
        {
            textField.setFocused(false);
            return false;
        }

        textField.mouseClicked(mouseX, mouseY, 0);
        return true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY)
    {
        if (!mouseOver(mouseX, mouseY) && textField.isFocused())
        {
            textField.setFocused(false);
        }
    }

    @Override
    public boolean keyTyped(char c, int i)
    {
        if (!textField.isFocused() || !isEnabled())
        {
            return false;
        }

        if (i == Keyboard.KEY_RETURN || i == Keyboard.KEY_NUMPADENTER)
        {
            textField.setFocused(false);
            return true;
        }

        boolean changed = textField.textboxKeyTyped(c, i);
        if (changed && binding != null)
        {
            binding.set(textField.getText());
        }
        return changed;
    }

    @Override
    public void refresh()
    {
        if (binding == null || textField.isFocused())
        {
            return;
        }

        String value = binding.get();
        value = value == null ? "" : value;
        if (!value.equals(textField.getText()))
        {
            textField.setText(value);
        }
    }

    @Override
    public StringBinding getProperty()
    {
        return binding;
    }

    @Override
    public String getPropertyValue()
    {
        return binding == null ? "" : binding.get();
    }

    @Override
    public void setPropertyValue(String value)
    {
        if (binding == null)
        {
            return;
        }

        binding.set(value == null ? "" : value);
        refresh();
    }
}
