package ui;

import com.google.common.base.Stopwatch;
import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.app.beans.SVGPanel;
import control.Logex;
import enums.TrialEvent;
import enums.TrialStatus;
import listener.MooseListener;
import model.PanTrial;
import moose.Memo;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.Utils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import static tool.Constants.*;

public class PanViewPort extends JPanel implements MouseListener, MouseMotionListener, MooseListener {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    private final int BLINKER_DELAY = 50; // ms

    //-- Trial
    private final PanTrial trial;
    private final AbstractAction endTrialAction; // Received from higher levels
    private int nScansCurveInsideFocus, nScans;

    // View
    private final SVGIcon icon;
    private int rotate;
    private Point dragPoint;
    private Integer xDiff;
    private Integer yDiff;
    private final PanFocusArea focusArea;
    private boolean hasFocus;
    private boolean isPanning;
    private static final int startPosX = 100;
    private static final int startPosY = 500;
//    private static final int startBorderSize = 100;
    private final int focusAreaSize;

    private BufferedImage image;
    private final Stopwatch insideFocusStopwatch;

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

    /**
     * Constructor
     * @param pt PanTrial
     * @param endTrAction AbstractAction
     */
    public PanViewPort(Moose moose, PanTrial pt, AbstractAction endTrAction) {
        icon = new SVGIcon();
        icon.setAntiAlias(true);
        icon.setAutosize(SVGPanel.AUTOSIZE_NONE);

        trial = pt;
        endTrialAction = endTrAction;

        // Init
        focusAreaSize = Utils.mm2px(PanTaskPanel.FOCUS_SIZE_mm);
        insideFocusStopwatch = Stopwatch.createUnstarted();

        // Add the focus area
        focusArea = new PanFocusArea();
        focusArea.setLayout(null);
        focusArea.setOpaque(false);
        focusArea.setFocusable(false);
        add(focusArea);

        // Set listeners
        addMouseListener(this);
        addMouseMotionListener(this);
        moose.addMooseListener(this);
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        
        if (aFlag) startTrial(trial.uri, trial.rotation);
    }

    /**
     * Start the trial
     * @param uri URI
     * @param rotation int
     */
    public void startTrial(URI uri, int rotation) {

        icon.setSvgURI(uri);
        rotate = rotation;
        xDiff = null;
        yDiff = null;

        focusArea.setActive(false);

        SVGDiagram diagram = SVGCache.getSVGUniverse().getDiagram(uri);
        SVGRoot root = diagram.getRoot();

        StringBuilder builder = new StringBuilder();
        builder.append("\"rotate(").append(rotate).append(" ").append(startPosX).append(" ").append(startPosY).append(")\"");
        try {
            if (root.hasAttribute("transform", AnimationElement.AT_XML)) {
                root.setAttribute("transform", AnimationElement.AT_XML, builder.toString());
            } else {
                root.addAttribute("transform", AnimationElement.AT_XML, builder.toString());
            }
            root.updateTime(0f);
        } catch (SVGException ignored) {
        }

        repaint();
    }


    /**
     * Check whether the trial is at the end (circle is inside focus area)
     * Also, manages the focus area's activatino/deactivation
     * @return True (Hit) or False
     */
    protected boolean isTrialFinished() {
        if (image == null) return false;

        this.paintComponent(image.getGraphics());

        int[] focusAreaPixels = image.getRGB(
                focusArea.getX(),
                focusArea.getY(),
                focusArea.getWidth(),
                focusArea.getHeight(),
                null, 0, focusArea.getWidth());

        scanFocusAreaForCurve(focusAreaPixels);
        return scanFocusAreaForLineEnd(focusAreaPixels);
    }

    /**
     * Check whether a (RGB) pixel array include a color
     * @param pixelArray Array of TYPE_INT_ARGB
     * @param color COlor to check
     * @return True if there is one pixel with the input color
     */
    private boolean hasColor(int[] pixelArray, Color color) {
        for (int p : pixelArray) {
            Color pc = new Color(p);
            if (pc.equals(color)) return true;
        }

        return false;
    }

    /**
     * Scan the focus area for the curve
     * @param focusPixels Array of the RGBA pixels
     */
    private void scanFocusAreaForCurve(int[] focusPixels) {
        // Has the curve entered the focus area?
        boolean focusEntered = Logex.get().hasLogged(TrialEvent.getFirst(TrialEvent.FOCUS_ENTER));

        // Check if line is inside focus area
        focusArea.setActive(false);
        if (focusEntered) nScans++; // Only count after entering the focus area
        for (int c : focusPixels) {
            Color clr = new Color(c);
            if (clr.equals(Color.BLACK)) {
                focusArea.setActive(true);
                if (focusEntered) nScansCurveInsideFocus++; // Only count after entering the focus area

                logInsideFocus(); // LOG
                return;
            }
        }

        // No black was found
        logOutsideFocus();
    }

