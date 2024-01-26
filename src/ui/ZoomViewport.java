package ui;

import com.kitfox.svg.*;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.app.beans.SVGPanel;
import control.Logex;
import enums.TrialEvent;
import listener.MooseListener;
import model.ZoomTrial;
import moose.Memo;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.MoKey;
import tool.Pair;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static tool.Constants.*;
import static tool.Resources.*;

public class ZoomViewport extends JPanel implements MouseListener, MouseWheelListener, MooseListener {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    private final int BLINKER_DELAY = 100; // ms

    private final ZoomTrial trial;

    private final boolean isZoomIn;
    private double zoomLevel;
    private double mooseZoomStartLevel;
    private Boolean firstZoomInRightDirection;
    private boolean hasFocus;
    private final AbstractAction endTrialAction; // Received from higher levels

    // Tools
    private Robot robot;
    private final SVGIcon svgIcon;
    private final URI svgURI;


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
     * @param zt ZoomTrial
     * @param endTrAction AbstractAction
     */
    public ZoomViewport(Moose moose, ZoomTrial zt, AbstractAction endTrAction) {
        trial = zt;
        endTrialAction = endTrAction;
        isZoomIn = Objects.equals(trial.task, "ZoomIn");

        svgURI = isZoomIn ? SVG.ZOOM_IN_URI : SVG.ZOOM_OUT_URI;

        svgIcon = new SVGIcon();
        svgIcon.setAntiAlias(true);
        svgIcon.setAutosize(SVGPanel.AUTOSIZE_NONE);

        try {
            robot = new Robot();
        } catch (AWTException ignored) {
            conLog.warn("Robot could not be instantiated");
        }

        getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(MoKey.SPACE, MoKey.SPACE);
        getActionMap().put(MoKey.SPACE, SPACE_PRESS);

        addMouseWheelListener(this);
        addMouseListener(this);
        moose.addMooseListener(this);

    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);

