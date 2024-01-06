package ui;

import enums.Task;
import enums.Technique;
import moose.Moose;
import moose.Server;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static enums.Task.*;
import static tool.Constants.*;

public class ExperimentFrame extends JFrame {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    public static String pID = "100";

    public static final int NUM_ZOOM_BLOCKS = 1;
    public static final int NUM_ZOOM_REPETITIONS = 3;

    private Rectangle scrBound;
    private int scrW, scrH;
    private int frW, frH;

    private Moose moose;

    private JDialog infoDialog;

    // ------------------------------------------------------------------------------------------

    /**
     * Constructor
     */
    public ExperimentFrame() {
        setDisplayConfig();
        setBackground(Color.WHITE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                Server.get().shutDown();
            }
        });

        mapKeys();
    }

    /**
     * Start the experiment
     */
    public void begin() {
        moose = new Moose();

        Server.get().setMoose(moose);
        Server.get().start();

        // Show the info frame
        SwingUtilities.invokeLater(() -> {
            // First show the full panel, so the dimensions are correct
            setVisible(true);

            // Create and show the info dialog
            JPanel infoPanel = new InfoPanel(pID, showExperimentAction);

            infoDialog = new JDialog();
            infoDialog.add(infoPanel);
            infoDialog.setSize(680, 440);
            infoDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            infoDialog.addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent e) {
                    System.exit(0);
                }
            });
            // Put the dialog in the center
            infoDialog.setLocation(scrBound.x, scrBound.y);
            int dialogLocX = (scrW - infoDialog.getWidth()) / 2;
            int dialogLocY = (scrH - infoDialog.getHeight()) / 2;
            infoDialog.setLocation(scrBound.x + dialogLocX, scrBound.y + dialogLocY);

            // Show the dialog
            infoDialog.setVisible(true);
        });


    }

    // ------------------------------------------------------------------------------------------
    /**
     * Passed to the InfoPanel (called from there)
     */
    final AbstractAction showExperimentAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            conLog.trace("Info: {}, {}, {}",
                    getValue(STRINGS.PID),
                    getValue(STRINGS.TASK),
                    getValue(STRINGS.TECHNIQUE));

            // Get values from the info dialog
            pID = (String) getValue(STRINGS.PID);
            Task task = (Task) getValue(STRINGS.TASK);
            Technique technique = (Technique) getValue(STRINGS.TECHNIQUE);

            // Create the panel based on the chosen task
            JPanel taskPanel = new JPanel();
            switch (task) {
                case ZOOM_OUT -> {
                    taskPanel = new ZoomPanel(getSize(), moose, false);
                }

                case ZOOM_IN -> {
                    taskPanel = new ZoomPanel(getSize(), moose, true);
                }

                case PAN -> {
                    new PanPanel();
                }
            }

            // Open the frame with the panel
            add(taskPanel);
            setVisible(true);

            // Close the dialog
            infoDialog.setVisible(false);
        }
    };

    // ------------------------------------------------------------------------------------------

    /**
     * Set the config for showing panels
     */
    private void setDisplayConfig() {
        setExtendedState(JFrame.MAXIMIZED_BOTH); // maximized frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // close on exit

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();

        scrBound = gd[1].getDefaultConfiguration().getBounds();
        scrW = scrBound.width;
        scrH = scrBound.height;

        frW = getSize().width;
        frH = getSize().height;

        // Put at center
        setLocation(
                ((scrW / 2) - (frW / 2)) + scrBound.x,
                ((scrH / 2) - (frH / 2)) + scrBound.y
        );
    }

    private void mapKeys() {

    }


}
