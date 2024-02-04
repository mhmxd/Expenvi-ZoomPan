package ui;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.paint.SimplePaintSVGPaint;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.google.common.collect.Range;
import com.kitfox.svg.*;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.app.beans.SVGPanel;
import control.Logex;
import enums.Task;
import enums.TrialEvent;
import listener.MooseListener;
import model.ZoomTrial;
import moose.Memo;
import moose.Moose;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.MoCoord;
import tool.MoKey;
import tool.Utils;
import tool.ZoomParserProvider;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

import static tool.Constants.*;

public class ZoomViewport extends JPanel implements MouseListener, MouseWheelListener, MooseListener {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    private final int BLINKER_DELAY = 100; // ms

    private final ZoomTrial trial;
    private final ArrayList<MoCoord> zoomElements;
    private final AbstractAction endTrialAction; // Received from higher levels

    private double zoomLevel;
    private double mooseZoomStartLevel;
    private Boolean firstZoomInRightDirection;
    private boolean hasFocus;

    // Tools
    private Robot robot;
    private final URI svgURI;
    private final URL svgURL;
    private ViewBox svgViewBox;
    private SVGDocument svgDocument;

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

        //-- Set up svg
        try {
            svgURI = Paths.get(ZoomTaskPanel.ZOOM_OUT_SVG_FILE_NAME).toUri();
            svgURL = svgURI.toURL();

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

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

        //-- Set up listeners
        addMouseWheelListener(this);
        addMouseListener(this);
        moose.addMooseListener(this);

    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);

