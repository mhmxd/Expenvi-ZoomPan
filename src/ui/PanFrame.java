package ui;

import enums.Task;
import enums.Technique;
import listener.MooseListener;
import listener.TrialListener;
import model.BaseTrial;
import model.PanTrial;
import moose.Memo;
import moose.Moose;
import util.MooseConstants;

import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.*;

import static util.Constants.*;
import static util.StringConstants.deli;

public class PanFrame extends TrialFrame implements TrialListener, MooseListener {
    private Robot robot;
    private boolean released = true;
    private Point startPoint;

    public PanFrame(Moose moose, boolean debugOnScreen, int pId, Technique technique) {
        super(moose, debugOnScreen, pId, Task.PAN, technique, false);

        this.mainPanel = new PanPanel();

        try {
            this.robot = new Robot();
        } catch (AWTException ignored) {
        }

        initComponents();
        initTrials(MainFrame.NUM_PAN_TRIALS);
        showNextTrial();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        setTitle("Panning Panel");

        this.moose.addMooseListener(this);

        this.mainPanel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                mainPanel.setFocus(true);
                TrialFrame.LOGGER.info("focusGained");

//                if (isTrialFinished()) {
//                    return;
//                }

                if (trialRunning) {
                    debugFocusGained.add(System.currentTimeMillis());
//                    Border border = new LineBorder(BORDER_FOCUS_GAINED, BORDER_THICKNESS);
                    mainPanel.setBorder(FOCUS_GAIN_BORDER);
                    setDebugInfo();
                }
            }

            public void mouseExited(MouseEvent evt) {
                // Ignore loosing focus when trial is running
//                if (technique == Technique.MOOSE && mainPanel.isTrialRunning()) {
//                    return;
//                }

                TrialFrame.LOGGER.info("focusLost");

//                if (isTrialFinished()) {
//                    mainPanel.setFocus(false);
//                    return;
//                }

                if (!trialRunning) {
                    mainPanel.setFocus(false);
                    return;
                }

                debugFocusLost.add(System.currentTimeMillis());

                if (released) {
                    mainPanel.setBorder(FOCUS_LOST_BORDER);
                } else {
                    mainPanel.setFocus(false);
                }
                setDebugInfo();
            }

            @Override
            public void mousePressed(MouseEvent e) {
//                if (isTrialFinished()) {
//                    return;
//                }

                if (trialRunning) {
                    released = false;
                    startPoint = e.getLocationOnScreen();

                    mainPanel.setCursor(new Cursor(Cursor.MOVE_CURSOR));

                    TrialFrame.LOGGER.info("mousePressed");
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
//                if (isTrialFinished()) {
//                    return;
//                }

                if (trialRunning) {
                    released = true;
                    mainPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    if (!mainPanel.isFocus()) {
                        Border border = new LineBorder(Color.BLACK, BORDER_THICKNESS);
                        mainPanel.setBorder(border);
                        mainPanel.setFocus(false);
                    }
                    TrialFrame.LOGGER.info("mouseReleased");
                }
            }
        });

        this.mainPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
//                if (isTrialFinished()) {
//                    return;
//                }

                if (trialRunning) {
                    TrialFrame.LOGGER.info("mouseDragged");

                    Point curPoint = e.getLocationOnScreen();
                    int xDiff = curPoint.x - startPoint.x;
                    int yDiff = curPoint.y - startPoint.y;

                    startPoint = curPoint;

                    ((PanPanel) mainPanel).translate(xDiff, yDiff);
                }
            }
        });
    }

    @SuppressWarnings("SameParameterValue")
    private void initTrials(int numTrials) {
        List<BaseTrial> temp1 = new ArrayList<>();
        List<BaseTrial> temp2 = new ArrayList<>();
        List<BaseTrial> temp3 = new ArrayList<>();

        Random rand = new SecureRandom();
        int radius = 360 / numTrials;
        for (int i = 0; i < numTrials; i++) {
            int rotation = radius * i + rand.nextInt(radius);
            temp1.add(new PanTrial(1, rotation));
        }
        Collections.shuffle(temp1);

        radius = 360 / numTrials;
        for (int i = 0; i < numTrials; i++) {
            int rotation = radius * i + rand.nextInt(radius);
            temp2.add(new PanTrial(2, rotation));
        }
        Collections.shuffle(temp2);

        radius = 360 / numTrials;
        for (int i = 0; i < numTrials; i++) {
            int rotation = radius * i + rand.nextInt(radius);
            temp3.add(new PanTrial(3, rotation));
        }
        Collections.shuffle(temp3);

        List<BaseTrial> temp = new ArrayList<>();
        int sum = numTrials / 3;
        for (int i = 0; i < 3; i++) {
            temp.clear();
            for (int j = 0; j < sum; j++) {
                temp.add(temp1.remove(0));
                temp.add(temp2.remove(0));
                temp.add(temp3.remove(0));
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
        PanTrial t = (PanTrial) trial;
        ((PanPanel) mainPanel).startTrial(t.uri, t.rotation);
        trialRunning = true;
    }

    @Override
    boolean isTrialError() {
        int percent = 10;
        long duration = trialStopMoment - trialStartMoment;
        long debugTimeNoPanning = ((PanPanel) mainPanel).getDebugTimeNoPanning();
        return debugTimeNoPanning >= (duration / 100 * percent);
    }

    @Override
    void writeLog() {
        if (logWriter == null) return;

        PanTrial t = (PanTrial) currentTrial;
        float duration = (trialStopMoment - trialStartMoment) / 1000f;
        float reactionTime = (trialStartMoment - trialReadyMoment) / 1000f;
        float noPanningTime = ((PanPanel) mainPanel).getDebugTimeNoPanning() / 1000f;

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

            logWriter.write(t.rotation + deli);
            logWriter.write(String.format(Locale.US, "%.4f", noPanningTime) + deli);

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
                    "Duration" + deli + "ReactionTime" + deli + "Rotation" + deli + "NoPanningTime");
            logWriter.newLine();
            logWriter.flush();
        } catch (IOException ignored) {
        }
    }

    @Override
    void setCustomDebugInfo(StringBuilder builder) {
        if (this.currentTrial != null) {
            builder.append("<br>").append("Rotation: ").append(((PanTrial) this.currentTrial).rotation.toString()).append("<br>");
        }
    }

    @Override
    public void mooseClicked(Memo e) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (e.getMode()) {
            case MooseConstants.SINGLE -> {
                TrialFrame.LOGGER.info("Moose clicked");
                mouseClick(InputEvent.BUTTON1_DOWN_MASK);
            }
        }
    }

    @Override
    public void mooseMoved(Memo e) {
//        if (isTrialFinished()) {
//            return;
//        }

        if (trialRunning) {
            TrialFrame.LOGGER.info(e.getDebug() + " | x:" + e.getV1Int() + " | y: " + e.getV2Int());

            if (!mainPanel.isFocus()) {
                return;
            }

            ((PanPanel) mainPanel).translate(e.getV1Int(), e.getV2Int());
        }
    }

    @Override
    public void mooseWheelMoved(Memo e) {
        TrialFrame.LOGGER.info(e.getDebug() + " | Swipe");
    }

    @Override
    public void mooseZoomStart(Memo e) {
        TrialFrame.LOGGER.info(e.getDebug() + " | Zoom Start");
    }

    @SuppressWarnings("SameParameterValue")
    private void mouseClick(int button) {
        robot.mousePress(button);
        robot.mouseRelease(button);
    }
}
