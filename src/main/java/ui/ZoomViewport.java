package ui;

import com.kitfox.svg.*;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.app.beans.SVGPanel;
import control.Logex;
import enums.ErrorEvent;
import enums.Task;
import enums.TrialEvent;
import listener.MooseListener;
import model.ZoomTrial;
import moose.Memo;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.MoCoord;
import tool.MoKey;
import tool.Utils;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

import static tool.Constants.*;

/**
 * Border: 2 mm
 */
public class ZoomViewport extends JPanel implements MouseListener, MouseWheelListener, MooseListener {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    private final int BLINKER_DELAY = 100; // ms

    private final ZoomTrial trial;
    private final ArrayList<MoCoord> zoomElements;
    private final AbstractAction endTrialAction; // Received from higher levels

    private Boolean firstZoomInRightDirection;
    private boolean hasFocus;
    private int svgSize;
    private double nVisibleEl = ZoomTaskPanel.N_ELEMENTS;
    private float currentNotch;

    // Tools
    private Robot robot;
    private final URI svgURI;
    private SVGRoot svgRoot;
    private final SVGIcon svgIcon;

    // Visual
    private int lmCol, lCol, rmCol, rCol;

    // Timers ---------------------------------------------------------------------------------
    private final Timer borderBlinker = new Timer(BLINKER_DELAY, new ActionListener() {
        private Border currentBorder;
        private int count = 0;
        private final Border border1 = new LineBorder(Color.YELLOW, BORDERS.THICKNESS_2);
        private final Border border2 = new LineBorder(Color.RED, BORDERS.THICKNESS_2);

        @Override
        public void actionPerformed(ActionEvent e) {
            if (count == 0) {
                currentBorder = getBorder();
            }

            if (count % 2 == 0) {
                setBorder(border1);
            } else {
                setBorder(border2);
            }

            count++;

            if (count > 5) {
                borderBlinker.stop();
                setBorder(currentBorder);
                count = 0;
            }
        }
    });

    //-----------------------------------------------------------------------------------------

    /**
     * Constructor
     * @param zTrial ZoomTrial
     * @param endTrAction AbstractAction
     */
    public ZoomViewport(Moose moose, ZoomTrial zTrial, ArrayList<MoCoord> zElements, AbstractAction endTrAction) {
        trial = zTrial;
        zoomElements = new ArrayList<>(zElements);
        endTrialAction = endTrAction;
        conLog.info("Trial: {}", trial);
        conLog.info("Distance = {}", trial.targetNotch - trial.startNotch);

        setLayout(null);
        setBorder(BORDERS.BLACK_BORDER);

        //-- Set up svg
        final String path = trial.task.equals(Task.ZOOM_IN)
                ? ZoomTaskPanel.ZOOM_IN_SVG_FILE_NAME
                : ZoomTaskPanel.ZOOM_OUT_SVG_FILE_NAME;
        svgURI = Paths.get(path).toUri();

        svgIcon = new SVGIcon();
        svgIcon.setAntiAlias(true);
        svgIcon.setAutosize(SVGPanel.AUTOSIZE_BESTFIT);

        //-- Set up robot
        try {
            robot = new Robot();
        } catch (AWTException ignored) {
            conLog.warn("Robot could not be instantiated");
        }

        //-- Set up input map
        getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(MoKey.SPACE, MoKey.SPACE);
        getActionMap().put(MoKey.SPACE, SPACE_PRESS);
        getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(MoKey.UP, MoKey.UP);
        getActionMap().put(MoKey.UP, UP_PRESS);
        getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(MoKey.DOWN, MoKey.DOWN);
        getActionMap().put(MoKey.DOWN, DOWN_PRESS);

        //-- Set up listeners
        addMouseWheelListener(this);
        addMouseListener(this);
        moose.addMooseListener(this);
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);

