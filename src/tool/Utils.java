package tool;

import static tool.Constants.*;

public class Utils {

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
}
