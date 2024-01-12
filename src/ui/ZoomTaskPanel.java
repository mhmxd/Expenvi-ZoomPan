package ui;

import enums.Task;
import listener.MooseListener;
import model.BaseBlock;
import model.ZoomTrial;
import moose.Memo;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

import static tool.Constants.*;

public class ZoomTaskPanel
        extends TaskPanel
        implements MouseMotionListener, MouseWheelListener, MouseListener, MooseListener {

    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    // Constants
    public static final int NUM_ZOOM_BLOCKS = 3;
    public static final int NUM_ZOOM_REPETITIONS = 3;
    public static final int ZOOM_VP_SIZE_mm = 100;
    public static final double WHEEL_STEP_SIZE = 0.25;
    public static final int ERROR_ROW = 1;

    // Experiment
    private final Task task;
    private final Moose moose;
    private final boolean startOnLeft;
    private final int zvpSize; // Size of the viewport in px
    private final int lrMargin; // Left-right margin in px (mm comes from ExperimentFrame)

    // Viewport
    private ZoomViewport zoomViewPort;

    // -------------------------------------------------------------------------------------------
    /**
     * Constructor
     * @param dim Dimension – Desired dimension of the panel
     * @param ms Moose – Reference to the Moose
     * @param tsk Task – Type of the task
     */
    public ZoomTaskPanel(Dimension dim, Moose ms, Task tsk) {
        super();

        setSize(dim);
        setLayout(null);

        startOnLeft = new Random().nextBoolean(); // Randomly choose whether to start traials on the left or right
        zvpSize = Utils.mm2px(ZOOM_VP_SIZE_mm);
        lrMargin = Utils.mm2px(ExperimentFrame.LR_MARGIN_MM);

        task = tsk;
        moose = ms;

        createBlocks();

//        addMouseListener(this);
//        addMouseMotionListener(this);
//        addMouseWheelListener(this);
//        moose.addMooseListener(this);
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);

        if (aFlag) {
            // Begin
            starTask();
        }
    }

    /**
     * Cerate zoom blocks
     */
    @Override
    protected void createBlocks() {
        super.createBlocks();

        for (int i = 0; i < NUM_ZOOM_BLOCKS; i++) {
            blocks.add(new BaseBlock(i + 1, task, NUM_ZOOM_REPETITIONS));
        }
    }

    /**
     * Start a block
     */
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
        if (getIndexOf(zoomViewPort) != -1) {
            remove(zoomViewPort);
            repaint();
        }

        // Update prgogressLabel (trial/block)
        progressLabel.setText("Trial: " + activeTrial.trialNum + "/" + "Block: " + activeTrial.blockId);
        progressLabel.setVisible(true);

        // Create the viewport for showing the trial
        zoomViewPort = new ZoomViewport(moose, (ZoomTrial) activeTrial, endTrialAction);
        zoomViewPort.setBorder(BORDERS.BLACK_BORDER);
        Point position = findPositionForViewport(activeTrial.trialNum);
        zoomViewPort.setBounds(position.x, position.y, zvpSize, zvpSize);
        zoomViewPort.setVisible(true);
        add(zoomViewPort, JLayeredPane.PALETTE_LAYER);
    }

    /**
     * Generate a random position for the viewport
     * Position is alteranted between left and right
     * @return Point position
     */
    private Point findPositionForViewport(int trNum) {
        Point position = new Point();
        position.y = (getHeight() - zvpSize) / 2; // Center
        conLog.trace("PanelH = {}; TitleBarH = {}; ZVPSize = {}; Center = {}",
                getHeight(), getInsets().top, zvpSize, position.y);
        int randLeftX = new Random().nextInt(lrMargin, getWidth()/2 - zvpSize);
        int randRightX = new Random().nextInt(getWidth()/2, getWidth() - lrMargin - zvpSize);
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

    // Mouse -------------------------------------------------------------------------------------
    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

    }

    // Moose --------------------------------------------------------------------------------------
    @Override
    public void mooseClicked(Memo e) {

    }

    @Override
    public void mooseScrolled(Memo e) {

    }

    @Override
    public void mooseWheelMoved(Memo e) {

    }

    @Override
    public void mooseZoomStart(Memo e) {

    }
}
