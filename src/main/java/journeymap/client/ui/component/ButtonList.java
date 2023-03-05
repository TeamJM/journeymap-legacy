/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author techbrew 4/2/2014.
 */
public class ButtonList extends ArrayList<Button>
{
    private Layout layout = Layout.Horizontal;
    private Direction direction = Direction.LeftToRight;
    private String label;

    public ButtonList()
    {

    }

    public ButtonList(String label)
    {
        this.label = label;
    }

    public ButtonList(List<GuiButton> buttons)
    {
        for(GuiButton button : buttons)
        {
            if(button instanceof Button)
            {
                add((Button) button);
            }
        }
    }

    public ButtonList(Button... buttons)
    {
        super(Arrays.asList(buttons));
    }

    public int getWidth(int hgap)
    {
        return getWidth(-1, hgap);
    }

    private int getWidth(int buttonWidth, int hgap)
    {
        if (this.isEmpty())
        {
            return 0;
        }

        int total = 0;
        int visible = 0;
        for (Button button : this)
        {
            if (button.isDrawButton())
            {
                if (buttonWidth > 0)
                {
                    total += buttonWidth;
                }
                else
                {
                    total += button.getWidth();
                }
                visible++;
            }
        }

        if (visible > 1)
        {
            total += (hgap * (visible - 1));
        }
        return total;
    }

    public int getHeight(int vgap)
    {
        if (this.isEmpty())
        {
            return 0;
        }

        int total = 0;
        int visible = 0;
        for (Button button : this)
        {
            if (button.isDrawButton())
            {
                total += button.getHeight();
                visible++;
            }
        }

        if (visible > 1)
        {
            total += (vgap * (visible - 1));
        }
        return total;
    }

    public int getLeftX()
    {
        int left = Integer.MAX_VALUE;
        for (Button button : this)
        {
            if (button.isDrawButton())
            {
                left = Math.min(left, button.getX());
            }
        }
        if (left == Integer.MAX_VALUE)
        {
            left = 0;
        }
        return left;
    }

    public int getTopY()
    {
        int top = Integer.MAX_VALUE;
        for (Button button : this)
        {
            if (button.isDrawButton())
            {
                top = Math.min(top, button.getY());
            }
        }
        if (top == Integer.MAX_VALUE)
        {
            top = 0;
        }
        return top;
    }

    public int getBottomY()
    {
        int bottom = Integer.MIN_VALUE;
        for (Button button : this)
        {
            if (button.isDrawButton())
            {
                bottom = Math.max(bottom, button.getY() + button.getHeight());
            }
        }
        if (bottom == Integer.MIN_VALUE)
        {
            bottom = 0;
        }
        return bottom;
    }

    public int getRightX()
    {
        int right = 0;
        for (Button button : this)
        {
            if (button.isDrawButton())
            {
                right = Math.max(right, button.getX() + button.getWidth());
            }
        }
        return right;
    }

    public Button findButton(int id)
    {
        for (Button button : this)
        {
            if (button.id == id)
            {
                return button;
            }
        }
        return null;
    }

    public void setLayout(Layout layout, Direction direction)
    {
        this.layout = layout;
        this.direction = direction;
    }

    public ButtonList layoutHorizontal(int startX, final int y, boolean leftToRight, int hgap)
    {
        this.layout = Layout.Horizontal;
        this.direction = leftToRight ? Direction.LeftToRight : Direction.RightToLeft;

        Button last = null;
        for (Button button : this)
        {
            if (!button.visible)
            {
                continue;
            }

            if (last == null)
            {
                if (leftToRight)
                {
                    button.rightOf(startX).setY(y);
                }
                else
                {
                    button.leftOf(startX).setY(y);
                }
            }
            else
            {
                if (leftToRight)
                {
                    button.rightOf(last, hgap).setY(y);
                }
                else
                {
                    button.leftOf(last, hgap).setY(y);
                }
            }
            last = button;
        }
        return this;
    }

    public ButtonList layoutVertical(final int x, int startY, boolean leftToRight, int vgap)
    {
        this.layout = Layout.Vertical;
        this.direction = leftToRight ? Direction.LeftToRight : Direction.RightToLeft;

        Button last = null;
        for (Button button : this)
        {
            if (last == null)
            {
                if (leftToRight)
                {
                    button.rightOf(x).setY(startY);
                }
                else
                {
                    button.leftOf(x).setY(startY);
                }
            }
            else
            {
                if (leftToRight)
                {
                    button.rightOf(x).below(last, vgap);
                }
                else
                {
                    button.leftOf(x).below(last, vgap);
                }
            }
            last = button;
        }

        return this;
    }

    public ButtonList layoutCenteredVertical(final int x, final int centerY, final boolean leftToRight, final int vgap)
    {
        int height = getHeight(vgap);
        layoutVertical(x, centerY - (height / 2), leftToRight, vgap);
        this.layout = Layout.CenteredVertical;
        return this;
    }

    public ButtonList layoutCenteredHorizontal(final int centerX, final int y, final boolean leftToRight, final int hgap)
    {
        int width = getWidth(hgap);
        layoutHorizontal(centerX - (width / 2), y, leftToRight, hgap);
        this.layout = Layout.CenteredHorizontal;
        return this;
    }

