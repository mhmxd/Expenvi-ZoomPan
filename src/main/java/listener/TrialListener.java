package listener;

import java.util.EventListener;

public interface TrialListener extends EventListener {
    void trialStart();

    void trialEnd();

    void trialError();
}
