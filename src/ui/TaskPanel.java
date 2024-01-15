package ui;

import model.BaseBlock;
import model.BaseTrial;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/***
 * JlayeredPane to use indexes for objects
 */
public class TaskPanel extends JLayeredPane {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    // Experiment
    protected ArrayList<BaseBlock> blocks = new ArrayList<>();
    protected BaseBlock activeBlock;
    protected BaseTrial activeTrial;

    // UI
    JLabel progressLabel = new JLabel();

    public TaskPanel() {
        progressLabel.setBounds(2200, 50, 300, 30);
        progressLabel.setFont(new Font(progressLabel.getFont().getFontName(), Font.PLAIN, 20));
        progressLabel.setVerticalAlignment(JLabel.CENTER);
        progressLabel.setHorizontalAlignment(JLabel.CENTER);
        add(progressLabel, 1);
    }

    protected void createBlocks() {
        // Implemented by the subclasses
    }

    /**
     * Start the current block
     */
    protected void starTask() {
        activeBlock = blocks.get(0);
        startBlock();
    }

    /**
     * Show a trial
     */
    protected void startBlock() {
        // Implemented by the subclasses
    }


    protected void nextTrial() {

    }
}
