package ui;

import listener.MooseListener;
import model.BaseBlock;
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
import java.util.ArrayList;
import java.util.Random;

import static tool.Constants.*;

public class ZoomTaskPanel
        extends TaskPanel
        implements MouseMotionListener, MouseWheelListener, MouseListener, MooseListener {

    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    // Constants
    public static final int NUM_ZOOM_BLOCKS = 3;
    public static final int NUM_ZOOM_REPETITIONS = 3;
    public static final int ZOOM_VP_SIZE_mm = 200;
    private final double STEP_SIZE = 0.25;
    private final int ERROR_ROW = 1;

    // Experiment
    private ZoomTrial activeTrial;
    private boolean isZoomIn;
    private double zoomFactor;
    private int endLevel;
    private double startZoomFactor;
    private Boolean firstZoomInRightDirection;
    private Moose moose;
    private boolean startOnLeft;
    private int zvpSize; // Size of the viewport in px
    private int lrMargin; // Left-right margin in px (mm comes from ExperimentFrame)

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
    public ZoomTaskPanel(Dimension dim, Moose ms, boolean isModeZoomIn) {
        setSize(dim);
        setLayout(null);

        startOnLeft = new Random().nextBoolean(); // Randomly choose whether to start traials on the left or right
        zvpSize = Utils.mm2px(ZOOM_VP_SIZE_mm);
        lrMargin = Utils.mm2px(ExperimentFrame.LR_MARGIN_MM);

        isZoomIn = isModeZoomIn;
        moose = ms;
//
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

        createBlocks();
        startBlock(1);

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(MoKey.SPACE, MoKey.SPACE);
        getActionMap().put(MoKey.SPACE, SPACE_PRESS);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        moose.addMooseListener(this);
    }

    /**
     * Cerate zoom blocks
     */
    @Override
    protected void createBlocks() {
        super.createBlocks();

        for (int i = 0; i < NUM_ZOOM_BLOCKS; i++) {
            blocks.add(new BaseBlock(i + 1, isZoomIn, NUM_ZOOM_REPETITIONS));
        }
    }

    /**
     * Start a block
     * @param blkNum Block number (starting from 1)
     */
    @Override
    protected void startBlock(int blkNum) {
        super.startBlock(blkNum);
    }

    /**
     * Show a trial
     * @param trNum Trial number
     */
    @Override
    protected void startTrial(int trNum) {
        super.startTrial(trNum);

        activeTrial = (ZoomTrial) activeBlock.getTrial(trNum); // Get the trial

        // Create the viewport for showing the trial
        zoomViewPort = new ZoomViewport(activeTrial);
        zoomViewPort.setBorder(BORDERS.BLACK_BORDER);
        Point position = getZoomViewportPosition(trNum);
        zoomViewPort.setBounds(position.x, position.y, zvpSize, zvpSize);
        add(zoomViewPort);
        zoomViewPort.setVisible(true);
    }

    /**
     * Generate a random position for the viewport
     * Position is alteranted between left and right
     * @return Point position
     */
    private Point getZoomViewportPosition(int trNum) {
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
