package tool;

import java.awt.*;

public class MoDimension extends Dimension {

    public MoDimension(int size) {
        this.width = size;
        this.height = size;
    }

    /**
     * Constructor with translation
     * @param base Base dim
     * @param dW delta-W
     * @param dH delta-H
     */
    public MoDimension(Dimension base, int dW, int dH) {
        this.width = base.width + dW;
        this.height = base.height + dH;
    }

    /**
     * Constructor with translation
     * @param base Base dim
     * @param d Change in size
     */
    public MoDimension(Dimension base, int d) {
        this.width = base.width + d;
        this.height = base.height + d;
    }
}
