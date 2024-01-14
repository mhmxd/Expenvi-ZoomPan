package tool;

import static tool.Constants.*;

public class Utils {

    private static final long MS_IN_DAY = 24 * 60 * 60 * 1000; // Milliseconds in a day

    /**
     * mm to pixel
     * @param mm - millimeters
     * @return equivalant in pixels
     */
    public static int mm2px(double mm) {
        return (int) ((mm / DISP.INCH_MM) * DISP.PPI);
    }

    /**
     * mm to pixel
     * @param px - pixels
     * @return equivalant in mm
     */
    public static double px2mm(double px) {
        return (px / DISP.PPI) * DISP.INCH_MM;
    }

    /**
     * Get the time in millis (in each day)
     * @return Timestamp (long)
     */
    public static long nowMillisInDay() {
        return System.currentTimeMillis() % MS_IN_DAY;
    }
}