        if (aFlag) {
            // Log
            Logex.get().logEvent(TrialEvent.TRIAL_OPEN);

            // Find the target elements (to be colored green)
            final ArrayList<MoCoord> targetCoords = findTargetElements();
            conLog.trace("Targets: {}", targetCoords);
            final ArrayList<MoCoord> errorCoords = findErrorElements();

            // Remove the SVG document from the cache to prepare for reloading
            SVGCache.getSVGUniverse().removeDocument(svgURI);

            // Load the svg and set the init info
            svgIcon.setSvgURI(svgURI);
            svgRoot = SVGCache.getSVGUniverse().getDiagram(svgURI).getRoot();

            // Testing
//            String id = String.format("r%d_c%d", 1, 34);
//            SVGElement element = svgRoot.getChild(id);
//            try {
//                if (element != null) {
//                    element.setAttribute("fill", AnimationElement.AT_XML,
//                            COLORS.getHex(COLORS.GREEN));
//                }
//            } catch (SVGException ignored) {
//                conLog.error("Element not found!");
//            }

            // Color the elements
            for (MoCoord coord : targetCoords) {
                String id = String.format("r%d_c%d", coord.x, coord.y);
                SVGElement element = svgRoot.getChild(id);
                try {
                    if (element != null) {
                        element.setAttribute("fill", AnimationElement.AT_XML,
                                COLORS.getHex(COLORS.GREEN));
                    }
                } catch (SVGException ignored) {
                    conLog.error("Element not found!");
                }
            }

            for (MoCoord coord : errorCoords) {
                String id = String.format("r%d_c%d", coord.x, coord.y);
                SVGElement element = svgRoot.getChild(id);
                try {
                    if (element != null) {
                        element.setAttribute("fill", AnimationElement.AT_XML,
                                COLORS.getHex(COLORS.BLACK));
                    }
                } catch (SVGException ignored) {
                }
            }

            // Set the init at the start notch
//            currentNotch = trial.startNotch;
            svgSize = findSVGSize(trial.startNotch);
            conLog.debug("New Size = {}", svgSize);
        }
    }

    /**
     * Calculate and return the list of (r,c) to set as target
     * @return Map (keys: rows, values: cols)
     */
    private ArrayList<MoCoord> findTargetElements() {
        ArrayList<MoCoord> result = new ArrayList<>();

        final int EL_NOTCH_RATIO = ExperimentFrame.NOTCHES_IN_ELEMENT;
        final int NOTCH_TOL = ExperimentFrame.TARGET_TOLERANCE;
        final int N_ELEMENTS = ZoomTaskPanel.N_ELEMENTS;
        final int lastElement = N_ELEMENTS - 1;

        // Top side
        lmCol = (trial.targetNotch - NOTCH_TOL) / EL_NOTCH_RATIO;
        lCol = (trial.targetNotch + NOTCH_TOL - 1) / EL_NOTCH_RATIO; // -1 to not cover the next circle
//        int leftCol = leftmostCol;
        conLog.debug("LMC {}; LC {}", lmCol, lCol);
//        int rightmostCol = N_ELEMENTS - ((trial.targetNotch - NOTCH_TOL - 1) / EL_NOTCH_RATIO);
        rmCol = lastElement - lmCol;
        rCol = lastElement - lCol;
//        int rightCol = N_ELEMENTS - ((trial.targetNotch + NOTCH_TOL) / EL_NOTCH_RATIO);
        conLog.debug("RMC {}; RC {}", rmCol, rCol);
        int topmostRow = lmCol;
        int topRow = lCol;
//        int topRow = topmostRow;

        int lowermostRow = rmCol;
        int lowerRow = rCol;

        for (int row = topmostRow; row <= topRow; row++) {
            for (int col = lmCol; col <= rmCol; col++) {
                result.add(new MoCoord(row, col));
            }
        }

        // Right side
        for (int row = topmostRow; row <= lowermostRow; row++) {
            for (int col = rCol; col <= rmCol; col++) {
                result.add(new MoCoord(row, col));
            }
        }

        // Bottom side
        for (int row = lowerRow; row <= lowermostRow; row++) {
            for (int col = lmCol; col <= rmCol; col++) {
                result.add(new MoCoord(row, col));
            }
        }

        // Left side
        for (int row = topmostRow; row <= lowermostRow; row++) {
            for (int col = lmCol; col <= lCol; col++) {
                result.add(new MoCoord(row, col));
            }
        }

        return result;
    }

    /**
     * Find the error elements (uses lmCol, ... calculated in findTargetElements)
     * @return List of MoCoord
     */
    private ArrayList<MoCoord> findErrorElements() {
        ArrayList<MoCoord> result = new ArrayList<>();

        final int tol = ExperimentFrame.TARGET_TOLERANCE;
        final int notchInElement = ExperimentFrame.NOTCHES_IN_ELEMENT;
        final int centerElement = ZoomTaskPanel.N_ELEMENTS / 2 + 1;

        // Sort the target elements' X
//        final List<Integer> targetList = new ArrayList<>();
//        for (MoCoord coord : targetElements) {
//            targetList.add(coord.x);
//        }
//        Collections.sort(targetList);
//
//        // Find the bounds of targetElements
//        int leftOutBoundary = targetList.get(0);
//        int rightOutBoundary = targetList.get(targetList.size() - 1);
//        int leftInBoundary = leftOutBoundary + 2 * tol / notchInElement;
//        int rightInBoundary = rightOutBoundary - 2 * tol / notchInElement;

        if (trial.task.equals(Task.ZOOM_OUT)) {
            // Color the outside
            for (MoCoord element : zoomElements) {
                if (element.isEitherLess(lmCol) || element.isEitherMore(rmCol)) {
                    result.add(element);
                }
            }
        }

        if (trial.task.equals(Task.ZOOM_IN)) {
            conLog.debug("lcol, rcol: {}, {}", lCol, rCol);
            // Color the inside
            for (MoCoord element : zoomElements) {
                if (element.isBothInBetween(lCol, rCol, "00")) {
                    result.add(element);
                }
            }
        }

        return result;
    }

    /**
     * Check whether the trial is a hit (zoom level is in the correct range)
     * @return True (Hit) or False
     */
    protected boolean checkHit() {
        conLog.debug("CheckHit – currentNotch: {}", currentNotch);
        return Utils.isBetween(Math.floor(currentNotch),
                trial.targetNotch - ExperimentFrame.TARGET_TOLERANCE,
                trial.targetNotch + ExperimentFrame.TARGET_TOLERANCE,
                "11");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
//        Graphics2D g2D = (Graphics2D) g;

        // Transform the svg
        svgIcon.setPreferredSize(new Dimension(svgSize, svgSize));
        int newCoord = - (svgSize - getWidth()) / 2;
        svgIcon.paintIcon(this, g, newCoord, newCoord);
//        conLog.info("Paint svgSize = {}", svgSize);
    }

    /**
     * Find SVG size based on the current notch
     * @return SVG size (px)
     */
