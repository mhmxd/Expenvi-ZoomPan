package tool;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static tool.Constants.*;

public class Utils {

    private static final long MS_IN_DAY = 24 * 60 * 60 * 1000; // Milliseconds in a day

    /**
     * Returns a random int between the min (inclusive) and the bound (exclusive)
     * @param min Minimum (inclusive)
     * @param bound Bound (exclusive)
     * @return Random int
     */
    public static int randInt(int min, int bound) {
        if (bound <= min) return min;
        else return ThreadLocalRandom.current().nextInt(min, bound);
    }

    public static int getMinXFromList(List<MoCoord> list) {
        if (list == null || list.isEmpty()) return -1;

        int min = list.get(0).x;
        for (MoCoord p : list) {
            if (p.x < min) min = p.x;
        }

        return min;
    }

    public static int getMaxXFromList(List<MoCoord> list) {
        if (list == null || list.isEmpty()) return -1;

        int max = list.get(0).x;
        for (MoCoord p : list) {
            if (p.x > max) max = p.x;
        }

        return max;
    }

    public static boolean isBetween(double value, int min, int max, String excl) {
        switch (excl) {
            case "00" -> {
                return (value > min) && (value < max);
            }

            case "01" -> {
                return (value > min) && (value <= max);
            }

            case "10" -> {
                return (value >= min) && (value < max);
            }

            case "11" -> {
                return (value >= min) && (value <= max);
            }
        }

        return false;
    }

    public static double modifyInRange(double value, double change, int min, int max) {
        if (isBetween(value + change, min, max, "11")) return value + change;
        else if (value + change < min) return min;
        else if (value + change > max) return max;
        return value;
    }

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
