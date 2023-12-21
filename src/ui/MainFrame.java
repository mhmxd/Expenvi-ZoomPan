package ui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import enums.Task;
import enums.Technique;
import moose.Moose;
import moose.Server;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

public class MainFrame extends JFrame {
    private final TaggedLogger tlog = Logger.tag(getClass().getSimpleName());

    private static final Integer PId = 200;

    public static final int NUM_ZOOM_BLOCKS = 1;
    public static final int NUM_ZOOM_REPETITIONS = 3;

    public static final int NUM_PAN_TRIALS = 6; // No number of blocks is used

    private final Moose moose;
    private BufferedImage image;
    private JComboBox<String> ipAddresses;
    private QRCodePanel qrCode;

    public MainFrame() {
        this.moose = new Moose();

        Server.get(moose).start();

        setTitle("Moose Test Suite");
        setSize(680, 440);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize components
        initComponents();

        // Initialize QR code
        initQrCode();
        tlog.info("QR loaded");
        setVisible(true);
    }

    public static void main(String[] args) {
        // Set the Look and Feel of the Swing application
        UIManager.LookAndFeelInfo info = Arrays.stream(UIManager.getInstalledLookAndFeels())
                .filter(i -> i.getName().equals("Nimbus")).findFirst().orElse(null);
        if (info != null) {
            try {
                UIManager.setLookAndFeel(info.getClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                     UnsupportedLookAndFeelException ignored) {
            }
        }

        // Create the Swing application frame
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new MainFrame();
            frame.setVisible(true);
        });
//        EventQueue.invokeLater(() -> {
//            JFrame frame = new MainFrame();
//            frame.setVisible(true);
//        });
    }

    private void initComponents() {
        tlog.info("Initializing components");
        // Create components
        JLabel pIdLabel = new JLabel("PId:", SwingConstants.RIGHT);
        JTextField pIdTextField = new JTextField(PId.toString());
        JLabel taskLabel = new JLabel("Task:", SwingConstants.RIGHT);
        JComboBox<Task> taskComboBox = new JComboBox<>(Task.values());
        JLabel techniqueLabel = new JLabel("Technique:", SwingConstants.RIGHT);
        JComboBox<Technique> techniqueComboBox = new JComboBox<>(Technique.values());
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        JLabel labelDebug = new JLabel("Debug:", SwingConstants.RIGHT);
        JCheckBox debugCheckBox = new JCheckBox();
        debugCheckBox.setSelected(true);
        tlog.info("Components initialized");
        // Set layout to null for absolute positioning
        setLayout(null);
        tlog.info("Layout set to null");
        // Set bounds for each component
        pIdLabel.setBounds(10, 50, 100, 30);
        pIdTextField.setBounds(120, 50, 130, 30);
        taskLabel.setBounds(10, 90, 100, 30);
        taskComboBox.setBounds(120, 90, 130, 30);
        techniqueLabel.setBounds(10, 130, 100, 30);
        techniqueComboBox.setBounds(120, 130, 130, 30);

        labelDebug.setBounds(10, 170, 100, 30);
        debugCheckBox.setBounds(120, 170, 100, 30);
        tlog.info("Set buttons");
        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.setBounds(50, 320, 210, 40);

        add(pIdLabel);
        add(pIdTextField);
        add(taskLabel);
        add(taskComboBox);
        add(techniqueLabel);
        add(techniqueComboBox);
        add(buttonPanel);
        add(labelDebug);
        add(debugCheckBox);

        // Set bounds for the buttons within the panel
        okButton.setBounds(0, 0, 100, 40);
        cancelButton.setBounds(110, 0, 100, 40);

        // Add action listeners to buttons
        okButton.addActionListener(e -> {
            int pId = Integer.parseInt(pIdTextField.getText());
            Task task = (Task) taskComboBox.getSelectedItem();
            Technique technique = (Technique) techniqueComboBox.getSelectedItem();
            JFrame frame;
            if (task != null) {
                boolean debug = debugCheckBox.isSelected();
                frame = switch (task) {
                    case ZOOM_OUT -> new ZoomFrame(moose, debug, pId, technique, false);
                    case ZOOM_IN -> new ZoomFrame(moose, debug, pId, technique, true);
                    case PAN -> new PanFrame(moose, debug, pId, technique);
                };
            } else {
                frame = null;
            }

            if (frame != null) {
                setVisible(false);

                EventQueue.invokeLater(() -> frame.setVisible(true));
            }
        });

        cancelButton.addActionListener(e -> {
            // Close the JFrame
            dispose();

            // Exit the application
            System.exit(0);
        });
        tlog.info("Set the location");
        // Center the JFrame on the screen
        setLocationRelativeTo(null);
    }

    private void initQrCode() {
        qrCode = new QRCodePanel();
        qrCode.setBounds(300, 50, 300, 300);
        qrCode.setLayout(null);
        getContentPane().add(qrCode);
        qrCode.setVisible(true);

        ipAddresses = new JComboBox<>();
        ipAddresses.setBounds(120, 210, 130, 30);
        for (String ipAddress : getIpAddresses()) {
            ipAddresses.addItem(ipAddress);
        }
        ipAddresses.addActionListener(e -> generateQrCode());
        getContentPane().add(ipAddresses);
        ipAddresses.setVisible(true);

        JLabel labelIpAddress = new JLabel("IP Address:", SwingConstants.RIGHT);
        labelIpAddress.setBounds(10, 210, 100, 30);
        labelIpAddress.setLayout(null);
        getContentPane().add(labelIpAddress);
        labelIpAddress.setVisible(true);

        JLabel ipPort = new JLabel();
        ipPort.setBounds(120, 250, 130, 30);
        ipPort.setText(String.valueOf(Server.PORT));
        ipPort.setLayout(null);
        getContentPane().add(ipPort);
        ipPort.setVisible(true);

        JLabel labelIpPort = new JLabel("IP Port:", SwingConstants.RIGHT);
        labelIpPort.setBounds(10, 250, 100, 30);
        labelIpPort.setLayout(null);
        getContentPane().add(labelIpPort);
        labelIpPort.setVisible(true);

        generateQrCode();
    }

    private java.util.List<String> getIpAddresses() {
        List<String> inetAddresses = new ArrayList<>();
        try {

            Enumeration<?> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();
                Enumeration<?> e2 = ni.getInetAddresses();
                while (e2.hasMoreElements()) {
                    InetAddress inetAddress = (InetAddress) e2.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.getHostAddress().contains(":")) {
                        inetAddresses.add(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return inetAddresses;
    }

    private void generateQrCode() {
        String message = ipAddresses.getSelectedItem() + "|" + Server.PORT;
        tlog.info("IP|Port: {}", message);
        try {
            int size = qrCode.getWidth();
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix matrix = qrCodeWriter.encode(message, BarcodeFormat.QR_CODE, size, size);
            image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            image.createGraphics();

            Graphics2D g2d = (Graphics2D) image.getGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, size, size);
            g2d.setColor(Color.BLACK);
            for (int i = 0; i < matrix.getHeight(); i++) {
                for (int j = 0; j < matrix.getHeight(); j++) {
                    if (matrix.get(i, j)) {
                        g2d.fillRect(i, j, 1, 1);
                    }
                }
            }

            qrCode.repaint();
        } catch (WriterException ignored) {
        }
    }

    private class QRCodePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (image != null) {
                g.drawImage(image, 0, 0, null);
            }
        }
    }
}
