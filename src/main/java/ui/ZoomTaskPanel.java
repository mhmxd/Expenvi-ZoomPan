package ui;

import control.Logex;
import enums.ErrorEvent;
import enums.Task;
import enums.TrialEvent;
import enums.TrialStatus;
import listener.MooseListener;
import model.BaseBlock;
import model.ZoomTrial;
import moose.Memo;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.MoCoord;
import tool.MoDimension;
import tool.MoSVG;
import tool.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

import static tool.Constants.*;
import static ui.ExperimentFrame.*;

public class ZoomTaskPanel
        extends TaskPanel
        implements MouseMotionListener, MouseWheelListener, MouseListener, MooseListener {

    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    // Constants

    public static final double VIEWPPORT_SIZE_mm = 200;
//    public static final double WHEEL_STEP_SIZE = 0.25;
//    public static final float NOTCH_SCALE = 0.1f; // Based on Windows 10

//    public static final int ZOOM_N_ELEMENTS = 31; // # elements in rows = columns
//    public static final int ELEMENT_NOTCH_RATIO = 3;
    public static final double NOTCH_MM = 1;
    public static int N_ELEMENTS;
//    public static final int N_ELEMENTS = (ExperimentFrame.TOTAL_N_NOTCHES / ELEMENT_NOTCH_RATIO) * 2 + 1;
    public static final int ZOOM_OUT_ELEMENT_SIZE = 80; // Diameter of the elements (px)
    public static final int ZOOM_IN_ELEMENT_SIZE = 170; // W of the elements (px)
    public static final int ZOOM_IN_ELEMENT_RADIUS = 50; // Corner radius of the elements (px)
    public static final int GUTTER_RATIO = 3;

    public static final int MAX_ZOOM_LEVEL = 1700;

    public static final String ZOOM_OUT_SVG_FILE_NAME = "zoom_out.svg";
    public static final String ZOOM_IN_SVG_FILE_NAME = "zoom_in.svg";

    // Experiment
    private final Task task;
    private final Moose moose;
    private final boolean startOnLeft;
    private final int zvpSize; // Size of the viewport in px
    private final int lrMargin; // Left-right margin in px (mm comes from ExperimentFrame)

    // View
    private ZoomViewport zoomViewPort;
    private final ArrayList<MoCoord> zoomElements = new ArrayList<>(); // Hold the grid coords + ids

    // -------------------------------------------------------------------------------------------
    /**
     * Constructor
     * @param dim Dimension – Desired dimension of the panel
     * @param ms Moose – Reference to the Moose
     * @param tsk Task – Type of the task
     */
    public ZoomTaskPanel(Dimension dim, Moose ms, Task tsk) {
        super(dim);

        setSize(dim);
        setLayout(null);

        startOnLeft = new Random().nextBoolean(); // Randomly choose whether to start traials on the left or right
        zvpSize = Utils.mm2px(VIEWPPORT_SIZE_mm);
        lrMargin = Utils.mm2px(ExperimentFrame.LR_MARGIN_MM);

        task = tsk;
        moose = ms;

        createBlocks();

        // Generate the zooming SVG
        N_ELEMENTS = (ExperimentFrame.MAX_NOTCHES / ExperimentFrame.NOTCHES_IN_ELEMENT) * 2 + 1;
//        final int N_ELEMENTS = (ExperimentFrame.TOTAL_N_NOTCHES / ELEMENT_NOTCH_RATIO) + 1;
        if (task.equals(Task.ZOOM_IN)) {
            MoSVG.genCircleGrid(
                    ZOOM_IN_SVG_FILE_NAME,
                    N_ELEMENTS,
                    ZOOM_IN_ELEMENT_SIZE,
                    0,
                    COLORS.BLUE);
        } else {
            MoSVG.genCircleGrid(
                    ZOOM_OUT_SVG_FILE_NAME,
                    N_ELEMENTS,
                    ZOOM_OUT_ELEMENT_SIZE,
                    0,
                    COLORS.YELLOW);
        }


        // Add the elements to the list (done once)
        for (int r = 0; r < N_ELEMENTS; r++) {
            for (int c = 0; c < N_ELEMENTS; c++) {
                zoomElements.add(new MoCoord(r, c, String.format("r%d_c%d", r, c)));
            }
        }

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
            blocks.add(new BaseBlock(i + 1, task, NUM_ZOOM_REPS));
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
    @Override
    protected void showActiveTrial() {

        // Clear the viewport (if added)
        if (getIndexOf(zoomViewPort) != -1) {
            remove(zoomViewPort);
            repaint();
        }

        // Update prgogressLabel (trial/block)
        progressLabel.setText("Trial: " + activeTrial.trialNum + " – " + "Block: " + activeTrial.blockNum);
//        progressLabel.setVisible(true);

        // Create the viewport for showing the trial
//        zoomViewPort = new ZoomViewport(moose, (ZoomTrial) activeTrial, zoomElements, endTrialAction);
        MoDimension moDim = new MoDimension(zvpSize);
        zoomViewPort = new ZoomViewport(moDim, moose, (ZoomTrial) activeTrial, endTrialAction);
        Point position = findPositionForViewport(activeTrial.trialNum);
        zoomViewPort.setLocation(position);
//        zoomViewPort.setBounds(position.x, position.y, zvpSize, zvpSize);
        zoomViewPort.setVisible(true);
        add(zoomViewPort, PALETTE_LAYER);

        // Log
        Logex.get().activateTrial(activeTrial);
        Logex.get().logEvent(TrialEvent.TRIAL_OPEN);
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

    // Actions -----------------------------------------------------------------------------------
    private final AbstractAction endTrialAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Logex.get().logEvent(TrialEvent.SPACE_PRESS);
            double enterToSpace = Logex.get().getDurationSec(
                    TrialEvent.getLast(TrialEvent.VIEWPORT_ENTER),
                    TrialEvent.SPACE_PRESS);
            double firstZoomToSpace = Logex.get().getDurationSec(
                    TrialEvent.getFirst(TrialEvent.ZOOM),
                    TrialEvent.SPACE_PRESS);
            double enterToLastZoom = Logex.get().getDurationSec(
                    TrialEvent.getLast(TrialEvent.VIEWPORT_ENTER),
                    TrialEvent.getLast(TrialEvent.ZOOM));
            double firstZoomToLastZoom = Logex.get().getDurationSec(
                    TrialEvent.getFirst(TrialEvent.ZOOM),
                    TrialEvent.getLast(TrialEvent.ZOOM));

            conLog.info("En->lZ = {}, fZ->lZ = {}", enterToLastZoom, firstZoomToLastZoom);

            endTrial(TrialStatus.HIT);
        }
    };

    // Mouse -------------------------------------------------------------------------------------
    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Show an error (ptc. shouldn't click)
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Show an error (ptc. shouldn't press)
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
        // Press errro should cover this
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Log movement AFTER trial has been opened
        if (Logex.get().hasLogged(TrialEvent.TRIAL_OPEN)) Logex.get().logEvent(TrialEvent.MOVE);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // TODO Show error if not over the ViewPort
    }

    // Moose --------------------------------------------------------------------------------------
    @Override
    public void mooseClicked(Memo mem) {
        Logex.get().logError(ErrorEvent.CLICK, ErrorEvent.OUTSIDE_ZVP);
        // TODO Show error (no Moose clicking)
    }

    @Override
    public void mooseScrolled(Memo mem) {

    }

    @Override
    public void mooseWheelMoved(Memo mem) {
        Logex.get().logError(ErrorEvent.CLICK, ErrorEvent.OUTSIDE_ZVP);
        // TODO Show error if not over ViewPort
    }

    @Override
    public void mooseZoomStart(Memo mem) {

    }
}
