package ui;

import enums.Task;
import enums.Technique;
import listener.TrialListener;
import model.BaseTrial;
import moose.Moose;
import org.tinylog.TaggedLogger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

import static tool.Constants.*;

public abstract class TrialFrame extends JFrame implements TrialListener {
    private final TaggedLogger conLog = org.tinylog.Logger.tag("TrialFrame");
    public static final Logger LOGGER = Logger.getLogger("DetailLog");

    protected final int pId;
    protected final Task task;
    protected final Technique technique;

    protected final Moose moose;
    protected TrialPanel mainPanel;
    protected PauseTestPanel pausePanel;
    protected JLabel mText;
    protected JButton mButton;
    protected JLabel mDebug;

//    private TrialStatus trialStatus;
    protected boolean trialRunning;

    private final int width;
    private final int height;
    protected final List<BaseTrial> trials = new ArrayList<>();
    private final SecureRandom random = new SecureRandom();
    protected int caseNum = 0;
    private int runTrials = 0;
    protected int nTrials;
    private final boolean pauseCleanScreen;
    private static final int pauseTimeInit = 10;
    private final int[] pauseTime = new int[1];

    protected long trialReadyMoment;
    protected long trialStartMoment;
    protected long trialStopMoment;

    protected final List<Long> debugFocusGained = new ArrayList<>();
    protected final List<Long> debugFocusLost = new ArrayList<>();
    protected final List<Long> debugError = new ArrayList<>();

    protected BaseTrial currentTrial;

    protected BufferedWriter logWriter = null;
    private final boolean debugOnScreen;

    public TrialFrame(Moose moose, boolean debugOnScreen, int pId, Task task, Technique technique, boolean pauseCleanScreen) {
        super();

        this.moose = moose;
        this.debugOnScreen = debugOnScreen;
        this.pId = pId;
        this.task = task;
        this.technique = technique;
        this.pauseCleanScreen = pauseCleanScreen;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.width = (int) screenSize.getWidth();
        // TODO: Temp 13 title bar
        this.height = (int) screenSize.getHeight() - 13;
    }