        if (aFlag) {
            svgViewBox = new ViewBox(0, 0, getWidth(), getHeight());
            // Find the target elements (to be colored green)
            final ArrayList<MoCoord> targetElements = findTargetElements();
            final ArrayList<MoCoord> errorElements = findErrorElements(targetElements);
            conLog.trace("Targets: {}", targetElements);

            // Load svg with custom zoom parser
            svgDocument = new SVGLoader().load(
                    svgURL,
                    new ZoomParserProvider(targetElements, errorElements));

        }
    }

    /**
     * Calculate and return the list of (r,c) to set as target
     * @return Map (keys: rows, values: cols)
     */
    private ArrayList<MoCoord> findTargetElements() {
        ArrayList<MoCoord> result = new ArrayList<>();

        final int TOL = ZoomTaskPanel.TOLERANCE;
        final int LAST = ZoomTaskPanel.GRID_SIZE;
        conLog.info("Trial: {}", trial);

        // Top side
        int leftmostCol = Math.max(1, trial.endLevel - TOL);
        int rightmostCol = LAST - (trial.endLevel - 1) + 1; // -1 is bc ZLs start from 1
        int topmostRow = Math.max(1, trial.endLevel - TOL);
        int topRow = trial.endLevel + TOL;
        for (int row = topmostRow; row <= topRow; row++) {
            for (int col = leftmostCol; col <= rightmostCol; col++) {
                result.add(new MoCoord(row, col));
            }
        }

        // Right side
        int rightCol = LAST - (trial.endLevel - 1) - TOL;
        int lowermostRow = Math.min(LAST, LAST - (trial.endLevel - 1) + TOL);
        for (int row = topmostRow; row <= lowermostRow; row++) {
            for (int col = rightCol; col <= rightmostCol; col++) {
                result.add(new MoCoord(row, col));
            }
        }

        // Bottom side
        int lowerRow = LAST - (trial.endLevel - 1) - TOL;
        for (int row = lowerRow; row <= lowermostRow; row++) {
            for (int col = leftmostCol; col <= rightmostCol; col++) {
                result.add(new MoCoord(row, col));
            }
        }

        // Left side
        int leftCol = trial.endLevel + TOL;
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

        // Find the bounds of targetElements
        int leftOutBoundary = Utils.getMinXFromList(targetElements);
        int rightOutBoundary = Utils.getMaxXFromList(targetElements);
        int leftInBoundary = leftOutBoundary + ZoomTaskPanel.TOLERANCE;
        int rightInBoundary = rightOutBoundary - ZoomTaskPanel.TOLERANCE;

        if (trial.task.equals(Task.ZOOM_OUT)) {
            // Color the outside circles
            for (MoCoord element : zoomElements) {
                if (element.isEitherLess(leftOutBoundary) || element.isEitherMore(rightOutBoundary)) {
                    result.add(element);
                }
            }
        }

        if (trial.task.equals(Task.ZOOM_IN)) {
            // Color the inside circles
            for (MoCoord element : zoomElements) {
                if (element.isBothInBetween(leftInBoundary, rightInBoundary, "00")) {
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
        double tolUp = trial.endLevel + 2;
        double tolDown = trial.endLevel - 2 - 0.1;

        return (zoomLevel < tolUp) && (zoomLevel > tolDown);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2D = (Graphics2D) g;

        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (svgDocument != null) {
            svgDocument.render(this, g2D, svgViewBox);
        }

    }

    // -------------------------------------------------------------------------------------------
    private final AbstractAction SPACE_PRESS = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
            conLog.debug("SPACE pressed");
            if (checkHit()) {
                endTrialAction.actionPerformed(e);
            } else { // Not the correct zoom level
                borderBlinker.start();
            }
        }
    };

    // --------------------------------------------------------------------------------------------
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        conLog.trace("Rotation = {}, {}", e.getWheelRotation(), e.getUnitsToScroll());
        // If not in focus, exit
        if (!hasFocus) return;

        // If a timer is running, stop it
        if (borderBlinker.isRunning()) {
            borderBlinker.stop();
            setBorder(BORDERS.FOCUSED_BORDER);
        }

        // If the zoomLevel is at the maximum (100& zoomed-out) and user scrolls down (positive rotation), exit
//        if (zoomLevel >= 35 + 1 && e.getWheelRotation() < 0) return;
//
//        // If the zoomLevel is at the minimum (100% zoomed-in) and user scrolls up (negative rotation), exit
//        if (zoomLevel <= 1 && e.getWheelRotation() > 0) return;

        // If it's the first zoom and the direction is not determined, set the direction
        // TODO: Replace with time (instant)
//        if (firstZoomInRightDirection == null) {
//            if (isZoomIn) firstZoomInRightDirection = e.getWheelRotation() < 0;
//            else firstZoomInRightDirection = e.getWheelRotation() > 0;
//        }

        // TODO: Put instant when the correct zoom level is reached

        // Calculate the scale based on the step size and mouse wheel rotation
        // 1 Zoom-Level is 16 notches
//        double scale = ZoomTaskPanel.WHEEL_STEP_SIZE * e.getWheelRotation();

        // Calculate the zoom level difference
//        double dZL = e.getWheelRotation() * (zoomLevel * ZoomTaskPanel.WHEEL_SCALE);
//        double dZL = e.getWheelRotation() * ZoomTaskPanel.WHEEL_SCALE;
//        double dZL = e.getWheelRotation() * (ZoomTaskPanel.WHEEL_SCALE * 100 / zoomLevel);
//        conLog.trace("ZL {} + dZL {}", zoomLevel, dZL);
//
//        // Only update the zoomLevel if it stays inside the range [1, 35]
//        if (Range.closed(1.0, 35.0).contains(zoomLevel - dZL)) {
//            zoomLevel -= dZL;
//        }

        int rot = e.getWheelRotation();
        svgViewBox.width += rot;
        svgViewBox.height += rot;
        svgViewBox.x -= (float) rot / 2;
        svgViewBox.y -= (float) rot / 2;

        // Repaint to reflect the changes
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
    public void mooseScrolled(Memo e) {

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

        // Parse the scaling factor from the Memo
        float scale = Float.parseFloat(e.getValue1());

        // If the scaling factor is zero, do nothing
        if (scale == 0.0) {
            return;
        }

        // Determine the first zoom direction if not set
        if (firstZoomInRightDirection == null) {
            if (trial.task.equals(Task.ZOOM_IN)) {
                firstZoomInRightDirection = scale < 0;
            } else {
                firstZoomInRightDirection = scale > 0;
            }
        }

        // If the zoom level is already at maximum and the scale is positive, do nothing
        if (zoomLevel >= 35 + 1 && scale > 0) {
            return;
        }

        // Update the zoom level based on the scaling input
        // scale * 4: Is 2 rows for 1 Zoom-Level
        zoomLevel = mooseZoomStartLevel + (scale * 4);

        // Repaint the component to reflect the zooming
        repaint();

        // LOG
        Logex.get().log(TrialEvent.ZOOM);
    }

    @Override
    public void mooseZoomStart(Memo e) {
        mooseZoomStartLevel = zoomLevel;
    }
}