    /**
     * Scan the focus area for the end-circle color.
     * The moment the color of the end circle is found, return.
     * @param focusPixels Array of RGBA pixels
     */
    private boolean scanFocusAreaForLineEnd(int[] focusPixels) {
        for (int c : focusPixels) {
            Color clr = new Color(c);
            if (clr.equals(PanTaskPanel.END_CIRCLE_COLOR)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check the accruacy (called AFTER finishing the trial)
     * @return True (> 90% of the curved traversed inside the focus area)
     */
    private boolean wasTrialAccurate() {
        return ((double) nScansCurveInsideFocus / nScans) >= 0.9;
    }

    /**
     * Translate the content inside by dX and dY
     * @param dX Delta-X
     * @param dY Delta-Y
     */
    public void translate(int dX, int dY) {
        Logex.get().logEvent(TrialEvent.PAN); // LOG

        this.xDiff += dX;
        this.yDiff += dY;

        repaint();

        // Check for the end of the trial
        SwingUtilities.invokeLater(() -> {
            if (isTrialFinished()) {
                Duration totalTrialDuration = Duration.between(
                        Logex.get().getTrialInstant(TrialEvent.getFirst(TrialEvent.FOCUS_ENTER)),
                        Instant.now());

                // < 90% of the curve traversed inside the focus area => error
                ActionEvent endTrialEvent = (wasTrialAccurate())
                        ? new ActionEvent(this, TrialStatus.HIT, "")
                        : new ActionEvent(this, TrialStatus.ERROR, TrialStatus.TEN_PERCENT_OUT);
                endTrialAction.actionPerformed(endTrialEvent);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (xDiff == null || yDiff == null) {
            int offsetX;
            int offsetY;
            int offset = focusAreaSize / 2 + BORDERS.THICKNESS_2;
            if (rotate >= 0 && rotate < 90) {
                offsetX = offset;
                offsetY = -offset;
            } else if (rotate >= 90 && rotate < 180) {
                offsetX = offset;
                offsetY = offset;
            } else if (rotate >= 180 && rotate < 270) {
                offsetX = -offset;
                offsetY = offset;
            } else {
                offsetX = -offset;
                offsetY = -offset;
            }

            xDiff = getWidth() / 2 - startPosX + offsetX;
            yDiff = getHeight() / 2 - startPosY + offsetY;

            focusArea.setBounds(
                    getWidth() / 2 - focusAreaSize / 2,
                    getHeight() / 2 - focusAreaSize / 2,
                    focusAreaSize,
                    focusAreaSize);
            focusArea.setVisible(true);

            image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        }

        icon.paintIcon(this, g, xDiff, yDiff);
    }

    // ------------------------------------------------------------------------
    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (hasFocus) { // Pressed inside
            isPanning = true;

            // Get the start point
            dragPoint = e.getLocationOnScreen();

            // Change the cursor
            getParent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Change back the cursor and the border
        getParent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        if (!hasFocus) {
            setBorder(BORDERS.BLACK_BORDER);
        }

        isPanning = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        hasFocus = true;
        setBorder(BORDERS.FOCUSED_BORDER);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hasFocus = false;
        if (!isPanning) setBorder(BORDERS.BLACK_BORDER);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (isPanning) {
            Point curPoint = e.getLocationOnScreen();
            int xDiff = curPoint.x - dragPoint.x;
            int yDiff = curPoint.y - dragPoint.y;

            dragPoint = curPoint;

            translate(xDiff, yDiff);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void mooseClicked(Memo mem) {
        borderBlinker.start();
    }

    @Override
    public void mooseScrolled(Memo mem) {
        conLog.trace("Translate: {}, {}", mem.getV1Int(), mem.getV2Int());
        if (hasFocus) {
            translate((int) (mem.getV1Int() * PanTaskPanel.GAIN), (int) (mem.getV2Int() * PanTaskPanel.GAIN));
        }
    }

    @Override
    public void mooseWheelMoved(Memo mem) {

    }

    @Override
    public void mooseZoomStart(Memo mem) {

    }

    // Logs ------------------------------------------------------------------------
    private void logInsideFocus() {
        // If hasn't entered before or has exited before
        if (!Logex.get().hasLoggedKey(TrialEvent.FOCUS_ENTER) ||
                Logex.get().hasLoggedKey(TrialEvent.FOCUS_EXIT)) {
            Logex.get().logEvent(TrialEvent.FOCUS_ENTER);

            // Start the stopwatch (if not already started)
            if (!insideFocusStopwatch.isRunning()) insideFocusStopwatch.start();
        }

    }

    private void logOutsideFocus() {
        // If hasn't exited before or has entered before
        if (Logex.get().hasLoggedKey(TrialEvent.FOCUS_ENTER)) {
            Logex.get().logEvent(TrialEvent.FOCUS_EXIT);

            // Start the stopwatch (if not already started)
            if (insideFocusStopwatch.isRunning()) insideFocusStopwatch.stop();
        }
    }

}
