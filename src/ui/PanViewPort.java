package ui;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.app.beans.SVGPanel;
import listener.MooseListener;
import model.PanTrial;
import moose.Memo;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.Constants;
import tool.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.net.URI;

public class PanViewPort extends JPanel implements MouseListener, MouseMotionListener, MooseListener {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    private final PanTrial trial;
    private final AbstractAction endTrialAction; // Received from higher levels

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
    private static final int startBorderSize = 100;

    private BufferedImage image;

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
        
        if (aFlag) {
            startTrial(trial.uri, trial.rotation);
        }
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
     * Check whether the trial is a hit (end circle is inside focus area and < 10% was outside)
     * @return True (Hit) or False
     */
    protected boolean checkHit() {
        if (image == null) return false;

        this.paintComponent(image.getGraphics());

        // Check if line is inside focus area
        boolean insideFocusArea = false;

//        int[] focusAreaPixels = image.getRGB(
//                focusArea.getX() + PanTaskPanel.CIRCLE_SIZE,
//                focusArea.getY() + PanTaskPanel.CIRCLE_SIZE,
//                focusArea.getWidth() - (PanTaskPanel.CIRCLE_SIZE * 2),
//                focusArea.getHeight() - (PanTaskPanel.CIRCLE_SIZE * 2),
//                null, 0, focusArea.getWidth() - (PanTaskPanel.CIRCLE_SIZE * 2));

        int[] focusAreaPixels = image.getRGB(
                focusArea.getX(),
                focusArea.getY(),
                focusArea.getWidth(),
                focusArea.getHeight(),
                null, 0, focusArea.getWidth());

        for (int c : focusAreaPixels) {
            // Color RGB of BLACK?
            Color clr = new Color(c);
            if (clr.equals(Color.BLACK)) {
                insideFocusArea = true;
                break;
            }

//            if (c == -16776961) {
//                focusArea.setActive(true);
////                return true;
//            }
        }

        // Check if pan has focus


//        int[] checkFocusColors = image.getRGB(
//                focusArea.getX(),
//                focusArea.getY(),
//                focusArea.getWidth(),
//                focusArea.getHeight(),
//                null,
//                0,
//                focusArea.getWidth());

        //-- Check if the circle is *completely* inside the focus area
        // Define four 'bands' around the area (scanSize = W -> scans row by row)
        int[] outerBand1 = image.getRGB(
                focusArea.getX() - Utils.mm2px(1),
                focusArea.getY() - Utils.mm2px(1),
                focusArea.getWidth() + Utils.mm2px(2),
                Utils.mm2px(1),
                null, 0, focusArea.getWidth() + Utils.mm2px(2));

        int[] outerBand2 = image.getRGB(
                focusArea.getX() + focusArea.getWidth(),
                focusArea.getY(),
                Utils.mm2px(2),
                focusArea.getHeight(),
                null, 0, Utils.mm2px(2));

        int[] outerBand3 = image.getRGB(
                focusArea.getX() - Utils.mm2px(1),
                focusArea.getY() + focusArea.getHeight(),
                focusArea.getWidth() + Utils.mm2px(2),
                Utils.mm2px(2),
                null, 0, focusArea.getWidth() + Utils.mm2px(2));

        int[] outerBand4 = image.getRGB(
                focusArea.getX() - Utils.mm2px(1),
                focusArea.getY(),
                Utils.mm2px(2),
                focusArea.getHeight(),
                null, 0, Utils.mm2px(2));

        for (int c : focusAreaPixels) {
            Color clr = new Color(c);
            if (clr.equals(Color.BLUE)) { // At least one pixel of the circle is inside
//                return true; // Uncomment for "the moment circle enters"

                // Check so the circle is not in any of the four bands
                if (!hasColor(outerBand1, Color.BLUE)
                && !hasColor(outerBand2, Color.BLUE)
                && !hasColor(outerBand3, Color.BLUE)
                && !hasColor(outerBand4, Color.BLUE)) {
                    return true;
                }

            }
            // Color RGB of BLUE
//            if (c == -16777216) {
////                startTrial();
//
////                if (!panFocus.isActive() && debugTimeNoPanningStart != 0) {
////                    debugTimeNoPanning += (System.currentTimeMillis() - debugTimeNoPanningStart);
////                }
//
//                insideFocusArea = true;
//                return true;
////                break;
//            }
        }

        // TODO: Check for the 10% time
//        if (!insideFocusArea && panFocus.isActive()) {
//            this.debugTimeNoPanningStart = System.currentTimeMillis();
//            errorTrial();
//        }

        focusArea.setActive(insideFocusArea);

        return false;
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
     * Translate the content inside by dX and dY
     * @param dX Delta-X
     * @param dY Delta-Y
     */
    public void translate(int dX, int dY) {
        this.xDiff += dX;
        this.yDiff += dY;

        repaint();

        SwingUtilities.invokeLater(() -> {
            if (checkHit()) endTrialAction.actionPerformed(null);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (xDiff == null || yDiff == null) {
            int offsetX;
            int offsetY;
            int offset = PanTaskPanel.FOCUS_SIZE / 2 + startBorderSize;
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
                    getWidth() / 2 - PanTaskPanel.FOCUS_SIZE / 2, 
                    getHeight() / 2 - PanTaskPanel.FOCUS_SIZE / 2,
                    PanTaskPanel.FOCUS_SIZE,
                    PanTaskPanel.FOCUS_SIZE);
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
        conLog.trace("Mouse Pressed");

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
        conLog.trace("Mouse Released");
        // Change back the cursor and the border
        getParent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        if (!hasFocus) {
            setBorder(Constants.BORDERS.BLACK_BORDER);
        }

        isPanning = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        hasFocus = true;
        setBorder(Constants.BORDERS.FOCUSED_BORDER);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hasFocus = false;
        if (!isPanning) setBorder(Constants.BORDERS.BLACK_BORDER);
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
    public void mooseClicked(Memo e) {

    }

    @Override
    public void mooseScrolled(Memo e) {
        conLog.trace("Translate: {}, {}", e.getV1Int(), e.getV2Int());
        if (hasFocus) {
            translate((int) (e.getV1Int() * PanTaskPanel.GAIN), (int) (e.getV2Int() * PanTaskPanel.GAIN));
        }
    }

    @Override
    public void mooseWheelMoved(Memo e) {

    }

    @Override
    public void mooseZoomStart(Memo e) {

    }
}
