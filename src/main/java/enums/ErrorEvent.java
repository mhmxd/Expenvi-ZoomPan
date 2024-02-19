package enums;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoField;

public class ErrorEvent {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    // Zoom
    public static final String FIRST_ZOOM = "err_zoom";

    // Clicking
    public static final String CLICK = "err_click";

    // Scrolling/Zooming (for both mouse and moose); can be used in Pan as well
    public static final String SCROLL = "err_scroll";

    // Space pressed, wrong zoom target -> value will be distance (-/+ in notches) to the target; def = 0
    public static final String SPACE_ZOOM = "err_space_zoom";

    // Space pressed during Pan (inside/outside with above values)
    public static final String SPACE_PAN = "err_space_pan";

    //-- Codes
    public static final int OUTSIDE_ZVP = 1; // Outside zoom viewport
    public static final int INSIDE_ZVP = 2;
    public static final int OUTSIDE_PVP = 3; // Outside pan viewport
    public static final int INSIDE_PVP = 4;
    public static final int WRONG_DIRECTION = 5;

    // Fields
    private String key;
    private int code;
    private final Instant instant;

    public ErrorEvent(String key, int code) {
        this.key = key;
        this.code = code;
        this.instant = Instant.now();

        conLog.debug("{} – {} - {}", key, code, instant.toString());
    }

}
