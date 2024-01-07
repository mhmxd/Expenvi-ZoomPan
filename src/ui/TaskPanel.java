package ui;

import model.BaseBlock;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import javax.swing.*;
import java.util.ArrayList;

public class TaskPanel extends JPanel {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    // Experiment
    protected ArrayList<BaseBlock> blocks = new ArrayList<>();
    protected BaseBlock activeBlock;
    protected int blockNum, trialNum;

    // Flags
    protected boolean trialActive = false;


    protected void createBlocks() {
        // Implemented by the subclasses
    }

    /**
     * Start a block of trials
     * @param blkNum Block number
     */
    protected void startBlock(int blkNum) {
        activeBlock = blocks.get(blkNum - 1);
        startTrial(1);
    }

    /**
     * Show a trial
     * @param trNum Trial number
     */
    protected void startTrial(int trNum) {
        // Implemented by the subclasses
    }

    /**
     * Implemented by subclasses
     * @return Was it a hit?
     */
    protected boolean checkHit() {
        return false; // Placeholder
    }

    /**
     * Trial was ended with success
     */
    protected void endTrialHit() {
//        SOUNDS.playHit();

        trialActive = false;

        // Next...
//        if (trialNum < mBlock.getNumTrials()) { // Trial -------------------------------------
//            trialNum++;
//
//            //region LOG
//            mGenLog.trial_num = trialNum;
//            //endregion
//
//            executorService.schedule(() ->
//                            showTrial(trialNum),
//                    mTask.NT_DELAY_ms,
//                    TimeUnit.MILLISECONDS);
//
//        } else if (blockNum < mTask.getNumBlocks()) { // Block -------------------------------
//
//            // Break dialog
//            if (mPracticeMode) {
//                // Show dialog after each break
//                MainFrame.get().showDialog(new PracticeBreakDialog());
//            } else if (mDemoMode) {
//                // Just continue with the blocks
//            } else { // Real experiment -> show break dialog
//                if (blockNum == 3) {
//                    MainFrame.get().showDialog(new BreakDialog());
//                }
//            }
//
//            logBlockEnd(); // LOG
//
//            // Next block
//            blockNum++;
//            trialNum = 1;
//
//            // LOG
//            mGenLog.block_num = blockNum;
//            mGenLog.trial_num = trialNum;
//            //---
//
//            executorService.schedule(() ->
//                            startBlock(blockNum),
//                    mTask.NT_DELAY_ms,
//                    TimeUnit.MILLISECONDS);
//        } else { // Task is finished -----------------------------------------------------------
//
//            // LOG
//            logBlockEnd();
//            logTaskEnd();
//            //---
//
//            MainFrame.get().showEndPanel();
//
//            SOUNDS.playTaskEnd();
//        }
    }

    /**
     * Trial was ended with failure
     */
    protected void endTrialMiss() {

    }


}
