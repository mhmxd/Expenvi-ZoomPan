package ui;

import com.kitfox.svg.*;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.app.beans.SVGPanel;
import control.Logex;
import enums.Task;
import enums.TrialEvent;
import jdk.jshell.execution.Util;
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
    private int currentNotch;

    // Tools
    private Robot robot;
    private final URI svgURI;
    private SVGRoot svgRoot;
    private final SVGIcon svgIcon;

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

            // Find the target elements (to be colored green)
            final ArrayList<MoCoord> targetCoords = findTargetElements();
            conLog.trace("Targets: {}", targetCoords);
            final ArrayList<MoCoord> errorCoords = findErrorElements(targetCoords);

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
            currentNotch = trial.startNotch;
            svgSize = findSVGSize(trial.startNotch);
            conLog.info("New Size = {}", svgSize);

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
        int leftmostCol = (trial.targetNotch - NOTCH_TOL) / EL_NOTCH_RATIO;
        int leftCol = (trial.targetNotch + NOTCH_TOL) / EL_NOTCH_RATIO;
//        conLog.info("LMC {}; LC {}", leftmostCol, leftCol);
//        int rightmostCol = N_ELEMENTS - ((trial.targetNotch - NOTCH_TOL - 1) / EL_NOTCH_RATIO);
        int rightmostCol = lastElement - leftmostCol;
        int rightCol = lastElement - leftCol;
//        int rightCol = N_ELEMENTS - ((trial.targetNotch + NOTCH_TOL) / EL_NOTCH_RATIO);
//        conLog.info("RMC {}; RC {}", rightmostCol, rightCol);
        int topmostRow = (trial.targetNotch - NOTCH_TOL) / EL_NOTCH_RATIO;
        int topRow = (trial.targetNotch + NOTCH_TOL) / EL_NOTCH_RATIO;

        int lowermostRow = lastElement - topmostRow;
        int lowerRow = lastElement - topRow;

        for (int row = topmostRow; row <= topRow; row++) {
            for (int col = leftmostCol; col <= rightmostCol; col++) {
                result.add(new MoCoord(row, col));
            }
        }

        // Right side
        for (int row = topmostRow; row <= lowermostRow; row++) {
            for (int col = rightCol; col <= rightmostCol; col++) {
                result.add(new MoCoord(row, col));
            }
        }

        // Bottom side
        for (int row = lowerRow; row <= lowermostRow; row++) {
            for (int col = leftmostCol; col <= rightmostCol; col++) {
                result.add(new MoCoord(row, col));
            }
        }

        // Left side
        for (int row = topmostRow; row <= lowermostRow; row++) {
            for (int col = leftmostCol; col <= leftCol; col++) {
                result.add(new MoCoord(row, col));
            }
        }

        return result;
    }

    /**
     * Find the error elements (depends on the target elements and tolerance)
     * @param targetElements Target elements
     * @return List of MoCoord
     */
    private ArrayList<MoCoord> findErrorElements(ArrayList<MoCoord> targetElements) {
        ArrayList<MoCoord> result = new ArrayList<>();

        final int tol = ExperimentFrame.TARGET_TOLERANCE;
        final int notchInElement = ExperimentFrame.NOTCHES_IN_ELEMENT;
        final int centerElement = ZoomTaskPanel.N_ELEMENTS / 2 + 1;

        // Sort the target elements' X
        final List<Integer> xList = new ArrayList<>();
        for (MoCoord coord : targetElements) {
            xList.add(coord.x);
        }
        Collections.sort(xList);

        // Find the bounds of targetElements
        int leftOutBoundary = xList.get(0);
        int rightOutBoundary = xList.get(xList.size() - 1);
        int leftInBoundary = leftOutBoundary + 2 * tol / notchInElement;
        int rightInBoundary = rightOutBoundary - 2 * tol / notchInElement;
        conLog.info("Boundaries: {}; {}", leftInBoundary, rightInBoundary);
        if (trial.task.equals(Task.ZOOM_OUT)) {
            // Color the outside
            for (MoCoord element : zoomElements) {
                if (element.isEitherLess(leftOutBoundary) || element.isEitherMore(rightOutBoundary)) {
                    result.add(element);
                }
            }
        }

        if (trial.task.equals(Task.ZOOM_IN)) {
            // Color the inside
            for (MoCoord element : zoomElements) {
//                conLog.info("Element: {}", element);
                if (element.isBothInBetween(leftInBoundary, rightInBoundary, "00")) {
                    conLog.info("Element In: {}", element);
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
        return Utils.isBetween(currentNotch,
                trial.targetNotch - ExperimentFrame.TARGET_TOLERANCE,
                trial.targetNotch + ExperimentFrame.TARGET_TOLERANCE,
                "11");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2D = (Graphics2D) g;

        // Load the svg root
        SVGDiagram diagram = SVGCache.getSVGUniverse().getDiagram(svgURI);
        svgRoot = diagram.getRoot();

        // Transform the svg
        svgIcon.setPreferredSize(new Dimension(svgSize, svgSize));
        int newCoord = - (svgSize - getWidth()) / 2;
        svgIcon.paintIcon(this, g2D, newCoord, newCoord);
        conLog.info("Paint svgSize = {}", svgSize);
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

    private int findSVGSize(int notch) {
        double dVisibleEl = -((double) 1 / ExperimentFrame.NOTCHES_IN_ELEMENT) * 2 * notch; // 2 for both sides
        conLog.info("dVisibleEl = {}", dVisibleEl);
        return findSVGSize(dVisibleEl);
    }

    /**
     * Set the svg size and repaint
     * @param dVisibleElements Diff in the number of visible elements
     */
    private int findSVGSize(double dVisibleElements) {
        nVisibleEl = Utils.modifyInRange(
                nVisibleEl, dVisibleElements,
                1, ZoomTaskPanel.N_ELEMENTS);
        conLog.info("nVis = {}", nVisibleEl);
        double cirSize = getWidth() / nVisibleEl;
        return (int) (cirSize * ZoomTaskPanel.N_ELEMENTS);
    }

    // -------------------------------------------------------------------------------------------
    private final AbstractAction SPACE_PRESS = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {

            if (checkHit()) {
                conLog.debug("Time from first zoom = {}",
                        Logex.get().getTrialInstant(TrialEvent.FIRST_ZOOM).until(
                                Instant.now(),
                                ChronoUnit.MILLIS));
                endTrialAction.actionPerformed(e);
            } else { // Not the correct zoom level
                borderBlinker.start();
            }
        }
    };

    private final AbstractAction UP_PRESS = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
            double dNVisibleEl = -0.5;
            svgSize = findSVGSize(dNVisibleEl);
            repaint();
        }
    };

    private final AbstractAction DOWN_PRESS = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
            double dNVisibleEl = 0.5;
            svgSize = findSVGSize(dNVisibleEl);
            repaint();
        }
    };

    // --------------------------------------------------------------------------------------------
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // If not in focus, exit
        if (!hasFocus) return;

        // If a timer is running, stop it
        if (borderBlinker.isRunning()) {
            borderBlinker.stop();
            setBorder(BORDERS.FOCUSED_BORDER);
        }

        // If it's the first zoom and the direction is not determined, set the direction
        Logex.get().log(TrialEvent.ZOOM);