        if (aFlag) {
            int temp = (int) Math.ceil(trial.endLevel / 2f) - (isZoomIn ? 1 : 0);
            Set<Pair<Integer, Integer>> pointSet = new HashSet<>();

            if (isZoomIn) {
                for (int i = temp; i <= 35 - temp + 1; i++) {
                    pointSet.add(Pair.create(temp, i));
                    pointSet.add(Pair.create(temp + 1, i));
                    pointSet.add(Pair.create(35 - temp + 1, i));
                    pointSet.add(Pair.create(35 - temp + 1 - 1, i));
                    pointSet.add(Pair.create(i, temp));
                    pointSet.add(Pair.create(i, temp + 1));
                    pointSet.add(Pair.create(i, 35 - temp + 1));
                    pointSet.add(Pair.create(i, 35 - temp + 1 - 1));
                }
            } else {
                for (int i = temp - 1; i <= 35 - temp + 1 + 1; i++) {
                    pointSet.add(Pair.create(temp - 1, i));
                    pointSet.add(Pair.create(temp, i));
                    pointSet.add(Pair.create(35 - temp + 1, i));
                    pointSet.add(Pair.create(35 - temp + 1 + 1, i));
                    pointSet.add(Pair.create(i, temp));
                    pointSet.add(Pair.create(i, temp - 1));
                    pointSet.add(Pair.create(i, 35 - temp + 1));
                    pointSet.add(Pair.create(i, 35 - temp + 1 + 1));
                }
            }

            startTrial(trial.startLevel, trial.endLevel, pointSet);
        }
    }

    /**
     * Start the trial
     * @param startLevel Start zoom level
     * @param endLevel End zoom level (to reach)
     * @param points Set of pairs of points
     */
    private void startTrial(int startLevel, int endLevel, Set<Pair<Integer, Integer>> points) {

        // Initialize variables and clear previous data
        zoomLevel = startLevel;
        this.firstZoomInRightDirection = null;

        // Remove the SVG document from the cache to prepare for reloading
        SVGCache.getSVGUniverse().removeDocument(svgURI);

        // Set the SVG icon's URI
        this.svgIcon.setSvgURI(svgURI);

        // Get the SVG diagram and root
        SVGDiagram diagram = SVGCache.getSVGUniverse().getDiagram(svgURI);
        SVGRoot root = diagram.getRoot();

        // Update the fill color of specific SVG elements based on the given points
        for (Pair<Integer, Integer> p : points) {
            String id = "r" + String.format("%02d", p.first) + "_c" + String.format("%02d", p.second);
            SVGElement element = root.getChild(id);
            try {
                if (element != null) {
                    element.setAttribute("fill", AnimationElement.AT_XML, "#4caf50");
                }
            } catch (SVGException ignored) {
            }
        }

        if (isZoomIn) {
            // Adjust SVG elements for zoom-in mode
            int temp1 = (int) Math.ceil(endLevel / 2f);
            for (int i = temp1 + ZoomTaskPanel.ERROR_ROW; i < 35 - temp1 + ZoomTaskPanel.ERROR_ROW; i++) {
                for (int j = temp1 + ZoomTaskPanel.ERROR_ROW; j < 35 - temp1 + ZoomTaskPanel.ERROR_ROW; j++) {
                    String id = "r" + String.format("%02d", i) + "_c" + String.format("%02d", j);
                    SVGElement element = root.getChild(id);
                    try {
                        if (element != null) {
                            element.setAttribute("fill", AnimationElement.AT_XML, "black");
                        }
                    } catch (SVGException ignored) {
                    }
                }
            }
        } else {
            // Adjust SVG elements for zoom-out mode
            int temp1 = (int) Math.ceil(endLevel / 2f);

            // Rows above the end level
            for (int i = 1; i < temp1 - ZoomTaskPanel.ERROR_ROW; i++) {
                for (int j = 1; j <= 35; j++) {
                    String id = "r" + String.format("%02d", i) + "_c" + String.format("%02d", j);
                    SVGElement element = root.getChild(id);
                    try {
                        if (element != null) {
                            element.setAttribute("fill", AnimationElement.AT_XML, "black");
                        }
                    } catch (SVGException ignored) {
                    }
                }
            }

            int temp2 = 35 - temp1 + 2;

            // Rows below the end level
            for (int i = temp2 + ZoomTaskPanel.ERROR_ROW; i <= 35; i++) {
                for (int j = 1; j <= 35; j++) {
                    String id = "r" + String.format("%02d", i) + "_c" + String.format("%02d", j);
                    SVGElement element = root.getChild(id);
                    try {
                        if (element != null) {
                            element.setAttribute("fill", AnimationElement.AT_XML, "black");
                        }
                    } catch (SVGException ignored) {
                    }
                }
            }

            // Columns left of the end level
            for (int i = temp1 - ZoomTaskPanel.ERROR_ROW; i < 35 - ZoomTaskPanel.ERROR_ROW; i++) {
                for (int j = 1; j < temp1 - ZoomTaskPanel.ERROR_ROW; j++) {
                    String id = "r" + String.format("%02d", i) + "_c" + String.format("%02d", j);
                    SVGElement element = root.getChild(id);
                    try {
                        if (element != null) {
                            element.setAttribute("fill", AnimationElement.AT_XML, "black");
                        }
                    } catch (SVGException ignored) {
                    }
                }
            }

            // Columns right of the end level
            for (int i = temp1 - ZoomTaskPanel.ERROR_ROW; i < 35 - ZoomTaskPanel.ERROR_ROW; i++) {
                for (int j = temp2 + ZoomTaskPanel.ERROR_ROW; j <= 35; j++) {
                    String id = "r" + String.format("%02d", i) + "_c" + String.format("%02d", j);
                    SVGElement element = root.getChild(id);
                    try {
                        if (element != null) {
                            element.setAttribute("fill", AnimationElement.AT_XML, "black");
                        }
                    } catch (SVGException ignored) {
                    }
                }
            }
        }

        try {
            // Update the SVG elements
            root.updateTime(0.0);
        } catch (SVGException ignored) {
        }

        // Repaint the component to reflect the changes
        repaint();
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
        SVGDiagram svgDiagram = SVGCache.getSVGUniverse().getDiagram(svgURI);
        SVGRoot svgRoot = svgDiagram.getRoot();

        int width = getSize().width;
        int scaleFactor = 1; // Always 1 (don't know why)

        // Calculate the scale
        int insideWidth = width - 2 * BORDERS.THICKNESS_2;
        double svgDiagRectWidth = svgDiagram.getViewRect().getWidth();
        double scale = insideWidth / (svgDiagRectWidth / 200.0 + scaleFactor - zoomLevel) / 200;
        double x = (svgDiagram.getViewRect().getWidth() * scale / 2) - (width / 2.0);
        double y = (svgDiagram.getViewRect().getHeight() * scale / 2) - (width / 2.0);

        StringBuilder builder = new StringBuilder();
        builder.append("\"").append("translate(").append(-x).append(" ").append(-y).append(")").append(" ")
                .append("scale(").append(scale).append(")\"");

        try {
            if (svgRoot.hasAttribute("transform", AnimationElement.AT_XML)) {
                svgRoot.setAttribute("transform", AnimationElement.AT_XML, builder.toString());
            } else {
                svgRoot.addAttribute("transform", AnimationElement.AT_XML, builder.toString());
            }
            svgRoot.updateTime(0f);
        } catch (SVGException ignored) {
        }

        this.svgIcon.paintIcon(this, g, 0, 0);
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

        // If not in focus, exit
        if (!hasFocus) return;

        // If a timer is running, stop it
        if (borderBlinker.isRunning()) {
            borderBlinker.stop();
            setBorder(BORDERS.FOCUSED_BORDER);
        }

        // If the zoomLevel is at the maximum (100& zoomed-out) and user scrolls down (positive rotation), exit
        if (zoomLevel >= 35 + 1 && e.getWheelRotation() < 0) return;

        // If the zoomLevel is at the minimum (100% zoomed-in) and user scrolls up (negative rotation), exit
        if (zoomLevel <= 1 && e.getWheelRotation() > 0) return;

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
        double dZL = e.getWheelRotation() * ZoomTaskPanel.WHEEL_SCALE * zoomLevel;

        // Update the zoomFactor accordingly
        this.zoomLevel -= dZL;

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
            if (isZoomIn) {
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
