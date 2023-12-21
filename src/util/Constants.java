package util;

import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;

public class Constants {
    public static final Color BORDER_FOCUS_GAINED = new Color(76, 175, 80);
    public static final Color BORDER_FOCUS_LOST = new Color(255, 0, 0);
    public static final Color MAIN_BACKGROUND = Color.WHITE;
    public static final int BORDER_THICKNESS = 8;

    public static final LineBorder FOCUS_GAIN_BORDER = new LineBorder(BORDER_FOCUS_GAINED, BORDER_THICKNESS);
    public static final LineBorder FOCUS_LOST_BORDER = new LineBorder(BORDER_FOCUS_LOST, BORDER_THICKNESS);

}
