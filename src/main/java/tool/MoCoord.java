package tool;

public class MoCoord {
    public int x;
    public int y;
    public String id = "";

    public MoCoord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public MoCoord(int x, int y, String id) {
        this.x = x;
        this.y = y;
        this.id = id;
    }

    public String formatRowCol() {
        return String.format("r%d_c%d", x, y);
    }

    /**
     * Checks if either of x or y < value
     * @param value Value to compare to
     * @return Boolean
     */
    public boolean isEitherLess(int value) {
        return (x < value) || (y < value);
    }

    /**
     * Checks if either of x or y < value
     * @param value Value to compare to
     * @return Boolean
     */
    public boolean isEitherMore(int value) {
        return (x > value) || (y > value);
    }

    /**
     * Checks if both x and y < value
     * @param value Value to compare to
     * @return Boolean
     */
    public boolean isBothLess(int value) {
        return (x < value) && (y < value);
    }

    /**
     * Checks if both x and y > value
     * @param value Value to compare to
     * @return Boolean
     */
    public boolean isBothMore(int value) {
        return (x > value) && (y > value);
    }

    /**
     * Checks if both fall in a range
     * @param min Min
     * @param max Max
     * @param excl Exclusivity ("00", "01", "10", "11")
     * @return Boolean
     */
    public boolean isBothInBetween(int min, int max, String excl) {
        switch (excl) {
            case "00" -> {
                return (x > min) && (x < max) && (y > min) && (y < max);
            }

            case "01" -> {
                return (x > min) && (x <= max) && (y > min) && (y <= max);
            }

            case "10" -> {
                return (x >= min) && (x < max) && (y >= min) && (y < max);
            }

            case "11" -> {
                return (x >= min) && (x <= max) && (y >= min) && (y <= max);
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return String.format("(%d,%d)", x, y);
    }
}
