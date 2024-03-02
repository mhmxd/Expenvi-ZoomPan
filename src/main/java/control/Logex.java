package control;

import enums.ErrorEvent;
import enums.TrialEvent;
import model.BaseTrial;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Logex {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());
    private static Logex self;

    private BaseTrial activeTrial;

    private final Map<String, TrialEvent> trialLogs = new HashMap<>(); // Keys: TrialEvent strings
    private final Map<String, ErrorEvent> errorLogs = new HashMap<>(); // Keys: ErrorEvent strings

    /**
     * Constructor
     */
    private Logex() {

    }

    /**
     * Get singleton
     * @return Logex instance
     */
    public static Logex get() {
        if (self == null) self = new Logex();
        return self;
    }

    /**
     * Start logging events for this trial
     * @param trial BaseTrial
     */
    public void activateTrial(BaseTrial trial) {
        activeTrial = trial;

        // Clear the log maps
        trialLogs.clear();
        errorLogs.clear();

        // Add the trial open log
        logEvent(TrialEvent.TRIAL_OPEN);
    }

    /**
     * Log a trial event (get current tiem yourself!)
     * @param event TrialEvent
     */
    public void logEvent(TrialEvent event) {
        // Add the event to the list
        trialLogs.put(event.getKey(), event);

        // TODO process the specific times
    }

    /**
     * Log an event using only the key (manage the first/last yourself!)
     * @param key Key from TrialEvent
     */
    public void logEvent(String key) {

        switch (key) {
            case null -> {
                // Do nothing
            }

            // No first, last
            case TrialEvent.TRIAL_OPEN, TrialEvent.TRIAL_CLOSE, TrialEvent.SPACE_PRESS -> {
                trialLogs.put(key, new TrialEvent(key));
                conLog.debug("Logged {}", key);
            }


            // Log first, last
            default -> {
                String eventFirstName = TrialEvent.getFirst(key);
                String eventLastName = TrialEvent.getLast(key);

                // If first is empty, add first
                if (!trialLogs.containsKey(eventFirstName)) {
                    trialLogs.put(eventFirstName, new TrialEvent(eventFirstName));
                    conLog.debug("Logged {}, {}", eventFirstName, getTrialInstant(eventFirstName));
                }

                // Add last
                trialLogs.put(eventLastName, new TrialEvent(eventLastName));
                conLog.trace("Logged {}", eventLastName);
            }
        }


    }

    /**
     * Log an error
     * @param errKey String key
     * @param errCode int code (from ErrorEvent)
     */
    public void logError(String errKey, int errCode) {
        errorLogs.put(errKey, new ErrorEvent(errKey, errCode));
    }

    /**
     * Get a TrialEvent
     * @param name Name of the event
     * @return TrialEvent
     */
    public TrialEvent getTrialEvent(String name) {
        return trialLogs.get(name);
    }

    /**
     * Get the Instant of a TrialEvent
     * @param name TrialEvent name
     * @return Instant
     */
    public Instant getTrialInstant(String name) {
        if (hasLogged(name)) return getTrialEvent(name).getInstant();
        else return Instant.MIN;
    }

    /**
     * Return the duration (in sec.) between two Instants (indicated by begin and end keys)
     * @param beginKey String begin key
     * @param endKey String end key
     * @return double Duration (in seconds)
     */
    public double getDurationSec(String beginKey, String endKey) {
        Instant beginInst = getTrialInstant(beginKey);
        Instant endInst = getTrialInstant(endKey);
        conLog.debug("Begin = {}, End = {}", beginInst, endInst);
        if (beginInst.equals(Instant.MIN) || endInst.equals(Instant.MIN)) return Double.NaN;
        else return Duration.between(beginInst, endInst).toMillis() / 1000.0;
    }

    /**
     * Has it logged this key (currently checking last occurance)
     * @param key String key from TrialEvent keys
     * @return True if events for this key are logged
     */
    public boolean hasLoggedKey(String key) {
        String lastName = TrialEvent.getLast(key);
        if (!lastName.isEmpty()) return trialLogs.containsKey(lastName);
        return false;
    }

    /**
     * Extract the time from the map
     * @param eventName Name of the event
     * @return Time (long)
     */
    public boolean hasLogged(String eventName) {
        return trialLogs.containsKey(eventName);
    }
}
