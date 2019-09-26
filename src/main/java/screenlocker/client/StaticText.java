package screenlocker.client;

import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.image.BufferedImage;

public class StaticText {
    public static final int SHADOW_OFFSET = 2;

    private Font baseFont;
    private float horizontalPosition;
    private float verticalPosition;
    private float margin;
    private final Color color;
    private String[] text;

    public StaticText(String text, Font baseFont, Color color, float horizontalPosition, float verticalPosition, float margin) {
        this.baseFont = baseFont;
        this.color = color;
        this.verticalPosition = verticalPosition;
        this.horizontalPosition = horizontalPosition;
        this.margin = margin;
        this.text = text.split("\\\\");
    }

    public void setText(String text) {
        this.text = text.split("\\\\");
    }

    public BufferedImage modify(BufferedImage image) {
        Graphics2D g = Util.createGraphics(image, baseFont);
        int margin = (int) (this.margin * image.getHeight());
        int innerHeight = image.getHeight() - 2 * margin;
        int innerWidth = image.getWidth() - margin * 2;

        List<Font> fonts = new ArrayList<>();
        float height = 0;
        for (String line : text) {
            Font font = Util.fitText(g, baseFont, innerWidth, line);
            fonts.add(font);
            height += getHeight(g, font);
        }

        float y = margin + (innerHeight - height) * verticalPosition;
        for (int i = 0; i < text.length; i++) {
            String line = text[i];
            Font font = fonts.get(i);

            int width = Util.getStringWidth(g, font, line);
            float x = margin + (innerWidth - width) * horizontalPosition;

            Util.drawText(g, line, font, Color.BLACK, x, y + SHADOW_OFFSET);
            Util.drawText(g, line, font, color, x, y);

            y += getHeight(g, font);
        }

        return image;
    }

    private int getHeight(Graphics2D g, Font font) {
        return g.getFontMetrics(font).getHeight();
    }

    public Font getBaseFont() {
        return baseFont;
    }

    public float getHorizontalPosition() {
        return horizontalPosition;
    }

    public float getVerticalPosition() {
        return verticalPosition;
    }

    public float getMargin() {
        return margin;
    }

    public Color getColor() {
        return color;
    }
}
