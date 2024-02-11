package tool;

import java.awt.*;

public class MoDimension extends Dimension {

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
}
