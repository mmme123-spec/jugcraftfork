package io.github.jimbozoomer.jugcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A button in the Concordance's arcane look (docs/features/themed-screens.md): a violet plate edged in gold that lights
 * when hovered or focused and dims when it can't be pressed. A plain vanilla button underneath, so keyboard focus,
 * narration and clicks are unchanged.
 */
final class ArcaneButton extends Button {
    ArcaneButton(int x, int y, int w, int h, Component message, OnPress action) {
        super(x, y, w, h, message, action, DEFAULT_NARRATION);
    }

    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean lit = active && (isHovered() || isFocused());
        int edge = !active ? 0xFF5C4016 : lit ? 0xFFF6DA8C : 0xFFCEA652;
        int plate = !active ? 0xFF1A1330 : lit ? 0xFF4B3486 : 0xFF34245E;
        g.fill(x, y, x + w, y + h, 0xFF0A0614);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, edge);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, plate);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, active ? 0xFF6E56B4 : 0xFF2C2448);
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, 0xFF1E1438);
        if (isFocused()) g.outline(x - 1, y - 1, w + 2, h + 2, 0xFFFFD98A);
        var font = Minecraft.getInstance().font;
        String label = BookArt.fit(font, getMessage().getString(), w - 8);
        g.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, active ? 0xFFF2ECFF : 0xFF7A7096, active);
    }
}