    protected void initComponents() {
//        Server.get(moose).start();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        setLayout(null);

        this.mainPanel.addListener(this);

        int size = Math.min(width, height) - 150;
        Border border = new LineBorder(Color.BLACK, BORDERS.BORDER_THICKNESS);

        mainPanel.setBounds((width - size) / 2, 50, size, size);
        mainPanel.setBorder(border);
        mainPanel.setBackground(COLORS.MAIN_BACKGROUND);
        mainPanel.setLayout(null);
        getContentPane().add(mainPanel);
        mainPanel.setVisible(true);

        pausePanel = new PauseTestPanel();
        pausePanel.setBounds((width - size) / 2, 50, size, size);
        pausePanel.setBorder(border);
        pausePanel.setFont(new Font(pausePanel.getFont().getFontName(), Font.PLAIN, 256));
        pausePanel.setLayout(null);
        pausePanel.setFocusable(false);
        getContentPane().add(pausePanel);
        pausePanel.setVisible(false);

        mButton = new JButton("Next");
        mButton.setFont(new Font(mButton.getFont().getFontName(), Font.PLAIN, 20));
        border = new LineBorder(Color.BLACK, 4);
        mButton.setBorder(border);
        mButton.setFocusable(false);
        mButton.setLayout(null);
        getContentPane().add(mButton);
        mButton.setVisible(false);
        mButton.addActionListener(e -> showNextTrial());

        mText = new JLabel();
        mText.setBounds(width - 200, 50, 150, 50);
        mText.setFont(new Font(mText.getFont().getFontName(), Font.PLAIN, 36));
        mText.setVerticalAlignment(JLabel.TOP);
        mText.setHorizontalAlignment(JLabel.RIGHT);
        mText.setFocusable(false);
        mText.setLayout(null);
        getContentPane().add(mText);
        mText.setVisible(true);

        mDebug = new JLabel();
        mDebug.setBounds(50, 50, 150, 300);
        mDebug.setVerticalAlignment(JLabel.TOP);
        mDebug.setLayout(null);
        getContentPane().add(mDebug, 2);
        mDebug.setVisible(false);

        String fileName = getLogFileName();
        try {
            logWriter = Files.newBufferedWriter(Path.of("log/" + fileName + ".csv"));
        } catch (IOException ignored) {
        }
        writeLogHeader();

        LogManager.getLogManager().reset();
        LOGGER.setLevel(Level.ALL);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
//        FileHandler fileHandler = null;
//        try {
//            fileHandler = new FileHandler("log/" + fileName + "_detail.txt");
//            fileHandler.setLevel(Level.ALL);
//        } catch (IOException ignored) {
//        }

        // Create a custom formatter and set it on the console handler
//        SimpleFormatter formatter = new SimpleFormatter() {
//            private static final String FORMAT = "[%1$tF] [%2$s] [%3$d-%4$d-%5$d] [%6$d] [%7$d] %8$s %n";
//
//            @Override
//            public synchronized String format(LogRecord record) {
//                long millis = record.getMillis();
//                Date date = new Date(millis);
//                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
//
//                return String.format(FORMAT,
//                        new java.util.Date(millis),
//                        sdf.format(date),
//                        currentTrial.blockId,
//                        currentTrial.trialInBlock,
//                        currentTrial.retries,
//                        millis - debugTimeReady,
//                        debugTimeStart == 0 ? 0 : millis - debugTimeStart,
//                        record.getMessage()
//                );
//            }
//        };

//        consoleHandler.setFormatter(formatter);
//        LOGGER.addHandler(consoleHandler);
//        if (fileHandler != null) {
//            fileHandler.setFormatter(formatter);
//            LOGGER.addHandler(fileHandler);
//        }
    }

