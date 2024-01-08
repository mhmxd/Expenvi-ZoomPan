package ui;

import model.BaseBlock;
import model.BaseTrial;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/***
 * JlayeredPane to use indexes for objects
 */
public class TaskPanel extends JLayeredPane {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    // Experiment
    protected ArrayList<BaseBlock> blocks = new ArrayList<>();
    protected BaseBlock activeBlock;
    protected BaseTrial activeTrial;
//    protected int blockNum, trialNum;

    // Flags
    protected boolean trialActive = false;

    // UI
    JLabel progressLabel = new JLabel();

    public TaskPanel() {
        progressLabel.setBounds(2200, 50, 300, 30);
        progressLabel.setFont(new Font(progressLabel.getFont().getFontName(), Font.PLAIN, 20));
        progressLabel.setVerticalAlignment(JLabel.TOP);
        progressLabel.setHorizontalAlignment(JLabel.RIGHT);
//        progressLabel.setFocusable(false);
        add(progressLabel, 1);
    }

    protected void createBlocks() {
        // Implemented by the subclasses
    }

    /**
     * Start the current block
     */
    protected void starTask() {
        activeBlock = blocks.get(0);
        startBlock();
    }

    /**
     * Show a trial
     */
    protected void startBlock() {
        // Implemented by the subclasses
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

    protected void nextTrial() {

    }
}
