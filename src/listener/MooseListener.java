package listener;

import moose.Memo;

import java.util.EventListener;

public interface MooseListener extends EventListener {
    void mooseClicked(Memo e);

    void mooseMoved(Memo e);

    void mooseWheelMoved(Memo e);

    void mooseZoomStart(Memo e);
}
