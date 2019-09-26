package screenlocker.client;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Util {
    public static final Map<RenderingHints.Key, Object> HINTS = new HashMap<>();
    static {
        Util.HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Util.HINTS.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    /** Utility class. */
    private Util() {}

    public static void drawText(Graphics g, String s, Font f, Color c, float x, float y) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, (int) x, (int) y + g.getFontMetrics(f).getAscent());
    }

    public static int getStringWidth(Graphics g, Font f, String s) {
        return g.getFontMetrics(f).stringWidth(s);
    }

    public static Font fitText(Graphics g, Font font, int width, String str) {
        int l = 0;
        int r = font.getSize();
        while (l != r) {
            int m = (l + r + 1) / 2;
            font = new Font(font.getName(), font.getStyle(), m);
            int w = getStringWidth(g, font, str);
            if (w <= width) {
                l = m;
            } else {
                r = m - 1;
            }
        }
        return font;
    }

    public static BufferedImage loadJpegImage(String filename) throws IOException {
        ImageReader reader = ImageIO.getImageReadersByMIMEType("image/jpeg").next();
        ImageInputStream is = new FileImageInputStream(new File(filename));
        reader.setInput(is);
        return reader.read(0);
    }

    public static Graphics2D createGraphics(BufferedImage image) {
        Graphics g = image.createGraphics();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHints(HINTS);
        return g2d;
    }

    public static Graphics2D createGraphics(BufferedImage image, Font font) {
        Graphics2D g2d = createGraphics(image);
        g2d.setFont(font);
        return g2d;
    }
}
