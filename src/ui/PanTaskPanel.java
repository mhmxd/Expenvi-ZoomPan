package ui;

import enums.Task;
import jdk.jshell.execution.Util;
import model.BaseBlock;
import model.PanTrial;
import model.ZoomTrial;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.Constants;
import tool.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;

import static tool.Constants.*;

public class PanTaskPanel extends TaskPanel {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    // Constants
    public static final int NUM_PAN_BLOCKS = 1;
    public static final int NUM_PAN_TRIALS_IN_BLOCK = 6; // Christian only used this, no blocking
    public static final int PAN_VP_SIZE_mm = 200;
    public static final int FOCUS_SIZE = 200;
    public static final int CIRCLE_SIZE = 60;

    // Experiment
    private final Task task;
    private final Moose moose;
    private final boolean startOnLeft;
    private final int pvpSize; // Size of the viewport in px
    private final int lrMargin; // Left-right margin in px (mm comes from ExperimentFrame)

    // Viewport
    private PanViewPort panViewPort;

    /**
     * Constructor
     * @param dim Dimension – Desired dimension of the panel
     * @param ms Moose – Reference to the Moose
     * @param tsk Task – Type of the task
     */
    public PanTaskPanel(Dimension dim, Moose ms, Task tsk) {
        super();

        setSize(dim);
        setLayout(null);

        startOnLeft = new Random().nextBoolean(); // Randomly choose whether to start traials on the left or right
        lrMargin = Utils.mm2px(ExperimentFrame.LR_MARGIN_MM);
        pvpSize = Utils.mm2px(PAN_VP_SIZE_mm);

        task = tsk;
        moose = ms;

        createBlocks();

    }

    @Override
    protected void createBlocks() {
        super.createBlocks();

        for (int i = 0; i < NUM_PAN_BLOCKS; i++) {
            blocks.add(new BaseBlock(i + 1, task, 1));
        }
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);

        if (aFlag) {
            starTask();
        }
    }

    @Override
    protected void startBlock() {
        super.startBlock();

        activeTrial = activeBlock.getTrial(1); // Get the trial
        showActiveTrial();
    }

    /**
     * Show the active trial
     */
    private void showActiveTrial() {

        // Clear the viewport (if added)
        if (getIndexOf(panViewPort) != -1) {
            remove(panViewPort);
            repaint();
        }

        // Update prgogressLabel (trial/block)
        progressLabel.setText("Trial: " + activeTrial.trialNum + "/" + "Block: " + activeTrial.blockId);
        progressLabel.setVisible(true);

        // Create the viewport for showing the trial
        panViewPort = new PanViewPort((PanTrial) activeTrial, endTrialAction);
        panViewPort.setBorder(BORDERS.BLACK_BORDER);
        Point position = findPositionForViewport(activeTrial.trialNum);
        panViewPort.setBounds(position.x, position.y, pvpSize, pvpSize);
        panViewPort.setVisible(true);
        add(panViewPort, JLayeredPane.PALETTE_LAYER);
    }

    /**
     * Generate a random position for the viewport
     * Position is alteranted between left and right
     * @return Point position
     */
    private Point findPositionForViewport(int trNum) {
        Point position = new Point();
        position.y = (getHeight() - pvpSize) / 2; // Center
        conLog.trace("PanelH = {}; TitleBarH = {}; ZVPSize = {}; Center = {}",
                getHeight(), getInsets().top, pvpSize, position.y);
        int randLeftX = new Random().nextInt(lrMargin, getWidth()/2 - pvpSize);
        int randRightX = new Random().nextInt(getWidth()/2, getWidth() - lrMargin - pvpSize);
        if (startOnLeft) {
            if (trNum % 2 == 1) position.x = randLeftX; // Trials 1, 3, ... are on left
            else position.x = randRightX; // Trials 2, 4, ... on right
        } else {
            if (trNum % 2 == 1) position.x = randRightX; // Trials 1, 3, ... on right
            else position.x = randLeftX; // Trials 2, 4, ... on left
        }

        return position;
    }

    @Override
    protected void nextTrial() {
        super.nextTrial();
        conLog.trace("nextTrial");
        if (activeBlock.isBlockFinished(activeTrial.trialNum)) { // Block finished

        } else { // More trials in the block
            activeTrial = activeBlock.getTrial(activeTrial.trialNum + 1);
            showActiveTrial();
        }

    }

    // Actions -----------------------------------------------------------------------------------
    private final AbstractAction endTrialAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            conLog.trace("Trial Ended");
            nextTrial();
        }
    };
}
