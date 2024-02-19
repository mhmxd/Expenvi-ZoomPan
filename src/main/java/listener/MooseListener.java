package listener;

import moose.Memo;

import java.util.EventListener;

public interface MooseListener extends EventListener {
    void mooseClicked(Memo mem);

    void mooseScrolled(Memo mem);

    void mooseWheelMoved(Memo mem);

    void mooseZoomStart(Memo mem);
}
