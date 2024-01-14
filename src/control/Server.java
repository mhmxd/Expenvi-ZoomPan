package control;

import moose.Memo;
import moose.Moose;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.Constants.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    private static Server instance; // Singelton

    public static final int PORT = 8000; // always the same

    private ServerSocket serverSocket;
    private Socket openSocket;
    private PrintWriter outPW;
    private BufferedReader inBR;
    private final ExecutorService executor;

    private Moose moose;

    //----------------------------------------------------------------------------------------
    /**
     * Get the instance
     * @return single instance
     */
    public static Server get() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    /**
     * Constructor
     */
    private Server() {
        // Init executerService for running threads
        executor = Executors.newCachedThreadPool();
    }

    /**
     * Set the Moose
     * @param moose Moose
     */
    public void setMoose(Moose moose) {
        this.moose = moose;
    }

    //----------------------------------------------------------------------------------------

    //-- Runnable for waiting for incoming connections
    private class ConnWaitRunnable implements Runnable {
        @Override
        public void run() {
            try {
                conLog.info("Opening socket...");
                if (serverSocket == null) {
                    conLog.info("Socket was null");
                    serverSocket = new ServerSocket(PORT);
                }
                conLog.info("Accepting connections...");
                openSocket = serverSocket.accept();

                // Create streams
                inBR = new BufferedReader(new InputStreamReader(openSocket.getInputStream()));
                outPW = new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(openSocket.getOutputStream())),
                        true);
                conLog.info("Ready! Listening to incoming messages...");
                // Start receiving
                executor.execute(new InRunnable());

            } catch (IOException e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }
    }

    //-- Runnable for sending messages to Moose
    private class OutRunnable implements Runnable {
        private final Memo message;

        public OutRunnable(Memo msg) {
            this.message = msg;
        }

        @Override
        public void run() {
            if (message != null && outPW != null) {
                outPW.println(message);
                outPW.flush();
            }
        }
    }

    //-- Runnable for receiving messages from Moose
    private class InRunnable implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted() && inBR != null) {
                try {
                    String message = inBR.readLine();
                    conLog.trace("Message: {}", message);
                    if (message != null) {
                        Memo memo = Memo.valueOf(message);

                        switch (memo.getAction()) {
                            case STRINGS.CLICK, STRINGS.SCROLL, STRINGS.ZOOM -> {
                                moose.processMooseEvent(memo);
                            }
                            case STRINGS.CONNECTION -> {
                                if (memo.getMode().equals(STRINGS.KEEP_ALIVE)) {
                                    // Send back the message (as confirmation)
                                    send(memo);
                                }
                            }
                        }
                    } else {
                        conLog.trace("Moose Disconnected");
                        start();
                        break;
                    }
                } catch (IOException e) {
                    conLog.warn("Error in reading from Moose");
                    start();
                }
            }

            conLog.trace("inBR: {}", inBR);
        }
    }

    //----------------------------------------------------------------------------------------
    /**
     * Start the server
     */
    public void start() {
        executor.execute(new ConnWaitRunnable());
    }

    /**
     * Shut down the server
     */
    public void shutDown() {
        try {
            // Send end message to the Moose
            send(new Memo(STRINGS.CONNECTION, STRINGS.END, ""));

            // Close the socket, etc.
            if (serverSocket != null && openSocket != null) {
                conLog.trace("Closing the socket...");
                serverSocket.close();
                openSocket.close();
            }
            conLog.trace("Shutting down the executer...");
            if (executor != null) executor.shutdownNow();
        } catch (IOException e) {
            conLog.trace("Couldn't close the socket!");
            e.printStackTrace();
        }
    }

    /**
     * Send a Memo to the Moose
     * Called from outside
     *
     * @param msg Memo message
     */
    public void send(Memo msg) {
        executor.execute(new OutRunnable(msg));
    }
}
