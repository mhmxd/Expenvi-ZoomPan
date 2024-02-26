package ui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import enums.Task;
import enums.Technique;
import control.Server;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import tool.Constants.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class InfoPanel extends JPanel {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    private QRCodePanel qrCodePanel;
    private BufferedImage image;
    private JComboBox<String> ipAddresses;

    public InfoPanel(String pId, AbstractAction okAction) {
        // Create components
        JLabel pIdLabel = new JLabel("PId:", SwingConstants.RIGHT);
        JTextField pIdTextField = new JTextField(pId);

        JLabel taskLabel = new JLabel("Task:", SwingConstants.RIGHT);
        JComboBox<Task> taskComboBox = new JComboBox<>(Task.values());

        JLabel techniqueLabel = new JLabel("Technique:", SwingConstants.RIGHT);
        JComboBox<Technique> techniqueComboBox = new JComboBox<>(Technique.values());

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

//        JLabel labelDebug = new JLabel("Debug:", SwingConstants.RIGHT);
//        JCheckBox debugCheckBox = new JCheckBox();
//
//        debugCheckBox.setSelected(true);

        // Set layout to null for absolute positioning
        setLayout(null);

        // Set bounds for each component
        pIdLabel.setBounds(10, 50, 100, 30);
        pIdTextField.setBounds(120, 50, 130, 30);
        taskLabel.setBounds(10, 90, 100, 30);
        taskComboBox.setBounds(120, 90, 130, 30);
        techniqueLabel.setBounds(10, 130, 100, 30);
        techniqueComboBox.setBounds(120, 130, 130, 30);

//        labelDebug.setBounds(10, 170, 100, 30);
//        debugCheckBox.setBounds(120, 170, 100, 30);

        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(null);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.setBounds(30, 320, 220, 40);

        add(pIdLabel);
        add(pIdTextField);
        add(taskLabel);
        add(taskComboBox);
        add(techniqueLabel);
        add(techniqueComboBox);
        add(buttonPanel);
//        add(labelDebug);
//        add(debugCheckBox);

        // Buttons
        cancelButton.setBounds(0, 0, 80, 35);
        okButton.setBounds(120, 0, 100, 35);

        // Add action listeners to buttons
        okButton.addActionListener(e -> {

            // Set the values and call the action
            okAction.putValue(STRINGS.PID, pIdTextField.getText());
            okAction.putValue(STRINGS.TASK, taskComboBox.getSelectedItem());
            okAction.putValue(STRINGS.TECHNIQUE, techniqueComboBox.getSelectedItem());
            okAction.actionPerformed(e);
//            JFrame frame;
//
//            if (task != null) {
//                boolean debug = debugCheckBox.isSelected();
////                frame = switch (task) {
////                    case ZOOM_OUT -> new ZoomFrame(moose, debug, pId, technique, false);
////                    case ZOOM_IN -> new ZoomFrame(moose, debug, pId, technique, true);
////                    case PAN -> new PanFrame(moose, debug, pId, technique);
////                };
//            } else {
//                frame = null;
//            }
//
//            if (frame != null) {
//                setVisible(false);
//
//                EventQueue.invokeLater(() -> frame.setVisible(true));
//            }
        });

//        cancelButton.addActionListener(e -> {
//            // Close the JFrame
//            dispose();
//
//            // Exit the application
//            System.exit(0);
//        });

        initQrCode();
        // Center the JFrame on the screen
//        setLocationRelativeTo(null);
    }

    /**
     * Initialize the QR code panel
     */
    private void initQrCode() {

        qrCodePanel = new QRCodePanel();
        qrCodePanel.setBounds(320, 50, 300, 300);
        qrCodePanel.setLayout(null);
        add(qrCodePanel);
        qrCodePanel.setVisible(true);

        ipAddresses = new JComboBox<>();
        ipAddresses.setBounds(120, 210, 130, 30);
        for (String ipAddress : getIpAddresses()) {
            ipAddresses.addItem(ipAddress);
        }
        ipAddresses.addActionListener(e -> generateQrCode());
        add(ipAddresses);
        ipAddresses.setVisible(true);

        JLabel labelIpAddress = new JLabel("IP Address:", SwingConstants.RIGHT);
        labelIpAddress.setBounds(10, 210, 100, 30);
        labelIpAddress.setLayout(null);
        add(labelIpAddress);
        labelIpAddress.setVisible(true);

        JLabel ipPort = new JLabel();
        ipPort.setBounds(125, 250, 130, 30);
        ipPort.setText(String.valueOf(Server.PORT));
        ipPort.setLayout(null);
        add(ipPort);
        ipPort.setVisible(true);

        JLabel labelIpPort = new JLabel("IP Port:", SwingConstants.RIGHT);
        labelIpPort.setBounds(10, 250, 100, 30);
        labelIpPort.setLayout(null);
        add(labelIpPort);
        labelIpPort.setVisible(true);

        generateQrCode();
    }

    /**
     * Generate the QR code image
     */
    private void generateQrCode() {
        String message = ipAddresses.getSelectedItem() + "|" + Server.PORT;

        try {
            int size = qrCodePanel.getWidth();
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

            qrCodePanel.repaint();
        } catch (WriterException ignored) {
        }
    }

    /**
     * Get all the network IP addresses
     * @return String List of addresses
     */
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

    /**
     * Inner class for QR panel
     */
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
