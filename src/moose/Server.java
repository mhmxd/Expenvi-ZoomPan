package moose;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import util.MooseConstants;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final TaggedLogger tlog = Logger.tag(getClass().getSimpleName());

    public static final int PORT = 8000; // always the same
    private static Server instance;
    private ServerSocket serverSocket;
    private PrintWriter outPW;
    private BufferedReader inBR;
    private final ExecutorService executor;
    private final Moose moose;

    //----------------------------------------------------------------------------------------

    //-- Runnable for waiting for incoming connections
    private class ConnWaitRunnable implements Runnable {
        @Override
        public void run() {
            try {
                tlog.info("Opening socket...");
                if (serverSocket == null) {
                    tlog.info("Socket was null");
                    serverSocket = new ServerSocket(PORT);
                }
                tlog.info("Accepting connections...");
                Socket socket = serverSocket.accept();

                // Create streams
                inBR = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outPW = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                tlog.info("Ready! Listening to incoming messages...");
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
                    tlog.trace("Message: {}", message);
                    if (message != null) {
                        Memo memo = Memo.valueOf(message);

                        switch (memo.getAction()) {
                            case MooseConstants.CLICK, MooseConstants.SCROLL, MooseConstants.ZOOM -> {
                                moose.processMooseEvent(memo);
                            }
                            case MooseConstants.CONNECTION -> {
                                if (memo.getMode().equals(MooseConstants.KEEP_ALIVE)) {
                                    // Send back the message (as confirmation)
                                    send(memo);
                                }
                            }
                        }
                    } else {
                        tlog.debug("Moose Disconnected");
                        start();
                        break;
                    }
                } catch (IOException e) {
                    tlog.debug("Error in reading from Moose");
                    start();
                }
            }
        }
    }

    /**
     * Get the instance
     *
     * @return single instance
     */
    public static Server get(Moose moose) {
        if (instance == null) {
            instance = new Server(moose);
        }
        return instance;
    }

    /**
     * Constructor
     */
    public Server(Moose moose) {
        this.moose = moose;

        // Init executerService for running threads
        executor = Executors.newCachedThreadPool();
    }

    /**
     * Start the server
     */
    public void start() {
        executor.execute(new ConnWaitRunnable());
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