    private String getLogFileName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        return String.format("%02d_%s_%s_%s", pId, task.getText(), technique.getText(), now.format(formatter));
    }

    protected void showNextTrial() {
//        conLog.info("(showNextTrial) Current Status: {}", trialStatus);
        if (!trials.isEmpty()) { // There are more trials
//            trialStatus = TrialStatus.READY;
//            conLog.info("(showNextTrial) New Status: {}", trialStatus);
            this.trialReadyMoment = System.currentTimeMillis();
            this.trialStartMoment = 0;
            this.trialStopMoment = 0;
            this.debugFocusGained.clear();
            this.debugFocusLost.clear();
            this.debugError.clear();
            mDebug.setVisible(debugOnScreen);

            mButton.setVisible(false);
            pausePanel.setVisible(false);

            setDebugInfo();

            Border border = new LineBorder(Color.BLACK, BORDERS.BORDER_THICKNESS);
            mainPanel.setBorder(border);
            mainPanel.setVisible(true);

            this.caseNum++;
            this.currentTrial = trials.remove(0);
            if (this.currentTrial.retries == 0) {
                runTrials++;
            }
            mText.setText(runTrials + " / " + nTrials);

            LOGGER.info("Trial ready / button clicked");
            execute(this.currentTrial);

            setDebugInfo();
        } else {
//            dispose();
            this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }

    abstract void execute(BaseTrial trial);

    @Override
    public void trialStart() {
//        conLog.info("(trialStart) Current Status: {}", trialStatus);
//        trialStatus = TrialStatus.RUNNING;
//        conLog.info("(trialStart) New Status: {}", trialStatus);
        this.trialStartMoment = System.currentTimeMillis();

        setDebugInfo();
        mDebug.setVisible(debugOnScreen);

//        if (!isTrialRunning()) {
//            trialStatus = TrialStatus.RUNNING;
//            conLog.info("(trialStart) New Status: {}", trialStatus);
//            this.trialStartMoment = System.currentTimeMillis();
//
//            setDebugInfo();
//            mDebug.setVisible(debugOnScreen);
//        }
    }

    @Override
    public void trialEnd() {
//        conLog.info("(trialEnd) Current Status: {}", trialStatus);
        trialStopMoment = System.currentTimeMillis();

        writeLog();

        setDebugInfo();

//        trialStatus = TrialStatus.FINISHED;
//        conLog.info("(trialEnd) New Status: {}", trialStatus);
        Border border = new LineBorder(Color.BLACK, BORDERS.BORDER_THICKNESS);
        mainPanel.setBorder(border);

        boolean trialHasError = isTrialError();
        if (trialHasError) {
            conLog.info("Error in trial");
            this.currentTrial.retries++;

            int max = (int) trials.stream().filter(t -> t.blockId == currentTrial.blockId).count() + 1;
            int r = random.nextInt(max);
            trials.add(r, currentTrial);
        }

        if (!trialHasError && (trials.stream().noneMatch(t -> t.blockId == currentTrial.blockId))) {
            conLog.info("Trial has no error");
            pauseTime[0] = pauseTimeInit;

            mainPanel.setVisible(false);
            pausePanel.setVisible(true);
            pausePanel.setText(null);
        } else {
            pauseTime[0] = 0;

            if (pauseCleanScreen) {
                mainPanel.setVisible(false);
                pausePanel.setVisible(true);
                pausePanel.setText(null);
            }
        }

        Point nextButtonPos = getNextButtonPos();

        if (pauseTime[0] > 0 && !trials.isEmpty()) {
            conLog.info("Next button");

            Runnable helloRunnable = () -> {
                pausePanel.setText(String.valueOf(pauseTime[0]));
                pauseTime[0]--;
                if (pauseTime[0] < 0) {
                    pausePanel.setFont(new Font(pausePanel.getFont().getFontName(), Font.PLAIN, 128));
                    pausePanel.setText("Pause finished.");

                    mButton.setBounds(nextButtonPos.x, nextButtonPos.y, 200, 100);
                    mButton.setVisible(true);
                }
            };

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            ScheduledFuture<?> promise = executor.scheduleAtFixedRate(
                    helloRunnable, 0,
                    1, TimeUnit.SECONDS);
            executor.schedule(() ->
                    promise.cancel(false), pauseTime[0], TimeUnit.SECONDS);
        } else {
            mButton.setBounds(nextButtonPos.x, nextButtonPos.y, 200, 100);
            mButton.setVisible(true);
        }

//        if (isTrialRunning()) {
//            trialStopMoment = System.currentTimeMillis();
//
//            writeLog();
//
//            setDebugInfo();
//
//            trialStatus = TrialStatus.FINISHED;
//            conLog.info("(trialEnd) New Status: {}", trialStatus);
//            Border border = new LineBorder(Color.BLACK, BORDER_THICKNESS);
//            mainPanel.setBorder(border);
//
//            boolean trialHasError = isTrialError();
//            if (trialHasError) {
//                conLog.info("Error in trial");
//                this.currentTrial.retries++;
//
//                int max = (int) trials.stream().filter(t -> t.blockId == currentTrial.blockId).count() + 1;
//                int r = random.nextInt(max);
//                trials.add(r, currentTrial);
//            }
//
//            if (!trialHasError && (trials.stream().noneMatch(t -> t.blockId == currentTrial.blockId))) {
//                conLog.info("Trial has no error");
//                pauseTime[0] = pauseTimeInit;
//
//                mainPanel.setVisible(false);
//                pausePanel.setVisible(true);
//                pausePanel.setText(null);
//            } else {
//                pauseTime[0] = 0;
//
//                if (pauseCleanScreen) {
//                    mainPanel.setVisible(false);
//                    pausePanel.setVisible(true);
//                    pausePanel.setText(null);
//                }
//            }
//
//            Point nextButtonPos = getNextButtonPos();
//
//            if (pauseTime[0] > 0 && !trials.isEmpty()) {
//                conLog.info("Next button");
//
//                Runnable helloRunnable = () -> {
//                    pausePanel.setText(String.valueOf(pauseTime[0]));
//                    pauseTime[0]--;
//                    if (pauseTime[0] < 0) {
//                        pausePanel.setFont(new Font(pausePanel.getFont().getFontName(), Font.PLAIN, 128));
//                        pausePanel.setText("Pause finished.");
//
//                        mButton.setBounds(nextButtonPos.x, nextButtonPos.y, 200, 100);
//                        mButton.setVisible(true);
//                    }
//                };
//
//                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
//                ScheduledFuture<?> promise = executor.scheduleAtFixedRate(
//                        helloRunnable, 0,
//                        1, TimeUnit.SECONDS);
//                executor.schedule(() ->
//                        promise.cancel(false), pauseTime[0], TimeUnit.SECONDS);
//            } else {
//                mButton.setBounds(nextButtonPos.x, nextButtonPos.y, 200, 100);
//                mButton.setVisible(true);
//            }
//        }

        if (trials.isEmpty()) {
            mButton.setText("Finished");
        }
    }

    private Point getNextButtonPos() {
        int size = Math.min(width, height) - 150;
        int buttonPos = random.nextInt(4);

        Point position = new Point();
        switch (buttonPos) {
            case 0 -> {
                position.x = (width - size) / 2 - 200 - 50;
                position.y = 50;
            }
            case 1 -> {
                position.x = width - (width - size) / 2 + 50;
                position.y = 50;
            }
            case 2 -> {
                position.x = (width - size) / 2 - 200 - 50;
                position.y = size - 100 + 50;
            }
            case 3 -> {
                position.x = width - (width - size) / 2 + 50;
                position.y = size - 100 + 50;
            }
            default -> {
                position.x = 0;
                position.y = 0;
            }
        }

        return position;
    }

    abstract boolean isTrialError();

    abstract void writeLog();

    abstract void writeLogHeader();

    @Override
    public void trialError() {
        debugError.add(System.currentTimeMillis());
        setDebugInfo();
    }


    protected void setDebugInfo() {
        if (!this.debugOnScreen) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<html><body>");
        builder.append("Block: ").append(this.currentTrial == null ? "" : this.currentTrial.blockId).append("<br>");
        builder.append("TrialInBlock: ").append(this.currentTrial == null ? "" : this.currentTrial.trialNum).append("<br>");
        builder.append("Retries: ").append(this.currentTrial == null ? "" : this.currentTrial.retries).append("<br>");
        builder.append("<br>");
        builder.append("Ready: ").append(getFormattedDate(trialReadyMoment)).append("<br>");
        builder.append("Start: ").append(getFormattedDate(trialStartMoment)).append("<br>");
        builder.append("Stop: ").append(getFormattedDate(trialStopMoment)).append("<br>");
        builder.append("FocusGained: ").append(debugFocusGained.size()).append("<br>");
        builder.append("FocusLost: ").append(debugFocusLost.size()).append("<br>");

        builder.append("<br>");
        setCustomDebugInfo(builder);

        if (trialStopMoment != 0) {
            builder.append("<br>");
            builder.append("Running: ").append(String.format("%.4f", (trialStopMoment - trialStartMoment) / 1000f)).append("<br>");
            builder.append("Error: ").append(isTrialError() ? "1" : "0").append("<br>");
        }

        builder.append("</body></html>");
        mDebug.setText(builder.toString());
    }

    abstract void setCustomDebugInfo(StringBuilder builder);


    private String getFormattedDate(long time) {
        if (time == 0) {
            return "";
        }

        Date date = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        return sdf.format(date);
    }

//    public boolean isTrialRunning() {
//        return trialStatus == TrialStatus.RUNNING;
//    }

//    public boolean isTrialFinished() {
//        return trialStatus == TrialStatus.FINISHED;
//    }
}
