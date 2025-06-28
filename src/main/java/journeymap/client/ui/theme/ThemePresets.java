/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.theme;

import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Themes that come.
 */
public class ThemePresets
{
    public static final Theme THEME_VICTORIAN = createVictorian();
    public static final Theme THEME_PURIST = createPurist();
    public static final String DIR_VAULT = "Vault";

    public static List<Theme> getPresets()
    {
        return Arrays.asList(THEME_PURIST, THEME_VICTORIAN);
    }

    public static List<String> getPresetDirs()
    {
        return Arrays.asList(THEME_PURIST.directory, THEME_VICTORIAN.directory, DIR_VAULT);
    }

    private static Theme createVictorian()
    {
        Theme theme = new Theme();
        theme.name = "Victorian";
        theme.author = "techbrew";
        theme.directory = "Victorian";

        String vicRed = "#A56B46";
        String controlColor = Theme.toHexColor(new Color(132, 125, 102));
        String veryDarkGray = Theme.toHexColor(new Color(34, 34, 34));

        Theme.ImageSpec icon = theme.icon;
        icon.height = 24;
        icon.width = 24;

        {
            Theme.Control.ButtonSpec button = theme.control.button;
            button.useThemeImages = true;
            button.width = 24;
            button.height = 24;
            button.tooltipOnStyle = EnumChatFormatting.WHITE.toString();
            button.tooltipOffStyle = EnumChatFormatting.WHITE.toString();
            button.tooltipDisabledStyle = EnumChatFormatting.DARK_GRAY.toString() + EnumChatFormatting.ITALIC.toString();
            button.iconOnColor = controlColor;
            button.iconOffColor = controlColor;
            button.iconHoverColor = Theme.toHexColor(Color.white);
            button.iconDisabledColor = Theme.toHexColor(Color.darkGray);
        }

        {
            Theme.Control.ButtonSpec toggle = theme.control.toggle;
            toggle.useThemeImages = true;
            toggle.width = 24;
            toggle.height = 24;
            toggle.tooltipOnStyle = EnumChatFormatting.WHITE.toString();
            toggle.tooltipOffStyle = EnumChatFormatting.WHITE.toString();
            toggle.tooltipDisabledStyle = EnumChatFormatting.DARK_GRAY.toString() + EnumChatFormatting.ITALIC.toString();
            toggle.iconOnColor = Theme.toHexColor(Color.darkGray);
            toggle.iconOffColor = controlColor;
            toggle.iconHoverColor = Theme.toHexColor(Color.white);
            toggle.iconDisabledColor = Theme.toHexColor(Color.darkGray);
        }

        {
            Theme.Container.Toolbar.ToolbarSpec hToolbar = theme.container.toolbar.horizontal;
            hToolbar.useThemeImages = true;
            hToolbar.prefix = "h";
            hToolbar.margin = 4;
            hToolbar.padding = 4;
            hToolbar.begin = hToolbar.end = new Theme.ImageSpec(8, 32);
            hToolbar.inner = new Theme.ImageSpec(28, 32);
        }

        {
            Theme.Container.Toolbar.ToolbarSpec vToolbar = theme.container.toolbar.vertical;
            vToolbar.useThemeImages = true;
            vToolbar.prefix = "v";
            vToolbar.margin = 4;
            vToolbar.padding = 4;
            vToolbar.begin = vToolbar.end = new Theme.ImageSpec(32, 8);
            vToolbar.inner = new Theme.ImageSpec(32, 28);
        }

        Theme.LabelSpec commonLabel = new Theme.LabelSpec();
        commonLabel.backgroundAlpha = 200;
        commonLabel.foregroundAlpha = 255;
        commonLabel.backgroundColor = veryDarkGray;
        commonLabel.foregroundColor = Theme.toHexColor(Color.lightGray);
        commonLabel.shadow = true;

        {
            Theme.Fullscreen fullscreen = theme.fullscreen;
            fullscreen.mapBackgroundColor = veryDarkGray;
            fullscreen.statusLabel = new Theme.LabelSpec();
            fullscreen.statusLabel.backgroundAlpha = 235;
            fullscreen.statusLabel.foregroundAlpha = 255;
            fullscreen.statusLabel.backgroundColor = veryDarkGray;
            fullscreen.statusLabel.foregroundColor = Theme.toHexColor(Color.lightGray);
            fullscreen.statusLabel.shadow = true;
        }

        Theme.LabelSpec compassLabel = new Theme.LabelSpec();
        compassLabel.backgroundAlpha = 0;
        compassLabel.foregroundAlpha = 255;
        compassLabel.backgroundColor = veryDarkGray;
        compassLabel.foregroundColor = controlColor;
        compassLabel.shadow = false;

        {
            Theme.Minimap.MinimapSquare minimapSquare = theme.minimap.square;
            minimapSquare.prefix = "vic_";
            minimapSquare.margin = 4;
            minimapSquare.labelBottomInside = false;
            minimapSquare.labelTopInside = true;
            minimapSquare.labelTopMargin = 4;
            minimapSquare.labelBottomMargin = 8;
            minimapSquare.top = minimapSquare.bottom = new Theme.ImageSpec(1, 20);
            minimapSquare.left = minimapSquare.right = new Theme.ImageSpec(20, 1);
            minimapSquare.topLeft = minimapSquare.topRight = minimapSquare.bottomRight = minimapSquare.bottomLeft = new Theme.ImageSpec(20, 20);
            minimapSquare.frameColor = controlColor;
            minimapSquare.fpsLabel = commonLabel;
            minimapSquare.locationLabel = commonLabel;
            minimapSquare.biomeLabel = commonLabel;
            minimapSquare.timeLabel = commonLabel;
            minimapSquare.compassLabel = compassLabel;
            minimapSquare.compassPoint = new Theme.ImageSpec(28, 28);
            minimapSquare.compassPointLabelPad = 6;
            minimapSquare.compassPointColor = Theme.toHexColor(Color.white);
            minimapSquare.reticleThickness = 2;
            minimapSquare.reticleHeadingThickness = 2.75;
            minimapSquare.reticleOffset = -3;
            minimapSquare.reticleColor = vicRed;
        }

        {
            Theme.Minimap.MinimapCircle minimapCircle = theme.minimap.circle;
            minimapCircle.prefix = "";
            minimapCircle.margin = 4;
            minimapCircle.labelTopMargin = 4;
            minimapCircle.labelBottomMargin = 4;
            minimapCircle.frameColor = vicRed;
            minimapCircle.fpsLabel = commonLabel;
            minimapCircle.locationLabel = commonLabel;
            minimapCircle.biomeLabel = commonLabel;
            minimapCircle.timeLabel = commonLabel;
            minimapCircle.compassLabel = compassLabel;
            minimapCircle.compassPointOffset = -3;
            minimapCircle.compassPoint = new Theme.ImageSpec(28, 28);
            minimapCircle.compassPointLabelPad = 3;
            minimapCircle.compassPointColor = Theme.toHexColor(Color.white);
            minimapCircle.reticleColor = vicRed;
            minimapCircle.reticleOffset = -3;
            minimapCircle.reticleThickness = 2;
            minimapCircle.reticleHeadingThickness = 2.75;
            minimapCircle.waypointOffset = -2.5;
        }

        return theme;
    }

