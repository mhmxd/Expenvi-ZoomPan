package ui;

import control.Logex;
import enums.TrialEvent;
import enums.TrialStatus;
import model.BaseBlock;
import model.BaseTrial;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Random;

import static tool.Constants.*;
/***
 * JlayeredPane to use indexes for objects
 */
public class TaskPanel extends JLayeredPane {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    // Experiment
    protected ArrayList<BaseBlock> blocks = new ArrayList<>();
    protected BaseBlock activeBlock;
    protected BaseTrial activeTrial;

    // UI
    JLabel progressLabel = new JLabel();
    JLabel endTaskLabel = new JLabel();

    public TaskPanel(Dimension dim) {
        conLog.trace("Width: {}", getWidth());
        progressLabel.setBounds(dim.width - 300, 50, 300, 30);
        progressLabel.setFont(new Font(progressLabel.getFont().getFontName(), Font.PLAIN, 20));
        progressLabel.setVerticalAlignment(JLabel.CENTER);
        progressLabel.setHorizontalAlignment(JLabel.CENTER);
//        progressLabel.setVisible(true);
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
        for (Component c : getComponentsInLayer(JLayeredPane.DEFAULT_LAYER)) {
            remove(c);
            repaint();
        }

        add(progressLabel, DEFAULT_LAYER);

        // Implemented by the subclasses
    }


    protected void endTrial(int status) {
        Logex.get().logEvent(TrialEvent.TRIAL_CLOSE);
        double openToClose = Logex.get().getDurationSec(TrialEvent.TRIAL_OPEN, TrialEvent.TRIAL_CLOSE);
        conLog.info("Time: Open to Close = {}", openToClose);
        conLog.info("--------------------------");
        if (status == TrialStatus.HIT) {
            if (activeBlock.isBlockFinished(activeTrial.trialNum)) { // Block finished -> show break|end
                conLog.info("Block Finished");
                endBlock(); // Got to the next block (checks are done inside that method)
            } else { // More trials in the block
                activeTrial = activeBlock.getTrial(activeTrial.trialNum + 1);
                showActiveTrial();
            }
        } else { // Error
            // Re-insert the trial into the rest randomly, then get the next one
            activeBlock.reInsertTrial(activeTrial.trialNum);
            activeTrial = activeBlock.getTrial(activeTrial.trialNum + 1);
            showActiveTrial();
        }
    }

    protected void endBlock() {
        if (blocks.size() == activeBlock.blockNum) { // No more blocks
            conLog.info("Task Ended!");
            endTask();
        } else { // More blocks -> show break -> (action) next block
            showBreak();
        }
    }

    protected void showActiveTrial() {

    }

    protected void showBreak() {
        BreakPanel breakPanel = new BreakPanel(getSize(), onEndBreakAction);
        removeAll();
        add(breakPanel, DEFAULT_LAYER);
        repaint();
    }

    protected void endTask() {
        endTaskLabel.setText("Task finished. Thank You!");
        endTaskLabel.setFont(new Font("Roboto", Font.BOLD, 50));
        endTaskLabel.setForeground(COLORS.BLACK);
        endTaskLabel.setSize(700, 50);
        endTaskLabel.setHorizontalAlignment(SwingConstants.CENTER);
        endTaskLabel.setVerticalAlignment(SwingConstants.CENTER);
        int centX = (getWidth() - endTaskLabel.getWidth()) / 2;
        int centY = (getHeight() - endTaskLabel.getHeight()) / 2;
        endTaskLabel.setBounds(centX, centY, endTaskLabel.getWidth(), endTaskLabel.getWidth());

        removeAll();
        add(endTaskLabel, DEFAULT_LAYER);
        repaint();
    }

    private final AbstractAction onEndBreakAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            conLog.trace("Break Ended");
            activeBlock = blocks.get(activeBlock.blockNum);
            startBlock();
        }
    };
}
