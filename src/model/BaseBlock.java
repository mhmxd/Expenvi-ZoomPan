package model;

import com.google.gson.Gson;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.util.ArrayList;
import java.util.Collections;

public class BaseBlock {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    public final ArrayList<BaseTrial> trials = new ArrayList<>();

    /**
     * Constructor
     * @param isModeZoomIn Is Zoom-In?
     */
    public BaseBlock(int blkId, boolean isModeZoomIn, int repetition) {
        conLog.trace("Block ID: " + blkId);
        for (int j = 0; j < repetition; j++) {

            if (!isModeZoomIn) {
                trials.add(new ZoomTrial("ZoomOut", 1, 35, 29));
                trials.add(new ZoomTrial("ZoomOut", 2, 35, 23));
                trials.add(new ZoomTrial("ZoomOut", 3, 35, 17));
            } else {
                trials.add(new ZoomTrial("ZoomIn", 1, 27, 33));
                trials.add(new ZoomTrial("ZoomIn", 2, 21, 33));
                trials.add(new ZoomTrial("ZoomIn", 3, 15, 33));
            }

            Collections.shuffle(trials);

            for (int t = 0; t < trials.size(); t++) {
                trials.get(t).blockId = blkId;
                trials.get(t).trialNum = t + 1;
            }
        }
    }

    /**
     * Get a trial
     * @param trNum Trial number (starting from 1)
     * @return Trial
     */
    public BaseTrial getTrial(int trNum) {
        if (trNum > trials.size()) return null;
        else return cloneTrial(trials.get(trNum - 1));
    }

    public boolean isBlockFinished(int trNum) {
        return trNum >= trials.size();
    }

    /**
     * Clone a trial
     * @param inTr Input trial
     * @return Clone trial
     */
    public BaseTrial cloneTrial(BaseTrial inTr) {
        final Gson gson = new Gson();
        final String trialJSON = gson.toJson(inTr);
        final Class<? extends BaseTrial> trialType = inTr.getClass();

        return gson.fromJson(trialJSON, trialType);
    }
}
