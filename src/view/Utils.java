package view;

import java.awt.*;

public class Utils {
    public static Color brighten(Color c, float fraction) {
        int r = Math.min(255, (int)(c.getRed() + (255 - c.getRed()) * fraction));
        int g = Math.min(255, (int)(c.getGreen() + (255 - c.getGreen()) * fraction));
        int b = Math.min(255, (int)(c.getBlue() + (255 - c.getBlue()) * fraction));
        return new Color(r, g, b, c.getAlpha());
    }

    public static Color darken(Color c, float fraction) {
        int r = Math.max(0, (int)(c.getRed() * (1 - fraction)));
        int g = Math.max(0, (int)(c.getGreen() * (1 - fraction)));
        int b = Math.max(0, (int)(c.getBlue() * (1 - fraction)));
        return new Color(r, g, b, c.getAlpha());
    }
}
