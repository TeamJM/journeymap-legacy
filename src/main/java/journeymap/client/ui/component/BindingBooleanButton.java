package journeymap.client.ui.component;

import cpw.mods.fml.client.config.GuiUtils;
import journeymap.client.cartography.RGB;
import journeymap.client.api.settings.BooleanSettingBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

public class BindingBooleanButton extends Button implements IPropertyHolder<BooleanSettingBinding, Boolean>
{
    protected int boxWidth = 11;
    protected String glyph = "\u2714";
    protected BooleanSettingBinding binding;
    protected boolean toggled;

    public BindingBooleanButton(String displayString, BooleanSettingBinding binding)
    {
        super(displayString);
        this.binding = binding;
        this.height = fontRenderer.FONT_HEIGHT + 2;
        this.width = getFitWidth(fontRenderer);
        refresh();
    }

    @Override
    public int getFitWidth(FontRenderer fr)
    {
        return super.getFitWidth(fr) + this.boxWidth + 2;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY)
    {
        if (!this.visible)
        {
            return;
        }

        this.setHovered(isEnabled() && mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height);

        int yoffset = (this.height - this.boxWidth) / 2;
        GuiUtils.drawContinuousTexturedBox(buttonTextures, this.xPosition, this.yPosition + yoffset, 0, 46, this.boxWidth,
                this.boxWidth, 200, 20, 2, 3, 2, 2, this.zLevel);
        this.mouseDragged(mc, mouseX, mouseY);

        int color = 14737632;
        if (this.isHovered())
        {
            color = 16777120;
        }
        else if (!isEnabled())
        {
            color = RGB.DARK_GRAY_RGB;
        }
        else if (labelColor != null)
        {
            color = labelColor;
        }
        else if (packedFGColour != 0)
        {
            color = packedFGColour;
        }

        if (this.toggled)
        {
            this.drawCenteredString(fontRenderer, glyph, this.xPosition + this.boxWidth / 2 + 1, this.yPosition + 1 + yoffset, color);
        }

        this.drawString(fontRenderer, displayString, xPosition + this.boxWidth + 4, yPosition + 2 + yoffset, color);
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY)
    {
        if (this.isEnabled() && this.visible && mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height)
        {
            setPropertyValue(!toggled);
            return true;
        }

        return false;
    }

    @Override
    public boolean keyTyped(char c, int i)
    {
        if (this.isEnabled() && this.isMouseOver() && i == Keyboard.KEY_SPACE)
        {
            setPropertyValue(!toggled);
            return true;
        }
        return false;
    }

    @Override
    public void refresh()
    {
        if (binding != null)
        {
            toggled = binding.get();
        }
    }

    @Override
    public BooleanSettingBinding getProperty()
    {
        return binding;
    }

    @Override
    public Boolean getPropertyValue()
    {
        return binding == null ? Boolean.FALSE : binding.get();
    }

    @Override
    public void setPropertyValue(Boolean value)
    {
        if (binding == null || value == null)
        {
            return;
        }

        binding.set(value);
        toggled = binding.get();
    }
}
