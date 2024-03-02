package ui;

import control.Logex;
import enums.Task;
import enums.TrialEvent;
import enums.TrialStatus;
import model.BaseBlock;
import model.PanTrial;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static tool.Constants.*;

public class PanTaskPanel extends TaskPanel {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    // Constants
//    public static final int NUM_PAN_TRIALS_IN_BLOCK = 6; // Christian only used this, no blocking
    public static final double VP_SIZE_mm = 200;
    public static final double FOCUS_SIZE_mm = 0.3 * VP_SIZE_mm;
    public static final int ERROR_DURATION = 3 * 1000; // (ms) Duration to keep the error visible

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
        super(dim);

        setSize(dim);
        setLayout(null);

        startOnLeft = new Random().nextBoolean(); // Randomly choose whether to start traials on the left or right
        lrMargin = Utils.mm2px(ExperimentFrame.LR_MARGIN_MM);
        pvpSize = Utils.mm2px(VP_SIZE_mm);

        task = tsk;
        moose = ms;

        createBlocks();

    }

    @Override
    protected void createBlocks() {
        super.createBlocks();

        for (int i = 0; i < ExperimentFrame.NUM_PAN_BLOCKS; i++) {
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
     * Clear the viewport (if added)
     */
    private void clearActiveLayer() {
        for (Component c : getComponentsInLayer(JLayeredPane.PALETTE_LAYER)) {
            remove(c);
            repaint();
        }
//        if (getIndexOf(panViewPort) != -1) {
//            remove(panViewPort);
//            repaint();
//        }
    }

    /**
     * Clear the viewport and show an error in the middle of the panel
     * @param message Error mesage
     */
    private void showError(String message) {
        clearActiveLayer();

        JLabel errLabel = new JLabel(message);
        errLabel.setBounds(getWidth() - 1200, getHeight() / 2, 1000, 30);
        errLabel.setFont(new Font(errLabel.getFont().getFontName(), Font.PLAIN, 30));
        errLabel.setForeground(Color.red);
        errLabel.setVerticalAlignment(JLabel.CENTER);
        errLabel.setHorizontalAlignment(JLabel.CENTER);
        add(errLabel, JLayeredPane.PALETTE_LAYER);
        repaint();

        // Disappear after set time
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                endTrial(TrialStatus.ERROR);
            }
        }, ERROR_DURATION);

    }

    /**
     * Show the active trial
     */
    @Override
    protected void showActiveTrial() {
        conLog.info(activeTrial);
        // Clear the viewport
        clearActiveLayer();

        // Update prgogressLabel (trial/block)
        progressLabel.setText("Trial: " + activeTrial.trialNum + " – " + "Block: " + activeTrial.blockNum);
//        progressLabel.setVisible(true);

        // Create the viewport for showing the trial
        panViewPort = new PanViewPort(moose, (PanTrial) activeTrial, onFinishTrialAction);
        panViewPort.setBorder(BORDERS.BLACK_BORDER);
        Point position = findPositionForViewport(activeTrial.trialNum);
        panViewPort.setBounds(position.x, position.y, pvpSize, pvpSize);
        panViewPort.setVisible(true);
        add(panViewPort, JLayeredPane.PALETTE_LAYER);

        // Set up the Logex for this trial
        Logex.get().activateTrial(activeTrial);
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

    // Actions -----------------------------------------------------------------------------------
    private final AbstractAction onFinishTrialAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            conLog.debug("Trial {} Ended", activeTrial.id);
            // There was an error
            if (e.getID() == TrialStatus.ERROR) {
                conLog.info("Error: {}", e.getActionCommand());
                // Curve was out more than 10% of the time
                if (e.getActionCommand() == TrialStatus.TEN_PERCENT_OUT) {
                    // Show the error (automatically goes to the next trial)
                    showError("The curve must not be outside for more than 10% of the time!");
                }
            } else {
                double firstPanToLastPan = Logex.get().getDurationSec(
                        TrialEvent.getFirst(TrialEvent.PAN),
                        TrialEvent.getLast(TrialEvent.PAN));

                conLog.info("Time: FP-LP = {}", firstPanToLastPan);
                endTrial(TrialStatus.HIT);
            }
        }
    };
}
