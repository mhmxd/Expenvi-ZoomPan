package control;

import enums.TrialEvent;
import model.BaseTrial;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Logex {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());
    private static Logex self;

    private BaseTrial activeTrial;
    // Keys are from TrialEvent strings
    private final Map<String, TrialEvent> trialEvenLog = new HashMap<>();

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
        // Clear the log map
        trialEvenLog.clear();
    }

    /**
     * Log a trial event (get current tiem yourself!)
     * @param event TrialEvent
     */
    public void log(TrialEvent event) {
        // Add the event to the list
        trialEvenLog.put(event.getName(), event);

        // TODO process the specific times
    }

    /**
     * Log an event using only the key (manage the first/last yourself!)
     * @param key Key from TrialEvent
     */
    public void log(String key) {
        if (key == TrialEvent.TRIAL_OPEN || key == TrialEvent.TRIAL_CLOSE || key == TrialEvent.SPACE_PRESS) {
            trialEvenLog.put(key, new TrialEvent(key));
            return;
        }

        String eventFirstName = TrialEvent.getFirstName(key);
        String eventLastName = TrialEvent.getLastName(key);

        // If first is empty, add first
        if (!trialEvenLog.containsKey(eventFirstName)) {
            trialEvenLog.put(eventFirstName, new TrialEvent(eventFirstName));
            conLog.trace("Logged {}, {}", eventFirstName, getTrialInstant(eventFirstName));
        }

        // Add last
        trialEvenLog.put(eventLastName, new TrialEvent(eventLastName));
        conLog.trace("Logged {}", eventLastName);
    }

    public TrialEvent getTrialEvent(String name) {
        return trialEvenLog.get(name);
    }

    public Instant getTrialInstant(String name) {
        if (hasLogged(name)) return getTrialEvent(name).getInstant();
        else return Instant.MIN;
    }

    /**
     * Has it logged this key (currently checking last occurance)
     * @param key String key from TrialEvent keys
     * @return True if events for this key are logged
     */
    public boolean hasLoggedKey(String key) {
        String lastName = TrialEvent.getLastName(key);
        if (lastName != "") return trialEvenLog.containsKey(lastName);
        return false;
    }

    /**
     * Extract the time from the map
     * @param eventName Name of the event
     * @return Time (long)
     */
    public boolean hasLogged(String eventName) {
        return trialEvenLog.containsKey(eventName);
    }
}
