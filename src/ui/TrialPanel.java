package ui;

import listener.TrialListener;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import javax.swing.*;
import javax.swing.event.EventListenerList;

/**
 * An abstract panel that serves as a base for test-related panels.
 */
public abstract class TrialPanel extends JPanel {
    private TaggedLogger conLog = Logger.tag("TrialPanel");

    private final EventListenerList listeners;
    private boolean focus;
    private boolean isTrialRunning;

    /**
     * Constructs a TrialPanel.
     */
    public TrialPanel() {
        this.listeners = new EventListenerList();
        this.isTrialRunning = false;
    }

    /**
     * Checks if the panel has focus.
     *
     * @return true if the panel has focus, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isFocus() {
        return focus;
    }

    /**
     * Sets the focus state of the panel.
     *
     * @param focus true to set the panel as focused, false otherwise
     */
    public void setFocus(boolean focus) {
        this.focus = focus;
    }

    /**
     * Adds a TrialListener to the panel.
     *
     * @param l the TrialListener to add
     */
    protected void addListener(TrialListener l) {
        if (l == null) {
            return;
        }
        listeners.add(TrialListener.class, l);
    }

    /**
     * Removes a TrialListener from the panel.
     *
     * @param l the TrialListener to remove
     */
    @SuppressWarnings("unused")
    protected void removeListener(TrialListener l) {
        if (l == null) {
            return;
        }
        listeners.remove(TrialListener.class, l);
    }

    /**
     * Notifies all listeners that the test has started.
     */
    protected void startTrial() {
        if (isTrialRunning) {
            return;
        }

        isTrialRunning = true;
//        TrialFrame.LOGGER.info("Trial started");
        for (TrialListener l : listeners.getListeners(TrialListener.class)) {
            l.trialStart();
        }
    }

    /**
     * Notifies all listeners that the test has ended.
     */
    protected void endTrial() {
        conLog.info("(endTrial) Num of listeners: {}", listeners.getListenerCount());
        isTrialRunning = false;
        for (TrialListener l : listeners.getListeners(TrialListener.class)) {
            l.trialEnd();
        }
    }

    /**
     * Notifies all listeners that an error has occurred during the trial.
     */
    protected void errorTrial() {
        for (TrialListener l : listeners.getListeners(TrialListener.class)) {
            l.trialError();
        }
    }

//    public boolean isTrialRunning() {
//        return isTrialRunning;
//    }
}
