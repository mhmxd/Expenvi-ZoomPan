package ui;

import com.kitfox.svg.*;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.app.beans.SVGPanel;
import listener.MooseListener;
import moose.Memo;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.MoKey;
import tool.Pair;
import tool.Resources;
import tool.Tools;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.List;
import java.util.*;

import static tool.Constants.*;
import static tool.Resources.*;

import static tool.Constants.BORDER_THICKNESS;

public class ZoomPanel extends TrialPanel implements MouseWheelListener, MooseListener {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());
//    private final TaggedLogger detlog = Logger.tag("DETAIL");

    private static final double STEP_SIZE = 0.25;
    private static final int ERROR_ROW = 1;

    private final boolean isModeZoomIn;
    private Robot robot;
    private final SVGIcon svgIcon;
    private URI svgURI;
    private double zoomFactor;
    private int endLevel;
    private double startZoomFactor;

    private Boolean firstZoomInRightDirection;
    private boolean canFinishTrial;
    private final List<Long> debugCanFinish;

    // Actions --------------------------------------------------------------------------------
    private final AbstractAction SPACE_PRESS = new AbstractAction() {
        public final static String KEY = "SPACE_PRESS";

        @Override
        public void actionPerformed(ActionEvent e) {
            conLog.debug("SPACE pressed");
//            detlog.info("SPACE pressed | endLevel: " + (endLevel / 2 + 1) + " | zoomFactor: " + getZoomFactor());
            TrialFrame.LOGGER.info("SPACE pressed | endLevel: " + (endLevel / 2 + 1) +
                    " | zoomFactor: " + getZoomFactor());
            endTrial();
        }
    };

    // Timers ---------------------------------------------------------------------------------
    private final Timer borderBlinker = new Timer(200, new ActionListener() {
        private Border currentBorder;
        private int count = 0;
        private final Border border1 = new LineBorder(Color.YELLOW, BORDER_THICKNESS);
        private final Border border2 = new LineBorder(Color.RED, BORDER_THICKNESS);

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

    // ----------------------------------------------------------------------------------------

    /**
     * Constructor
     * @param moose Moose
     * @param isModeZoomIn Zoom-in (true) or Zoom-out (false)
     */
    public ZoomPanel(Moose moose, boolean isModeZoomIn) {
        this.isModeZoomIn = isModeZoomIn;
        this.debugCanFinish = new ArrayList<>();

        svgIcon = new SVGIcon();

        try {
            robot = new Robot();
        } catch (AWTException ignored) {
            conLog.warn("Robot could not be instantiated");
        }

        svgIcon.setAntiAlias(true);
        svgIcon.setAutosize(SVGPanel.AUTOSIZE_NONE);

        svgURI = isModeZoomIn ? SVG.ZOOM_IN_URI : SVG.ZOOM_OUT_URI;
//        conLog.trace("svgURI: {}", svgURI);

        getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(MoKey.SPACE, MoKey.SPACE);
        getActionMap().put(MoKey.SPACE, SPACE_PRESS);

        addMouseWheelListener(this);
        moose.addMooseListener(this);
    }

    /**
     * Constructor
     * @param panelDim Dimension of the panel
     * @param moose Moose
     * @param isModeZoomIn Zoom-in (true) or Zoom-out (false)
     */
    public ZoomPanel(Dimension panelDim, Moose moose, boolean isModeZoomIn) {
        this.isModeZoomIn = isModeZoomIn;
        this.debugCanFinish = new ArrayList<>();

        svgIcon = new SVGIcon();

        try {
            robot = new Robot();
        } catch (AWTException ignored) {
            conLog.warn("Robot could not be instantiated");
        }

        svgIcon.setAntiAlias(true);
        svgIcon.setAutosize(SVGPanel.AUTOSIZE_NONE);

        svgURI = isModeZoomIn ? SVG.ZOOM_IN_URI : SVG.ZOOM_OUT_URI;
//        conLog.trace("svgURI: {}", svgURI);

        getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(MoKey.SPACE, MoKey.SPACE);
        getActionMap().put(MoKey.SPACE, SPACE_PRESS);

        addMouseWheelListener(this);
        moose.addMooseListener(this);

        // Set the attributes
        setBounds(new Rectangle(panelDim));
    }

    /**
     * End the trial
     */
    protected void endTrial() {
        boolean canFinish = canFinishTrial();
        double tolUp = (endLevel + 2.0) / 2 + 1;
        double tolDown = (endLevel - 2.0 - 0.1) / 2 + 1;
        conLog.trace("(endTrial) Can finish trial? {}", canFinish);
        if (!canFinish) {
            conLog.trace("Trial cannot be finished");
//            detlog.info("Trial not finished | endLevel: " + (endLevel / 2 + 1) + " | zoomFactor: " + getZoomFactor() + " | range: " + tolDown + " - " + tolUp);
            TrialFrame.LOGGER.info("Trial not stopped | endLevel: " + (endLevel / 2 + 1) +
                    " | zoomFactor: " + getZoomFactor() + " | range: " + tolDown + " - " + tolUp);

            errorTrial();
            borderBlinker.start();

            return;
        }

//        conLog.trace("(endTrial) Trial finished");
//        detlog.info("Trial finished | endLevel: " + (endLevel / 2 + 1) + " | zoomFactor: " + getZoomFactor() + "  | range: " + tolDown + " - " + tolUp);
        TrialFrame.LOGGER.info("Trial stopped | endLevel: " + (endLevel / 2 + 1) +
                " | zoomFactor: " + getZoomFactor() + "  | range: " + tolDown + " - " + tolUp);

        super.endTrial();
    }

    private boolean canFinishTrial() {
        double tolUp = endLevel + 2;
        double tolDown = endLevel - 2 - 0.1;

        return (zoomFactor < tolUp) && (zoomFactor > tolDown);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        SVGDiagram svgDiagram = SVGCache.getSVGUniverse().getDiagram(svgURI);
        SVGRoot svgRoot = svgDiagram.getRoot();

        Dimension size = this.getSize();
        conLog.trace("Size: {}", size);
        int width = size.width;
        int scaleFactor = 1;

        double scale = (width - 2 * BORDER_THICKNESS) / (svgDiagram.getViewRect().getWidth() / 200.0 + scaleFactor - this.zoomFactor) / 200;
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

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
//        detlog.info("zoomFactor: " + getZoomFactor() + " | mouseWheelMoved: " + e.getWheelRotation());
        TrialFrame.LOGGER.info("zoomFactor: " + getZoomFactor() + " | mouseWheelMoved: " + e.getWheelRotation());

        // Check if the component has focus; if not, exit
        if (!isFocus()) {
            return;
        }

        // If there's no change in the wheel rotation, exit
        if (e.getWheelRotation() == 0) {
            return;
        }

        // If the zoomFactor is at the maximum and user scrolls down (negative rotation), exit
        if (zoomFactor >= 35 + 1 && e.getWheelRotation() < 0) {
            return;
        }

        // If the zoomFactor is at the minimum and user scrolls up (positive rotation), exit
        if (this.zoomFactor <= 1 && e.getWheelRotation() > 0) {
            return;
        }

        // If a timer is running, stop it
        if (borderBlinker.isRunning()) {
            borderBlinker.stop();
        }

        // Start the trial
        startTrial();

        // If it's the first zoom and the direction is not determined, set the direction
        if (firstZoomInRightDirection == null) {
            if (isModeZoomIn) {
                firstZoomInRightDirection = e.getWheelRotation() < 0;
            } else {
                firstZoomInRightDirection = e.getWheelRotation() > 0;
            }
        }

        // Determine if the trial is finished
        boolean trialFinished = canFinishTrial();
        if (!canFinishTrial && trialFinished) {
            canFinishTrial = true;
            debugCanFinish.add(System.currentTimeMillis());
        } else if (canFinishTrial && !trialFinished) {
            canFinishTrial = false;
        }

        // Calculate the scale based on the step size and mouse wheel rotation
        // 1 Zoom-Level is 16 notches
        double scale = STEP_SIZE * e.getWheelRotation();

        // Update the zoomFactor accordingly
        this.zoomFactor -= scale;

        // Repaint to reflect the changes
        repaintZooming();
    }

    @Override
    public void mooseClicked(Memo e) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (e.getMode()) {
            case STRINGS.SINGLE -> mouseClick(InputEvent.BUTTON1_DOWN_MASK);
        }
    }

    @Override
    public void mooseMoved(Memo e) {
    }

    @Override
    public void mooseWheelMoved(Memo e) {
        conLog.debug("(mooseWheelMoved) ZoomFactor: {}",
                Tools.threeDig(getZoomFactor()));
//        detlog.info(e.getDebug() + " | zoomFactor:" + getZoomFactor() + " | mooseWheelMoved: " + Float.parseFloat(e.getValue1()));
        TrialFrame.LOGGER.info(e.getDebug() + " | zoomFactor:" + getZoomFactor() +
                " | mooseWheelMoved: " + Float.parseFloat(e.getValue1()));

        // Check if the component has focus; if not, do nothing
        if (!isFocus()) {
            return;
        }

        // If a timer is running, stop it
        if (borderBlinker.isRunning()) {
            borderBlinker.stop();
        }

        // Start the test
//        startTrial();

        // Parse the scaling factor from the Memo
        float scale = Float.parseFloat(e.getValue1());

        // If the scaling factor is zero, do nothing
        if (scale == 0.0) {
            return;
        }

        // Determine the first zoom direction if not set
        if (firstZoomInRightDirection == null) {
            if (isModeZoomIn) {
                firstZoomInRightDirection = scale < 0;
            } else {
                firstZoomInRightDirection = scale > 0;
            }
        }

        // If the zoom factor is already at maximum and the scale is positive, do nothing
        if (zoomFactor >= 35 + 1 && scale > 0) {
            return;
        }

        // Check if the trial can be finished
        boolean trialFinished = canFinishTrial();
        if (!canFinishTrial && trialFinished) {
            canFinishTrial = true;
            debugCanFinish.add(System.currentTimeMillis());
        } else if (canFinishTrial && !trialFinished) {
            canFinishTrial = false;
        }

        // Update the zoom factor based on the scaling input
        // scale * 4: Is 2 rows for 1 Zoom-Level
        zoomFactor = startZoomFactor + (scale * 4);

        // Repaint the component to reflect the zooming
        repaintZooming();
    }

    @Override
    public void mooseZoomStart(Memo e) {
        this.startZoomFactor = this.zoomFactor;
//        detlog.info(e.getDebug() + " | startZoomFactor: " + startZoomFactor + " | zoomFactor:" + getZoomFactor());
        TrialFrame.LOGGER.info(e.getDebug() + " | startZoomFactor: " + startZoomFactor +
                " | zoomFactor:" + getZoomFactor());

        startTrial();
    }

    public void startTrial(int startLevel, int endLevel) {
        conLog.trace("(startTrial) int startLevel, int endLevel");
        int temp = (int) Math.ceil(endLevel / 2f) - (isModeZoomIn ? 1 : 0);
        Set<Pair<Integer, Integer>> p = new HashSet<>();

        if (isModeZoomIn) {
            for (int i = temp; i <= 35 - temp + 1; i++) {
                p.add(Pair.create(temp, i));
                p.add(Pair.create(temp + 1, i));
                p.add(Pair.create(35 - temp + 1, i));
                p.add(Pair.create(35 - temp + 1 - 1, i));
                p.add(Pair.create(i, temp));
                p.add(Pair.create(i, temp + 1));
                p.add(Pair.create(i, 35 - temp + 1));
                p.add(Pair.create(i, 35 - temp + 1 - 1));
            }
        } else {
            for (int i = temp - 1; i <= 35 - temp + 1 + 1; i++) {
                p.add(Pair.create(temp - 1, i));
                p.add(Pair.create(temp, i));
                p.add(Pair.create(35 - temp + 1, i));
                p.add(Pair.create(35 - temp + 1 + 1, i));
                p.add(Pair.create(i, temp));
                p.add(Pair.create(i, temp - 1));
                p.add(Pair.create(i, 35 - temp + 1));
                p.add(Pair.create(i, 35 - temp + 1 + 1));
            }
        }

        startTrial(startLevel, endLevel, p);
    }

    private void startTrial(int startLevel, int endLevel, Set<Pair<Integer, Integer>> points) {
        conLog.trace("(startTrial) int startLevel, int endLevel, Set<Pair<Integer, Integer>> points");
        // Initialize variables and clear previous data
        this.zoomFactor = startLevel;
        this.endLevel = endLevel;
        this.firstZoomInRightDirection = null;
        this.canFinishTrial = false;
        this.debugCanFinish.clear();

//        try {
//            // Load the appropriate SVG resource based on zoom mode (in or out)
//            String resourceName = this.isModeZoomIn ? "zoom_in.svg" : "zoom_out.svg";
//            this.uri = Objects.requireNonNull(TrialPanel.class.getResource("resources/" + resourceName)).toURI();
//        } catch (URISyntaxException ignored) {
//        }

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

        if (isModeZoomIn) {
            // Adjust SVG elements for zoom-in mode
            int temp1 = (int) Math.ceil(endLevel / 2f);
            for (int i = temp1 + ERROR_ROW; i < 35 - temp1 + ERROR_ROW; i++) {
                for (int j = temp1 + ERROR_ROW; j < 35 - temp1 + ERROR_ROW; j++) {
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
            for (int i = 1; i < temp1 - ERROR_ROW; i++) {
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
            for (int i = temp2 + ERROR_ROW; i <= 35; i++) {
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
            for (int i = temp1 - ERROR_ROW; i < 35 - ERROR_ROW; i++) {
                for (int j = 1; j < temp1 - ERROR_ROW; j++) {
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
            for (int i = temp1 - ERROR_ROW; i < 35 - ERROR_ROW; i++) {
                for (int j = temp2 + ERROR_ROW; j <= 35; j++) {
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

    public double getZoomFactor() {
        return zoomFactor / 2 + 1;
    }

    private void repaintZooming() {
        repaint();
    }

    @SuppressWarnings("SameParameterValue")
    private void mouseClick(int button) {
        Point curPos = MouseInfo.getPointerInfo().getLocation();
        robot.mouseMove(curPos.x, curPos.y);
        robot.mousePress(button);
        robot.mouseRelease(button);
    }

    public boolean isFirstZoomInRightDirection() {
        return firstZoomInRightDirection;
    }

    public List<Long> getDebugCanFinish() {
        return debugCanFinish;
    }
}
