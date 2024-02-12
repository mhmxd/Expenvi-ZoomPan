package moose;

import listener.MooseListener;
import static tool.Constants.*;

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

    public void processMooseEvent(Memo mem) {
        MooseListener[] listeners = mooseListener.getListeners(MooseListener.class);
        if (listeners.length > 0) {
            switch (mem.getAction()) {
                case STRINGS.CLICK -> {
                    for (MooseListener l : listeners) {
                        l.mooseClicked(mem);
                    }
                }
                case STRINGS.SCROLL -> {
                    for (MooseListener l : listeners) {
                        l.mooseScrolled(mem);
                    }
                }
                case STRINGS.ZOOM -> {
                    switch (mem.getMode()) {
                        case STRINGS.ZOOM -> {
                            for (MooseListener l : listeners) {
                                l.mooseWheelMoved(mem);
                            }
                        }
                        case STRINGS.ZOOM_START -> {
                            for (MooseListener l : listeners) {
                                l.mooseZoomStart(mem);
                            }
                        }
                    }
                }
            }
        }
    }
}
