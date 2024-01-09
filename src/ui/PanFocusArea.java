package ui;

import javax.swing.*;
import java.awt.*;

/**
 * A custom JPanel that represents a focus area. It can be either active or inactive.
 */
public class PanFocusArea extends JPanel {
    private boolean active;

    /**
     * Paints the component, drawing a rectangle representing the focus area.
     * If active, the rectangle is filled with green color, otherwise with red color.
     *
     * @param g the graphics context
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(active ? Color.GREEN : Color.RED);
        g2d.setStroke(new BasicStroke(4f));
        g2d.drawRect(0, 0, getWidth(), getHeight());
        g2d.dispose();
    }

    /**
     * Checks if the focus area is active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Sets the focus area as active or inactive.
     *
     * @param active true to set active, false to set inactive
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}