//        if (firstZoomInRightDirection == null) {
//            if (isZoomIn) firstZoomInRightDirection = e.getWheelRotation() < 0;
//            else firstZoomInRightDirection = e.getWheelRotation() > 0;
//        }

        // TODO: Put instant when the correct zoom level is reached

        int rot = e.getWheelRotation();
        conLog.info("Rotation = {}", rot);
        currentNotch -= rot;
        svgSize = findSVGSize(-rot);
        repaint();

        // LOG
        Logex.get().log(TrialEvent.ZOOM);
    }

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
        hasFocus = true;
        setBorder(BORDERS.FOCUSED_BORDER);

        Logex.get().log(TrialEvent.FOCUS_ENTER);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hasFocus = false;
        setBorder(BORDERS.FOCUS_LOST_BORDER);

        Logex.get().log(TrialEvent.FOCUS_EXIT);
    }

    @Override
    public void mooseClicked(Memo e) {
        borderBlinker.start();
    }

    @Override
    public void mooseScrolled(Memo mem) {

    }

    @Override
    public void mooseWheelMoved(Memo e) {

        // If not in focus, exit
        if (!hasFocus) return;

        // If a timer is running, stop it
        if (borderBlinker.isRunning()) {
            borderBlinker.stop();
            setBorder(BORDERS.FOCUSED_BORDER);
        }

        //-- Christian's
        // Parse the scaling factor from the Memo
//        float scale = Float.parseFloat(e.getValue1());

        // If the scaling factor is zero, do nothing
//        if (scale == 0.0) {
//            return;
//        }

        // Determine the first zoom direction if not set
//        if (firstZoomInRightDirection == null) {
//            if (trial.task.equals(Task.ZOOM_IN)) {
//                firstZoomInRightDirection = scale < 0;
//            } else {
//                firstZoomInRightDirection = scale > 0;
//            }
//        }

        // If the zoom level is already at maximum and the scale is positive, do nothing
//        if (zoomLevel >= 35 + 1 && scale > 0) {
//            return;
//        }

        // Update the zoom level based on the scaling input
        // scale * 4: Is 2 rows for 1 Zoom-Level
//        zoomLevel = (int) (mooseZoomStartLevel + (scale * 4));

        Logex.get().log(TrialEvent.ZOOM);

//        final float zoomAmp = e.getV1Float();
//
//        // Exponential
////        final float newZoomLevel = zoomLevel + zoomAmp * scale * zoomLevel;
////        if (Utils.isBetween(newZoomLevel, MIN_ZOOM_LEVEL, MAX_ZOOM_LEVEL, "11")) {
////            zoomLevel = newZoomLevel;
////            conLog.info("Zoom Level = {}", zoomLevel);
////            // Repaint to reflect the changes
////            repaint();
////        }
//
//        // Constant
//        final float GAIN = 0.08f;
//        final float newZoomLevel = level + zoomAmp * GAIN;
//        if (Utils.isBetween(newZoomLevel, MIN_ZOOM_LEVEL, MAX_ZOOM_LEVEL, "11")) {
//            level = newZoomLevel;
//            conLog.info("Zoom Level = {}", level);
//            // Repaint to reflect the changes
//            repaint();
//        }

        // Repaint the component to reflect the zooming

        repaint();

        // LOG
        Logex.get().log(TrialEvent.ZOOM);
    }

    @Override
    public void mooseZoomStart(Memo e) {
//        mooseZoomStartLevel = level;
    }
}
