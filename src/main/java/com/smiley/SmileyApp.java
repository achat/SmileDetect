package com.smiley;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

public final class SmileyApp extends JFrame {
    private static final Path MODEL_PATH = Path.of("models", "emotion-ferplus-8.onnx");
    private static final Path CASCADE_PATH = Path.of("models", "haarcascade_frontalface_default.xml");

    private final FacePanel facePanel = new FacePanel();
    private final PreviewPanel previewPanel = new PreviewPanel();
    private final JLabel statusLabel = new JLabel("Camera is off", SwingConstants.LEFT);
    private final JLabel expressionLabel = new JLabel("NEUTRAL");
    private final JButton startButton = new JButton("Start camera");
    private final JButton stopButton = new JButton("Stop");
    private CameraWorker cameraWorker;

    private SmileyApp() {
        super("Smiley | Expression mirror");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(820, 600));
        setSize(980, 690);
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                stopCamera();
                dispose();
            }
        });
        setContentPane(buildContent());
        stopButton.setEnabled(false);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 22));
        root.setBackground(new Color(248, 247, 242));
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Smiley");
        title.setFont(new Font("SansSerif", Font.BOLD, 29));
        title.setForeground(new Color(43, 50, 48));
        JLabel subtitle = new JLabel("A little expression mirror");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(new Color(111, 118, 111));
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 3));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(subtitle);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(96, 105, 98));
        header.add(titles, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(1, 2, 18, 0));
        content.setOpaque(false);
        content.add(makeSection("LIVE CAMERA", previewPanel));
        JPanel faceSection = makeSection("YOUR EXPRESSION", facePanel);
        expressionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        expressionLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        expressionLabel.setForeground(new Color(43, 103, 80));
        faceSection.add(expressionLabel, BorderLayout.SOUTH);
        content.add(faceSection);
        root.add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel privacy = new JLabel("Video is analyzed on this computer and is not recorded.");
        privacy.setFont(new Font("SansSerif", Font.PLAIN, 12));
        privacy.setForeground(new Color(111, 118, 111));
        JPanel controls = new JPanel();
        controls.setOpaque(false);
        styleButton(startButton, true);
        styleButton(stopButton, false);
        controls.add(startButton);
        controls.add(stopButton);
        footer.add(privacy, BorderLayout.WEST);
        footer.add(controls, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        startButton.addActionListener(event -> startCamera());
        stopButton.addActionListener(event -> stopCamera());
        return root;
    }

    private JPanel makeSection(String heading, JPanel child) {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 226, 217)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        JLabel label = new JLabel(heading);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(new Color(124, 132, 122));
        section.add(label, BorderLayout.NORTH);
        section.add(child, BorderLayout.CENTER);
        return section;
    }

    private void styleButton(JButton button, boolean primary) {
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 18));
        button.setBackground(primary ? new Color(43, 103, 80) : new Color(238, 238, 230));
        button.setForeground(primary ? Color.WHITE : new Color(43, 50, 48));
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    private void startCamera() {
        if (cameraWorker != null && !cameraWorker.isDone()) {
            return;
        }
        startButton.setEnabled(false);
        statusLabel.setText("Starting camera...");
        cameraWorker = new CameraWorker();
        cameraWorker.execute();
    }

    // Stops the camera and cancels the camera worker if it is running. Updates the UI to reflect that the camera is off.
    private void stopCamera() {
        if (cameraWorker != null) {
            cameraWorker.cancel(true);
            cameraWorker = null;
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("Camera is off");
    }

    //cameraWorker is a SwingWorker that captures frames from the camera, analyzes them for facial expressions, and updates the UI accordingly. It also plays sounds when the user smiles or frowns.
    private final class CameraWorker extends SwingWorker<Void, FrameUpdate> {
        private boolean wasHappy;
        private boolean wasSad;
        private long lastExpressionSoundNanos;

        @Override
        protected Void doInBackground() throws Exception {
            OpenCV.loadLocally();
            VideoCapture capture = new VideoCapture();
            Mat frame = new Mat();
            try {
                if (!openCamera(capture)) {
                    publish(new FrameUpdate(null, EmotionAnalyzer.Expression.NEUTRAL,
                            "Could not open camera. Check that it is connected and available.", false));
                    return null;
                }
                if (capture.read(frame) && !frame.empty()) {
                    publish(new FrameUpdate(toBufferedImage(frame), EmotionAnalyzer.Expression.NEUTRAL,
                            "Camera connected; loading expression model...", true));
                }

                try (EmotionAnalyzer analyzer = new EmotionAnalyzer(MODEL_PATH, CASCADE_PATH)) {
                    publish(new FrameUpdate(null, EmotionAnalyzer.Expression.NEUTRAL, "Camera is live", true));
                    while (!isCancelled()) {
                        if (!capture.read(frame) || frame.empty()) {
                            publish(new FrameUpdate(null, EmotionAnalyzer.Expression.NEUTRAL,
                                    "Waiting for camera frames...", true));
                            continue;
                        }
                        EmotionAnalyzer.Analysis analysis = analyzer.analyze(frame);
                        String status = analysis.detail().equals("No face detected")
                            ? analysis.detail() : "Detected: " + analysis.detail();
                        publish(new FrameUpdate(toBufferedImage(frame), analysis.expression(), status, true));
                    }
                }
            } finally {
                frame.release();
                capture.release();
            }
            return null;
        }

        // OpenCV on Windows can fail to open the camera if the DirectShow backend is not used, so try that first.
        private boolean openCamera(VideoCapture capture) {
            if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
                if (capture.open(0, Videoio.CAP_DSHOW)) {
                    return true;
                }
                capture.release();
            }
            return capture.open(0);
        }

        @Override
        protected void process(List<FrameUpdate> updates) {
            for (FrameUpdate update : updates) {
                boolean isHappy = update.expression() == EmotionAnalyzer.Expression.HAPPY;
                boolean isSad = update.expression() == EmotionAnalyzer.Expression.SAD;
                long now = System.nanoTime();
                boolean becameExpressive = (isHappy && !wasHappy) || (isSad && !wasSad);
                if (becameExpressive && now - lastExpressionSoundNanos > 3_000_000_000L) {
                    if (isHappy) {
                        CheerSound.play();
                    } else {
                        SadSound.play();
                    }
                    lastExpressionSoundNanos = now;
                }
                wasHappy = isHappy;
                wasSad = isSad;
            }

            FrameUpdate latest = updates.get(updates.size() - 1);
            if (latest.image() != null) {
                previewPanel.setFrame(latest.image());
            }
            facePanel.setExpression(latest.expression());
            expressionLabel.setText(latest.expression().label().toUpperCase());
            statusLabel.setText(latest.status());
                stopButton.setEnabled(latest.cameraOpen());
        }

        @Override
        protected void done() {
            if (cameraWorker == this) {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                if (!isCancelled()) {
                    try {
                        get();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    } catch (java.util.concurrent.ExecutionException exception) {
                        statusLabel.setText("Camera setup failed");
                        javax.swing.JOptionPane.showMessageDialog(SmileyApp.this,
                                exception.getCause().getMessage() + "\n\nRun scripts/download-models.ps1 if model files are missing.",
                                "Smiley could not start", javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    private static BufferedImage toBufferedImage(Mat frame) {
        BufferedImage image = new BufferedImage(frame.cols(), frame.rows(), BufferedImage.TYPE_3BYTE_BGR);
        byte[] pixels = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();
        frame.get(0, 0, pixels);
        return image;
    }

    private record FrameUpdate(BufferedImage image, EmotionAnalyzer.Expression expression, String status,
                              boolean cameraOpen) { }

    private static final class PreviewPanel extends JPanel {
        private BufferedImage frame;

        PreviewPanel() {
            setBackground(new Color(32, 40, 38));
            setPreferredSize(new Dimension(360, 330));
        }

        void setFrame(BufferedImage frame) {
            this.frame = frame;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (frame == null) {
                g.setColor(new Color(208, 215, 205));
                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                String message = "Camera preview";
                int x = (getWidth() - g.getFontMetrics().stringWidth(message)) / 2;
                g.drawString(message, x, getHeight() / 2);
            } else {
                double scale = Math.min((double) getWidth() / frame.getWidth(), (double) getHeight() / frame.getHeight());
                int width = (int) (frame.getWidth() * scale);
                int height = (int) (frame.getHeight() * scale);
                Image scaled = frame.getScaledInstance(width, height, Image.SCALE_FAST);
                g.drawImage(scaled, (getWidth() - width) / 2, (getHeight() - height) / 2, null);
            }
            g.dispose();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep Swing's default look and feel if the platform theme is unavailable.
        }
        SwingUtilities.invokeLater(() -> new SmileyApp().setVisible(true));
    }
}
