package ui;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.app.beans.SVGPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;

public class PanPanel extends TrialPanel {
    private static final int focusSize = 200;
    private static final int circleSize = 60;

    private final SVGIcon icon;
    private int rotate;
    private Integer xDiff;
    private Integer yDiff;
    private final PanFocusArea mPanFocus;
    private static final int startPosX = 100;
    private static final int startPosY = 500;
    private static final int startBorderSize = 100;

    private long debugTimeNoPanning;
    private long debugTimeNoPanningStart;

    private BufferedImage image;

    public PanPanel() {
        super();

        this.icon = new SVGIcon();
        this.icon.setAntiAlias(true);
        this.icon.setAutosize(SVGPanel.AUTOSIZE_NONE);

        this.mPanFocus = new PanFocusArea();
        this.mPanFocus.setLayout(null);
        this.mPanFocus.setOpaque(false);
        this.mPanFocus.setFocusable(false);
        this.add(this.mPanFocus);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (xDiff == null || yDiff == null) {
            int offsetX;
            int offsetY;
            int offset = focusSize / 2 + startBorderSize;
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

            xDiff = this.getWidth() / 2 - startPosX + offsetX;
            yDiff = this.getHeight() / 2 - startPosY + offsetY;

            mPanFocus.setBounds(this.getWidth() / 2 - focusSize / 2, this.getHeight() / 2 - focusSize / 2, focusSize, focusSize);
            mPanFocus.setVisible(true);

            image = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
        }

        icon.paintIcon(this, g, xDiff, yDiff);
    }

    public void startTrial(URI uri, int rotation) {
        this.icon.setSvgURI(uri);
        this.rotate = rotation;
        this.xDiff = null;
        this.yDiff = null;
        this.debugTimeNoPanning = 0;
        this.debugTimeNoPanningStart = 0;

        this.mPanFocus.setActive(false);

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

        refreshPanel();
    }

    public void translate(int x, int y) {
        this.xDiff += x;
        this.yDiff += y;

        refreshPanel();
    }


    private void refreshPanel() {
        repaint();

        SwingUtilities.invokeLater(this::checkPanningPosition);
    }

    private void checkPanningPosition() {
        if (image == null) {
            return;
        }

        this.paintComponent(image.getGraphics());

        // Check if test is finished
        int[] checkFinished = image.getRGB(mPanFocus.getX() + circleSize, mPanFocus.getY() + circleSize,
                mPanFocus.getWidth() - (circleSize * 2), mPanFocus.getHeight() - (circleSize * 2),
                null, 0, mPanFocus.getWidth() - (circleSize * 2));
        for (int c : checkFinished) {
            // Color RGB of BLACK
            if (c == -16776961) {
                mPanFocus.setActive(true);
                endTrial();
                return;
            }
        }

        // Check if pan has focus
        boolean isFocus = false;
        int[] checkFocus = image.getRGB(mPanFocus.getX(), mPanFocus.getY(),
                mPanFocus.getWidth(), mPanFocus.getHeight(), null,
                0, mPanFocus.getWidth());
        for (int c : checkFocus) {
            // Color RGB of BLUE
            if (c == -16777216) {
                startTrial();

                if (!mPanFocus.isActive() && debugTimeNoPanningStart != 0) {
                    debugTimeNoPanning += (System.currentTimeMillis() - debugTimeNoPanningStart);
                }

                isFocus = true;
                break;
            }
        }

        if (!isFocus && mPanFocus.isActive()) {
            this.debugTimeNoPanningStart = System.currentTimeMillis();
            errorTrial();
        }

        mPanFocus.setActive(isFocus);
    }

    public long getDebugTimeNoPanning() {
        return debugTimeNoPanning;
    }
}
