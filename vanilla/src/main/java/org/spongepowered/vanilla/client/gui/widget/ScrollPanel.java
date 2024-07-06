/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.vanilla.client.gui.widget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

/**
 * Credit: MinecraftForge
 * Changes: Minor tweaks, fixed scroll limits able to hit negative
 */
public abstract class ScrollPanel extends AbstractContainerEventHandler implements Widget {

    private final Minecraft client;
    protected final int width;
    protected final int height;
    protected final int top;
    protected final int bottom;
    protected final int right;
    protected final int left;
    private boolean scrolling;
    protected float scrollDistance;
    protected final int border = 4;

    private final int barWidth = 6;
    private final int barLeft;

    public ScrollPanel(final Minecraft client, final int width, final int height, final int top, final int left) {
        this.client = client;
        this.width = width;
        this.height = height;
        this.top = top;
        this.left = left;
        this.bottom = height + this.top;
        this.right = width + this.left;
        this.barLeft = this.left + this.width - this.barWidth;
    }

    protected abstract int getContentHeight();

    protected void drawBackground() {
    }

    /**
     * Draw anything special on the screen. GL_SCISSOR is enabled for anything that
     * is rendered outside of the view box. Do not mess with SCISSOR unless you support this.
     *
     * @param mouseX
     * @param mouseY
     */
    protected abstract void drawPanel(final PoseStack stack, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY);

    protected boolean clickPanel(final double mouseX, final double mouseY, final int button) {
        return false;
    }

    private int getMaxScroll() {
        return this.getContentHeight() - (this.height - this.border);
    }

    private void applyScrollLimits() {
        final int max = Math.max(0, this.getMaxScroll());

        if (this.scrollDistance < 0.0F) {
            this.scrollDistance = 0.0F;
        }

        if (this.scrollDistance > max) {
            this.scrollDistance = max;
        }
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scroll) {
        if (scroll != 0) {
            this.scrollDistance += -scroll * this.getScrollAmount();
            this.applyScrollLimits();
            return true;
        }
        return false;
    }

    protected int getScrollAmount() {
        return 20;
    }

    @Override
    public boolean isMouseOver(final double mouseX, final double mouseY) {
        return mouseX >= this.left && mouseX <= this.left + this.width && mouseY >= this.top && mouseY <= this.bottom;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        this.scrolling = button == 0 && mouseX >= this.barLeft && mouseX < this.barLeft + this.barWidth;
        if (this.scrolling) {
            return true;
        }
        final int mouseListY = ((int) mouseY) - this.top - this.getContentHeight() + (int) this.scrollDistance - this.border;
        if (mouseX >= this.left && mouseX <= this.right && mouseListY < 0) {
            return this.clickPanel(mouseX - this.left, mouseY - this.top + (int) this.scrollDistance - this.border, button);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final double p_mouseReleased_1_, final double p_mouseReleased_3_, final int p_mouseReleased_5_) {
        if (super.mouseReleased(p_mouseReleased_1_, p_mouseReleased_3_, p_mouseReleased_5_)) {
            return true;
        }
        final boolean ret = this.scrolling;
        this.scrolling = false;
        return ret;
    }

    private int getBarHeight() {
        int barHeight = (this.height * this.height) / this.getContentHeight();

        if (barHeight < 32) {
            barHeight = 32;
        }

        if (barHeight > this.height - this.border * 2) {
            barHeight = this.height - this.border * 2;
        }

        return barHeight;
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double deltaX, final double deltaY) {
        if (this.scrolling) {
            final int maxScroll = this.height - this.getBarHeight();
            final double moved = deltaY / maxScroll;
            this.scrollDistance += this.getMaxScroll() * moved;
            this.applyScrollLimits();
            return true;
        }
        return false;
    }

    @Override
    public void render(final PoseStack stack, final int mouseX, final int mouseY, final float partialTicks) {
        this.drawBackground();

        final Tesselator tess = Tesselator.getInstance();
        final BufferBuilder worldr = tess.getBuilder();

        final double scale = this.client.getWindow().getGuiScale();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (this.left * scale), (int) (this.client.getWindow().getHeight() - (this.bottom * scale)), (int) (this.width * scale),
            (int) (this.height * scale));

        GlStateManager._disableLighting();
        GlStateManager._disableFog();
        this.client.getTextureManager().bind(GuiComponent.BACKGROUND_LOCATION);
        GlStateManager._color4f(1.0F, 1.0F, 1.0F, 1.0F);
        final float texScale = 32.0F;
        worldr.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        worldr.vertex(this.left, this.bottom, 0.0f).uv(this.left / texScale, (this.bottom + (int) this.scrollDistance) / texScale)
            .color(0x20, 0x20, 0x20, 0xFF).endVertex();
        worldr.vertex(this.right, this.bottom, 0.0f).uv(this.right / texScale, (this.bottom + (int) this.scrollDistance) / texScale)
            .color(0x20, 0x20, 0x20, 0xFF).endVertex();
        worldr.vertex(this.right, this.top, 0.0f).uv(this.right / texScale, (this.top + (int) this.scrollDistance) / texScale)
            .color(0x20, 0x20, 0x20, 0xFF).endVertex();
        worldr.vertex(this.left, this.top, 0.0f).uv(this.left / texScale, (this.top + (int) this.scrollDistance) / texScale)
            .color(0x20, 0x20, 0x20, 0xFF).endVertex();
        tess.end();

        final int baseY = this.top + this.border - (int) this.scrollDistance;
        this.drawPanel(stack, this.right, baseY, tess, mouseX, mouseY);

        GlStateManager._disableDepthTest();

        final int extraHeight = (this.getContentHeight() + this.border) - this.height;
        if (extraHeight > 0) {
            final int barHeight = this.getBarHeight();

            int barTop = (int) this.scrollDistance * (this.height - barHeight) / extraHeight + this.top;
            if (barTop < this.top) {
                barTop = this.top;
            }

            GlStateManager._disableTexture();
            worldr.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            worldr.vertex(this.barLeft, this.bottom, 0.0f).uv(0.0f, 1.0f).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            worldr.vertex(this.barLeft + this.barWidth, this.bottom, 0.0f).uv(1.0f, 1.0f).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            worldr.vertex(this.barLeft + this.barWidth, this.top, 0.0f).uv(1.0f, 0.0f).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            worldr.vertex(this.barLeft, this.top, 0.0f).uv(0.0f, 0.0f).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            tess.end();
            worldr.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            worldr.vertex(this.barLeft, barTop + barHeight, 0.0f).uv(0.0f, 1.0f).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            worldr.vertex(this.barLeft + this.barWidth, barTop + barHeight, 0.0f).uv(1.0f, 1.0f).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            worldr.vertex(this.barLeft + this.barWidth, barTop, 0.0f).uv(1.0f, 0.0f).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            worldr.vertex(this.barLeft, barTop, 0.0f).uv(0.0f, 0.0f).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            tess.end();
            worldr.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            worldr.vertex(this.barLeft, barTop + barHeight - 1, 0.0f).uv(0.0f, 1.0f).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            worldr.vertex(this.barLeft + this.barWidth - 1, barTop + barHeight - 1, 0.0f).uv(1.0f, 1.0f).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            worldr.vertex(this.barLeft + this.barWidth - 1, barTop, 0.0f).uv(1.0f, 0.0f).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            worldr.vertex(this.barLeft, barTop, 0.0f).uv(0.0f, 0.0f).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            tess.end();
        }

        GlStateManager._enableTexture();
        GlStateManager._shadeModel(GL11.GL_FLAT);
        GlStateManager._enableAlphaTest();
        GlStateManager._disableBlend();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }
}
