/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.theme;

import journeymap.client.Constants;
import journeymap.client.cartography.RGB;
import journeymap.client.properties.PropertiesBase;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.texture.TextureCache;
import journeymap.client.render.texture.TextureImpl;
import journeymap.client.ui.component.BooleanPropertyButton;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Mark on 8/30/2014.
 */
public class ThemeButton extends BooleanPropertyButton
{
    protected Theme theme;
    protected Theme.Control.ButtonSpec buttonSpec;
    protected TextureImpl textureOn;
    protected TextureImpl textureHover;
    protected TextureImpl textureOff;
    protected TextureImpl textureDisabled;
    protected TextureImpl textureIcon;
    protected Integer iconOnColor;
    protected Integer iconOffColor;
    protected Integer iconHoverColor;
    protected Integer iconDisabledColor;
    protected String iconName;
    protected List<String> additionalTooltips;

    public ThemeButton(Theme theme, String rawLabel, String iconName)
    {
        this(theme, Constants.getString(rawLabel), Constants.getString(rawLabel), false, iconName);
    }

    public ThemeButton(Theme theme, String labelOn, String labelOff, boolean toggled, String iconName)
    {
        super(labelOn, labelOff, null, null);
        this.iconName = iconName;
        this.setToggled(toggled);
        updateTheme(theme);
    }

    protected ThemeButton(Theme theme, String labelOn, String labelOff, String iconName, PropertiesBase properties, AtomicBoolean property)
    {
        super(labelOn, labelOff, properties, property);
        this.iconName = iconName;
        updateTheme(theme);
    }

    public void updateTheme(Theme theme)
    {
        this.theme = theme;
        this.buttonSpec = getButtonSpec(theme);
        TextureCache tc = TextureCache.instance();

        if (buttonSpec.useThemeImages)
        {
            String pattern = getPathPattern();
            String prefix = buttonSpec.prefix;
            textureOn = tc.getThemeTexture(theme, String.format(pattern, prefix, "on"));
            textureOff = tc.getThemeTexture(theme, String.format(pattern, prefix, "off"));
            textureHover = tc.getThemeTexture(theme, String.format(pattern, prefix, "hover"));
            textureDisabled = tc.getThemeTexture(theme, String.format(pattern, prefix, "disabled"));
        }
        else
        {
            textureOn = null;
            textureOff = null;
            textureHover = null;
            textureDisabled = null;
        }

        iconOnColor = Theme.getColor(buttonSpec.iconOnColor);
        iconOffColor = Theme.getColor(buttonSpec.iconOffColor);
        iconHoverColor = Theme.getColor(buttonSpec.iconHoverColor);
        iconDisabledColor = Theme.getColor(buttonSpec.iconDisabledColor);

        textureIcon = tc.getThemeTexture(theme, String.format("icon/%s.png", iconName));

        setWidth(buttonSpec.width);
        setHeight(buttonSpec.height);
        setToggled(false, false);
    }

    public boolean hasValidTextures()
    {
        if (buttonSpec.useThemeImages)
        {
            return GL11.glIsTexture(textureOn.getGlTextureId(false))
                    && GL11.glIsTexture(textureOff.getGlTextureId(false));
        }
        else
        {
            return true;
        }
    }

    protected String getPathPattern()
    {
        return "control/%sbutton_%s.png";
    }

    protected Theme.Control.ButtonSpec getButtonSpec(Theme theme)
    {
        return theme.control.button;
    }

    protected TextureImpl getActiveTexture(boolean isMouseOver)
    {
        if (isEnabled())
        {
            TextureImpl activeTexture = isMouseOver ? Mouse.isButtonDown(0) ? textureOn : textureHover : textureOff;
            return activeTexture;
        }
        else
        {
            return textureDisabled;
        }
    }

    protected Integer getIconColor(boolean isMouseOver)
    {
        if (!isEnabled())
        {
            return iconDisabledColor;
        }

        if (isMouseOver)
        {
            return iconHoverColor;
        }

        return toggled ? iconOnColor : iconOffColor;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY)
    {
        if (!isDrawButton())
        {
            return;
        }

        // Check hover
        boolean hover = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
        setMouseOver(hover);

        // Returns 0 if the button is disabled, 1 if the mouse is NOT hovering over this button and 2 if it IS hovering over
        int hoverState = this.getHoverState(hover);
        boolean isMouseOver = (hoverState == 2);

        TextureImpl activeTexture = getActiveTexture(isMouseOver);

        int drawX = getX();
        int drawY = getY();

        if (buttonSpec.useThemeImages)
        {
            float buttonScale = 1f;
            if (buttonSpec.width != activeTexture.getWidth())
            {
                buttonScale = (1f * buttonSpec.width / activeTexture.getWidth());
            }

            // Theme Button Background
            DrawUtil.drawImage(activeTexture, drawX, drawY, false, buttonScale, 0);
        }
        else
        {
            // Use resourcepack textures
            drawNativeButton(minecraft, mouseX, mouseY);
        }

        // Icon
        float iconScale = 1f;
        if (theme.icon.width != textureIcon.getWidth())
        {
            iconScale = (1f * theme.icon.width / textureIcon.getWidth());
        }

        //drawX += (((width - textureIcon.width)/2));
        //drawY += (((height - textureIcon.height)/2));
        //DrawUtil.drawImage(textureIcon, drawX, drawY, false, scale, 0);

        if (!buttonSpec.useThemeImages)
        {
            DrawUtil.drawColoredImage(textureIcon, 255, RGB.BLACK_RGB, drawX + .5, drawY + .5, iconScale, 0);
        }

        Integer iconColor = getIconColor(isMouseOver);
        DrawUtil.drawColoredImage(textureIcon, 255, iconColor, drawX, drawY, iconScale, 0);
    }

    public void drawNativeButton(Minecraft minecraft, int mouseX, int mouseY)
    {
        int magic = 20;
        minecraft.getTextureManager().bindTexture(buttonTextures);
        renderHelper.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int k = this.getHoverState(isMouseOver());
        renderHelper.glEnableBlend();
        //renderHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        renderHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + k * magic, this.width / 2, this.height);
        this.drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2, 46 + k * magic, this.width / 2, this.height);
        this.mouseDragged(minecraft, mouseX, mouseY);
        int l = 14737632;
    }

    public void setAdditionalTooltips(List<String> additionalTooltips)
    {
        this.additionalTooltips = additionalTooltips;
    }

    @Override
    public List<String> getTooltip()
    {
        if (!visible)
        {
            return null;
        }
        List<String> list = super.getTooltip();

        String style = null;
        if (!isEnabled())
        {
            style = buttonSpec.tooltipDisabledStyle;
        }
        else
        {
            style = toggled ? buttonSpec.tooltipOnStyle : buttonSpec.tooltipOffStyle;
        }

        list.add(0, style + displayString);

        if (additionalTooltips != null)
        {
            list.addAll(additionalTooltips);
        }
        return list;
    }
}