//    private int findSVGSize() {
//        // 2 for both sides, (-) bc zooming int decreases num. visible elements
//        double dVisibleEl = -((double) 1 / ExperimentFrame.NOTCHES_IN_ELEMENT) * 2 * currentNotch;
//        conLog.info("dVisibleEl = {}", dVisibleEl);
//        return findSVGSize(dVisibleEl);
//    }

//    private int findSVGSize(int notch) {
//        double dVisibleEl = -((double) 1 / ExperimentFrame.NOTCHES_IN_ELEMENT) * 2 * notch; // 2 for both sides
//        conLog.info("dVisibleEl = {}", dVisibleEl);
//        nVisibleEl = Utils.modifyInRange(
//                nVisibleEl, dVisibleEl,
//                1, ZoomTaskPanel.N_ELEMENTS);
//        conLog.info("nVis = {}", nVisibleEl);
//        double cirSize = getWidth() / nVisibleEl;
//        return (int) (cirSize * ZoomTaskPanel.N_ELEMENTS);
//    }

    /**
     * Set the svg size and repaint
     * @param dNotch Notch (float, from Moose prolly)
     */
    private int findSVGSize(float dNotch) {
//        conLog.info("dVisibleEl = {}", dVisibleEl);
        if (Utils.isBetween(currentNotch + dNotch, 0, ExperimentFrame.MAX_NOTCHES, "11")) {
            nVisibleEl -= ((double) 1 / ExperimentFrame.NOTCHES_IN_ELEMENT) * 2 * dNotch; // 2 for both sides
            currentNotch += dNotch;
        } else if (currentNotch + dNotch < 0) {
            nVisibleEl = ZoomTaskPanel.N_ELEMENTS;
            currentNotch = 0;
        } else {
            nVisibleEl = 1;
            currentNotch = ExperimentFrame.MAX_NOTCHES;
        }

        conLog.trace("dNotch = {} -> currentNotch = {} -> nVisibleEl = {}",
                dNotch, currentNotch, nVisibleEl);
        double cirSize = getWidth() / nVisibleEl;
        return (int) (cirSize * ZoomTaskPanel.N_ELEMENTS);
    }

    // -------------------------------------------------------------------------------------------
    private final AbstractAction SPACE_PRESS = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
            // Pressed outside
            if (!hasFocus) {
                Logex.get().logError(ErrorEvent.SPACE_ZOOM, ErrorEvent.OUTSIDE_ZVP);
            } else {
                if (checkHit()) {
                    endTrialAction.actionPerformed(e);
                } else { // Not the correct zoom level
                    borderBlinker.start();
                }
            }
        }
    };

    private final AbstractAction UP_PRESS = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
            float dNVisibleEl = 1f;
            currentNotch += dNVisibleEl;
            svgSize = findSVGSize(dNVisibleEl);
            repaint();
        }
    };

    private final AbstractAction DOWN_PRESS = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
            float dNVisibleEl = -1f;
            currentNotch += dNVisibleEl;
            svgSize = findSVGSize(dNVisibleEl);
            repaint();
        }
    };

    // --------------------------------------------------------------------------------------------
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // If not in focus, log and exit
        if (!hasFocus) {
            Logex.get().logError(ErrorEvent.SCROLL, ErrorEvent.OUTSIDE_ZVP);
            return;
        }

        // If a timer is running, stop it
        if (borderBlinker.isRunning()) {
            borderBlinker.stop();
            setBorder(BORDERS.FOCUSED_BORDER);
        }

        // Zoom
        int rot = e.getWheelRotation();
        svgSize = findSVGSize(-rot);
        repaint();

        //-- Log
        Logex.get().logEvent(TrialEvent.ZOOM);
        // Wrong direction in first zoom
        if ((rot > 0 && trial.task.equals(Task.ZOOM_IN)) || (rot < 0 && trial.task.equals(Task.ZOOM_OUT))) {
            Logex.get().logError(ErrorEvent.FIRST_ZOOM, ErrorEvent.WRONG_DIRECTION);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Logex.get().logError(ErrorEvent.CLICK, ErrorEvent.INSIDE_ZVP);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        borderBlinker.start();
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        hasFocus = true;
        setBorder(BORDERS.FOCUSED_BORDER);

        // Log
        Logex.get().logEvent(TrialEvent.VIEWPORT_ENTER);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hasFocus = false;
        setBorder(BORDERS.FOCUS_LOST_BORDER);

        // Log
        Logex.get().logEvent(TrialEvent.VIEWPORT_EXIT);
    }

    @Override
    public void mooseClicked(Memo mem) {
        Logex.get().logError(ErrorEvent.CLICK, ErrorEvent.INSIDE_ZVP);
        borderBlinker.start();
    }

    @Override
    public void mooseScrolled(Memo mem) {

    }

    @Override
    public void mooseWheelMoved(Memo mem) {

        // If not in focus, log and exit
        if (!hasFocus) {
            Logex.get().logError(ErrorEvent.SCROLL, ErrorEvent.OUTSIDE_ZVP);
            return;
        }

        // If a timer is running, stop it
        if (borderBlinker.isRunning()) {
            borderBlinker.stop();
            setBorder(BORDERS.FOCUSED_BORDER);
        }

        Logex.get().logEvent(TrialEvent.ZOOM);

        // Repaint the component to reflect the zooming
        final float dYmm = mem.getV1Float();
        final float zoomAmp = dYmm * ExperimentFrame.MOOSE_MM_TO_NOTCH;
        conLog.trace("dYmm; zoomAmp = {}; {}", dYmm, zoomAmp);
        svgSize = findSVGSize(-zoomAmp); // (-) for zoom-in
        repaint();

        //-- Log
        Logex.get().logEvent(TrialEvent.ZOOM);
        // Wrong direction in first zoom
        if ((zoomAmp > 0 && trial.task.equals(Task.ZOOM_IN)) || (zoomAmp < 0 && trial.task.equals(Task.ZOOM_OUT))) {
            Logex.get().logError(ErrorEvent.FIRST_ZOOM, ErrorEvent.WRONG_DIRECTION);
        }
    }

    @Override
    public void mooseZoomStart(Memo mem) {
//        mooseZoomStartLevel = level;
    }
}