    private static Theme createPurist()
    {
        Theme theme = new Theme();
        theme.name = "Purist";
        theme.author = "techbrew";
        theme.directory = "Victorian";

        String veryDarkGray = Theme.toHexColor(new Color(34, 34, 34));

        Theme.LabelSpec commonLabel = new Theme.LabelSpec();
        commonLabel.backgroundAlpha = 200;
        commonLabel.foregroundAlpha = 255;
        commonLabel.backgroundColor = Theme.toHexColor(Color.black);
        commonLabel.foregroundColor = Theme.toHexColor(Color.lightGray);
        commonLabel.shadow = true;

        Theme.LabelSpec compassLabel = new Theme.LabelSpec();
        compassLabel.backgroundAlpha = 0;
        compassLabel.foregroundAlpha = 255;
        compassLabel.backgroundColor = Theme.toHexColor(Color.black);
        compassLabel.foregroundColor = Theme.toHexColor(Color.lightGray);
        compassLabel.shadow = true;

        {
            Theme.ImageSpec icon = theme.icon;
            icon.height = 20;
            icon.width = 20;
        }

        {
            Theme.Control.ButtonSpec button = theme.control.button;
            button.useThemeImages = false;
            button.width = 20;
            button.height = 20;
            button.tooltipOnStyle = EnumChatFormatting.WHITE.toString();
            button.tooltipOffStyle = EnumChatFormatting.WHITE.toString();
            button.tooltipDisabledStyle = EnumChatFormatting.DARK_GRAY.toString() + EnumChatFormatting.ITALIC.toString();
            button.iconOnColor = Theme.toHexColor(new Color(16777120));
            button.iconOffColor = Theme.toHexColor(new Color(14737632));
            button.iconHoverColor = Theme.toHexColor(new Color(16777120));
            button.iconDisabledColor = Theme.toHexColor(new Color(10526880));
        }

        {
            Theme.Control.ButtonSpec toggle = theme.control.toggle;
            toggle.useThemeImages = false;
            toggle.width = 20;
            toggle.height = 20;
            toggle.tooltipOnStyle = EnumChatFormatting.WHITE.toString();
            toggle.tooltipOffStyle = EnumChatFormatting.WHITE.toString();
            toggle.tooltipDisabledStyle = EnumChatFormatting.DARK_GRAY.toString() + EnumChatFormatting.ITALIC.toString();
            toggle.iconOnColor = Theme.toHexColor(Color.white);
            toggle.iconOffColor = Theme.toHexColor(Color.gray);
            toggle.iconHoverColor = Theme.toHexColor(new Color(16777120));
            toggle.iconDisabledColor = Theme.toHexColor(Color.darkGray);
        }

        {
            Theme.Container.Toolbar.ToolbarSpec hToolbar = theme.container.toolbar.horizontal;
            hToolbar.useThemeImages = false;
            hToolbar.prefix = "h_";
            hToolbar.margin = 4;
            hToolbar.padding = 2;
            hToolbar.begin = hToolbar.end = new Theme.ImageSpec(4, 24);
            hToolbar.inner = new Theme.ImageSpec(24, 24);
        }

        {
            Theme.Container.Toolbar.ToolbarSpec vToolbar = theme.container.toolbar.vertical;
            vToolbar.useThemeImages = false;
            vToolbar.prefix = "v_";
            vToolbar.margin = 4;
            vToolbar.padding = 2;
            vToolbar.begin = vToolbar.end = new Theme.ImageSpec(24, 4);
            vToolbar.inner = new Theme.ImageSpec(24, 24);
        }

        {
            Theme.Fullscreen fullscreen = theme.fullscreen;
            fullscreen.mapBackgroundColor = veryDarkGray;
            fullscreen.statusLabel.backgroundAlpha = 235;
            fullscreen.statusLabel.foregroundAlpha = 255;
            fullscreen.statusLabel.backgroundColor = veryDarkGray;
            fullscreen.statusLabel.foregroundColor = Theme.toHexColor(Color.lightGray);
            fullscreen.statusLabel.shadow = true;
        }

        {
            Theme.Minimap.MinimapSquare minimapSquare = theme.minimap.square;
            minimapSquare.prefix = "pur_";
            minimapSquare.margin = 8;
            minimapSquare.labelTopInside = false;
            minimapSquare.labelTopMargin = 4;
            minimapSquare.labelBottomInside = false;
            minimapSquare.labelBottomMargin = 4;
            minimapSquare.top = minimapSquare.bottom = new Theme.ImageSpec(1, 8);
            minimapSquare.left = minimapSquare.right = new Theme.ImageSpec(8, 1);
            minimapSquare.topLeft = minimapSquare.topRight = minimapSquare.bottomRight = minimapSquare.bottomLeft = new Theme.ImageSpec(8, 8);
            minimapSquare.frameColor = Theme.toHexColor(Color.lightGray);
            minimapSquare.fpsLabel = commonLabel;
            minimapSquare.locationLabel = commonLabel;
            minimapSquare.biomeLabel = commonLabel;
            minimapSquare.timeLabel = commonLabel;
            minimapSquare.compassLabel = compassLabel;
            minimapSquare.compassPoint = null;
            minimapSquare.reticleColor = Theme.toHexColor(Color.lightGray);
        }

        {
            Theme.Minimap.MinimapCircle minimapCircle = theme.minimap.circle;
            minimapCircle.prefix = "";
            minimapCircle.margin = 8;
            minimapCircle.labelTopMargin = 4;
            minimapCircle.labelBottomMargin = 4;
            minimapCircle.frameColor = Theme.toHexColor(Color.lightGray);
            minimapCircle.fpsLabel = commonLabel;
            minimapCircle.locationLabel = commonLabel;
            minimapCircle.biomeLabel = commonLabel;
            minimapCircle.timeLabel = commonLabel;
            minimapCircle.compassLabel = compassLabel;
            minimapCircle.compassPoint = null;
            minimapCircle.reticleColor = Theme.toHexColor(Color.lightGray);
            minimapCircle.reticleOffset = -3;
            minimapCircle.waypointOffset = -2.5;
        }

        return theme;
    }
}
