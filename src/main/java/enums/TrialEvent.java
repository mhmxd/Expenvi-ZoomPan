package enums;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.time.Instant;

public class TrialEvent {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    public static final String TRIAL_OPEN = "trial_open";

    public static final String FIRST_MOVE = "first_move";
    public static final String LAST_MOVE = "last_move";

//    public static final String FIRST_VIEWPORT_ENTER = "first_viewport_enter";
//    public static final String LAST_VIEWPORT_ENTER = "last_viewport_enter";

//    public static final String FIRST_ZOOM = "first_zoom";
//    public static final String LAST_ZOOM = "last_zoom";

//    public static final String FIRST_PAN = "first_pan";
//    public static final String LAST_PAN = "last_pan";

//    public static final String FIRST_FOCUS_ENTER = "first_focus_enter";
//    public static final String LAST_FOCUS_ENTER = "last_focus_enter";

//    public static final String FIRST_FOCUS_EXIT = "first_focus_exit";
//    public static final String LAST_FOCUS_EXIT = "last_focus_exit";
//
//    public static final String FIRST_VIEWPORT_EXIT = "viewport_first_exit";
//    public static final String LAST_VIEWPORT_EXIT = "viewport_last_exit";

    public static final String SPACE_PRESS = "space_press";

    public static final String TRIAL_CLOSE = "trial_close";

    //-- Keys (used only inter communication) – for all the enter/exit pairs
    public static final String MOVE = "move";
    public static final String VIEWPORT_ENTER = "viewport_enter";
    public static final String ZOOM = "zoom";
    public static final String PAN = "pan";
    public static final String FOCUS_ENTER = "focus_enter";
    public static final String FOCUS_EXIT = "focus_exit";
    public static final String VIEWPORT_EXIT = "viewport_exit";

    private final String key;
    private final Instant instant;

    /**
     * Create an instant (time is always Instant.now())
     * @param n Name of the event (constant String from the class itself)
     */
    public TrialEvent(String n) {
        key = n;
        instant = Instant.now();
    }

    /**
     * Get the name of the event
     * @return Name of the event
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the instant of the event
     * @return Instant
     */
    public Instant getInstant() {
        return instant;
    }

    /**
     * Get the name of the 'last' event for this key
     * e.g., ZOOM -> LAST_ZOOM
     * @param key Key (form the keys here)
     * @return Event name of the last occurance
     */
    public static String getLast(String key) {
        return "last_" + key;
    }

    /**
     * Get the name of the 'first' event for this key
     * e.g., ZOOM -> FIRST_ZOOM
     * @param key Key (form the keys here)
     * @return Event name of the first occurance
     */
    public static String getFirst(String key) {
        return "first_" + key;
    }
}