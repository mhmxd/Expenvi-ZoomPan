package model;

import com.google.gson.Gson;
import enums.Task;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.Utils;
import ui.PanTaskPanel;

import java.util.ArrayList;
import java.util.Collections;

public class BaseBlock {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    public int blockNum;
    public final ArrayList<BaseTrial> trials = new ArrayList<>();

    /**
     * Constructor
     * @param blkNum Block number
     * @param task Which task? (ZOOM-IN, ZOOM-OUT, PAN)
     * @param repetition Number of repetitions (in each block)
     */
    public BaseBlock(int blkNum, Task task, int repetition) {
        blockNum = blkNum;

        switch (task) {
            case ZOOM_IN -> {

                for (int j = 0; j < repetition; j++) {

                    trials.add(new ZoomTrial("ZoomIn", 1, 27, 33));
                    trials.add(new ZoomTrial("ZoomIn", 2, 21, 33));
                    trials.add(new ZoomTrial("ZoomIn", 3, 15, 33));

                    Collections.shuffle(trials);

                    for (int t = 0; t < trials.size(); t++) {
                        trials.get(t).blockNum = blkNum;
                        trials.get(t).trialNum = t + 1;
                        trials.get(t).id = trials.get(t).blockNum * 100 + trials.get(t).trialNum;
                    }
                }
            }

            case ZOOM_OUT -> {

                for (int j = 0; j < repetition; j++) {
                    trials.add(new ZoomTrial("ZoomOut", 1, 35, 29));
                    trials.add(new ZoomTrial("ZoomOut", 2, 35, 23));
                    trials.add(new ZoomTrial("ZoomOut", 3, 35, 17));
                }

                Collections.shuffle(trials);

                for (int t = 0; t < trials.size(); t++) {
                    trials.get(t).blockNum = blkNum;
                    trials.get(t).trialNum = t + 1;
                    trials.get(t).id = trials.get(t).blockNum * 100 + trials.get(t).trialNum;
                }

            }

            case PAN -> {
                // For each repetition: randomly choose the rotation for the short curve. Next two will be +120 and +240
                for (int i = 0; i < PanTaskPanel.NUM_REP_IN_BLOCK; i++) {
                    int rotation = Utils.randInt(0, 360);
                    trials.add(new PanTrial(1, rotation));
                    trials.add(new PanTrial(2, (rotation + 120) % 360)); // Go over the next rotation
                    trials.add(new PanTrial(3, (rotation + 240) % 360)); // Go over the next rotation
                }
                // Shuffle the trials
                Collections.shuffle(trials);

                for (BaseTrial trial : trials) {
                    conLog.info(trial);
                }
            }
        }

        // Set the numbers and IDs
        for (int t = 0; t < trials.size(); t++) {
            trials.get(t).blockNum = blkNum;
            trials.get(t).trialNum = t + 1;
            trials.get(t).id = trials.get(t).blockNum * 100 + trials.get(t).trialNum;
        }

    }

    /**
     * Create the Pan trials in this block
     * (uses constants from PanTaskPanel)
     */
    public void createPanTrials() {
//        List<BaseTrial> tempList = new ArrayList<>();
//
//        List<BaseTrial> temp1 = new ArrayList<>();
//        List<BaseTrial> temp2 = new ArrayList<>();
//        List<BaseTrial> temp3 = new ArrayList<>();
//
//        Random rand = new SecureRandom();
//
//        int angle = 120; // Angle between the trials is always 120 (cause 3 levels)





//        int radius = 360 / PanTaskPanel.NUM_PAN_TRIALS_IN_BLOCK;
//        for (int i = 0; i < PanTaskPanel.NUM_PAN_TRIALS_IN_BLOCK; i++) {
//            int rotation = radius * i + rand.nextInt(radius);
//            temp1.add(new PanTrial(1, rotation));
//        }
//        Collections.shuffle(temp1);
//
//        radius = 360 / PanTaskPanel.NUM_PAN_TRIALS_IN_BLOCK;
//        for (int i = 0; i < PanTaskPanel.NUM_PAN_TRIALS_IN_BLOCK; i++) {
//            int rotation = radius * i + rand.nextInt(radius);
//            temp2.add(new PanTrial(2, rotation));
//        }
//        Collections.shuffle(temp2);
//
//        radius = 360 / PanTaskPanel.NUM_PAN_TRIALS_IN_BLOCK;
//        for (int i = 0; i < PanTaskPanel.NUM_PAN_TRIALS_IN_BLOCK; i++) {
//            int rotation = radius * i + rand.nextInt(radius);
//            temp3.add(new PanTrial(3, rotation));
//        }
//        Collections.shuffle(temp3);
//
//        List<BaseTrial> temp = new ArrayList<>();
//        int sum = PanTaskPanel.NUM_PAN_TRIALS_IN_BLOCK / 3;
//        for (int i = 0; i < 3; i++) {
//            temp.clear();
//            for (int j = 0; j < sum; j++) {
//                temp.add(temp1.remove(0));
//                temp.add(temp2.remove(0));
//                temp.add(temp3.remove(0));
//            }
//            Collections.shuffle(temp);
//
//            for (int j = 0; j < temp.size(); j++) {
//                BaseTrial t = temp.get(j);
//                t.blockId = i + 1;
//                t.trialNum = j + 1;
//                t.id = t.blockId * 100 + t.trialNum;
//            }
//
//            trials.addAll(temp);
//        }
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

    public void reInsertTrial(int trNum) {
        BaseTrial trial = trials.get(trNum - 1);
        int randomIndex = Utils.randInt(trNum, trials.size());
        conLog.info("New num: {}", randomIndex + 1);
        trials.add(randomIndex, cloneTrial(trial));
        // Refesh the nums
        for (int i = 0; i < trials.size(); i++) {
            trials.get(i).trialNum = i + 1;
        }
        conLog.info("List: {}", trials);
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
