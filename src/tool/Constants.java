package tool;

import javax.swing.border.LineBorder;
import java.awt.*;

public class Constants {

    //-- Borders
    public static class BORDERS {
        public static final double THICKNESS_2_mm = 2;
        public static final double THICKNESS_1_mm = 1;
        public static final int THICKNESS_2 = Utils.mm2px(THICKNESS_2_mm);
        public static final int THICKNESS_1 = Utils.mm2px(THICKNESS_1_mm);

        public static final LineBorder BLACK_BORDER = new LineBorder(
                Color.BLACK, THICKNESS_2);
        public static final LineBorder FOCUSED_BORDER = new LineBorder(
                COLORS.GREEN, THICKNESS_2);
        public static final LineBorder FOCUS_LOST_BORDER = new LineBorder(
                COLORS.RED, THICKNESS_2);
    }

    //-- Colors
    public static class COLORS {
        public static final Color BLACK = Color.BLACK;
        public static final Color GREEN = new Color(76, 175, 80);
        public static final Color RED = new Color(255, 0, 0);
//        public static final Color MAIN_BACKGROUND = Color.WHITE;
        public static final Color YELLOW = new Color(227, 206, 23);
    }

    //-- Display properties
    public static class DISP {
//        public final static int PPI = 109;
        public final static int PPI = 89;
        public final static double INCH_MM = 25.4;
        public final static double LR_MARGIN_mm = 20; // (mm) Left-right margin
    }


    //-- Strings
    public static class STRINGS {
        public final static String LD = ";";
        public final static String MSP = "&";
        public static final String INTRO = "INTRO";
        public static final String MOOSE = "MOOSE";
        public final static String TECH = "TECH";
        public final static String CONFIG = "CONFIG";
        public final static String CONNECTION = "CONNECTION";
        public final static String LOG = "LOG";
        public final static String EXP_ID = "EXPID"; // Id for an experiment
        public final static String GENLOG = "GENLOG";
        public final static String BLOCK = "BLOCK";
        public final static String TRIAL = "TRIAL";
        public final static String TSK = "TASK"; // TSK to not confuse with TASK
        public final static String END = "END";
        public final static String P_INIT = "P";

        public static final String SP = ",";
        public static final String SINGLE = "SINGLE";
        public static final String CLICK = "CLICK";
        public static final String SCROLL = "SCROLL";
        public static final String ZOOM = "ZOOM";
        public static final String ZOOM_START = "ZOOM_START";
        public static final String KEEP_ALIVE = "KEEP_ALIVE";

        public final static String GRAB = "GRAB";
        public final static String DRAG = "DRAG";
        public final static String RELEASE = "RELEASE";
        public final static String REVERT = "REVERT";

        public final static String DEMO_TITLE = "Welcome to the scrolling experiment!";
        public final static String DEMO_NEXT = "First, let's have a demo >";

        public final static String SHORT_BREAK_TEXT =
                "<html>Time for a quick break! To continue, press <B>ENTER</B>.</html>";

        public static final String DLG_BREAK_TITLE  = "Time for a break!";
        public static final String DLG_BREAK_TEXT   =
                "<html>When ready, press <B>BLUE + RED</B> keys to start the next block</html>";

        public final static String EXP_START_MESSAGE =
                "To begin the experiment, press SPACE.";
        public final static String END_EXPERIMENT_MESSAGE =
                "All finished! Thank you for participating in this experiment!";

        public final static String PID = "PID";
        public final static String TASK = "TASK";
        public final static String TECHNIQUE = "TECHNIQUE";

    }
}
