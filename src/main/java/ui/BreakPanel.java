package ui;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static tool.Constants.*;

public class BreakPanel extends JPanel {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    private final AbstractAction onTimerEndAction;
    private int countdownValue = 10; // seconds
    private final JLabel countdownLabel;

    /**
     * Constructor
     * @param size Size of the panel
     * @param onTimerEndAction AbstractAction to call at the end of the timer
     */
    public BreakPanel(Dimension size, AbstractAction onTimerEndAction) {
        setSize(size);
        setLayout(null);

        this.onTimerEndAction = onTimerEndAction;

        countdownLabel = new JLabel("10", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Roboto", Font.BOLD, 100));
        countdownLabel.setForeground(COLORS.BLACK);
        countdownLabel.setSize(200, 300);
        countdownLabel.setHorizontalAlignment(SwingConstants.CENTER);
        countdownLabel.setVerticalAlignment(SwingConstants.CENTER);
        int centX = (getWidth() - countdownLabel.getWidth()) / 2;
        int centY = (getHeight() - countdownLabel.getHeight()) / 2;
        countdownLabel.setBounds(centX, centY, countdownLabel.getWidth(), countdownLabel.getHeight());
        add(countdownLabel);

        Timer timer = new Timer(1000, this::updateCountdown);
        timer.start();
    }

    /**
     * Counting down
     * @param e ActionEvent to call at the end
     */
    private void updateCountdown(ActionEvent e) {
        countdownValue--;

        if (countdownValue < 0) {
            ((Timer) e.getSource()).stop();
            onTimerEndAction.actionPerformed(null);
        } else {
            countdownLabel.setText(String.valueOf(countdownValue));
        }
    }

}
