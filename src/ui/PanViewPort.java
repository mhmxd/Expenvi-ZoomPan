package ui;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.app.beans.SVGPanel;
import model.PanTrial;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.net.URI;

public class PanViewPort extends JPanel implements MouseListener, MouseMotionListener {
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
    public PanViewPort(PanTrial pt, AbstractAction endTrAction) {
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

        // Check if test is finished
        int[] checkFinished = image.getRGB(
                focusArea.getX() + PanTaskPanel.CIRCLE_SIZE,
                focusArea.getY() + PanTaskPanel.CIRCLE_SIZE,
                focusArea.getWidth() - (PanTaskPanel.CIRCLE_SIZE * 2),
                focusArea.getHeight() - (PanTaskPanel.CIRCLE_SIZE * 2),
                null, 0, focusArea.getWidth() - (PanTaskPanel.CIRCLE_SIZE * 2));

        for (int c : checkFinished) {
            // Color RGB of BLACK
            if (c == -16776961) {
                focusArea.setActive(true);
                return true;
            }
        }

        // Check if pan has focus
        boolean insideFocusArea = false;
        int[] checkFocus = image.getRGB(
                focusArea.getX(),
                focusArea.getY(),
                focusArea.getWidth(),
                focusArea.getHeight(),
                null,
                0,
                focusArea.getWidth());

        for (int c : checkFocus) {
            // Color RGB of BLUE
            if (c == -16777216) {
//                startTrial();

//                if (!panFocus.isActive() && debugTimeNoPanningStart != 0) {
//                    debugTimeNoPanning += (System.currentTimeMillis() - debugTimeNoPanningStart);
//                }

                insideFocusArea = true;
                break;
            }
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
}
