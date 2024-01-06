package ui;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.app.beans.SVGPanel;
import jdk.jshell.execution.Util;
import listener.MooseListener;
import model.ZoomTrial;
import moose.Memo;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.MoKey;
import tool.Utils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;

import static tool.Constants.*;
import static tool.Resources.*;

public class ZoomTaskPanel
        extends TaskPanel
        implements MouseMotionListener, MouseWheelListener, MouseListener, MooseListener {

    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    // Constants
    private final double STEP_SIZE = 0.25;
    private final int ERROR_ROW = 1;

    // Experiment
    private ZoomTrial activeTrial;
    private boolean isZoomIn;
    private double zoomFactor;
    private int endLevel;
    private double startZoomFactor;
    private Boolean firstZoomInRightDirection;

    // Tools
    private Robot robot;
//    private final SVGIcon svgIcon;
//    private final URI svgURI;

    // UI
    private JPanel zoomViewPort;

    // Timers ---------------------------------------------------------------------------------
    private final Timer borderBlinker = new Timer(200, new ActionListener() {
        private Border currentBorder;
        private int count = 0;
        private final Border border1 = new LineBorder(Color.YELLOW, BORDERS.BORDER_THICKNESS);
        private final Border border2 = new LineBorder(Color.RED, BORDERS.BORDER_THICKNESS);

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

    // -------------------------------------------------------------------------------------------
    /**
     * Constructor
     * @param dim Desired dimension of the panel
     */
    public ZoomTaskPanel(Dimension dim, Moose moose, boolean isModeZoomIn) {
        setSize(dim);
        setLayout(null);

//        isZoomIn = isModeZoomIn;
//        svgURI = isModeZoomIn ? SVG.ZOOM_IN_URI : SVG.ZOOM_OUT_URI;
//
//        svgIcon = new SVGIcon();
//        svgIcon.setAntiAlias(true);
//        svgIcon.setAutosize(SVGPanel.AUTOSIZE_NONE);
//
//        try {
//            robot = new Robot();
//        } catch (AWTException ignored) {
//            conLog.warn("Robot could not be instantiated");
//        }

//        int size = Math.min(getWidth(), getHeight()) - 150;
//        Border border = new LineBorder(Color.BLACK, BORDER_THICKNESS);
//
//        ZoomPanel zoomPanel = new ZoomPanel(moose, isModeZoomIn);
//        zoomPanel.setBounds((getWidth() - size) / 2, 50, size, size);
//        zoomPanel.setBorder(border);
//        zoomPanel.setBackground(MAIN_BACKGROUND);
//        zoomPanel.setLayout(null);
//        add(zoomPanel);
//        zoomPanel.setVisible(true);

        ZoomViewport zVP = new ZoomViewport(moose, isModeZoomIn);
        int zvpSize = Utils.mm2px(ExperimentFrame.ZOOM_VP_SIZE_mm);
        zVP.setBounds((getWidth() - zvpSize) / 2, 200, zvpSize, zvpSize);
        zVP.setBorder(BORDERS.BLACK_BORDER);
        add(zVP);
        zVP.setVisible(true);

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(MoKey.SPACE, MoKey.SPACE);
        getActionMap().put(MoKey.SPACE, SPACE_PRESS);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        moose.addMooseListener(this);
    }

    /**
     * SPACE was pressed on an incorrect level
     */
    protected void showError() {
        borderBlinker.start();
    }

    @Override
    protected boolean checkHit() {
        double tolUp = endLevel + 2;
        double tolDown = endLevel - 2 - 0.1;

        return (zoomFactor < tolUp) && (zoomFactor > tolDown);
    }

//    @Override
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//
//        SVGDiagram svgDiagram = SVGCache.getSVGUniverse().getDiagram(svgURI);
//        SVGRoot svgRoot = svgDiagram.getRoot();
//
//        Dimension size = this.getSize();
//        conLog.trace("Size: {}", size);
//        int width = size.width;
//        int scaleFactor = 1;
//
//        double scale =
//                (width - 2 * BORDER_THICKNESS)
//                        /
//                (svgDiagram.getViewRect().getWidth() / 200.0 + scaleFactor - zoomFactor) / 200;
//        double x = (svgDiagram.getViewRect().getWidth() * scale / 2) - (width / 2.0);
//        double y = (svgDiagram.getViewRect().getHeight() * scale / 2) - (width / 2.0);
//
//        StringBuilder builder = new StringBuilder();
//        builder.append("\"").append("translate(").append(-x).append(" ").append(-y).append(")").append(" ")
//                .append("scale(").append(scale).append(")\"");
//
//        try {
//            if (svgRoot.hasAttribute("transform", AnimationElement.AT_XML)) {
//                svgRoot.setAttribute("transform", AnimationElement.AT_XML, builder.toString());
//            } else {
//                svgRoot.addAttribute("transform", AnimationElement.AT_XML, builder.toString());
//            }
//            svgRoot.updateTime(0f);
//        } catch (SVGException ignored) {
//
//        }
//
//        this.svgIcon.paintIcon(this, g, 0, 0);
//    }

    // -------------------------------------------------------------------------------------------
    private final AbstractAction SPACE_PRESS = new AbstractAction() {
        public final static String KEY = "SPACE_PRESS";

        @Override
        public void actionPerformed(ActionEvent e) {
            conLog.debug("SPACE pressed");
            if (checkHit()) endTrialHit();
            else showError();
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
    public void mooseMoved(Memo e) {

    }

    @Override
    public void mooseWheelMoved(Memo e) {

    }

    @Override
    public void mooseZoomStart(Memo e) {

    }
}