    public ButtonList layoutDistributedHorizontal(final int leftX, final int y, final int rightX, final boolean leftToRight)
    {
        if (this.size() == 0)
        {
            return this;
        }

        int width = getWidth(0);
        int filler = (rightX - leftX) - width;
        int gaps = this.size() - 1;
        int hgap = gaps == 0 ? 0 : (filler >= gaps) ? filler / gaps : 0;

        if (leftToRight)
        {
            layoutHorizontal(leftX, y, true, hgap);
        }
        else
        {
            layoutHorizontal(rightX, y, false, hgap);
        }
        this.layout = Layout.DistributedHorizontal;
        return this;
    }

    public ButtonList layoutFilledHorizontal(FontRenderer fr, final int leftX, final int y, final int rightX, final int hgap, final boolean leftToRight)
    {
        if (this.size() == 0)
        {
            return this;
        }

        this.equalizeWidths(fr);

        int width = getWidth(hgap);
        int remaining = (rightX - leftX) - width;
        if (remaining > this.size())
        {
            int gaps = hgap * (size());
            int area = (rightX - leftX) - gaps;
            int wider = area / size();
            setWidths(wider);
            layoutDistributedHorizontal(leftX, y, rightX, leftToRight);
        }
        else
        {
            layoutCenteredHorizontal((rightX - leftX) / 2, y, leftToRight, hgap);
        }
        this.layout = Layout.FilledHorizontal;
        return this;
    }

    public void setFitWidths(FontRenderer fr)
    {
        fitWidths(fr);
    }

    public boolean isHorizontal()
    {
        return layout != Layout.Vertical && layout != Layout.CenteredVertical;
    }

    public ButtonList setEnabled(boolean enabled)
    {
        for (Button button : this)
        {
            button.setEnabled(enabled);
        }
        return this;
    }

    public ButtonList setOptions(boolean enabled, boolean drawBackground, boolean drawFrame)
    {
        for (Button button : this)
        {
            button.setEnabled(enabled);
            button.setDrawFrame(drawFrame);
            button.setDrawBackground(drawBackground);
        }
        return this;
    }

    public ButtonList setDefaultStyle(boolean defaultStyle)
    {
        for (Button button : this)
        {
            button.setDefaultStyle(defaultStyle);
        }
        return this;
    }

    public ButtonList draw(Minecraft minecraft, int mouseX, int mouseY)
    {
        for (Button button : this)
        {
            button.drawButton(minecraft, mouseX, mouseY);
        }
        return this;
    }

    public void setHeights(int height)
    {
        for (Button button : this)
        {
            button.setHeight(height);
        }
    }

    public void setWidths(int width)
    {
        for (Button button : this)
        {
            button.setWidth(width);
        }
    }

    public void fitWidths(FontRenderer fr)
    {
        for (Button button : this)
        {
            button.fitWidth(fr);
        }
    }

    public void setDrawButtons(boolean draw)
    {
        for (Button button : this)
        {
            button.setDrawButton(draw);
        }
    }

    public void equalizeWidths(FontRenderer fr)
    {
        int maxWidth = 0;
        for (Button button : this)
        {
            if (button.isDrawButton())
            {
                button.fitWidth(fr);
                maxWidth = Math.max(maxWidth, button.getWidth());
            }
        }
        setWidths(maxWidth);
    }

    /**
     * Try to equalize all button widths, but set a max on the total horizontal
     * space that can be used.  If the fit widths still exceed maxTotalWidth,
     * they won't be made smaller; you need to provide more room or remove buttons.
     *
     * @param fr            font renderer
     * @param hgap          horizontal gap
     * @param maxTotalWidth max horizontal space allowed
     */
    public void equalizeWidths(FontRenderer fr, int hgap, int maxTotalWidth)
    {
        int maxWidth = 0;
        for (Button button : this)
        {
            button.fitWidth(fr);
            maxWidth = Math.max(maxWidth, button.getWidth());
        }

        int totalWidth = getWidth(maxWidth, hgap);
        if (totalWidth <= maxTotalWidth)
        {
            setWidths(maxWidth); // same result as equalizeWidths
        }
        else
        {
            totalWidth = getWidth(hgap);
        }

        if (totalWidth < maxTotalWidth)
        {
            // Pad the buttons to get up to maxTotalWidth
            int pad = (maxTotalWidth - totalWidth) / this.size();
            if (pad > 0)
            {
                for (Button button : this)
                {
                    button.setWidth(button.getWidth() + pad);
                }
            }
        }
    }

    public int getVisibleButtonCount()
    {
        int count = 0;
        for (Button button : this)
        {
            if (button.visible)
            {
                count++;
            }
        }
        return count;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public ButtonList reverse()
    {
        Collections.reverse(this);
        return this;
    }

    public enum Layout
    {
        Horizontal, Vertical, CenteredHorizontal, CenteredVertical, DistributedHorizontal, FilledHorizontal
    }

    public enum Direction
    {
        LeftToRight, RightToLeft
    }
}
