package moose;

import listener.MooseListener;
import org.tinylog.Logger;
import util.MooseConstants;

import javax.swing.event.EventListenerList;

@SuppressWarnings("unused")
public class Moose {
    private final EventListenerList mooseListener;

    public Moose() {
        this.mooseListener = new EventListenerList();
    }

    public synchronized void addMooseListener(MooseListener l) {
        if (l == null) {
            return;
        }
        mooseListener.add(MooseListener.class, l);
    }

    public synchronized void removeMooseListener(MooseListener l) {
        if (l == null) {
            return;
        }
        mooseListener.remove(MooseListener.class, l);
    }

    public void processMooseEvent(Memo e) {
        MooseListener[] listeners = mooseListener.getListeners(MooseListener.class);
        if (listeners.length > 0) {
            switch (e.getAction()) {
                case MooseConstants.CLICK -> {
                    for (MooseListener l : listeners) {
                        l.mooseClicked(e);
                    }
                }
                case MooseConstants.SCROLL -> {
                    for (MooseListener l : listeners) {
                        l.mooseMoved(e);
                    }
                }
                case MooseConstants.ZOOM -> {
                    switch (e.getMode()) {
                        case MooseConstants.ZOOM -> {
                            for (MooseListener l : listeners) {
                                l.mooseWheelMoved(e);
                            }
                        }
                        case MooseConstants.ZOOM_START -> {
                            for (MooseListener l : listeners) {
                                l.mooseZoomStart(e);
                            }
                        }
                    }
                }
            }
        }
    }
}
