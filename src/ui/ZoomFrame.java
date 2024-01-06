package ui;

import enums.Task;
import enums.Technique;
import listener.TrialListener;
import model.BaseTrial;
import model.ZoomTrial;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.util.*;

import static tool.Constants.*;
import static tool.StringConstants.deli;

public class ZoomFrame extends TrialFrame implements TrialListener {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());
    private final TaggedLogger detlog = Logger.tag("DETAIL");

    private static final boolean isZoomButtons = false;
    protected final boolean isModeZoomIn;

    public ZoomFrame(Moose moose, boolean debugOnScreen, int pId, Technique technique, boolean isModeZoomIn) {
        super(moose, debugOnScreen, pId, isModeZoomIn ? Task.ZOOM_IN : Task.ZOOM_OUT, technique, true);

        this.isModeZoomIn = isModeZoomIn;

        this.mainPanel = new ZoomPanel(moose, isModeZoomIn);

        initComponents();
        initTrials();
        showNextTrial();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        setTitle("Zoom Panel");

        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
//                TrialFrame.LOGGER.info("mouseMoved: x: " + e.getX() + " y: " + e.getY());
            }
        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
//                TrialFrame.LOGGER.info("mousePressed");
            }
        });

        this.mainPanel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {

//                if (isTrialFinished()) {
//                    return;
//                }

//                TrialFrame.LOGGER.info("zoomFactor: " + ((ZoomPanel) mainPanel).getZoomFactor() + " | focusGained");
                if (trialRunning) {
                    debugFocusGained.add(System.currentTimeMillis());
                    mainPanel.setFocus(true);
                    mainPanel.setBorder(BORDERS.FOCUS_GAIN_BORDER);
                    setDebugInfo();
                }
            }

            public void mouseExited(MouseEvent evt) {
//                if (isTrialFinished()) {
//                    return;
//                }

//                TrialFrame.LOGGER.info("zoomFactor: " + ((ZoomPanel) mainPanel).getZoomFactor() + " | focusLost");
                if (trialRunning) {
                    debugFocusLost.add(System.currentTimeMillis());
                    mainPanel.setFocus(false);
                    mainPanel.setBorder(BORDERS.FOCUS_LOST_BORDER);
                    setDebugInfo();
                }
            }
        });

        if (isZoomButtons) {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("resources/ic_zoom_in.png")));
            JButton plus = new JButton(icon);
            plus.setBounds(mainPanel.getX() + mainPanel.getWidth() + 16, mainPanel.getWidth() / 2 + 50 - 48, 48, 48);
            plus.setFocusable(false);
            plus.setLayout(null);
            getContentPane().add(plus);
            plus.addActionListener(e -> mouseWheel(-1));

            plus.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent evt) {
//                    if (isTrialFinished()) {
//                        return;
//                    }

                    if (trialRunning) {
                        TrialFrame.LOGGER.info("focusGained");
                        debugFocusGained.add(System.currentTimeMillis());
                        mainPanel.setFocus(true);
                        mainPanel.setBorder(BORDERS.FOCUS_GAIN_BORDER);
                        setDebugInfo();
                    }
                }
            });

            icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("resources/ic_zoom_out.png")));
            JButton minus = new JButton(icon);
            minus.setBounds(plus.getX(), plus.getY() + 48, 48, 48);
            minus.setFocusable(false);
            minus.setLayout(null);
            getContentPane().add(minus);
            minus.addActionListener(e -> mouseWheel(1));

            minus.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent evt) {
//                    if (isTrialFinished()) {
//                        return;
//                    }

//                    TrialFrame.LOGGER.info("focusGained");
                    if (trialRunning) {
                        debugFocusGained.add(System.currentTimeMillis());
                        mainPanel.setFocus(true);
                        mainPanel.setBorder(BORDERS.FOCUS_GAIN_BORDER);
                        setDebugInfo();
                    }
                }
            });
        }
    }

    private void initTrials() {
        List<BaseTrial> temp = new ArrayList<>();

        for (int i = 0; i < MainFrame.NUM_ZOOM_BLOCKS; i++) {
            temp.clear();

            for (int j = 0; j < MainFrame.NUM_ZOOM_REPETITIONS; j++) {
                if (!isModeZoomIn) {
                    temp.add(new ZoomTrial("ZoomOut", 1, 35, 29));
                    temp.add(new ZoomTrial("ZoomOut", 2, 35, 23));
                    temp.add(new ZoomTrial("ZoomOut", 3, 35, 17));
                } else {
                    temp.add(new ZoomTrial("ZoomIn", 1, 27, 33));
                    temp.add(new ZoomTrial("ZoomIn", 2, 21, 33));
                    temp.add(new ZoomTrial("ZoomIn", 3, 15, 33));
                }
            }
            Collections.shuffle(temp);

            for (int j = 0; j < temp.size(); j++) {
                BaseTrial t = temp.get(j);
                t.blockId = i + 1;
                t.trialInBlock = j + 1;
            }

            trials.addAll(temp);
        }

        this.nTrials = trials.size();
    }

    @Override
    void execute(BaseTrial trial) {
        conLog.info("(execute)");
        ZoomTrial t = (ZoomTrial) trial;

        mainPanel.setFocus(false);

        ((ZoomPanel) mainPanel).startTrial(t.startLevel, t.endLevel);
        trialRunning = true;
    }

    @Override
    boolean isTrialError() {
        return (!debugError.isEmpty());
    }

    @Override
    void writeLog() {
        if (logWriter == null) return;

        ZoomTrial t = (ZoomTrial) currentTrial;
        double tolUp = (t.endLevel + 2.0) / 2 + 1;
        double tolDown = (t.endLevel - 2.0 - 0.1) / 2 + 1;
        float duration = (trialStopMoment - trialStartMoment) / 1000f;
        float reactionTime = (trialStartMoment - trialReadyMoment) / 1000f;

        List<Long> debugCanFinish = ((ZoomPanel) mainPanel).getDebugCanFinish();
//        float firstPossibleFinishTime = (debugCanFinish.get(0) - trialStartMoment) / 1000f;
//        float lastPossibleFinishTime = (debugCanFinish.get(debugCanFinish.size() - 1) - trialStartMoment) / 1000f;
        float firstPossibleFinishTime = 0F;
        float lastPossibleFinishTime = 0F;
        int possibleFinishCount = debugCanFinish.size();

        try {
            logWriter.write(caseNum + deli);
            logWriter.write(pId + deli);
            logWriter.write(task.getId() + deli);
            logWriter.write(technique.getId() + deli);
            logWriter.write(t.level + deli);
            logWriter.write(t.blockId + deli);
            logWriter.write(t.trialInBlock + deli);
            logWriter.write(t.retries + deli);
            logWriter.write(debugError.size() + deli);
            logWriter.write((isTrialError() ? "0" : "1") + deli);
            logWriter.write(String.format(Locale.US, "%.4f", duration) + deli);
            logWriter.write(String.format(Locale.US, "%.4f", reactionTime) + deli);

            logWriter.write((Math.abs(t.startLevel - t.endLevel) / 2) + deli);
            logWriter.write((t.startLevel / 2 + 1) + deli);
            logWriter.write((t.endLevel / 2 + 1) + deli);
            logWriter.write((tolDown) + deli);
            logWriter.write((tolUp) + deli);
            logWriter.write(String.format(Locale.US, "%.2f", (((ZoomPanel) mainPanel).getZoomFactor())) + deli);
            logWriter.write((((ZoomPanel) mainPanel).isFirstZoomInRightDirection() ? "1" : "0") + deli);
            logWriter.write(String.format(Locale.US, "%.4f", firstPossibleFinishTime) + deli);
            logWriter.write(String.format(Locale.US, "%.4f", lastPossibleFinishTime) + deli);
            logWriter.write(Integer.toString(possibleFinishCount));

            logWriter.newLine();
            logWriter.flush();
        } catch (IOException ignored) {
        }
    }

    @Override
    void writeLogHeader() {
        if (logWriter == null) return;

        try {
            logWriter.write("CaseNum" + deli + "PId" + deli + "Task" + deli + "Technique" + deli + "Level" + deli +
                    "Block" + deli + "TrialInBlock" + deli + "Retries" + deli + "Errors" + deli + "Ok" + deli +
                    "Duration" + deli + "ReactionTime" + deli + "Levels" + deli + "StartLevel" + deli + "EndLevel" + deli +
                    "ToleranceLevelDown" + deli + "ToleranceLevelUp" + deli + "ZoomFactor" + deli +
                    "FirstZoomCorrect" + deli + "FirstPossibleFinishTime" + deli + "LastPossibleFinishTime" + deli +
                    "PossibleFinishCount");
            logWriter.newLine();
            logWriter.flush();
        } catch (IOException ignored) {
        }
    }

    @Override
    void setCustomDebugInfo(StringBuilder builder) {
        builder.append("Zoom-Level: ").append(String.format("%.3f", ((ZoomPanel) mainPanel).getZoomFactor())).append("<br>");
        builder.append("Possible Finish: ").append(((ZoomPanel) mainPanel).getDebugCanFinish().size()).append("<br>");
    }

    private void mouseWheel(int wheelAmt) {
        MouseWheelEvent event = new MouseWheelEvent(mainPanel, MouseWheelEvent.MOUSE_WHEEL,
                System.currentTimeMillis(), 0, 0, 0, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, wheelAmt);
        mainPanel.dispatchEvent(event);
    }
}
