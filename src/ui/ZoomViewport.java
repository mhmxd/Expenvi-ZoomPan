package ui;

import com.kitfox.svg.*;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.app.beans.SVGPanel;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.MoKey;
import tool.Pair;
import tool.Resources;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static tool.Constants.*;

public class ZoomViewport extends JPanel {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    private static final double STEP_SIZE = 0.25;
    private static final int ERROR_ROW = 1;

    private boolean isZoomIn;
    private double zoomFactor;
    private int endLevel;
    private double startZoomFactor;
    private Boolean firstZoomInRightDirection;

    // Tools
    private Robot robot;
    private SVGIcon svgIcon;
    private URI svgURI;

    private boolean canFinishTrial;

    public ZoomViewport(Moose moose, boolean isModeZoomIn) {
        isZoomIn = isModeZoomIn;

        svgIcon = new SVGIcon();

        try {
            robot = new Robot();
        } catch (AWTException ignored) {
            conLog.warn("Robot could not be instantiated");
        }

        svgIcon.setAntiAlias(true);
        svgIcon.setAutosize(SVGPanel.AUTOSIZE_NONE);

        svgURI = isModeZoomIn ? Resources.SVG.ZOOM_IN_URI : Resources.SVG.ZOOM_OUT_URI;

//        getInputMap(
//                JComponent.WHEN_IN_FOCUSED_WINDOW)
//                .put(MoKey.SPACE, MoKey.SPACE);
//        getActionMap().put(MoKey.SPACE, SPACE_PRESS);

//        addMouseWheelListener(this);
//        moose.addMooseListener(this);

        startTrial(1, 3);
    }

    public void startTrial(int startLevel, int endLevel) {
        conLog.trace("(startTrial) int startLevel, int endLevel");
        int temp = (int) Math.ceil(endLevel / 2f) - (isZoomIn ? 1 : 0);
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

        startTrial(startLevel, endLevel, pointSet);
    }

    private void startTrial(int startLevel, int endLevel, Set<Pair<Integer, Integer>> points) {
        conLog.trace("(startTrial) int startLevel, int endLevel, Set<Pair<Integer, Integer>> points");
        // Initialize variables and clear previous data
        this.zoomFactor = startLevel;
        this.endLevel = endLevel;
        this.firstZoomInRightDirection = null;
        this.canFinishTrial = false;

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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        conLog.trace("painting ZoomViewport");
        SVGDiagram svgDiagram = SVGCache.getSVGUniverse().getDiagram(svgURI);
        SVGRoot svgRoot = svgDiagram.getRoot();

        int width = getSize().width;
        int scaleFactor = 1;
        conLog.trace("Width = {}", width);

        // Calculate the scale
        int insideWidth = width - 2 * BORDERS.BORDER_THICKNESS;
        double svgDiagRectWidth = svgDiagram.getViewRect().getWidth();
        double scale = insideWidth / (svgDiagRectWidth / 200.0 + scaleFactor - zoomFactor) / 200;
        double x = (svgDiagram.getViewRect().getWidth() * scale / 2) - (width / 2.0);
        double y = (svgDiagram.getViewRect().getHeight() * scale / 2) - (width / 2.0);
        conLog.trace("x,y = {},{}", x, y);
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
}
